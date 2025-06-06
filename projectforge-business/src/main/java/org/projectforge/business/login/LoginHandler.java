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

package org.projectforge.business.login;

import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Different implementations of login handling are supported.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public interface LoginHandler {
  /**
   * A login handler will be initialized by ProjectForge during start-up.
   */
  void initialize();

  /**
   * @param username
   * @param password As char array due to security reasons (don't wait for the garbage collector to remove the
   *                 password in memory).
   * @return {@link LoginResultStatus#SUCCESS} only and only if the login credentials were accepted.
   */
  LoginResult checkLogin(final String username, final char[] password);

  /**
   * The simplest implementation is: UserRights.getAccessChecker().isUserMemberOfAdminGroup(user). The default login
   * handler has an own implementation to check an user if the data-base was changed and the Hibernate objects may not
   * be valid (plain jdbc is used then).
   *
   * @param user
   * @return true if the user is an admin user of ProjectForge, otherwise false.
   */
  boolean isAdminUser(final PFUserDO user);

  /**
   * ProjectForge has checked the cookie of the user successfully. The login handler should deny the request if the user
   * e. g. is deleted.
   *
   * @param user
   * @return true if the stay logged in process should be accepted, otherwise false (the user has to be redirected to
   * the login page).
   */
  boolean checkStayLoggedIn(PFUserDO user);

  /**
   * @return All defined groups (also deleted groups).
   */
  List<GroupDO> getAllGroups();

  /**
   * @return All defined users (also deleted users).
   */
  List<PFUserDO> getAllUsers();

  /**
   * Will be called directly after updating the user group cache. The assigned users of the groups should be fetched.
   *
   * @param users
   * @param groups
   */
  void afterUserGroupCacheRefresh(Collection<PFUserDO> users, Collection<GroupDO> groups);

  /**
   * @return true, if the login handler supports an external user management system. This flag is used by
   * {User|Group}EditForm for displaying/hiding field localUser|loclaGroup.
   */
  boolean hasExternalUsermanagementSystem();

  /**
   * Will be called while changing the user's password. The access and password quality is already checked.
   *
   * @param user
   * @param newPassword
   */
  void passwordChanged(PFUserDO user, char[] newPassword);

  /**
   * Will be called while changing the user's WLAN password. The access and password quality is already checked.
   * <p>
   * Only supported by {@link org.projectforge.business.ldap.LdapMasterLoginHandler}
   *
   * @param user
   * @param newPassword
   */
  void wlanPasswordChanged(PFUserDO user, char[] newPassword);

  /**
   * If the functionality of changing passwords isn't supported for a given user then the password change functionality
   * isn't visible for the user (no such menu item is displayed).
   *
   * @param user
   * @return true if the functionality of changing password is supported by this login handler for the given user,
   * otherwise false.
   */
  boolean isPasswordChangeSupported(PFUserDO user);

  /**
   * If the functionality of changing WLAN passwords isn't supported for a given user then the WLAN password change functionality
   * isn't visible for the user (no such menu item is displayed).
   *
   * @param user
   * @return true if the functionality of changing WLAN password is supported by this login handler for the given user,
   * otherwise false.
   */
  boolean isWlanPasswordChangeSupported(PFUserDO user);

  /**
   * Should be called after checking login due to security reasons (for not having the passwords e. g. in heap dumps etc.).
   *
   * @param password The password array will be cleared (filled with '*').
   */
  static void clearPassword(char[] password) {
    Arrays.fill(password, '*'); // Clear password due to security reasons.
  }
}
