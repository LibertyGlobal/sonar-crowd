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

import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.service.client.CrowdClient;
import org.junit.jupiter.api.Test;
import org.sonar.api.security.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrowdUsersProviderTest {

  @Test
  void returnsNullIfTheUserWasNotFound() throws Exception {
    CrowdClient client = mock(CrowdClient.class);
    when(client.getUser(anyString())).thenThrow(new UserNotFoundException(""));

    CrowdUsersProvider provider = new CrowdUsersProvider(client);
    assertThat(provider.doGetUserDetails("user")).isNull();
  }

  @Test
  void returnsTheCrowdDisplayNameAndEmailAddress() throws Exception {
    CrowdClient client = mock(CrowdClient.class);
    User user = mock(User.class);
    when(user.getDisplayName()).thenReturn("display name");
    when(user.getEmailAddress()).thenReturn("foo@acme.corp");
    when(client.getUser(anyString())).thenReturn(user);

    CrowdUsersProvider provider = new CrowdUsersProvider(client);
    UserDetails userDetails = provider.doGetUserDetails("user");
    assertThat(userDetails).isNotNull();
    assertThat(userDetails.getEmail()).isEqualTo("foo@acme.corp");
    assertThat(userDetails.getName()).isEqualTo("display name");
  }

  @Test
  void throwsSonarExceptionIfCrowdCommunicationFails() throws Exception {
    assertThatThrownBy(() -> {
      CrowdClient client = mock(CrowdClient.class);
      when(client.getUser(anyString())).thenThrow(new OperationFailedException(""));
      new CrowdUsersProvider(client).doGetUserDetails("user");
    }).isInstanceOf(IllegalArgumentException.class);
  }

}
