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

package org.projectforge.business.user;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.login.Login;
import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * The group user relations will be cached with this class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class UserGroupCache extends AbstractCache {
  public static UserGroupCache getTenantInstance() {
    return TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
  }

  private static final long serialVersionUID = -6501106088529363341L;

  private static Logger log = LoggerFactory.getLogger(UserGroupCache.class);

  private final TenantDO tenant;

  /**
   * The key is the user id and the value is a list of assigned groups.
   */
  private Map<Integer, Set<Integer>> userGroupIdMap;

  /**
   * The key is the group id.
   */
  private Map<Integer, GroupDO> groupMap;

  /**
   * List of all rights (value) defined for the user ids (key).
   */
  private Map<Integer, List<UserRightDO>> rightMap;

  private Map<Integer, PFUserDO> userMap;

  private Set<Integer> adminUsers;

  private Set<Integer> financeUsers;

  private Set<Integer> controllingUsers;

  private Set<Integer> projectManagers;

  private Set<Integer> projectAssistants;

  private Set<Integer> marketingUsers;

  private Set<Integer> orgaUsers;

  private Set<Integer> hrUsers;

  private TenantChecker tenantChecker;

  private TenantService tenantService;

  private UserRightService userRights;

  private UserRightDao userRightDao;

  public UserGroupCache(final TenantDO tenant, ApplicationContext applicationContext) {
    setExpireTimeInHours(1);
    this.tenant = tenant;
    this.tenantChecker = applicationContext.getBean(TenantChecker.class);
    this.userRights = applicationContext.getBean(UserRightService.class);
    this.userRightDao = applicationContext.getBean(UserRightDao.class);
    this.tenantService = applicationContext.getBean(TenantService.class);
  }

  private static Set<Integer> ensureAndGetUserGroupIdMap(final Map<Integer, Set<Integer>> ugIdMap, final Integer userId) {
    Set<Integer> set = ugIdMap.get(userId);
    if (set == null) {
      set = new HashSet<>();
      ugIdMap.put(userId, set);
    }
    return set;
  }

  public GroupDO getGroup(final ProjectForgeGroup group) {
    checkRefresh();
    for (final GroupDO g : groupMap.values()) {
      if (group.equals(g.getName())) {
        return g;
      }
    }
    return null;
  }

  public GroupDO getGroup(final Integer groupId) {
    checkRefresh();
    return groupMap.get(groupId);
  }

  public PFUserDO getUser(final Integer userId) {
    if (userId == null) {
      return null;
    }
    // checkRefresh(); Done by getUserMap().
    return getUserMap() != null ? userMap.get(userId) : null; // Only null in maintenance mode (if t_user isn't readable).
  }

  public PFUserDO getUser(final String username) {
    if (StringUtils.isEmpty(username)) {
      return null;
    }
    for (final PFUserDO user : getUserMap().values()) {
      if (username.equals(user.getUsername())) {
        return user;
      }
    }
    return null;
  }

  public PFUserDO getUserByFullname(final String fullname) {
    if (StringUtils.isEmpty(fullname)) {
      return null;
    }
    for (final PFUserDO user : getUserMap().values()) {
      if (fullname.equals(user.getFullname())) {
        return user;
      }
    }
    return null;
  }

  /**
   * @return all users (also deleted users).
   */
  public Collection<PFUserDO> getAllUsers() {
    // checkRefresh(); Done by getUserMap().
    return getUserMap().values();
  }

  /**
   * @return all groups (also deleted groups).
   */
  public Collection<GroupDO> getAllGroups() {
    // checkRefresh(); Done by getGMap().
    return getGroupMap().values();
  }

  /**
   * Only for internal use.
   */
  public int internalGetNumberOfUsers() {
    if (userMap == null) {
      return 0;
    } else {
      // checkRefresh(); Done by getUserMap().
      return getUserMap().size();
    }
  }

  public String getUsername(final Integer userId) {
    // checkRefresh(); Done by getUserMap().
    final PFUserDO user = getUserMap().get(userId);
    if (user == null) {
      return String.valueOf(userId);
    }
    return user.getUsername();
  }

  /**
   * Check for current logged in user.
   */
  public boolean isLoggedInUserMemberOfGroup(final Integer groupId) {
    return isUserMemberOfGroup(ThreadLocalUserContext.getUserId(), groupId);
  }

  public boolean isUserMemberOfGroup(final PFUserDO user, final Integer groupId) {
    if (user == null) {
      return false;
    }
    return isUserMemberOfGroup(user.getId(), groupId);
  }

  public boolean isUserMemberOfGroup(final Integer userId, final Integer groupId) {
    if (groupId == null) {
      return false;
    }
    checkRefresh();
    final Set<Integer> groupSet = getUserGroupIdMap().get(userId);
    return (groupSet != null) && groupSet.contains(groupId);
  }

  public boolean isUserMemberOfAtLeastOneGroup(final Integer userId, final Integer... groupIds) {
    if (groupIds == null) {
      return false;
    }
    checkRefresh();
    final Set<Integer> groupSet = getUserGroupIdMap().get(userId);
    if (groupSet == null) {
      return false;
    }
    for (final Integer groupId : groupIds) {
      if (groupId == null) {
        continue;
      }
      if (groupSet.contains(groupId)) {
        return true;
      }
    }
    return false;
  }

  public boolean isUserMemberOfAdminGroup() {
    return isUserMemberOfAdminGroup(ThreadLocalUserContext.getUserId());
  }

  public boolean isUserMemberOfAdminGroup(final Integer userId) {
    checkRefresh();
    // adminUsers should only be null in maintenance mode (e. g. if user table isn't readable).
    return adminUsers != null && adminUsers.contains(userId);
  }

  public boolean isUserMemberOfFinanceGroup() {
    return isUserMemberOfFinanceGroup(ThreadLocalUserContext.getUserId());
  }

  public boolean isUserMemberOfFinanceGroup(final Integer userId) {
    checkRefresh();
    // financeUsers should only be null in maintenance mode (e. g. if user table isn't readable).
    return financeUsers != null && financeUsers.contains(userId);
  }

  public boolean isUserMemberOfProjectManagers() {
    return isUserMemberOfProjectManagers(ThreadLocalUserContext.getUserId());
  }

  public boolean isUserMemberOfProjectManagers(final Integer userId) {
    checkRefresh();
    // projectManagers should only be null in maintenance mode (e. g. if user table isn't readable).
    return projectManagers != null && projectManagers.contains(userId);
  }

  public boolean isUserMemberOfProjectAssistant() {
    return isUserMemberOfProjectAssistant(ThreadLocalUserContext.getUserId());
  }

  public boolean isUserMemberOfProjectAssistant(final Integer userId) {
    checkRefresh();
    // projectAssistants should only be null in maintenance mode (e. g. if user table isn't readable).
    return projectAssistants != null && projectAssistants.contains(userId);
  }

  public boolean isUserProjectManagerOrAssistantForProject(final ProjektDO projekt) {
    if (projekt == null || projekt.getProjektManagerGroupId() == null) {
      return false;
    }
    final Integer userId = ThreadLocalUserContext.getUserId();
    if (!isUserMemberOfProjectAssistant(userId) && !isUserMemberOfProjectManagers(userId)) {
      return false;
    }
    return isUserMemberOfGroup(userId, projekt.getProjektManagerGroupId());
  }

  public boolean isUserMemberOfControllingGroup() {
    return isUserMemberOfControllingGroup(ThreadLocalUserContext.getUserId());
  }

  public boolean isUserMemberOfControllingGroup(final Integer userId) {
    checkRefresh();
    // controllingUsers should only be null in maintenance mode (e. g. if user table isn't readable).
    return controllingUsers != null && controllingUsers.contains(userId);
  }

  public boolean isUserMemberOfMarketingGroup() {
    return isUserMemberOfMarketingGroup(ThreadLocalUserContext.getUserId());
  }

  public boolean isUserMemberOfMarketingGroup(final Integer userId) {
    checkRefresh();
    return marketingUsers.contains(userId);
  }

  public boolean isUserMemberOfOrgaGroup() {
    return isUserMemberOfOrgaGroup(ThreadLocalUserContext.getUserId());
  }

  public boolean isUserMemberOfOrgaGroup(final Integer userId) {
    checkRefresh();
    // orgaUsers should only be null in maintenance mode (e. g. if user table isn't readable).
    return orgaUsers != null && orgaUsers.contains(userId);
  }

  public boolean isUserMemberOfHRGroup(final Integer userId) {
    checkRefresh();
    return hrUsers != null && hrUsers.contains(userId);
  }

  /**
   * Checks if the given user is at least member of one of the given groups.
   */
  public boolean isUserMemberOfGroup(final PFUserDO user, final ProjectForgeGroup... groups) {
    if (user == null) {
      return false;
    }
    Validate.notNull(groups);
    for (final ProjectForgeGroup group : groups) {
      boolean result = false;
      if (group == ProjectForgeGroup.ADMIN_GROUP) {
        result = isUserMemberOfAdminGroup(user.getId());
      } else if (group == ProjectForgeGroup.FINANCE_GROUP) {
        result = isUserMemberOfFinanceGroup(user.getId());
      } else if (group == ProjectForgeGroup.PROJECT_MANAGER) {
        result = isUserMemberOfProjectManagers(user.getId());
      } else if (group == ProjectForgeGroup.PROJECT_ASSISTANT) {
        result = isUserMemberOfProjectAssistant(user.getId());
      } else if (group == ProjectForgeGroup.CONTROLLING_GROUP) {
        result = isUserMemberOfControllingGroup(user.getId());
      } else if (group == ProjectForgeGroup.MARKETING_GROUP) {
        result = isUserMemberOfMarketingGroup(user.getId());
      } else if (group == ProjectForgeGroup.ORGA_TEAM) {
        result = isUserMemberOfOrgaGroup(user.getId());
      } else if (group == ProjectForgeGroup.HR_GROUP) {
        result = isUserMemberOfHRGroup(user.getId());
      } else {
        throw new UnsupportedOperationException("Group not yet supported: " + group);
      }
      if (result) {
        return true;
      }
    }
    return false;
  }

  public List<UserRightDO> getUserRights(final Integer userId) {
    return getUserRightMap().get(userId);
  }

  public UserRightDO getUserRight(final Integer userId, final UserRightId rightId) {
    final List<UserRightDO> rights = getUserRights(userId);
    if (rights == null) {
      return null;
    }
    for (final UserRightDO right : rights) {
      if (StringUtils.equals(right.getRightIdString(), rightId.getId())) {
        return right;
      }
    }
    return null;
  }

  private Map<Integer, List<UserRightDO>> getUserRightMap() {
    checkRefresh();
    return rightMap;
  }

  /**
   * Returns a collection of group id's to which the user is assigned to.
   *
   * @return collection if found, otherwise null.
   */
  public Collection<Integer> getUserGroups(final PFUserDO user) {
    checkRefresh();
    return getUserGroupIdMap().get(user.getId());
  }

  private Map<Integer, GroupDO> getGroupMap() {
    checkRefresh();
    return groupMap;
  }

  public Map<Integer, Set<Integer>> getUserGroupIdMap() {
    checkRefresh();
    return userGroupIdMap;
  }

  /**
   * Should be called after user modifications.
   */
  void updateUser(final PFUserDO user) {
    getUserMap().put(user.getId(), user);
  }

  private Map<Integer, PFUserDO> getUserMap() {
    checkRefresh();
    return userMap;
  }

  /**
   * This method will be called by CacheHelper and is synchronized.
   */
  @Override
  protected void refresh() {
    long begin = System.currentTimeMillis();
    String tenantLog = "";
    if (tenant != null) {
      tenantLog = " for tenant " + tenantService.getLogName(tenant);
    }
    log.info("Initializing UserGroupCache " + tenantLog + "...");
    // This method must not be synchronized because it works with a new copy of maps.
    final Map<Integer, PFUserDO> uMap = new HashMap<>();
    // Could not autowire UserDao because of cyclic reference with AccessChecker.
    log.info("Loading all users ...");
    final List<PFUserDO> users = Login.getInstance().getAllUsers();
    for (final PFUserDO user : users) {
      if (tenant != null) {
        if (!tenantChecker.isPartOfTenant(tenant, user)) {
          // Ignore users not assigned to current tenant.
          continue;
        }
      }
      final PFUserDO copiedUser = PFUserDO.createCopyWithoutSecretFields(user);
      uMap.put(user.getId(), copiedUser);
    }
    log.info("Loading all groups ...");
    final List<GroupDO> groups = Login.getInstance().getAllGroups();
    final Map<Integer, GroupDO> gMap = new HashMap<>();
    final Map<Integer, Set<Integer>> ugIdMap = new HashMap<>();
    final Set<Integer> nAdminUsers = new HashSet<>();
    final Set<Integer> nFinanceUser = new HashSet<>();
    final Set<Integer> nControllingUsers = new HashSet<>();
    final Set<Integer> nProjectManagers = new HashSet<>();
    final Set<Integer> nProjectAssistants = new HashSet<>();
    final Set<Integer> nMarketingUsers = new HashSet<>();
    final Set<Integer> nOrgaUsers = new HashSet<>();
    final Set<Integer> nhrUsers = new HashSet<>();
    for (final GroupDO group : groups) {
      if (tenant != null) {
        if (!tenantChecker.isPartOfTenant(tenant.getId(), group)) {
          // Ignore groups not assigned to current tenant.
          continue;
        }
      }
      gMap.put(group.getId(), group);
      if (group.getAssignedUsers() != null) {
        for (final PFUserDO user : group.getAssignedUsers()) {
          if (user != null) {
            final Set<Integer> groupIdSet = ensureAndGetUserGroupIdMap(ugIdMap, user.getId());
            groupIdSet.add(group.getId());
            if (ProjectForgeGroup.ADMIN_GROUP.equals(group.getName())) {
              log.debug("Adding user '" + user.getUsername() + "' as administrator.");
              nAdminUsers.add(user.getId());
            } else if (ProjectForgeGroup.FINANCE_GROUP.equals(group.getName())) {
              log.debug("Adding user '" + user.getUsername() + "' for finance.");
              nFinanceUser.add(user.getId());
            } else if (ProjectForgeGroup.CONTROLLING_GROUP.equals(group.getName())) {
              log.debug("Adding user '" + user.getUsername() + "' for controlling.");
              nControllingUsers.add(user.getId());
            } else if (ProjectForgeGroup.PROJECT_MANAGER.equals(group.getName())) {
              log.debug("Adding user '" + user.getUsername() + "' as project manager.");
              nProjectManagers.add(user.getId());
            } else if (ProjectForgeGroup.PROJECT_ASSISTANT.equals(group.getName())) {
              log.debug("Adding user '" + user.getUsername() + "' as project assistant.");
              nProjectAssistants.add(user.getId());
            } else if (ProjectForgeGroup.MARKETING_GROUP.equals(group.getName())) {
              log.debug("Adding user '" + user.getUsername() + "' as marketing user.");
              nMarketingUsers.add(user.getId());
            } else if (ProjectForgeGroup.ORGA_TEAM.equals(group.getName())) {
              log.debug("Adding user '" + user.getUsername() + "' as orga user.");
              nOrgaUsers.add(user.getId());
            } else if (ProjectForgeGroup.HR_GROUP.equals(group.getName())) {
              log.debug("Adding user '" + user.getUsername() + "' as hr user.");
              nhrUsers.add(user.getId());
            }
          }
        }
      }
    }
    this.userMap = uMap;
    this.groupMap = gMap;
    this.adminUsers = nAdminUsers;
    this.financeUsers = nFinanceUser;
    this.controllingUsers = nControllingUsers;
    this.projectManagers = nProjectManagers;
    this.projectAssistants = nProjectAssistants;
    this.marketingUsers = nMarketingUsers;
    this.orgaUsers = nOrgaUsers;
    this.hrUsers = nhrUsers;
    this.userGroupIdMap = ugIdMap;
    final Map<Integer, List<UserRightDO>> rMap = new HashMap<>();
    List<UserRightDO> rights;
    try {
      rights = userRightDao.internalGetAllOrdered();
    } catch (final Exception ex) {
      log.error(
              "******* Exception while getting user rights from data-base (only OK for migration from older versions): "
                      + ex.getMessage(),
              ex);
      rights = new ArrayList<>();
    }
    List<UserRightDO> list = null;
    Integer userId = null;
    for (final UserRightDO right : rights) {
      if (right.getUserId() == null) {
        log.warn("Oups, userId = null: " + right);
        continue;
      }
      if (!right.getUserId().equals(userId)) {
        list = new ArrayList<>();
        userId = right.getUserId();
        if (userId != null) {
          rMap.put(userId, list);
        }
      }
      if (userRights.getRight(right.getRightIdString()) != null
              && userRights.getRight(right.getRightIdString()).isAvailable(this, right.getUser())) {
        list.add(right);
      }
    }
    this.rightMap = rMap;
    log.info("Initializing of UserGroupCache done" + tenantLog + ".");
    Login.getInstance().afterUserGroupCacheRefresh(users, groups);
    long end = System.currentTimeMillis();
    log.info("UserGroupCache.refresh took: " + (end - begin) + " ms.");
  }

  public synchronized void internalSetAdminUser(final PFUserDO adminUser) {
    if (!UserFilter.isUpdateRequiredFirst()) {
      throw new IllegalStateException(
              "Can't set admin user internally! This method is only available if system is under maintenance (update required first is true)!");
    }
    checkRefresh();
    this.adminUsers.add(adminUser.getId());
  }

  /**
   * @return the tenant
   */
  public TenantDO getTenant() {
    return tenant;
  }
}
