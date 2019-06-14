/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.configuration.ConfigXml;

/**
 * Bean used by ConfigXML (config.xml) for configuring the storage (JackRabit repository). The storage runs on a separate web server or as
 * own war on the same web server.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class StorageConfig
{
  private String url;

  private String authenticationToken;

  /**
   * @return the url of the storage web app.
   */
  public String getUrl()
  {
    return url;
  }

  /**
   * @param url the url to set
   * @return this for chaining.
   */
  public StorageConfig setUrl(final String url)
  {
    this.url = url;
    return this;
  }

  /**
   * @return the authenticationToken which should be used for a communication between ProjectForge web app and the storage web app.
   */
  public String getAuthenticationToken()
  {
    return authenticationToken;
  }

  /**
   * @param authenticationToken the authenticationToken to set
   * @return this for chaining.
   */
  public StorageConfig setAuthenticationToken(final String authenticationToken)
  {
    this.authenticationToken = authenticationToken;
    return this;
  }

  /**
   * @see ConfigXml#toString(Object)
   */
  @Override
  public String toString()
  {
    return ConfigXml.toString(this);
  }
}
