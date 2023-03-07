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

import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrowdConfigurationTest {

  @Test
  void crowdUrlMissing() {
    assertThatThrownBy(() -> {
      MapSettings settings = new MapSettings();
      new CrowdConfiguration(settings.asConfig()).getCrowdUrl();
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void applicationPasswordMissing() {
    assertThatThrownBy(() -> {
      MapSettings settings = new MapSettings();
      settings.setProperty(CrowdConfiguration.KEY_CROWD_URL, "http://localhost:8095");
      new CrowdConfiguration(settings.asConfig()).getCrowdApplicationPassword();
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void usesFallbackForUnsetApplicationName() {
    MapSettings settings = new MapSettings();
    settings.setProperty(CrowdConfiguration.KEY_CROWD_URL, "http://localhost:8095");
    settings.setProperty(CrowdConfiguration.KEY_CROWD_APP_PASSWORD, "secret");
    new CrowdConfiguration(settings.asConfig()).getCrowdApplicationPassword();
    CrowdConfiguration crowdConfiguration = new CrowdConfiguration(settings.asConfig());
    assertThat(crowdConfiguration.getCrowdApplicationName()).isEqualTo(CrowdConfiguration.FALLBACK_NAME);
  }

  @Test
  void createsClientProperties() {
    MapSettings settings = new MapSettings();
    settings.setProperty(CrowdConfiguration.KEY_CROWD_URL, "http://localhost:8095");
    settings.setProperty(CrowdConfiguration.KEY_CROWD_APP_NAME, "SonarQube");
    settings.setProperty(CrowdConfiguration.KEY_CROWD_APP_PASSWORD, "secret");
    CrowdConfiguration crowdConfiguration = new CrowdConfiguration(settings.asConfig());

    assertThat(crowdConfiguration.getCrowdUrl()).isEqualTo("http://localhost:8095");
    assertThat(crowdConfiguration.getCrowdApplicationName()).isEqualTo("SonarQube");
    assertThat(crowdConfiguration.getCrowdApplicationPassword()).isEqualTo("secret");
  }

}
