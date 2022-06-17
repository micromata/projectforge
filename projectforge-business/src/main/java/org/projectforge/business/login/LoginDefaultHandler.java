/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.security.SecurityLogging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class LoginDefaultHandler implements LoginHandler {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoginDefaultHandler.class);

  @Autowired
  private UserService userService;

  @Autowired
  private UserGroupCache userGroupCache;

  /**
   * Only needed if the data-base needs an update first (may-be the PFUserDO can't be read because of unmatching
   * tables).
   */
  @Autowired
  private DataSource dataSource;

  @Autowired
  private GroupService groupService;

  @Override
  public void initialize() {
    //Nothing to do
  }

  @Override
  public LoginResult checkLogin(final String username, final char[] password) {
    final LoginResult loginResult = new LoginResult();
    PFUserDO user = userService.authenticateUser(username, password);
    if (user != null) {
      log.info("User with valid username/password: " + username + "/****");
      if (!user.hasSystemAccess()) {
        final String msg = "User has no system access (is deleted/deactivated): " + user.getUserDisplayName();
        log.warn(msg);
        SecurityLogging.logSecurityWarn(this.getClass(), "LOGIN FAILED", msg);
        return loginResult.setLoginResultStatus(LoginResultStatus.LOGIN_EXPIRED);
      } else {
        //
        return loginResult.setLoginResultStatus(LoginResultStatus.SUCCESS).setUser(user);
      }
    } else {
      final String msg = "User login failed: " + username + "/****";
      log.warn(msg);
      SecurityLogging.logSecurityWarn(this.getClass(), "LOGIN FAILED", msg);
      return loginResult.setLoginResultStatus(LoginResultStatus.FAILED);
    }
  }

  @Override
  public boolean isAdminUser(final PFUserDO user) {
    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    String sql = "select pk from t_group where name=?";
    final int adminGroupId = jdbc.queryForObject(
        sql, new Object[]{ProjectForgeGroup.ADMIN_GROUP.getKey()}, Integer.class);
    sql = "select count(*) from t_group_user where group_id=? and user_id=?";
    final int count = jdbc.queryForObject(sql, new Object[]{adminGroupId, user.getId()}, Integer.class);
    if (count != 1) {
      final String msg = "Admin login for maintenance (data-base update) failed for user '"
          + user.getUsername()
          + "' (user not member of admin group).";
      log.warn(msg);
      SecurityLogging.logSecurityWarn(this.getClass(), "LOGIN FAILED", msg);
      return false;
    }
    return true;
  }

  /**
   * @see org.projectforge.business.login.LoginHandler#checkStayLoggedIn(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean checkStayLoggedIn(final PFUserDO user) {
    final PFUserDO dbUser = userService.internalGetById(user.getId());
    if (dbUser != null && dbUser.hasSystemAccess()) {
      return true;
    }
    log.warn("User is deleted/deactivated, stay-logged-in denied for the given user: " + user);
    return false;
  }

  /**
   * The assigned users are fetched.
   *
   * @see org.projectforge.business.login.LoginHandler#getAllGroups()
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<GroupDO> getAllGroups() {
    try {
      List<GroupDO> list = groupService.getAllGroups();
      if (list != null) {
        list = (List<GroupDO>) selectUnique(list);
      }
      return list;
    } catch (final Exception ex) {
      log.error(
          "******* Exception while getting groups from data-base (OK only in case of migration from older versions): "
              + ex.getMessage(),
          ex);
      return new ArrayList<>();
    }
  }

  /**
   * @see org.projectforge.business.login.LoginHandler#getAllUsers()
   */
  @Override
  public List<PFUserDO> getAllUsers() {
    try {
      return userService.internalLoadAll();
    } catch (final Exception ex) {
      log.error(
          "******* Exception while getting users from data-base (OK only in case of migration from older versions): "
              + ex.getMessage(),
          ex);
      return new ArrayList<>();
    }
  }

  /**
   * Do nothing.
   -*/
  @Override
  public void afterUserGroupCacheRefresh(final Collection<PFUserDO> users, final Collection<GroupDO> groups) {
  }

  protected List<?> selectUnique(final List<?> list) {
    final List<?> result = (List<?>) CollectionUtils.select(list, PredicateUtils.uniquePredicate());
    return result;
  }

  /**
   * This login handler doesn't support an external user management system.
   *
   * @return false.
   * @see org.projectforge.business.login.LoginHandler#hasExternalUsermanagementSystem()
   */
  @Override
  public boolean hasExternalUsermanagementSystem() {
    return false;
  }

  /**
   * Do nothing.
   *
   * @see org.projectforge.business.login.LoginHandler#passwordChanged(org.projectforge.framework.persistence.user.entities.PFUserDO,
   * char[])
   */
  @Override
  public void passwordChanged(final PFUserDO user, final char[] newPassword) {
    // Do nothing.
  }

  @Override
  public void wlanPasswordChanged(final PFUserDO user, final char[] newPassword) {
    // Do nothing. The wlan password input field is not visible if this handler is used.
  }

  /**
   * @return always true.
   * @see org.projectforge.business.login.LoginHandler#isPasswordChangeSupported(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean isPasswordChangeSupported(final PFUserDO user) {
    return true;
  }

  @Override
  public boolean isWlanPasswordChangeSupported(PFUserDO user) {
    return false;
  }
}
