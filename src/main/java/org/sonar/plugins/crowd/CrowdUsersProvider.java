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

import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.service.client.CrowdClient;
import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.UserDetails;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.text.MessageFormat;

/**
 * External users provider implementation for Atlassian Crowd.
 */
public class CrowdUsersProvider extends ExternalUsersProvider {

  private static final Logger LOG = Loggers.get(CrowdUsersProvider.class);

  private final CrowdClient crowdClient;

  public CrowdUsersProvider(CrowdClient crowdClient) {
    this.crowdClient = crowdClient;
  }

  @Override
  public UserDetails doGetUserDetails(final Context context)
  {
    return doGetUserDetails(context.getUsername());
  }

  public UserDetails doGetUserDetails(String username) {
    LOG.debug("Looking up user details for user {}", username);
    try {
      User user = crowdClient.getUser(username);
      UserDetails details = new UserDetails();
      if (user.getDisplayName() != null) {
        details.setName(user.getDisplayName());
      }
      if (user.getEmailAddress() != null) {
        details.setEmail(user.getEmailAddress());
      }
      return details;
    } catch (UserNotFoundException e) {
      return null; // API contract for ExternalUsersProvider
    } catch (OperationFailedException e) {
      throw new IllegalArgumentException(MessageFormat.format("Unable to retrieve user details for user {0} from crowd", username), e);
    } catch (ApplicationPermissionException e) {
      throw new IllegalArgumentException(MessageFormat.format("Unable to retrieve user details for user {0} from crowd. The application is not permitted to perform the requested operation on the crowd server.", username), e);
    } catch (InvalidAuthenticationException e) {
      throw new IllegalArgumentException(MessageFormat.format("Unable to retrieve user details for user {0} from crowd. The application name and password are incorrect.", username), e);
    }
  }

}
