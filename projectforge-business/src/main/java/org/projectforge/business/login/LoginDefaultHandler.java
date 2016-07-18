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

package org.projectforge.business.login;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;

@Service
public class LoginDefaultHandler implements LoginHandler
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LoginDefaultHandler.class);

  @Autowired
  private UserService userService;

  /**
   * Only needed if the data-base needs an update first (may-be the PFUserDO can't be read because of unmatching
   * tables).
   */
  @Autowired
  private DataSource dataSource;

  @Autowired
  private GroupService groupService;

  /**
   * @see org.projectforge.business.login.LoginHandler#initialize(org.projectforge.registry.Registry)
   */
  @Override
  public void initialize()
  {
    //Nothing to do
  }

  /**
   * @see org.projectforge.business.login.LoginHandler#checkLogin(java.lang.String, java.lang.String, boolean)
   */
  @Override
  public LoginResult checkLogin(final String username, final String password)
  {
    final LoginResult loginResult = new LoginResult();
    PFUserDO user = null;
    if (UserFilter.isUpdateRequiredFirst() == true) {
      // Only administrator login is allowed. The login is checked without Hibernate because the data-base schema may be out-dated thus
      // Hibernate isn't functioning.
      try {
        final PFUserDO resUser = getUserWithJdbc(username, password);
        if (resUser == null || resUser.getUsername() == null) {
          log.info("Admin login for maintenance (data-base update) failed for user '" + username
              + "' (user/password not found).");
          return loginResult.setLoginResultStatus(LoginResultStatus.FAILED);
        }
        if (isAdminUser(resUser) == false) {
          return loginResult.setLoginResultStatus(LoginResultStatus.ADMIN_LOGIN_REQUIRED);
        }
        TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache().internalSetAdminUser(resUser); // User is now marked as admin user.
        return loginResult.setLoginResultStatus(LoginResultStatus.SUCCESS).setUser(resUser);
      } catch (final Exception ex) {
        log.error(ex.getMessage(), ex);
      }
    } else {
      user = userService.authenticateUser(username, password);
    }
    if (user != null) {
      log.info("User with valid username/password: " + username + "/****");
      if (user.hasSystemAccess() == false) {
        log.info("User has no system access (is deleted/deactivated): " + user.getDisplayUsername());
        return loginResult.setLoginResultStatus(LoginResultStatus.LOGIN_EXPIRED);
      } else {
        return loginResult.setLoginResultStatus(LoginResultStatus.SUCCESS).setUser(user);
      }
    } else {
      log.info("User login failed: " + username + "/****");
      return loginResult.setLoginResultStatus(LoginResultStatus.FAILED);
    }
  }

  /**
   * Only administrator login is allowed. The login is checked without Hibernate because the data-base schema may be
   * out-dated thus Hibernate isn't functioning.
   * 
   * @param jdbc
   * @param username
   * @param password
   * @return
   * @throws SQLException
   */
  private PFUserDO getUserWithJdbc(final String username, final String password) throws SQLException
  {
    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    String sql = "select pk, firstname, lastname, password, password_salt from t_pf_user where username=? and deleted=false and deactivated=false and restricted_user=false";
    PFUserDO user = null;
    try {
      user = loadUser(jdbc, sql, username, true);
    } catch (final Exception ex) {
      log.warn("This SQLException is only OK if you've a ProjectForge installation 5.2 or minor!");
      sql = "select pk, firstname, lastname, password from t_pf_user where username=? and deleted=false";
      user = loadUser(jdbc, sql, username, false);
    }
    if (user == null) {
      return null;
    }
    final PasswordCheckResult passwordCheckResult = userService.checkPassword(user, password);
    if (passwordCheckResult.isOK() == false) {
      log.warn("Login for admin user '" + username + "' in maintenance mode failed, wrong password.");
      return null;
    }
    return user;
  }

  /**
   * @param user
   * @param rs
   * @param username
   * @param withSaltString false before ProjectForge version 5.3.
   * @throws SQLException
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private PFUserDO loadUser(final JdbcTemplate jdbc, final String sql, final String username,
      final boolean withSaltString)
      throws SQLException
  {
    final PFUserDO user = (PFUserDO) jdbc.query(sql, new Object[] { username }, new ResultSetExtractor()
    {
      @Override
      public Object extractData(final ResultSet rs) throws SQLException, DataAccessException
      {
        if (rs.next() == true) {
          final PFUserDO user = new PFUserDO();
          user.setUsername(username);
          final String password = rs.getString("password");
          final int pk = rs.getInt("pk");
          final String firstname = rs.getString("firstname");
          final String lastname = rs.getString("lastname");
          if (withSaltString == true) {
            final String saltString = rs.getString("password_salt");
            user.setPasswordSalt(saltString);
          }
          user.setId(pk);
          user.setUsername(username).setFirstname(firstname).setLastname(lastname).setPassword(password);
          return user;
        }
        return null;
      }
    });
    return user;
  }

  public boolean isAdminUser(final PFUserDO user)
  {
    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    String sql = "select pk from t_group where name=?";
    final int adminGroupId = jdbc.queryForObject(
        sql, new Object[] { ProjectForgeGroup.ADMIN_GROUP.getKey() }, Integer.class);
    sql = "select count(*) from t_group_user where group_id=? and user_id=?";
    final int count = jdbc.queryForObject(sql, new Object[] { adminGroupId, user.getId() }, Integer.class);
    if (count != 1) {
      log.info("Admin login for maintenance (data-base update) failed for user '"
          + user.getUsername()
          + "' (user not member of admin group).");
      return false;
    }
    return true;
  }

  /**
   * @see org.projectforge.business.login.LoginHandler#checkStayLoggedIn(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean checkStayLoggedIn(final PFUserDO user)
  {
    final PFUserDO dbUser = userService.getById(user.getId());
    if (dbUser != null && dbUser.hasSystemAccess() == true) {
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
  public List<GroupDO> getAllGroups()
  {
    try {
      List<GroupDO> list = groupService.getAllGroups();
      if (list != null) {
        list = (List<GroupDO>) selectUnique(list);
      }
      return list;
    } catch (final Exception ex) {
      //Needed for migration, when tenant table not available.
      log.fatal(
          "******* Exception while getting groups from data-base (OK only in case of migration from older versions): "
              + ex.getMessage(),
          ex);
      return new ArrayList<GroupDO>();
    }
  }

  /**
   * @see org.projectforge.business.login.LoginHandler#getAllUsers()
   */
  @Override
  public List<PFUserDO> getAllUsers()
  {
    try {
      return userService.loadAll();
    } catch (final Exception ex) {
      //Needed for migration, when tenant table not available.
      log.fatal(
          "******* Exception while getting users from data-base (OK only in case of migration from older versions): "
              + ex.getMessage(),
          ex);
      return new ArrayList<PFUserDO>();
    }
  }

  /**
   * Do nothing.
   * 
   * @see org.projectforge.business.login.LoginHandler#afterUserGroupCacheRefresh(java.util.List, java.util.List)
   */
  @Override
  public void afterUserGroupCacheRefresh(final Collection<PFUserDO> users, final Collection<GroupDO> groups)
  {
  }

  protected List<?> selectUnique(final List<?> list)
  {
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
  public boolean hasExternalUsermanagementSystem()
  {
    return false;
  }

  /**
   * Do nothing.
   * 
   * @see org.projectforge.business.login.LoginHandler#passwordChanged(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.String)
   */
  @Override
  public void passwordChanged(final PFUserDO user, final String newPassword)
  {
    // Do nothing.
  }

  /**
   * @see org.projectforge.business.login.LoginHandler#isPasswordChangeSupported(org.projectforge.framework.persistence.user.entities.PFUserDO)
   * @return always true.
   */
  @Override
  public boolean isPasswordChangeSupported(final PFUserDO user)
  {
    return true;
  }
}
