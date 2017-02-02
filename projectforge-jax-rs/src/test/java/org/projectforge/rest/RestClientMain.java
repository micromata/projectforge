/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.projectforge.ProjectForgeVersion;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.model.rest.ServerInfo;
import org.projectforge.model.rest.UserObject;

public class RestClientMain
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RestClientMain.class);

  private static String url, username, password;

  public static void main(final String[] args)
  {
    final Client client = ClientBuilder.newClient();
    final UserObject user = authenticate(client);
    initialContact(client, user);
  }

  /**
   * Adds authentication and media type json.
   *
   * @param webResource
   * @param user
   * @return ClientResponse
   */
  public static Response getClientResponse(final WebTarget webResource, final UserObject user)
  {
    return webResource.request().accept(MediaType.APPLICATION_JSON)
        .header(Authentication.AUTHENTICATION_USER_ID, user.getId().toString())
        .header(Authentication.AUTHENTICATION_TOKEN, user.getAuthenticationToken()).get();

  }

  public static UserObject authenticate(final Client client)
  {
    initialize();
    return authenticate(client, username, password);
  }

  /**
   * @return authentication token for further rest calls.
   */
  public static UserObject authenticate(final Client client, final String username, final String password)
  {
    initialize();
    // http://localhost:8080/ProjectForge/rest/authenticate/getToken // username / password
    final String url = getUrl() + RestPaths.buildPath(RestPaths.AUTHENTICATE_GET_TOKEN);
    final WebTarget webResource = client.target(url);
    final Response response = webResource.request().accept(MediaType.APPLICATION_JSON)
        .header(Authentication.AUTHENTICATION_USERNAME, username)
        .header(Authentication.AUTHENTICATION_PASSWORD, password).get();
    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      log.error("Error while trying to connect to: " + url);
      throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
    }
    final String json = (String) response.getEntity();
    log.info(json);
    final UserObject user = JsonUtils.fromJson(json, UserObject.class);
    if (user == null) {
      throw new RuntimeException("Can't deserialize user : " + json);
    }
    final Integer userId = user.getId();
    final String authenticationToken = user.getAuthenticationToken();
    log.info("userId = " + userId + ", authenticationToken=" + authenticationToken);
    return user;
  }

  public static WebTarget setConnectionSettings(final WebTarget webResource, final ConnectionSettings settings)
  {
    if (settings == null) {
      return webResource;
    }
    WebTarget res = webResource;
    if (settings.isDefaultDateTimeFormat() == false) {
      res = webResource.queryParam(ConnectionSettings.DATE_TIME_FORMAT, settings.getDateTimeFormat().toString());
    }
    return res;
  }

  public static void initialContact(final Client client, final UserObject user)
  {
    initialize();
    // http://localhost:8080/ProjectForge/rest/authenticate/initialContact?clientVersion=5.0 // userId / token
    final WebTarget webResource = client.target(getUrl() + RestPaths.buildPath(RestPaths.AUTHENTICATE_INITIAL_CONTACT)).queryParam(
        "clientVersion", ProjectForgeVersion.VERSION_STRING);
    final Response response = getClientResponse(webResource, user);
    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
    }
    final String json = (String) response.getEntity();
    log.info(json);
    final ServerInfo serverInfo = JsonUtils.fromJson(json, ServerInfo.class);
    if (serverInfo == null) {
      throw new RuntimeException("Can't deserialize serverInfo : " + json);
    }
    log.info("serverInfo=" + serverInfo);
  }

  public static String getUrl()
  {
    return url;
  }

  private static void initialize()
  {
    if (username != null) {
      // Already initialized.
      return;
    }
    final String filename = System.getProperty("user.home") + "/ProjectForge/restauthentification.properties";
    Properties prop = null;
    FileReader reader = null;
    {
      try {
        reader = new FileReader(filename);
        prop = new Properties();
        prop.load(reader);
      } catch (final FileNotFoundException ex) {
        prop = null;
      } catch (final IOException ex) {
        prop = null;
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (final IOException ex) {
            prop = null;
          }
        }
      }
    }
    if (prop != null) {
      username = prop.getProperty("user");
      if (username == null) {
        log.warn("Property 'user' not found in '" + filename + "'. Assuming 'demo'.");
      }
      password = prop.getProperty("password");
      if (password == null) {
        log.warn("Property 'password' not found in '" + filename + "'. Assuming 'demo123'.");
      }
      url = prop.getProperty("url");
      if (url == null) {
        log.warn("Property 'url' not found in '" + filename + "'. Assuming 'http://localhost:8080/ProjectForge'.");
      }
    }
    if (username == null) {
      username = "demo";
    }
    if (password == null) {
      password = "demo123";
    }
    if (url == null) {
      url = "http://localhost:8080/ProjectForge";
    }
    if (prop == null) {
      log.info("For customized url and username/password please create file '"
          + filename
          + "' with following content:\n# For rest test calls\nurl="
          + url
          + "\nuser="
          + username
          + "\npassword="
          + password
          + "\n");
    }
    log.info("Testing with user '" + username + "': " + url);
  }
}
