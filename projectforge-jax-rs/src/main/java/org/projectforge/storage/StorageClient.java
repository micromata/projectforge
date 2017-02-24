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

package org.projectforge.storage;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.configuration.ConfigurationListener;
import org.projectforge.shared.storage.StorageConstants;
import org.springframework.stereotype.Component;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class StorageClient implements ConfigurationListener
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(StorageClient.class);

  private StorageConfig config;

  private boolean initialized;

  @PostConstruct
  public void init()
  {
    checkInitialized();
    ConfigXml.getInstance().register(this);
  }

  private void checkInitialized()
  {
    synchronized (this) {
      if (initialized == true) {
        return;
      }
      this.config = ConfigXml.getInstance().getStorageConfig();
      if (this.config == null) {
        log.info("No storageConfig given in config.xml. Storage not available.");
        return;
      }
      final Client client = ClientBuilder.newClient();
      WebTarget webResource = client.target(getUrl("/initialization"))//
          .queryParam(StorageConstants.PARAM_AUTHENTICATION_TOKEN, this.config.getAuthenticationToken())//
          .queryParam(StorageConstants.PARAM_BASE_DIR, ConfigXml.getInstance().getApplicationHomeDir());
      Response response = webResource.request(MediaType.TEXT_PLAIN).get();
      if (response.getStatus() != Response.Status.OK.getStatusCode()) {
        throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
      }
      String output = null;
      if (response.getEntity() instanceof String) {
        output = (String) response.getEntity();
      }
      if ("OK".equals(output) == false) {
        throw new RuntimeException("Initialization of ProjectForge's storage failed: " + output);
      }
      webResource = client.target(getUrl("/securityCheck"));
      response = webResource.request(MediaType.TEXT_PLAIN).get();
      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        final String message = "Security alert: storage is available without any authentication!!!!!!!!!!!!!!!!";
        log.fatal(message);
        throw new RuntimeException(message);
      }
      webResource = client.target(getUrl("/securityCheck"));
      addAuthenticationHeader(webResource);
      response = webResource.request(MediaType.TEXT_PLAIN).get();
      if (response.getStatus() != Response.Status.OK.getStatusCode()) {
        throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
      }
      if (response.getEntity() instanceof String) {
        output = (String) response.getEntity();
      }
      if (output.equals("authenticated") == false) {
        final String message = "Authentication didn't work. Storage isn't available.";
        log.fatal(message);
        throw new RuntimeException(message);
      }
      initialized = true;
      log.info("Initialization of ProjectForge's storage successfully done.");
    }
  }

  private String getUrl(final String service)
  {
    String url = this.config.getUrl();
    if (url == null) {
      url = System.getProperty(StorageConstants.SYSTEM_PROPERTY_URL);
    }
    return url + "/" + service;
  }

  private void addAuthenticationHeader(final WebTarget webResource)
  {
    webResource.request().header(StorageConstants.PARAM_AUTHENTICATION_TOKEN, config.getAuthenticationToken());
  }

  /**
   * @see org.projectforge.framework.configuration.ConfigurationListener#afterRead()
   */
  @Override
  public void afterRead()
  {
    initialized = false;
    checkInitialized();
  }
}
