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

import java.io.Serializable;

import org.projectforge.framework.persistence.utils.ReflectionToString;
import org.projectforge.framework.utils.NumberHelper;

/**
 * Bean used by ConfigXML (config.xml).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LdapSambaAccountsConfig implements Serializable
{
  private static final long serialVersionUID = -5861859244010004099L;

  private String sambaSIDPrefix = null;

  private Integer defaultSambaPrimaryGroupSID = null;

  private String[] objectClasses = { "sambaSamAccount", "shadowAccount", "userSecurityInformation" };

  /**
   * @return the sambaSID containing the sambaSIDPrefix followed by "-" and given uid.
   */
  public String getSambaSID(final Integer uid)
  {
    final String prefix = sambaSIDPrefix != null ? sambaSIDPrefix : "S-000-000-000";
    if (uid == null) {
      return prefix + "-???";
    }
    return prefix + "-" + uid;
  }

  /**
   * Gets the number after the last '-' character. e. g. "123-456-789-42" -> 42.
   * 
   * @param sambaSID
   * @return The extracted sambaSIDNumber of the whole sambaSID.
   */
  public Integer getSambaSIDNumber(final String sambaSID)
  {
    if (sambaSID == null) {
      return null;
    }
    final int pos = sambaSID.lastIndexOf('-');
    if (pos < 0 || sambaSID.length() <= pos + 1) {
      return null;
    }
    return NumberHelper.parseInteger(sambaSID.substring(pos + 1));
  }

  /**
   * @return the sambaSIDPrefix
   */
  public String getSambaSIDPrefix()
  {
    return sambaSIDPrefix;
  }

  /**
   * @param sambaSIDPrefix the sambaSIDPrefix to set
   * @return this for chaining.
   */
  public LdapSambaAccountsConfig setSambaSIDPrefix(final String sambaSIDPrefix)
  {
    this.sambaSIDPrefix = sambaSIDPrefix;
    return this;
  }

  /**
   * @return the sambaPrimaryGroupSID containing the sambaSIDPrefix followed by "-" and given gid number.
   */
  public String getSambaPrimaryGroupSID(final Integer gid)
  {
    if (gid == null) {
      return null;
    }
    final String prefix = sambaSIDPrefix != null ? sambaSIDPrefix : "S-000-000-000";
    return prefix + "-" + gid;
  }

  /**
   * This group SID is used for preselection of values in the {@link UserEditForm}.
   * 
   * @return the defaultSambaPrimaryGroupSID
   */
  public Integer getDefaultSambaPrimaryGroupSID()
  {
    return defaultSambaPrimaryGroupSID;
  }

  /**
   * @param defaultSambaPrimaryGroupSID the defaultSambaPrimaryGroupSID to set
   * @return this for chaining.
   */
  public LdapSambaAccountsConfig setDefaultSambaPrimaryGroupSID(final Integer defaultSambaPrimaryGroupSID)
  {
    this.defaultSambaPrimaryGroupSID = defaultSambaPrimaryGroupSID;
    return this;
  }

  /**
   * @return the objectClasses
   */
  public String[] getObjectClasses()
  {
    return objectClasses;
  }

  /**
   * @param objectClasses the objectClasses to set
   * @return this for chaining.
   */
  public LdapSambaAccountsConfig setObjectClasses(final String[] objectClasses)
  {
    this.objectClasses = objectClasses;
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
