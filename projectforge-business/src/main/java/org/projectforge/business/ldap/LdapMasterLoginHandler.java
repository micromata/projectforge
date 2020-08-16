/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.login.LoginResult;
import org.projectforge.business.login.LoginResultStatus;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * TODO: nested groups.<br/>
 * This LDAP login handler has read-write access to the LDAP server and acts as master of the user and group data. All
 * changes of ProjectForge's users and groups will be written through. Any change of the LDAP server will be ignored and
 * may be overwritten by ProjectForge. <br/>
 * Use this login handler if you want to configure your LDAP users and LDAP groups via ProjectForge.<br/>
 * <h1>Passwords</h1> After each successful login-in at ProjectForge (via LoginForm) ProjectForges tries to authenticate
 * the user with the given username/password credentials at LDAP. If the LDAP authentication fails ProjectForge changes
 * the password with the actual password of the user (given in the LoginForm).
 * <h1>Deactivated users</h1> Deactivated users will be moved to an sub userbase called "deactivated". The e-mail will
 * be invalidated and the password will be deleted. Deleted and deactivated users are removed from any LDAP group. After
 * reactivating the user, the password has to be reset if the user logins the next time via LoginForm.
 * <h1>Deleted Users</h1> Deleted users will not be synchronized and removed in LDAP if exist.
 * <h1>Stay-logged-in</h1> The stay-logged-in mechanism will be ignored if the LDAP password of the user isn't set (is
 * null). Any existing LDAP password doesn't interrupt the normal stay-logged-in mechanism.
 * <h1>New users</h1> New users (created with ProjectForge's UserEditPage) will be created first without password in the
 * LDAP system directly. Such users need to log-in first at ProjectForge, otherwise their LDAP passwords aren't set (no
 * log-in at any other system connecting to the LDAP is possible until the first log-in at ProjectForge).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class LdapMasterLoginHandler extends LdapLoginHandler
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LdapMasterLoginHandler.class);

  private boolean refreshInProgress;

  @Autowired
  private GroupDOConverter groupDOConverter;

  @Autowired
  private PFUserDOConverter pfUserDOConverter;

  /**
   * @see org.projectforge.business.ldap.LdapLoginHandler#initialize()
   */
  @Override
  public void initialize()
  {
    super.initialize();
    ldapOrganizationalUnitDao.createIfNotExist(userBase, "ProjectForge's user base.");
    ldapOrganizationalUnitDao.createIfNotExist(LdapUserDao.DEACTIVATED_SUB_CONTEXT,
        "ProjectForge's user base for deactivated users.",
        userBase);
    ldapOrganizationalUnitDao.createIfNotExist(LdapUserDao.RESTRICTED_USER_SUB_CONTEXT,
        "ProjectForge's user base for restricted users.",
        userBase);
    ldapOrganizationalUnitDao.createIfNotExist(groupBase, "ProjectForge's group base.");
  }

  /**
   * @see org.projectforge.business.login.LoginHandler#checkLogin(java.lang.String, java.lang.String, boolean)
   */
  @Override
  public LoginResult checkLogin(final String username, final String password)
  {
    final LoginResult loginResult = loginDefaultHandler.checkLogin(username, password);
    if (loginResult.getLoginResultStatus() != LoginResultStatus.SUCCESS) {
      return loginResult;
    }
    try {
      // User is now logged-in successfully.
      final LdapUser authLdapUser = ldapUserDao.authenticate(username, password, userBase);
      if (authLdapUser == null) {
        final PFUserDO user = loginResult.getUser();
        final LdapUser ldapUser = pfUserDOConverter.convert(user);
        ldapUser.setOrganizationalUnit(userBase);
        log.info("User's credentials in LDAP not up-to-date: " + username + ". Updating LDAP entry...");
        ldapUserDao.createOrUpdate(userBase, ldapUser);
        ldapUserDao.changePassword(ldapUser, null, password); // update the userPassword but not the (WLAN)sambaNTPassword
      }
    } catch (final Exception ex) {
      log.error("An exception occured while checking login against LDAP system (ignoring this error): " + ex.getMessage(), ex);
    }
    return loginResult;
  }

  /**
   * @see org.projectforge.business.login.LoginHandler#getAllGroups()
   */
  @Override
  public List<GroupDO> getAllGroups()
  {
    final List<GroupDO> groups = loginDefaultHandler.getAllGroups();
    return groups;
  }

  /**
   * @see org.projectforge.business.login.LoginHandler#getAllUsers()
   */
  @Override
  public List<PFUserDO> getAllUsers()
  {
    final List<PFUserDO> users = loginDefaultHandler.getAllUsers();
    return users;
  }

  /**
   * Refreshes the LDAP.
   *
   * @see org.projectforge.business.login.LoginHandler#afterUserGroupCacheRefresh(java.util.List, java.util.List)
   */
  @Override
  public void afterUserGroupCacheRefresh(final Collection<PFUserDO> users, final Collection<GroupDO> groups)
  {
    new Thread()
    {
      @Override
      public void run()
      {
        synchronized (LdapMasterLoginHandler.this) {
          try {
            refreshInProgress = true;
            updateLdap(users, groups);
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
      protected Object call() throws Exception
      {
        log.info("Updating LDAP...");
        // First, get set of all ldap entries:
        final List<LdapUser> ldapUsers = getAllLdapUsers(ctx);
        final List<LdapUser> updatedLdapUsers = new ArrayList<>();
        int error = 0, unmodified = 0, created = 0, updated = 0, deleted = 0, renamed = 0;
        final boolean sambaConfigured = ldapConfig.getSambaAccountsConfig() != null;
        for (final PFUserDO user : users) {
          final LdapUser updatedLdapUser = pfUserDOConverter.convert(user);
          try {
            final LdapUser ldapUser = getLdapUser(ldapUsers, user);
            if (ldapUser == null) {
              updatedLdapUser.setOrganizationalUnit(userBase);
              if (!user.isDeleted() && !user.getLocalUser() && StringUtils.isNotBlank(user.getUsername())) {
                // Do not add deleted or local users.
                // TODO: if (ldapConfig.isSupportPosixAccounts() == true &&) {
                // updatedLdapUser.addObjectClass(LdapUserDao.OBJECT_CLASS_POSIX_ACCOUNT);
                // }
                ldapUserDao.create(ctx, userBase, updatedLdapUser);
                created++;
              }
            } else {
              // Need to set organizational unit for detecting the change of deactivated flag. The updateLdapUser needs the organizational
              // unit of the original ldap object:
              updatedLdapUser.setOrganizationalUnit(ldapUser.getOrganizationalUnit());
              // Otherwise the NT password will be deleted in copy function below:
              updatedLdapUser.setSambaNTPassword(ldapUser.getSambaNTPassword());
              if (user.isDeleted() || user.getLocalUser()) {
                // Deleted and local users shouldn't be synchronized with LDAP:
                ldapUserDao.delete(ctx, updatedLdapUser);
                deleted++;
              } else {
                final boolean modified = pfUserDOConverter.copyUserFields(updatedLdapUser, ldapUser);
                if (!StringUtils.equals(updatedLdapUser.getUid(), ldapUser.getUid())) {
                  // uid (dn) changed.
                  ldapUserDao.rename(ctx, updatedLdapUser, ldapUser);
                  renamed++;
                }
                if (modified) {
                  updatedLdapUser.setObjectClasses(ldapUser.getObjectClasses());
                  ldapUserDao.update(ctx, userBase, updatedLdapUser);
                  updated++;
                } else {
                  unmodified++;
                }
                boolean passwordsGiven = false;
                if (ldapUser.isPasswordGiven()) {
                  // If the user has a Samba SID then the Samba NT password mustn't be blank:
                  if (!sambaConfigured
                      || ldapUser.getSambaSIDNumber() == null
                      || StringUtils.isNotBlank(ldapUser.getSambaNTPassword())) {
                    passwordsGiven = true;
                  }
                }
                // It's ok if there is no SambaNTPassword. The user has to set it manually.
                if (passwordsGiven) {
                  if (updatedLdapUser.isDeactivated()) {
                    log.warn("User password for deactivated user is set: " + ldapUser);
                    ldapUserDao.deactivateUser(ctx, updatedLdapUser);
                  }
                }
              }
            }
            ldapUserDao.buildDn(userBase, updatedLdapUser);
            updatedLdapUsers.add(updatedLdapUser);
          } catch (final Exception ex) {
            ldapUserDao.buildDn(userBase, updatedLdapUser);
            updatedLdapUsers.add(updatedLdapUser);
            log.error("Error while proceeding user '" + user.getUsername() + "'. Continuing with next user.", ex);
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
            + renamed
            + " renamed, "
            + deleted
            + " deleted.");
        // Now get all groups:
        final List<LdapGroup> ldapGroups = getAllLdapGroups(ctx);
        final Map<Integer, LdapUser> ldapUserMap = getUserMap(updatedLdapUsers);
        error = unmodified = created = updated = renamed = deleted = 0;
        for (final GroupDO group : groups) {
          try {
            final LdapGroup updatedLdapGroup = groupDOConverter.convert(group, baseDN, ldapUserMap);
            final LdapGroup ldapGroup = getLdapGroup(ldapGroups, group);
            if (ldapGroup == null) {
              updatedLdapGroup.setOrganizationalUnit(groupBase);
              if (!group.isDeleted() && !group.getLocalGroup()) {
                // Do not add deleted or local groups.
                setMembers(updatedLdapGroup, group.getAssignedUsers(), ldapUserMap);
                ldapGroupDao.create(ctx, groupBase, updatedLdapGroup);
                created++;
              }
            } else {
              updatedLdapGroup.setOrganizationalUnit(ldapGroup.getOrganizationalUnit());
              if (group.isDeleted() || group.getLocalGroup()) {
                // Deleted and local users shouldn't be synchronized with LDAP:
                ldapGroupDao.delete(ctx, updatedLdapGroup);
                deleted++;
              } else {
                final boolean modified = groupDOConverter.copyGroupFields(updatedLdapGroup, ldapGroup);
                if (modified) {
                  updatedLdapGroup.setObjectClasses(ldapGroup.getObjectClasses());
                  setMembers(updatedLdapGroup, group.getAssignedUsers(), ldapUserMap);
                  ldapGroupDao.update(ctx, groupBase, updatedLdapGroup);
                  updated++;
                } else {
                  unmodified++;
                }
                if (!StringUtils.equals(updatedLdapGroup.getCommonName(), ldapGroup.getCommonName())) {
                  // CommonName (cn) and therefor dn changed.
                  ldapGroupDao.rename(ctx, updatedLdapGroup, ldapGroup);
                  renamed++;
                }
              }
            }
          } catch (final Exception ex) {
            log.error("Error while proceeding group '" + group.getName() + "'. Continuing with next group.", ex);
            error++;
          }
        }
        log.info("Update of LDAP groups: "
            + (error > 0 ? "*** " + error + " errors ***, " : "")
            + unmodified
            + " unmodified, "
            + created
            + " created, "
            + updated
            + " updated, "
            + renamed
            + " renamed, "
            + deleted
            + " deleted.");
        log.info("LDAP update done.");
        return null;
      }
    }.excecute();
  }

  /**
   * @see org.projectforge.business.login.LoginHandler#passwordChanged(org.projectforge.framework.persistence.user.entities.PFUserDO,
   * java.lang.String)
   */
  @Override
  public void passwordChanged(final PFUserDO user, final String newPassword)
  {
    final LdapUser ldapUser = ldapUserDao.findById(user.getId());
    if (user.isDeleted() || user.getLocalUser()) {
      // Don't change passwords of such users.
      return;
    }
    if (ldapUser != null) {
      ldapUserDao.changePassword(ldapUser, null, newPassword);
      final LdapUser authenticatedUser = ldapUserDao.authenticate(user.getUsername(), newPassword);
      log.info("Password changed successfully for : " + authenticatedUser);
    } else {
      log.error("Can't change LDAP password for user '" + user.getUsername() + "'! Not such user found in LDAP!.");
    }
  }

  @Override
  public void wlanPasswordChanged(final PFUserDO user, final String newPassword)
  {
    final LdapUser ldapUser = ldapUserDao.findById(user.getId());
    if (user.isDeleted() || user.getLocalUser()) {
      // Don't change passwords of such users.
      return;
    }
    if (ldapUser != null) {
      ldapUserDao.changeWlanPassword(ldapUser, newPassword);
      log.info("WLAN Password changed successfully for : " + ldapUser);
    } else {
      log.error("Can't change LDAP WLAN password for user '" + user.getUsername() + "'! Not such user found in LDAP!.");
    }
  }

  /**
   * @return always true because the change of passwords is supported for every user.
   * @see org.projectforge.business.login.LoginHandler#isPasswordChangeSupported(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean isPasswordChangeSupported(final PFUserDO user)
  {
    return true;
  }

  @Override
  public boolean isWlanPasswordChangeSupported(PFUserDO user)
  {
    return true;
  }

  /**
   * @param updatedLdapGroup
   * @param assignedUsers
   * @param ldapUserMap
   */
  private void setMembers(final LdapGroup updatedLdapGroup, final Set<PFUserDO> assignedUsers,
      final Map<Integer, LdapUser> ldapUserMap)
  {
    updatedLdapGroup.clearMembers();
    if (assignedUsers == null) {
      // No user to assign.
      return;
    }
    for (final PFUserDO assignedUser : assignedUsers) {
      final LdapUser ldapUser = ldapUserMap.get(assignedUser.getId());
      if (ldapUser == null) {
        final PFUserDO cachedUser = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache()
            .getUser(assignedUser.getId());
        if (cachedUser == null || !cachedUser.isDeleted()) {
          log.warn("Can't assign ldap user to group: "
              + updatedLdapGroup.getCommonName()
              + "! Ldap user with id '"
              + assignedUser.getId()
              + "' not found, skipping user.");
        }
      } else {
        if (assignedUser.hasSystemAccess()) {
          // Do not add deleted or deactivated users.
          updatedLdapGroup.addMember(ldapUser, baseDN);
        }
      }
    }
  }

  private Map<Integer, LdapUser> getUserMap(final Collection<LdapUser> users)
  {
    final Map<Integer, LdapUser> map = new HashMap<>();
    if (users == null) {
      return map;
    }
    for (final LdapUser user : users) {
      final Integer id = PFUserDOConverter.getId(user);
      if (id != null) {
        map.put(id, user);
      } else {
        log.warn("Given ldap user has no id (employee number), ignoring user for group assignments: " + user);
      }
    }
    return map;
  }

  private LdapUser getLdapUser(final List<LdapUser> ldapUsers, final PFUserDO user)
  {
    for (final LdapUser ldapUser : ldapUsers) {
      if (StringUtils.equals(ldapUser.getUid(), user.getUsername())
          || StringUtils.equals(ldapUser.getEmployeeNumber(), PFUserDOConverter.buildEmployeeNumber(user))) {
        return ldapUser;
      }
    }
    return null;
  }

  private LdapGroup getLdapGroup(final List<LdapGroup> ldapGroups, final GroupDO group)
  {
    for (final LdapGroup ldapGroup : ldapGroups) {
      if (StringUtils.equals(ldapGroup.getBusinessCategory(), groupDOConverter.buildBusinessCategory(group))) {
        return ldapGroup;
      }
    }
    return null;
  }
}
