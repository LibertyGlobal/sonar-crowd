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

import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.service.client.CrowdClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrowdAuthenticatorTest {

  @Test
  void authenticatorReturnsTrueForSuccessfulLogin() throws Exception {
    CrowdClient client = mock(CrowdClient.class);
    User user = mock(User.class);
    CrowdAuthenticator authenticator = new CrowdAuthenticator(client);

    when(client.authenticateUser(eq("user1"), eq("secret"))).thenReturn(user);
    assertThat(authenticator.authenticate("user1", "secret")).isTrue();

    when(client.authenticateUser(anyString(), anyString())).thenThrow(new UserNotFoundException(""));
    assertThat(authenticator.authenticate("user2", "secret")).isFalse();
  }

  @Test
  void authenticatorReturnsFalseForInvalidPassword() throws Exception {
    CrowdClient client = mock(CrowdClient.class);
    CrowdAuthenticator authenticator = new CrowdAuthenticator(client);
    when(client.authenticateUser(anyString(), anyString())).thenThrow(new InvalidAuthenticationException(""));
    assertThat(authenticator.authenticate("user1", "secret")).isFalse();
  }

}
