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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@JsonIgnoreProperties(value = { "sambaNTPassword" })
public class LdapUser extends LdapPerson {
  private Integer uidNumber, gidNumber, sambaSIDNumber, sambaPrimaryGroupSIDNumber;

  private String loginShell, homeDirectory, sambaNTPassword;

  private Date sambaPwdLastSet;

  /**
   * @return The uid number of object class posixAccount.
   */
  public Integer getUidNumber() {
    return uidNumber;
  }

  public LdapUser setUidNumber(final Integer uidNumber) {
    this.uidNumber = uidNumber;
    return this;
  }

  /**
   * @return The gid number of object class posixAccount.
   */
  public Integer getGidNumber() {
    return gidNumber;
  }

  public LdapUser setGidNumber(final Integer gidNumber) {
    this.gidNumber = gidNumber;
    return this;
  }

  /**
   * @return The login shell of object class posixAccount.
   */
  public String getLoginShell() {
    return loginShell;
  }

  public LdapUser setLoginShell(final String loginShell) {
    this.loginShell = loginShell;
    return this;
  }

  /**
   * @return The home directory of object class posixAccount.
   */
  public String getHomeDirectory() {
    return homeDirectory;
  }

  public LdapUser setHomeDirectory(final String homeDirectory) {
    this.homeDirectory = homeDirectory;
    return this;
  }

  /**
   * @return the sambaSID (without prefix {@link LdapSambaAccountsConfig#getSambaSIDPrefix()}.
   */
  public Integer getSambaSIDNumber() {
    return sambaSIDNumber;
  }

  /**
   * @param sambaSIDNumber the sambaSIDNumber to set
   * @return this for chaining.
   */
  public LdapUser setSambaSIDNumber(final Integer sambaSIDNumber) {
    this.sambaSIDNumber = sambaSIDNumber;
    return this;
  }

  /**
   * @return the sambaPrimaryGroupSID
   */
  public Integer getSambaPrimaryGroupSIDNumber() {
    return sambaPrimaryGroupSIDNumber;
  }

  /**
   * @param sambaPrimaryGroupSIDNumber the sambaPrimaryGroupSID to set
   * @return this for chaining.
   */
  public LdapUser setSambaPrimaryGroupSIDNumber(final Integer sambaPrimaryGroupSIDNumber) {
    this.sambaPrimaryGroupSIDNumber = sambaPrimaryGroupSIDNumber;
    return this;
  }

  /**
   * @return the sambaNTPassword
   */
  public String getSambaNTPassword() {
    return sambaNTPassword;
  }

  /**
   * @param sambaNTPassword the sambaNTPassword to set
   * @return this for chaining.
   */
  public LdapUser setSambaNTPassword(final String sambaNTPassword) {
    this.sambaNTPassword = sambaNTPassword;
    return this;
  }

  /**
   * @return the sambaPwdLastSet
   */
  public Date getSambaPwdLastSet() {
    return sambaPwdLastSet;
  }

  /**
   * @return the sambaPwdLastSet as seconds since 1970 (Unix)
   */
  public long getSambaPwdLastSetAsUnixEpochSeconds() {
    if (sambaPwdLastSet != null) {
      return sambaPwdLastSet.getTime() / 1000;
    }
    return 0L;
  }

  /**
   * @param sambaPwdLastSet the sambaPwdLastSet to set
   * @return this for chaining.
   */
  public LdapUser setSambaPwdLastSet(final Date sambaPwdLastSet) {
    this.sambaPwdLastSet = sambaPwdLastSet;
    return this;
  }
}
