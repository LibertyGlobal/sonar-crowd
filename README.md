# Crowd Plugin for SonarQube
[![Build](https://github.com/LibertyGlobal/sonar-crowd/actions/workflows/ci.yml/badge.svg)](https://github.com/LibertyGlobal/sonar-crowd/actions/workflows/ci.yml) [![CodeQL](https://github.com/LibertyGlobal/sonar-crowd/actions/workflows/codeql.yml/badge.svg)](https://github.com/LibertyGlobal/sonar-crowd/actions/workflows/codeql.yml)

This plugin allows the delegation of SonarQube authentication and authorization to Atlassian Crowd.
The previous version of this plugin has been changed to provide the same functionality as the SonarQube LDAP plugin:

* Password checking against the external authentication engine.
* Automatic synchronization of usernames and emails.
* Automatic synchronization of relationships between users and groups (authorization).
* Ability to authenticate against both the external and the internal authentication systems
(for instance, technical SonarQube user accounts do not need to be defined in Crowd as there is an automatic
fallback on SonarQube engine if the user is not defined in Crowd or if the Crowd server is down).

During the first authentication trial, if the password is correct, the SonarQube database is automatically
populated with the new user. Each time a user logs into SonarQube, the username, the email and the
groups this user belongs to that are refreshed in the SonarQube database.

# Requirements

This plugin requires Atlassian Crowd 2.1.0 or later.

# Installation

Go to Administration > Marketplace > Search for "Crowd" > Install > Restart the SonarQube server

# Usage

1. Configure the crowd plugin by editing the _SONARQUBE_HOME/conf/sonar.properties_ file
1. Restart the SonarQube server and check the log file for:

    org.sonar.INFO  Security realm: Crowd
    ...
    o.s.p.c.CrowdRealm  Crowd configuration is valid, connection test successful.

1. Log into SonarQube

# Configuration

| **Property**                            | **Description**                                                                                                                                                                                                                                                                                                                     | **Default value** | **Mandatory** | **Example**                   |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|---------------|-------------------------------|
| **sonar.security.realm**                | To first try to authenticate against the external system. If the external system is not reachable or if the user is not defined in the external system, the authentication will be performed through the SonarQube internal system.                                                                                                 | -                 | Yes           | Crowd (only possible value)   |
| **sonar.security.savePassword**         | To save the user password in the SonarQube database. Then, users will be able to log into SonarQube even when the Crowd server is not reachable.	                                                                                                                                                                                   | false             | No            |                               |
| **sonar.authenticator.createUsers**     | By default, the SonarQube database is automatically populated when a new user logs into SonarQube. Setting this value to false, makes it mandatory for a System administrator to first declare a user through the SonarQube web interface before allowing this user to log into SonarQube.                                          | true              | No	           |                               |
| **sonar.security.updateUserAttributes** | If set to true, at each login, user's attributes (name and email) are re-synchronized. If set to false, user's attributes are not re-synchronized.<br/><br/>_Note that if set to false, user's attributes are synchronized just once, at the very first login._                                                                     | true              | No            |                               |
| **sonar.authenticator.downcase**        | Set to true when connecting to a Crowd server using a case-insensitive setup.                                                                                                                                                                                                                                                       | false             | No	           |                               |
| **crowd.url**                           | URL of the Crowd server. Note that if you are using https with a self certified certificate, then you should install the server certificate into the Java truststore. Since version 2.0 of the plugin the url must be the root URL of your crowd instance and not the /services/ endpoint like for previous versions of the plugin. | -                 | Yes           | https://my.company.com/crowd/ |
| **crowd.application**                   | Application name defined in Crowd to authenticate your sonar instance.                                                                                                                                                                                                                                                              | -                 | No            | sonarqube                     |
| **crowd.password**                      | Application password defined in Crowd to authenticate your sonar instance.                                                                                                                                                                                                                                                          | -                 | No            |                               |

### Example of CROWD Configuration
```
#-------------------
# SonarQube Crowd Plugin
#-------------------


# To first try to authenticate against the external sytem.
# If the external system is not reachable or if the user is not defined in the external system, the authentication will be performed through the SonarQube internal system.
sonar.security.realm=Crowd

# URL of the Crowd server.
crowd.url=https://my.company.com/crowd/


# Crowd application name.
# Default is 'sonar'.
crowd.application=sonar-prod


# Crowd application password.
crowd.password=bar


# Don't use crowd for sonar account
sonar.security.localUsers=admin,sonar
```

## Upgrades
### to SonarQube 5.0
* Only crowd plugin 2.0+ supports SonarQube 5.0+
* sonar.security.realm must be used instead of sonar.authenticator.class (deprecated since SonarQube 3.6 and removed in SonarQube 5.0)


### from Crowd plugin 1.0 to 2.0
* Crowd plugin 2.0+ uses the REST API provided by Crowd. The crowd url used in the configuration (crowd.url) must be the main URL of your crowd instance and not its /services/ end point (used with the previous SOAP integration)
* Crowd plugin 2.0+ synchronises groups from Crowd thus take care to create a group sonar-administrators in your Crowd directory and add in this group all users you'll want to use to administer SonarQube. You can also define some accounts to not synchronise with the property sonar.security.localUsers
