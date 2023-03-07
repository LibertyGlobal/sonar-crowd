/*
 * SonarQube Crowd Plugin
 * Copyright (C) 2023 Liberty Global
 * mailto:info AT libertyglobal DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.crowd;

import static com.google.common.collect.Collections2.transform;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.api.utils.SonarException;

import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.service.client.CrowdClient;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import javax.annotation.CheckForNull;

/**
 * External groups provider implementation for Atlassian Crowd. 
 */
public class CrowdGroupsProvider extends ExternalGroupsProvider {

  private static final Logger LOG = LoggerFactory.getLogger(CrowdGroupsProvider.class);
  private static final int PAGING_SIZE = 100; // no idea what a reasonable size might be
  private static final Function<Group, String> GROUP_TO_STRING = new Function<Group, String>() {
    public String apply(Group input) {
      return input.getName();
    }
  };
  private final CrowdClient crowdClient;

  public CrowdGroupsProvider(CrowdClient crowdClient) {
    this.crowdClient = crowdClient;
  }

  private Collection<String> getGroupsForUser(String username, int start, int pageSize)
    throws UserNotFoundException, OperationFailedException, InvalidAuthenticationException,
    ApplicationPermissionException {
    // Had to add that as from "not really a good idea" in
    // https://stackoverflow.com/questions/51518781/jaxb-not-available-on-tomcat-9-and-java-9-10
    ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      // This will enforce the crowClient to use the plugin classloader
      Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
      return transform(crowdClient.getGroupsForNestedUser(username, start, pageSize), GROUP_TO_STRING);
    } finally {
      // Bring back the original class loader for the thread
      Thread.currentThread().setContextClassLoader(threadClassLoader);
    }
  }

  private List<String> getGroupsForUser(String username)
    throws UserNotFoundException,
    OperationFailedException, InvalidAuthenticationException,
    ApplicationPermissionException {

    Collection<String> groups = new LinkedHashSet<String>();
    boolean mightHaveMore = true;
    int groupIndex = 0;
    Collection<String> newGroups;

    while (mightHaveMore) {
      newGroups = getGroupsForUser(username, groupIndex, PAGING_SIZE);
      if (newGroups.size() < PAGING_SIZE) {
        mightHaveMore = false;
      }
      groups.addAll(newGroups);
      groupIndex += newGroups.size();
    }
    return ImmutableList.copyOf(groups);
  }

  @Override
  public Collection<String> doGetGroups(final Context context)
  {
    return doGetGroups(context.getUsername());
  }

  public Collection<String> doGetGroups(String username) {
    LOG.debug("Looking up user groups for user {}", username);

    try {
      return getGroupsForUser(username);
    } catch (UserNotFoundException e) {
      return null; // API contract for ExternalGroupsProvider
    } catch (OperationFailedException e) {
      throw new SonarException("Unable to retrieve groups for user" + username + " from crowd.", e);
    } catch (ApplicationPermissionException e) {
      throw new SonarException("Unable to retrieve groups for user" + username
        + " from crowd. The application name and password are incorrect.", e);
    } catch (InvalidAuthenticationException e) {
      throw new SonarException(
        "Unable to retrieve groups for user" + username
          + " from crowd. The application is not permitted to perform the "
          + "requested operation on the crowd server.", e);
    }
  }
}
