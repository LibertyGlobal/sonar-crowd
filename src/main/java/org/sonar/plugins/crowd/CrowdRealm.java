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
import com.atlassian.crowd.integration.rest.service.DefaultHttpClientProvider;
import com.atlassian.crowd.integration.rest.service.JacksonBasicAuthRestExecutor;
import com.atlassian.crowd.integration.rest.service.RestCrowdClient;
import com.atlassian.crowd.service.client.ClientProperties;
import com.atlassian.crowd.service.client.ClientPropertiesImpl;
import com.atlassian.crowd.service.client.CrowdClient;
import org.sonar.api.security.Authenticator;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.Properties;

/**
 * Sonar security realm for Atlassian Crowd.
 */
public class CrowdRealm extends SecurityRealm {

  private static final Logger LOG = Loggers.get(CrowdRealm.class);

  private final CrowdConfiguration configuration;

  private CrowdClient client;
  private CrowdAuthenticator authenticator;
  private CrowdUsersProvider usersProvider;
  private CrowdGroupsProvider groupsProvider;

  public CrowdRealm(CrowdConfiguration crowdConfiguration) {
    this.configuration = crowdConfiguration;
  }

  private CrowdClient createCrowdClient() {
    Properties crowdProperties = new Properties();
    // The name that the application will use when authenticating with the Crowd server.
    crowdProperties.setProperty("application.name", configuration.getCrowdApplicationName());
    // The password that the application will use when authenticating with the Crowd server.
    crowdProperties.setProperty("application.password", configuration.getCrowdApplicationPassword());
    // Crowd will redirect the user to this URL if their authentication token expires or is invalid due to security restrictions.
    // crowdProperties.setProperty("application.login.url", "");
    // The URL to use when connecting with the integration libraries to communicate with the Crowd server.
    // crowdProperties.setProperty("crowd.server.url", "");
    // The URL used by Crowd to create the full URL to be sent to users that reset their passwords.
    crowdProperties.setProperty("crowd.base.url", configuration.getCrowdUrl());
    // The session key to use when storing a Boolean value indicating whether the user is authenticated or not.
    crowdProperties.setProperty("session.isauthenticated", "session.isauthenticated");
    // The session key to use when storing a String value of the user's authentication token.
    crowdProperties.setProperty("session.tokenkey", "session.tokenkey");
    // The number of minutes to cache authentication validation in the session. If this value is set to 0, each HTTP request will be
    // authenticated with the Crowd server.
    crowdProperties.setProperty("session.validationinterval", "1");
    // The session key to use when storing a Date value of the user's last authentication.
    crowdProperties.setProperty("session.lastvalidation", "session.lastvalidation");
    // Perhaps more things to let users to configure in the future
    // (see https://confluence.atlassian.com/display/CROWD/The+crowd.properties+file)
    ClientProperties clientProperties = ClientPropertiesImpl.newInstanceFromProperties(crowdProperties);
    return new RestCrowdClient(JacksonBasicAuthRestExecutor.createFrom(clientProperties, new DefaultHttpClientProvider().getClient(clientProperties)));
  }

  @Override
  public String getName() {
    return "Crowd";
  }

  @Override
  public void init() {
    this.client = createCrowdClient();
    this.authenticator = new CrowdAuthenticator(client);
    this.usersProvider = new CrowdUsersProvider(client);
    this.groupsProvider = new CrowdGroupsProvider(client);

    try {
      client.testConnection();
      LOG.info("Crowd configuration is valid, connection test successful.");
    } catch (OperationFailedException e) {
      throw new IllegalStateException("Unable to test connection to crowd", e);
    } catch (InvalidAuthenticationException e) {
      throw new IllegalStateException("Application name and password are incorrect", e);
    } catch (ApplicationPermissionException e) {
      throw new IllegalStateException("The application is not permitted to perform the requested operation on the crowd server", e);
    }
  }

  @Override
  public Authenticator doGetAuthenticator() {
    return authenticator;
  }

  @Override
  public ExternalGroupsProvider getGroupsProvider() {
    return groupsProvider;
  }

  @Override
  public ExternalUsersProvider getUsersProvider() {
    return usersProvider;
  }

}
