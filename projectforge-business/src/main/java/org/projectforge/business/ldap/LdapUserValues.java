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

package org.projectforge.business.ldap;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;

/**
 * Bean used for serialization and deserialization of the ldap values as xml string in {@link PFUserDO#getLdapValues()} ConfigXML
 * (config.xml).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XmlObject(alias = "values")
public class LdapUserValues implements Serializable
{
  private static final long serialVersionUID = 8313125939747632963L;

  private Integer uidNumber = null;

  private Integer gidNumber = null;

  @XmlField(asAttribute = true)
  private String homeDirectory;

  @XmlField(asAttribute = true)
  private String loginShell;

  private Integer sambaSIDNumber;

  private Integer sambaPrimaryGroupSIDNumber;

  public boolean isValuesEmpty()
  {
    return isPosixValuesEmpty() == true && isSambaValuesEmpty() == true;
  }

  public boolean isPosixValuesEmpty()
  {
    return getUidNumber() == null
        && StringUtils.isBlank(getHomeDirectory()) == true
        && StringUtils.isBlank(getLoginShell()) == true
        && getGidNumber() == null;
  }

  public boolean isSambaValuesEmpty()
  {
    return getSambaSIDNumber() == null && sambaPrimaryGroupSIDNumber == null;
  }

  public String getHomeDirectory()
  {
    return homeDirectory;
  }

  public LdapUserValues setHomeDirectory(final String homeDirectory)
  {
    this.homeDirectory = homeDirectory;
    return this;
  }

  public Integer getUidNumber()
  {
    return uidNumber;
  }

  public LdapUserValues setUidNumber(final Integer uidNumber)
  {
    this.uidNumber = uidNumber;
    return this;
  }

  public Integer getGidNumber()
  {
    return gidNumber;
  }

  public LdapUserValues setGidNumber(final Integer gidNumber)
  {
    this.gidNumber = gidNumber;
    return this;
  }

  public String getLoginShell()
  {
    return loginShell;
  }

  public LdapUserValues setLoginShell(final String loginShell)
  {
    this.loginShell = loginShell;
    return this;
  }

  /**
   * @return the sambaSID without {@link LdapSambaAccountsConfig#getSambaSIDPrefix()}.
   */
  public Integer getSambaSIDNumber()
  {
    return sambaSIDNumber;
  }

  /**
   * @param sambaSIDNumber the sambaSID to set
   * @return this for chaining.
   */
  public LdapUserValues setSambaSIDNumber(final Integer sambaSIDNumber)
  {
    this.sambaSIDNumber = sambaSIDNumber;
    return this;
  }

  /**
   * @return the sambaPrimaryGroupSIDNumber
   */
  public Integer getSambaPrimaryGroupSIDNumber()
  {
    return sambaPrimaryGroupSIDNumber;
  }

  /**
   * @param sambaPrimaryGroupSIDNumber the sambaPrimaryGroupSIDNumber to set
   * @return this for chaining.
   */
  public LdapUserValues setSambaPrimaryGroupSIDNumber(final Integer sambaPrimaryGroupSIDNumber)
  {
    this.sambaPrimaryGroupSIDNumber = sambaPrimaryGroupSIDNumber;
    return this;
  }
}
