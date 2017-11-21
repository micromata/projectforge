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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.login.LoginDefaultHandler;
import org.projectforge.business.login.LoginResult;
import org.projectforge.business.login.LoginResultStatus;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This LDAP login handler acts as a LDAP slave, meaning, that LDAP will be accessed in read-only mode. There are 3
 * modes available: simple, users and users-groups.
 * <h4>Simple mode</h4> The simple mode is assumed if no ldap managerUser is given in the config.xml.
 * <ul>
 * <li>Simple means that only username and password is checked, all other user settings such as assigned groups and user
 * name etc. are managed by ProjectForge.</li>
 * <li>No ldap user is needed for accessing users or groups of LDAP, only the user's login-name and password is checked
 * by trying to authenticate!</li>
 * <li>If a user is deactivated in LDAP the user has the possibility to work with ProjectForge unlimited as long as he
 * uses his stay-logged-in-method! (If not acceptable please use the normal user mode instead.)</li>
 * <li>For local users any LDAP setting is ignored.</li>
 * </ul>
 * <h4>Normal users mode</h4> The normal user mode is assumed if a ldap managerUser is given in the config.xml.
 * <ul>
 * <li>Normal means that username and password is checked and all other user settings such as user name etc. are read by
 * a given ldap manager user.</li>
 * <li>If a user is deleted in LDAP the user will be marked as deleted also in ProjectForge's data-base. Any login after
 * synchronizing isn't allowed (the stay-logged-in-feature fails also for deleted users).</li>
 * <li>For local users any LDAP setting is ignored.</li>
 * <li>All known ldap user fields of the users are synchronized (given name, surname, e-mail etc.).</li>
 * </ul>
 * <h4>Users-groups mode</h4> Not yet supported. No groups will be synchronized.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class LdapSlaveLoginHandler extends LdapLoginHandler
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LdapSlaveLoginHandler.class);

  @Autowired
  PFUserDOConverter pfUserDOConverter;

  enum Mode
  {
    SIMPLE, USERS, USER_GROUPS
  }

  private Mode mode;

  private boolean refreshInProgress;

  /**
   * Only for test cases.
   *
   * @param mode
   */
  void setMode(final Mode mode)
  {
    this.mode = mode;
  }

  /**
   * @see org.projectforge.business.ldap.LdapLoginHandler#initialize()
   */
  @Override
  public void initialize()
  {
    super.initialize();
    if (StringUtils.isBlank(ldapConfig.getManagerUser()) == true) {
      mode = Mode.SIMPLE;
    } else if (StringUtils.isNotBlank(ldapConfig.getGroupBase()) == true) {
      mode = Mode.USERS;// Mode.USER_GROUPS;
      log.warn("Groups aren't yet supported by this LDAP handler.");
    } else {
      mode = Mode.USERS;
    }
    switch (mode) {
      case SIMPLE:
        log.info("LDAP slave login handler works in mode 'simple'.");
        break;
      case USERS:
        log.info("LDAP slave login handler works in mode 'users'.");
        break;
      case USER_GROUPS:
        log.info("LDAP slave login handler works in mode 'user_groups'.");
        break;
    }
  }

  /**
   * Uses the standard implementation {@link LoginDefaultHandler#checkLogin(String, String)} for local users. For all
   * other users a LDAP authentication is checked. If the LDAP authentication fails then
   * {@link LoginResultStatus#FAILED} is returned. If successful then {@link LoginResultStatus#SUCCESS} is returned with
   * the user settings of ProjectForge database. If the user doesn't yet exist in ProjectForge's data-base, it will be
   * created after and then returned.
   *
   * @see org.projectforge.business.login.LoginHandler#checkLogin(java.lang.String, java.lang.String, boolean)
   */
  @Override
  public LoginResult checkLogin(final String username, final String password)
  {
    PFUserDO user = userService.getByUsername(username);
    if (user != null && user.isLocalUser() == true) {
      return loginDefaultHandler.checkLogin(username, password);
    }
    final LoginResult loginResult = new LoginResult();
    final String organizationalUnits = ldapConfig.getUserBase();
    final LdapUser ldapUser = ldapUserDao.authenticate(username, password, organizationalUnits);
    if (ldapUser == null) {
      log.info("User login failed: " + username);
      return loginResult.setLoginResultStatus(LoginResultStatus.FAILED);
    }
    log.info("LDAP authentication was successful for: " + username);
    user = userService.getByUsername(username); // Get again (may-be the user does no exist since last call of getInternalByName(String).
    if (user == null) {
      log.info("LDAP user '" + username + "' doesn't yet exist in ProjectForge's data base. Creating new user...");
      user = pfUserDOConverter.convert(ldapUser);
      user.setId(null); // Force new id.
      if (mode == Mode.SIMPLE || ldapConfig.isStorePasswords() == false) {
        user.setNoPassword();
      } else {
        userService.createEncryptedPassword(user, password);
      }
      userService.save(user);
    } else if (mode != Mode.SIMPLE) {
      PFUserDOConverter.copyUserFields(pfUserDOConverter.convert(ldapUser), user);
      if (ldapConfig.isStorePasswords() == true) {
        userService.createEncryptedPassword(user, password);
      }
      userService.update(user);
      if (user.hasSystemAccess() == false) {
        log.info("User has no system access (is deleted/deactivated): " + user.getDisplayUsername());
        return loginResult.setLoginResultStatus(LoginResultStatus.LOGIN_EXPIRED);
      }
    }
    loginResult.setUser(user);
    if (mode == Mode.USER_GROUPS) {
      // TODO: Groups: Get groups of user.
    }
    return loginResult.setLoginResultStatus(LoginResultStatus.SUCCESS).setUser(user);
  }

  /**
   * Currently return all ProjectForge groups (done by loginDefaultHandler). Not yet implemented: Updates also any (in
   * LDAP) modified group in ProjectForge's data-base.
   *
   * @see org.projectforge.business.login.LoginHandler#getAllGroups()
   */
  @Override
  public List<GroupDO> getAllGroups()
  {
    final List<GroupDO> groups = loginDefaultHandler.getAllGroups();
    return groups;
  }

  /**
   * Updates also any (in LDAP) modified user in ProjectForge's data-base. New users will be created and ProjectForge
   * users which are not available in ProjectForge's data-base will be created.
   *
   * @see org.projectforge.business.login.LoginHandler#getAllUsers()
   */
  @Override
  public List<PFUserDO> getAllUsers()
  {
    final List<PFUserDO> users = loginDefaultHandler.getAllUsers();
    return users;
  }

  private PFUserDO getUser(final Collection<PFUserDO> col, final String username)
  {
    if (col == null || username == null) {
      return null;
    }
    for (final PFUserDO user : col) {
      if (username.equals(user.getUsername()) == true) {
        return user;
      }
    }
    return null;
  }

  /**
   * @return true for local users only, false for ldap users.
   * @see org.projectforge.business.login.LoginHandler#isPasswordChangeSupported(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean isPasswordChangeSupported(final PFUserDO user)
  {
    return user.isLocalUser();
  }

  @Override
  public boolean isWlanPasswordChangeSupported(PFUserDO user)
  {
    return false;
  }

  /**
   * Refreshes the LDAP.
   *
   * @see org.projectforge.business.login.LoginHandler#afterUserGroupCacheRefresh(java.util.List, java.util.List)
   */
  @Override
  public void afterUserGroupCacheRefresh(final Collection<PFUserDO> users, final Collection<GroupDO> groups)
  {
    if (mode == Mode.SIMPLE || refreshInProgress == true) {
      return;
    }
    new Thread()
    {
      @Override
      public void run()
      {
        synchronized (LdapSlaveLoginHandler.this) {
          if (refreshInProgress == true) {
            return;
          }
          try {
            refreshInProgress = true;
            updateLdap(users, groups);
            TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache().internalGetNumberOfUsers(); // Force refresh of UserGroupCache.
          } finally {
            refreshInProgress = false;
          }
        }
      }
    }.start();
  }

  /**
   * @return true if currently a cache refresh is running, otherwise false.
   */
  public boolean isRefreshInProgress()
  {
    return refreshInProgress;
  }

  private void updateLdap(final Collection<PFUserDO> users, final Collection<GroupDO> groups)
  {
    new LdapTemplate(ldapConnector)
    {
      @Override
      protected Object call() throws NameNotFoundException, Exception
      {
        log.info("Updating LDAP...");
        final List<LdapUser> ldapUsers = getAllLdapUsers(ctx);
        final List<PFUserDO> dbUsers = userService.loadAll();
        final List<PFUserDO> users = new ArrayList<PFUserDO>(ldapUsers.size());
        int error = 0, unmodified = 0, created = 0, updated = 0, deleted = 0, undeleted = 0, ignoredLocalUsers = 0,
            localUsers = 0;
        for (final LdapUser ldapUser : ldapUsers) {
          try {
            final PFUserDO user = pfUserDOConverter.convert(ldapUser);
            users.add(user);
            PFUserDO dbUser = getUser(dbUsers, user.getUsername());
            if (dbUser == null) {
              // Double check if added between internalLoadAll() and here:
              dbUser = userService.getByUsername(user.getUsername());
            }
            if (dbUser != null) {
              if (dbUser.isLocalUser() == true) {
                // Ignore local users.
                log.warn("Please note: the user '"
                    + dbUser.getUsername()
                    + "' is declared as local user. LDAP settings of the same LDAP user are ignored!");
                ++ignoredLocalUsers;
                continue;
              }
              PFUserDOConverter.copyUserFields(user, dbUser);
              if (dbUser.isDeleted() == true) {
                userService.undelete(dbUser);
                ++undeleted;
              }
              final ModificationStatus modificationStatus = userService.update(dbUser);
              if (modificationStatus != ModificationStatus.NONE) {
                ++updated;
              } else {
                ++unmodified;
              }
            } else {
              // New user:
              user.setId(null);
              userService.save(user);
              ++created;
            }
          } catch (final Exception ex) {
            log.error("Error while proceeding LDAP user '" + ldapUser.getUid() + "'. Continuing with next user.", ex);
            error++;
          }
        }
        for (final PFUserDO dbUser : dbUsers) {
          try {
            if (dbUser.isLocalUser() == true) {
              // Ignore local users.
              ++localUsers;
              continue;
            }
            final PFUserDO user = getUser(users, dbUser.getUsername());
            if (user == null) {
              if (dbUser.isDeleted() == false) {
                // User isn't available in LDAP, therefore mark the db user as deleted.
                userService.markAsDeleted(dbUser);
                ++deleted;
              } else {
                ++unmodified;
              }
            }
          } catch (final Exception ex) {
            log.error(
                "Error while proceeding data-base user '" + dbUser.getUsername() + "'. Continuing with next user.", ex);
            error++;
          }
        }
        log.info("Update of LDAP users: "
            + (error > 0 ? "*** " + error + " errors ***, " : "")
            + unmodified
            + " unmodified, "
            + created
            + " created, "
            + updated
            + " updated, "
            + deleted
            + " deleted, "
            + undeleted
            + " undeleted, "
            + ignoredLocalUsers
            + " ignored ldap users (local users), "
            + localUsers
            + " local users.");
        return null;
      }
    }.excecute();
  }
}
