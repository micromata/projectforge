/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.ListHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.xmlstream.XmlObjectReader;
import org.projectforge.framework.xmlstream.XmlObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class PFUserDOConverter
{
  static final String ID_PREFIX = "pf-id-";

  @Autowired
  LdapUserDao ldapUserDao;

  @Autowired
  LdapService ldapService;

  public static Long getId(final LdapUser user)
  {
    final String employeeNumber = user.getEmployeeNumber();
    if (employeeNumber != null && employeeNumber.startsWith(ID_PREFIX)
        && employeeNumber.length() > ID_PREFIX.length()) {
      final String id = employeeNumber.substring(ID_PREFIX.length());
      return NumberHelper.parseLong(id);
    }
    return null;
  }

  public PFUserDO convert(final LdapUser ldapUser)
  {
    final PFUserDO user = new PFUserDO();
    user.setLastname(ldapUser.getSurname());
    user.setFirstname(ldapUser.getGivenName());
    user.setUsername(ldapUser.getUid());
    user.setId(getId(ldapUser));
    user.setOrganization(ldapUser.getOrganization());
    user.setDescription(ldapUser.getDescription());
    user.setLastWlanPasswordChange(ldapUser.getSambaPwdLastSet());
    final String[] mails = ldapUser.getMail();
    if (mails != null) {
      for (final String mail : mails) {
        if (StringUtils.isNotEmpty(mail)) {
          user.setEmail(mail);
          break;
        }
      }
    }
    if (ldapUser.isDeleted()) {
      user.setDeleted(true);
    }
    if (ldapUser.isDeactivated() || ldapUserDao.isDeactivated(ldapUser)) {
      user.setDeactivated(true);
    }
    if (ldapUser.isRestrictedUser() || ldapUserDao.isRestrictedUser(ldapUser)) {
      user.setRestrictedUser(true);
    }
    if (!isPosixAccountValuesEmpty(ldapUser)) {
      user.setLdapValues(getLdapValuesAsXml(ldapUser));
    }
    return user;
  }

  public LdapUser convert(final PFUserDO user)
  {
    final LdapUser ldapUser = new LdapUser();
    ldapUser.setSurname(user.getLastname());
    ldapUser.setGivenName(user.getFirstname());
    ldapUser.setUid(user.getUsername());
    if (user.getId() != null) {
      ldapUser.setEmployeeNumber(buildEmployeeNumber(user));
    }
    ldapUser.setOrganization(user.getOrganization());
    ldapUser.setDescription(user.getDescription());
    ldapUser.setMail(user.getEmail());
    ldapUser.setDeleted(user.getDeleted());
    ldapUser.setDeactivated(user.getDeactivated());
    ldapUser.setMobilePhoneNumber(user.getMobilePhone());
    if (user.getDeactivated()) {
      ldapUser.setMail(LdapUserDao.DEACTIVATED_MAIL);
    }
    ldapUser.setRestrictedUser(user.getRestrictedUser());
    setLdapValues(ldapUser, user.getLdapValues());
    ldapUser.setSambaPwdLastSet(user.getLastWlanPasswordChange() != null ? user.getLastWlanPasswordChange() : user.getCreated());
    return ldapUser;
  }

  public static boolean isPosixAccountValuesEmpty(final LdapUser ldapUser)
  {
    return ldapUser.getUidNumber() == null
        && StringUtils.isBlank(ldapUser.getHomeDirectory())
        && StringUtils.isBlank(ldapUser.getLoginShell())
        && ldapUser.getGidNumber() == null;
  }

  public static boolean isSambaAccountValuesEmpty(final LdapUser ldapUser)
  {
    return ldapUser.getSambaSIDNumber() == null && ldapUser.getSambaPrimaryGroupSIDNumber() == null;
  }

  /**
   * Sets the LDAP values such as posix account properties of the given ldapUser configured in the given xml string.
   *
   * @param ldapUser
   * @param ldapValuesAsXml Posix account values as xml.
   */
  public void setLdapValues(final LdapUser ldapUser, final String ldapValuesAsXml)
  {
    if (StringUtils.isBlank(ldapValuesAsXml)) {
      return;
    }
    final LdapConfig ldapConfig = ldapService.getLdapConfig();
    final LdapPosixAccountsConfig posixAccountsConfig = ldapConfig != null ? ldapConfig.getPosixAccountsConfig() : null;
    if (posixAccountsConfig == null) {
      // No posix account default values configured
      return;
    }
    final LdapUserValues values = readLdapUserValues(ldapValuesAsXml);
    if (values == null) {
      return;
    }
    if (values.getUidNumber() != null) {
      ldapUser.setUidNumber(values.getUidNumber());
    } else {
      ldapUser.setUidNumber(-1);
    }
    if (values.getGidNumber() != null) {
      ldapUser.setGidNumber(values.getGidNumber());
    } else {
      ldapUser.setGidNumber(posixAccountsConfig.getDefaultGidNumber());
    }
    if (StringUtils.isNotBlank(values.getHomeDirectory())) {
      ldapUser.setHomeDirectory(values.getHomeDirectory());
    } else {
      ldapUser.setHomeDirectory(posixAccountsConfig.getHomeDirectoryPrefix() + ldapUser.getUid());
    }
    if (StringUtils.isNotBlank(values.getLoginShell())) {
      ldapUser.setLoginShell(values.getLoginShell());
    } else {
      ldapUser.setLoginShell(posixAccountsConfig.getDefaultLoginShell());
    }
    if (values.getSambaSIDNumber() != null) {
      ldapUser.setSambaSIDNumber(values.getSambaSIDNumber());
    } else {
      ldapUser.setSambaSIDNumber(null);
    }
    if (values.getSambaPrimaryGroupSIDNumber() != null) {
      ldapUser.setSambaPrimaryGroupSIDNumber(values.getSambaPrimaryGroupSIDNumber());
    } else {
      ldapUser.setSambaPrimaryGroupSIDNumber(null);
    }
  }

  public static LdapUserValues readLdapUserValues(final String ldapValuesAsXml)
  {
    if (StringUtils.isBlank(ldapValuesAsXml)) {
      return null;
    }
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(LdapUserValues.class);
    final LdapUserValues values = (LdapUserValues) reader.read(ldapValuesAsXml);
    return values;
  }

  /**
   * Exports the LDAP values such as posix account properties of the given ldapUser as xml string.
   *
   * @param ldapUser
   */
  public String getLdapValuesAsXml(final LdapUser ldapUser)
  {
    final LdapConfig ldapConfig = ldapService.getLdapConfig();
    final LdapPosixAccountsConfig posixAccountsConfig = ldapConfig != null ? ldapConfig.getPosixAccountsConfig() : null;
    final LdapSambaAccountsConfig sambaAccountsConfig = ldapConfig != null ? ldapConfig.getSambaAccountsConfig() : null;
    LdapUserValues values = null;
    if (posixAccountsConfig != null) {
      values = new LdapUserValues();
      if (ldapUser.getUidNumber() != null) {
        values.setUidNumber(ldapUser.getUidNumber());
      }
      if (ldapUser.getGidNumber() != null) {
        values.setGidNumber(ldapUser.getGidNumber());
      }
      values.setHomeDirectory(ldapUser.getHomeDirectory());
      values.setLoginShell(ldapUser.getLoginShell());
    }
    if (sambaAccountsConfig != null) {
      if (values == null) {
        values = new LdapUserValues();
      }
      if (ldapUser.getSambaSIDNumber() != null) {
        values.setSambaSIDNumber(ldapUser.getSambaSIDNumber());
      }
      if (ldapUser.getSambaPrimaryGroupSIDNumber() != null) {
        values.setSambaPrimaryGroupSIDNumber(ldapUser.getSambaPrimaryGroupSIDNumber());
      }
    }
    return getLdapValuesAsXml(values);
  }

  /**
   * Exports the LDAP values such as posix account properties of the given ldapUser as xml string.
   *
   */
  public static String getLdapValuesAsXml(final LdapUserValues values)
  {
    final XmlObjectReader reader = new XmlObjectReader();
    reader.initialize(LdapUserValues.class);
    final String xml = XmlObjectWriter.writeAsXml(values);
    return xml;
  }

  public static String buildEmployeeNumber(final PFUserDO user)
  {
    return ID_PREFIX + user.getId();
  }

  /**
   * Copies the fields shared with ldap.
   *
   * @param src
   * @param dest
   * @return true if any modification is detected, otherwise false.
   */
  public static boolean copyUserFields(final PFUserDO src, final PFUserDO dest)
  {
    final boolean modified = BeanHelper.copyProperties(src, dest, true, "username", "firstname", "lastname", "email",
        "description",
        "organization", "deactivated", "restrictedUser");
    return modified;
  }

  /**
   * Copies the fields. The field commonName is also copied because the dn is built from the uid, ou and dc. The cn
   * isn't part of the dn.
   *
   * @param src
   * @param dest
   * @return true if any modification is detected, otherwise false.
   */
  public boolean copyUserFields(final LdapUser src, final LdapUser dest)
  {
    setMailNullArray(src);
    setMailNullArray(dest);
    setMobilePhoneNullArray(src);
    setMobilePhoneNullArray(dest);
    normalizeStringFields(src);
    normalizeStringFields(dest);
    boolean modified;
    final List<String> properties = new LinkedList<>();
    ListHelper.addAll(properties, "commonName", "givenName", "surname", "mail", "description", "organization",
        "deactivated", "restrictedUser", "mobilePhoneNumber", "telephoneNumber", "homePhoneNumber", "uid");
    if (ldapUserDao.isPosixAccountsConfigured() && !isPosixAccountValuesEmpty(src)) {
      ListHelper.addAll(properties, "uidNumber", "gidNumber", "homeDirectory", "loginShell");
    }
    if (ldapUserDao.isSambaAccountsConfigured() && !isSambaAccountValuesEmpty(src)) {
      ListHelper.addAll(properties, "sambaSIDNumber", "sambaPrimaryGroupSIDNumber", "sambaNTPassword");
    }
    modified = BeanHelper.copyProperties(src, dest, true, properties.toArray(new String[0]));
    if (ldapUserDao.isSambaAccountsConfigured() && !isSambaAccountValuesEmpty(src)) {
      final long diffSambaPwdLastSet = dest.getSambaPwdLastSetAsUnixEpochSeconds()
          - src.getSambaPwdLastSetAsUnixEpochSeconds();
      if (diffSambaPwdLastSet > 10 || diffSambaPwdLastSet < -10) {
        // Difference is more then 10 seconds:
        modified = true;
      }
    }
    return modified;
  }

  static void setMailNullArray(final LdapUser ldapUser)
  {
    if (ldapUser.getMail() == null) {
      return;
    }
    for (final String mail : ldapUser.getMail()) {
      if (StringUtils.isNotEmpty(mail)) {
        return;
      }
    }
    // All array entries are null or empty strings, therefore set the mail value itself to null:
    ldapUser.setMail((String[]) null);
  }

  static void setMobilePhoneNullArray(final LdapUser ldapUser)
  {
    if (ldapUser.getMobilePhoneNumber() == null) {
      return;
    }
    for (final String mobilePhone : ldapUser.getMobilePhoneNumber()) {
      if (StringUtils.isNotEmpty(mobilePhone)) {
        return;
      }
    }
    // All array entries are null or empty strings, therefore set the mobilePhoneNumber value itself to null:
    ldapUser.setMobilePhoneNumber((String[]) null);
  }

  /**
   * Normalizes empty strings to null for single-value string fields to avoid endless sync loops.
   * This ensures that empty strings in the database are treated the same as null values in LDAP.
   *
   * @param ldapUser The user whose fields should be normalized
   */
  static void normalizeStringFields(final LdapUser ldapUser)
  {
    if (ldapUser == null) {
      return;
    }
    // Normalize description
    if (StringUtils.isEmpty(ldapUser.getDescription())) {
      ldapUser.setDescription(null);
    }
    // Normalize telephoneNumber
    if (StringUtils.isEmpty(ldapUser.getTelephoneNumber())) {
      ldapUser.setTelephoneNumber(null);
    }
    // Normalize homePhoneNumber
    if (StringUtils.isEmpty(ldapUser.getHomePhoneNumber())) {
      ldapUser.setHomePhoneNumber(null);
    }
  }
}
