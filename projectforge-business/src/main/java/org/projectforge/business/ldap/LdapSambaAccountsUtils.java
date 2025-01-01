/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class LdapSambaAccountsUtils {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LdapSambaAccountsUtils.class);

  @Autowired
  private LdapService ldapService;

  @Autowired
  private UserGroupCache userGroupCache;

  /**
   * Get all given uid numbers of all ProjectForge users including any deleted user and get the next highest and free
   * number. The number is 1000 if no uid number (with a value greater than 999) is found.
   */
  public int getNextFreeSambaSIDNumber() {
    final Collection<PFUserDO> allUsers = userGroupCache.getAllUsers();
    int currentMaxNumber = 999;
    for (final PFUserDO user : allUsers) {
      final LdapUserValues ldapUserValues = PFUserDOConverter.readLdapUserValues(user.getLdapValues());
      if (ldapUserValues == null) {
        continue;
      }
      if (ldapUserValues.getSambaSIDNumber() != null
          && ldapUserValues.getSambaSIDNumber() > currentMaxNumber) {
        currentMaxNumber = ldapUserValues.getUidNumber();
      }
    }
    return currentMaxNumber + 1;
  }

  /**
   * For preventing double uidNumbers.
   *
   * @param currentUser
   * @param sambaSIDNumber
   * @return Returns true if any user (also deleted user) other than the given user has the given uidNumber, otherwise
   * false.
   */
  public boolean isGivenNumberFree(final PFUserDO currentUser, final Integer sambaSIDNumber) {
    return isGivenNumberFree(currentUser.getId(), sambaSIDNumber);
  }

  /**
   * For preventing double uidNumbers.
   *
   * @param currentUserId
   * @param sambaSIDNumber
   * @return Returns true if any user (also deleted user) other than the given user has the given uidNumber, otherwise
   * false.
   */
  public boolean isGivenNumberFree(final Long currentUserId, final Integer sambaSIDNumber) {
    if (sambaSIDNumber == null) {
      // Nothing to check.
      return true;
    }
    final Collection<PFUserDO> allUsers = userGroupCache.getAllUsers();
    for (final PFUserDO user : allUsers) {
      final LdapUserValues ldapUserValues = PFUserDOConverter.readLdapUserValues(user.getLdapValues());
      if (Objects.equals(user.getId(), currentUserId)) {
        // The current user may have the given sambaSIDNumber already, so ignore this entry.
        continue;
      }
      if (ldapUserValues != null
          && ldapUserValues.getSambaSIDNumber() != null
          && ldapUserValues.getSambaSIDNumber().intValue() == sambaSIDNumber) {
        // Number isn't free.
        log.info("The getSambaSIDNumber (samba account) '" + sambaSIDNumber + "' is already occupied by user: " + user);
        return false;
      }
    }
    return true;
  }

  /**
   * Sets next free SambaSID or, if free and given the same id as the posix UID.
   *
   * @param ldapUserValues
   * @param user
   */
  public void setDefaultValues(final LdapUserValues ldapUserValues, final PFUserDO user) {
    setDefaultValues(ldapUserValues, user.getId());
  }

  /**
   * Sets next free SambaSID or, if free and given the same id as the posix UID.
   *
   * @param ldapUserValues
   * @param userId
   */
  public void setDefaultValues(final LdapUserValues ldapUserValues, final Long userId) {
    final LdapConfig ldapConfig = ldapService.getLdapConfig();
    LdapSambaAccountsConfig ldapSambaAccountsConfig = ldapConfig != null ? ldapConfig.getSambaAccountsConfig() : null;
    if (ldapSambaAccountsConfig == null) {
      ldapSambaAccountsConfig = new LdapSambaAccountsConfig();
    }
    if (ldapUserValues.getUidNumber() != null && isGivenNumberFree(userId, ldapUserValues.getUidNumber())) {
      ldapUserValues.setSambaSIDNumber(ldapUserValues.getUidNumber());
    } else {
      ldapUserValues.setSambaSIDNumber(getNextFreeSambaSIDNumber());
    }
    if (ldapSambaAccountsConfig.getDefaultSambaPrimaryGroupSID() != null) {
      ldapUserValues.setSambaPrimaryGroupSIDNumber(ldapSambaAccountsConfig.getDefaultSambaPrimaryGroupSID());
    }
  }
}
