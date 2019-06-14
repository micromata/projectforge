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

package org.projectforge.business.ldap;

import org.projectforge.framework.persistence.utils.ReflectionToString;

/**
 * Bean used by ConfigXML (config.xml).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LdapPosixAccountsConfig
{
  private String homeDirectoryPrefix = "/home/";

  private int defaultGidNumber = -1;

  private String defaultLoginShell = "/bin/bash";

  /**
   * The home default directory is built out of this prefix followed by the uid (username). Default is "/home/".
   * 
   * @return
   */
  public String getHomeDirectoryPrefix()
  {
    return homeDirectoryPrefix;
  }

  public LdapPosixAccountsConfig setHomeDirectoryPrefix(final String homeDirectoryPrefix)
  {
    this.homeDirectoryPrefix = homeDirectoryPrefix;
    return this;
  }

  /**
   * @return The default gid of users.
   */
  public int getDefaultGidNumber()
  {
    return defaultGidNumber;
  }

  public LdapPosixAccountsConfig setDefaultGidNumber(final int defaultGidNumber)
  {
    this.defaultGidNumber = defaultGidNumber;
    return this;
  }

  /**
   * The default login shell is "/bin/bash" at default.
   */
  public String getDefaultLoginShell()
  {
    return defaultLoginShell;
  }

  public LdapPosixAccountsConfig setDefaultLoginShell(final String defaultLoginShell)
  {
    this.defaultLoginShell = defaultLoginShell;
    return this;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return ReflectionToString.toString(this);
  }
}
