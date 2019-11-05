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

import de.micromata.genome.jpa.Clauses;
import de.micromata.genome.jpa.CriteriaUpdate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.login.Login;
import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class UserDao extends BaseDao<PFUserDO> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserDao.class);

  private static final short AUTHENTICATION_TOKEN_LENGTH = 20;

  private final List<UserChangedListener> userChangedListeners = new LinkedList<>();

  @Autowired
  private ApplicationContext applicationContext;

  public UserDao() {
    super(PFUserDO.class);
  }

  public static List<PFUserDO> copyUsersWithoutSecrectFields(List<PFUserDO> list) {
    if (list == null)
      return null;
    List<PFUserDO> result = new ArrayList<>(list.size());
    for (PFUserDO user : list) {
      result.add(PFUserDO.createCopyWithoutSecretFields(user));
    }
    return result;
  }

  /**
   * Register given listener. The listener is called every time a user was inserted, updated or deleted.
   *
   * @param userChangedListener
   */
  public void register(final UserChangedListener userChangedListener) {
    userChangedListeners.add(userChangedListener);
  }

  public QueryFilter getDefaultFilter() {
    final QueryFilter queryFilter = new QueryFilter(null, false);
    queryFilter.add(QueryFilter.eq("deleted", false));
    return queryFilter;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#createQueryFilter(org.projectforge.framework.persistence.api.BaseSearchFilter)
   */
  @Override
  protected QueryFilter createQueryFilter(final BaseSearchFilter filter) {
    final boolean superAdmin = TenantChecker.isSuperAdmin(ThreadLocalUserContext.getUser());
    if (!superAdmin) {
      return super.createQueryFilter(filter);
    }
    return new QueryFilter(filter, true);
  }

  @Override
  public List<PFUserDO> getList(final BaseSearchFilter filter) {
    final PFUserFilter myFilter;
    if (filter instanceof PFUserFilter) {
      myFilter = (PFUserFilter) filter;
    } else {
      myFilter = new PFUserFilter(filter);
    }
    final QueryFilter queryFilter = createQueryFilter(myFilter);
    if (myFilter.getDeactivatedUser() != null) {
      queryFilter.add(QueryFilter.eq("deactivated", myFilter.getDeactivatedUser()));
    }
    if (Login.getInstance().hasExternalUsermanagementSystem()) {
      // Check hasExternalUsermngmntSystem because otherwise the filter is may-be preset for an user and the user can't change the filter
      // (because the fields aren't visible).
      if (myFilter.getRestrictedUser() != null) {
        queryFilter.add(QueryFilter.eq("restrictedUser", myFilter.getRestrictedUser()));
      }
      if (myFilter.getLocalUser() != null) {
        queryFilter.add(QueryFilter.eq("localUser", myFilter.getLocalUser()));
      }
    }
    if (myFilter.getHrPlanning() != null) {
      queryFilter.add(QueryFilter.eq("hrPlanning", myFilter.getHrPlanning()));
    }
    queryFilter.addOrder(SortProperty.asc("username"));
    List<PFUserDO> list = getList(queryFilter);
    if (myFilter.getIsAdminUser() != null) {
      final List<PFUserDO> origList = list;
      list = new LinkedList<>();
      for (final PFUserDO user : origList) {
        if (myFilter.getIsAdminUser() == accessChecker.isUserMemberOfAdminGroup(user, false)) {
          list.add(user);
        }
      }
    }
    if (applicationContext.getBean(TenantService.class).isMultiTenancyAvailable()
            && !TenantChecker.isSuperAdmin(ThreadLocalUserContext.getUser())) {
      final List<PFUserDO> origList = list;
      list = new LinkedList<>();
      for (final PFUserDO user : origList) {
        if (tenantChecker.isPartOfTenant(ThreadLocalUserContext.getUserContext().getCurrentTenant(), user)) {
          list.add(user);
        }
      }
    }
    return list;
  }

  /**
   * Removes secret fields for security reasons by copying all users without secret fields.
   * Result elements are evicted.
   *
   * @throws AccessException
   * @see BaseDao#internalGetList(QueryFilter)
   */
  @Override
  public List<PFUserDO> internalGetList(QueryFilter filter) throws AccessException {
    return copyUsersWithoutSecrectFields(super.internalGetList(filter));
  }

  /**
   * Removes secret fields for security reasons by copying all users without secret fields.
   * Result elements are evicted.
   *
   * @see BaseDao#internalLoadAll()
   */
  @Override
  public List<PFUserDO> internalLoadAll() {
    return copyUsersWithoutSecrectFields(super.internalLoadAll());
  }

  /**
   * Removes secret fields for security reasons by copying all users without secret fields.
   * Result elements are evicted.
   *
   * @see BaseDao#internalLoadAll(TenantDO)
   */
  @Override
  public List<PFUserDO> internalLoadAll(TenantDO tenant) {
    return copyUsersWithoutSecrectFields(super.internalLoadAll(tenant));
  }

  /**
   * Removes secret fields for security reasons by copying all users without secret fields.
   * Result elements are evicted.
   *
   * @see BaseDao#internalLoad(Collection)
   */
  @Override
  public List<PFUserDO> internalLoad(Collection<? extends Serializable> idList) {
    return copyUsersWithoutSecrectFields(super.internalLoad(idList));
  }

  /**
   * Removes secret fields for security reasons by copying all users without secret fields.
   * Result elements are evicted.
   *
   * @see BaseDao#getListByIds(Collection)
   */
  @Override
  public List<PFUserDO> getListByIds(Collection<? extends Serializable> idList) {
    return copyUsersWithoutSecrectFields(super.getListByIds(idList));
  }

  /**
   * Removes secret fields for security reasons.
   *
   * @see BaseDao#getOrLoad(Integer)
   */
  @Override
  public PFUserDO getOrLoad(Integer id) {
    return PFUserDO.createCopyWithoutSecretFields(super.getOrLoad(id));
  }

  public Collection<Integer> getAssignedGroups(final PFUserDO user) {
    return getUserGroupCache().getUserGroups(user);
  }

  public Collection<Integer> getAssignedTenants(final PFUserDO user) {
    final List<TenantDO> list = em
            .createNamedQuery(TenantDO.FIND_ASSIGNED_TENANTS, TenantDO.class)
            .setParameter("user", user)
            .getResultList();

    final Set<Integer> result = new HashSet<>();
    if (list != null) {
      for (final TenantDO tenant : list) {
        result.add(tenant.getId());
      }
    }
    return result;
  }

  public List<UserRightDO> getUserRights(final Integer userId) {
    return getUserGroupCache().getUserRights(userId);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#onChange(ExtendedBaseDO, ExtendedBaseDO)
   */
  @Override
  protected void onChange(final PFUserDO obj, final PFUserDO dbObj) {
    super.onChange(obj, dbObj);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSave(ExtendedBaseDO)
   */
  @Override
  protected void afterSave(final PFUserDO obj) {
    super.afterSave(obj);
    for (final UserChangedListener userChangedListener : userChangedListeners) {
      userChangedListener.afterUserChanged(obj, OperationType.INSERT);
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterUpdate(ExtendedBaseDO, ExtendedBaseDO)
   */
  @Override
  protected void afterUpdate(final PFUserDO obj, final PFUserDO dbObj) {
    super.afterUpdate(obj, dbObj);
    for (final UserChangedListener userChangedListener : userChangedListeners) {
      userChangedListener.afterUserChanged(obj, OperationType.UPDATE);
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterUndelete(ExtendedBaseDO)
   */
  @Override
  protected void afterUndelete(final PFUserDO obj) {
    super.afterUndelete(obj);
    for (final UserChangedListener userChangedListener : userChangedListeners) {
      userChangedListener.afterUserChanged(obj, OperationType.UPDATE);
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterDelete(ExtendedBaseDO)
   */
  @Override
  protected void afterDelete(final PFUserDO obj) {
    super.afterDelete(obj);
    for (final UserChangedListener userChangedListener : userChangedListeners) {
      userChangedListener.afterUserChanged(obj, OperationType.DELETE);
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSaveOrModify(ExtendedBaseDO)
   */
  @Override
  protected void afterSaveOrModify(final PFUserDO obj) {
    if (!obj.isMinorChange()) {
      getUserGroupCache().setExpired();
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(PFUserDO, ExtendedBaseDO, ExtendedBaseDO, OperationType, boolean)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final PFUserDO obj, final PFUserDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  /**
   * @return false, if no admin user and the context user is not at minimum in one groups assigned to the given user or
   * false. Also deleted and deactivated users are only visible for admin users.
   * @see org.projectforge.framework.persistence.api.BaseDao#hasUserSelectAccess(PFUserDO, ExtendedBaseDO, boolean) )
   * @see AccessChecker#areUsersInSameGroup(PFUserDO, PFUserDO)
   */
  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final PFUserDO obj, final boolean throwException) {
    boolean result = accessChecker.isUserMemberOfAdminGroup(user)
            || accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.CONTROLLING_GROUP);
    log.debug("UserDao hasSelectAccess. Check user member of admin, finance or controlling group: " + result);
    if (!result && obj.hasSystemAccess()) {
      result = accessChecker.areUsersInSameGroup(user, obj);
      log.debug("UserDao hasSelectAccess. Caller user: " + user.getUsername() + " Check user: " + obj.getUsername()
              + " Check user in same group: " + result);
    }
    if (throwException && !result) {
      throw new AccessException(user, AccessType.GROUP, OperationType.SELECT);
    }
    return result;
  }

  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final boolean throwException) {
    return true;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasInsertAccess(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user) {
    return accessChecker.isUserMemberOfAdminGroup(user, false);
  }

  @Override
  protected void onSaveOrModify(final PFUserDO obj) {
    obj.checkAndFixPassword();
  }

  /**
   * Update user after login success.
   *
   * @param user the user
   */
  public void updateUserAfterLoginSuccess(PFUserDO user) {
    PfEmgrFactory.get().runInTrans((emgr) -> {
      CriteriaUpdate<PFUserDO> cu = CriteriaUpdate.createUpdate(PFUserDO.class);
      cu
              .set("lastLogin", new Timestamp(new Date().getTime()))
              .set("loginFailures", 0)
              .addWhere(Clauses.equal("id", user.getId()));
      return emgr.update(cu);
    });
  }

  public void updateIncrementLoginFailure(String userName) {
    PfEmgrFactory.get().runInTrans((emgr) -> {
      CriteriaUpdate<PFUserDO> cu = CriteriaUpdate.createUpdate(PFUserDO.class);
      cu
              .setExpression("loginFailures", "loginFailures + 1")
              .addWhere(Clauses.equal("username", userName));
      return emgr.update(cu);
    });
  }

  @SuppressWarnings("unchecked")
  public PFUserDO getUserByStayLoggedInKey(final String username, final String stayLoggedInKey) {
    final List<PFUserDO> list = em
            .createNamedQuery(PFUserDO.FIND_BY_USERNAME_AND_STAYLOGGEDINKEY, PFUserDO.class)
            .setParameter("username", username)
            .setParameter("stayLoggedInKey", stayLoggedInKey)
            .getResultList();
    PFUserDO user = null;
    if (list != null && !list.isEmpty() && list.get(0) != null) {
      user = list.get(0);
    }
    if (user != null && !user.hasSystemAccess()) {
      log.warn("Deleted/deactivated user tried to login (via stay-logged-in): " + user);
      return null;
    }
    return user;
  }

  /**
   * Does an user with the given username already exists? Works also for existing users (if username was modified).
   */
  public boolean doesUsernameAlreadyExist(final PFUserDO user) {
    Validate.notNull(user);
    PFUserDO dbUser = null;
    if (user.getId() == null) {
      // New user
      dbUser = getInternalByName(user.getUsername());
    } else {
      // user already exists. Check maybe changed username:
      dbUser = SQLHelper.ensureUniqueResult(em.createNamedQuery(PFUserDO.FIND_OTHER_USER_BY_USERNAME, PFUserDO.class)
              .setParameter("username", user.getUsername())
              .setParameter("id", user.getId()));
    }
    return dbUser != null;
  }

  public PFUserDO getUserByAuthenticationToken(final Integer userId, final String authKey) {
    final PFUserDO user = SQLHelper.ensureUniqueResult(em
            .createNamedQuery(PFUserDO.FIND_BY_USERID_AND_AUTHENTICATIONTOKEN, PFUserDO.class)
            .setParameter("id", userId)
            .setParameter("authenticationToken", authKey));
    if (user != null && !user.hasSystemAccess()) {
      log.warn("Deleted user tried to login (via authentication token): " + user);
      return null;
    }
    return user;
  }

  /**
   * Returns the user's authentication token if exists (must be not blank with a size >= 10). If not, a new token key
   * will be generated.
   */
  public String getAuthenticationToken(final Integer userId) {
    final PFUserDO user = internalGetById(userId);
    if (StringUtils.isBlank(user.getAuthenticationToken()) || user.getAuthenticationToken().trim().length() < 10) {
      user.setAuthenticationToken(createAuthenticationToken());
      log.info("Authentication token renewed for user: " + userId + "" + user.getUsername());
      for (final UserChangedListener userChangedListener : userChangedListeners) {
        userChangedListener.afterUserChanged(user, OperationType.UPDATE);
      }
    }
    return user.getAuthenticationToken();
  }

  /**
   * Renews the user's authentication token (random string sequence).
   */
  public void renewAuthenticationToken(final Integer userId) {
    if (!ThreadLocalUserContext.getUserId().equals(userId)) {
      // Only admin users are able to renew authentication token of other users:
      accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    }
    accessChecker.checkRestrictedOrDemoUser(); // Demo users are also not allowed to do this.
    final PFUserDO user = internalGetById(userId);
    user.setAuthenticationToken(createAuthenticationToken());
    log.info("Authentication token renewed for user: " + userId + "" + user.getUsername());
    for (final UserChangedListener userChangedListener : userChangedListeners) {
      userChangedListener.afterUserChanged(user, OperationType.UPDATE);
    }
  }

  private String createAuthenticationToken() {
    return NumberHelper.getSecureRandomUrlSaveString(AUTHENTICATION_TOKEN_LENGTH);
  }

  public PFUserDO getInternalByName(final String username) {
    return SQLHelper.ensureUniqueResult(em.createNamedQuery(PFUserDO.FIND_BY_USERNAME, PFUserDO.class)
            .setParameter("username", username));
  }

  /**
   * User can modify own setting, this method ensures that only such properties will be updated, the user's are allowed
   * to.
   *
   * @param user
   */
  public void updateMyAccount(final PFUserDO user) {
    accessChecker.checkRestrictedOrDemoUser();
    final PFUserDO contextUser = ThreadLocalUserContext.getUser();
    Validate.isTrue(user.getId().equals(contextUser.getId()));
    final PFUserDO dbUser = em.getReference(clazz, user.getId());
    final String[] ignoreFields = {"deleted", "password", "lastLogin", "loginFailures", "username", "stayLoggedInKey",
            "authenticationToken", "rights"};
    final ModificationStatus result = HistoryBaseDaoAdapter.wrappHistoryUpdate(dbUser,
            () -> copyValues(user, dbUser, ignoreFields));
    if (result != ModificationStatus.NONE) {
      dbUser.setLastUpdate();
      log.info("Object updated: " + dbUser.toString());
      copyValues(user, contextUser, ignoreFields);
    } else {
      log.info("No modifications detected (no update needed): " + dbUser.toString());
    }
    getUserGroupCache().updateUser(user);
  }

  /**
   * Gets history entries of super and adds all history entries of the AuftragsPositionDO children.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#getDisplayHistoryEntries(ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final PFUserDO obj) {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (!hasLoggedInUserHistoryAccess(obj, false)) {
      return list;
    }
    if (CollectionUtils.isNotEmpty(obj.getRights())) {
      for (final UserRightDO right : obj.getRights()) {
        final List<DisplayHistoryEntry> entries = internalGetDisplayHistoryEntries(right);
        for (final DisplayHistoryEntry entry : entries) {
          final String propertyName = entry.getPropertyName();
          if (propertyName != null) {
            entry.setPropertyName(right.getRightIdString() + ":" + entry.getPropertyName()); // Prepend number of positon.
          } else {
            entry.setPropertyName(String.valueOf(right.getRightIdString()));
          }
        }
        list.addAll(entries);
      }
    }
    list.sort(new Comparator<DisplayHistoryEntry>() {
      @Override
      public int compare(final DisplayHistoryEntry o1, final DisplayHistoryEntry o2) {
        return (o2.getTimestamp().compareTo(o1.getTimestamp()));
      }
    });
    return list;
  }

  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final boolean throwException) {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  /**
   * Re-index all dependent objects only if the username, first or last name was changed.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#wantsReindexAllDependentObjects(ExtendedBaseDO, ExtendedBaseDO)
   */
  @Override
  protected boolean wantsReindexAllDependentObjects(final PFUserDO obj, final PFUserDO dbObj) {
    if (!super.wantsReindexAllDependentObjects(obj, dbObj)) {
      return false;
    }
    return !StringUtils.equals(obj.getUsername(), dbObj.getUsername())
            || !StringUtils.equals(obj.getFirstname(), dbObj.getFirstname())
            || !StringUtils.equals(obj.getLastname(), dbObj.getLastname());
  }

  @Override
  public PFUserDO newInstance() {
    return new PFUserDO();
  }

  public List<PFUserDO> findByUsername(String username) {
    return em.createNamedQuery(PFUserDO.FIND_BY_USERNAME, PFUserDO.class)
            .setParameter("username", username)
            .getResultList();
  }
}
