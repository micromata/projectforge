package org.projectforge.framework.access;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.lang.Validate;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRight;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.api.FallbackBaseDaoService;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of AccessChecker.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Service
public class AccessCheckerImpl implements AccessChecker, Serializable
{
  public static final String I18N_KEY_VIOLATION_USER_NOT_MEMBER_OF = "access.violation.userNotMemberOf";

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AccessChecker.class);

  @Autowired
  private UserRightService userRights;

  @Autowired
  private FallbackBaseDaoService fallbackBaseDaoService;

  /**
   * Tests for every group the user is assigned to, if the given permission is given.
   *
   * @return true, if the user owns the required permission, otherwise false.
   */
  public boolean hasLoggedInUserPermission(final Integer taskId, final AccessType accessType,
      final OperationType operationType,
      final boolean throwException)
  {
    return hasPermission(ThreadLocalUserContext.getUser(), taskId, accessType, operationType, throwException);
  }

  /**
   * Tests for every group the user is assigned to, if the given permission is given.
   *
   * @return true, if the user owns the required permission, otherwise false.
   */
  @Override
  public boolean hasPermission(final PFUserDO user, final Integer taskId, final AccessType accessType,
      final OperationType operationType,
      final boolean throwException)
  {
    Validate.notNull(user);
    final TaskNode node = getTaskTree().getTaskNodeById(taskId);
    if (node == null) {
      log.error("Task with " + taskId + " not found.");
      if (throwException == true) {
        throw new AccessException(taskId, accessType, operationType);
      }
      return false;
    }
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry(node.getTask())
        .getUserGroupCache();
    if (userGroupCache.isUserMemberOfAdminGroup(user.getId()) == true) {
      // A user group "Admin" has always access.
      return true;
    }
    final Collection<Integer> groupIds = userGroupCache.getUserGroups(user);
    if (groupIds == null) {
      // No groups are assigned to this user.
      if (throwException == true) {
        throw new AccessException(taskId, accessType, operationType);
      }
      return false;
    }
    for (final Integer groupId : groupIds) {
      if (node.hasPermission(groupId, accessType, operationType) == true) {
        return true;
      }
    }
    if (throwException == true) {
      throw new AccessException(taskId, accessType, operationType);
    }
    return false;
  }

  private TaskTree getTaskTree()
  {
    return TaskTreeHelper.getTaskTree();
  }

  /**
   * Checks if the user is an admin user (member of admin group). If not, an AccessException will be thrown.
   *
   * @see #isUserMemberOfAdminGroup()
   */
  @Override
  public void checkIsLoggedInUserMemberOfAdminGroup()
  {
    checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.ADMIN_GROUP);
  }

  /**
   * @param userId
   * @return
   * @see org.projectforge.business.user.UserGroupCache#isUserMemberOfAdminGroup(java.lang.Integer)
   */
  @Override
  public boolean isLoggedInUserMemberOfAdminGroup()
  {
    return isUserMemberOfAdminGroup(ThreadLocalUserContext.getUser());
  }

  /**
   * @param userId
   * @return
   * @see org.projectforge.business.user.UserGroupCache#isUserMemberOfAdminGroup(java.lang.Integer)
   */
  @Override
  public boolean isUserMemberOfAdminGroup(final PFUserDO user)
  {
    return isUserMemberOfGroup(user, ProjectForgeGroup.ADMIN_GROUP);
  }

  /**
   * @param tenant
   * @param userId
   * @return
   * @see org.projectforge.business.user.UserGroupCache#isUserMemberOfAdminGroup(java.lang.Integer)
   */
  @Override
  public boolean isUserMemberOfAdminGroup(final TenantDO tenant, final PFUserDO user)
  {
    return isUserMemberOfGroup(tenant, user, ProjectForgeGroup.ADMIN_GROUP);
  }

  /**
   * @param userId
   * @return
   * @see org.projectforge.business.user.UserGroupCache#isUserMemberOfAdminGroup(java.lang.Integer)
   */
  public boolean isLoggedInUserMemberOfAdminGroup(final boolean throwException)
  {
    return isLoggedInUserMemberOfGroup(throwException, ProjectForgeGroup.ADMIN_GROUP);
  }

  /**
   * @param userId
   * @return
   * @see org.projectforge.business.user.UserGroupCache#isUserMemberOfAdminGroup(java.lang.Integer)
   */
  @Override
  public boolean isUserMemberOfAdminGroup(final PFUserDO user, final boolean throwException)
  {
    return isUserMemberOfGroup(user, throwException, ProjectForgeGroup.ADMIN_GROUP);
  }

  /**
   * @param tenant
   * @param user
   * @return
   * @see org.projectforge.business.user.UserGroupCache#isUserMemberOfAdminGroup(java.lang.Integer)
   */
  public boolean isUserMemberOfAdminGroup(final TenantDO tenant, final PFUserDO user, final boolean throwException)
  {
    return isUserMemberOfGroup(tenant, user, throwException, ProjectForgeGroup.ADMIN_GROUP);
  }

  /**
   * Checks if the user is in one of the given groups. If not, an AccessException will be thrown.
   *
   * @see #isUserMemberOfGroup(ProjectForgeGroup...)
   */
  @Override
  public void checkIsLoggedInUserMemberOfGroup(final ProjectForgeGroup... groups)
  {
    checkIsUserMemberOfGroup(ThreadLocalUserContext.getUser(), groups);
  }

  /**
   * Checks if the user is in one of the given groups. If not, an AccessException will be thrown.
   *
   * @see #isUserMemberOfGroup(ProjectForgeGroup...)
   */
  @Override
  public void checkIsUserMemberOfGroup(final PFUserDO user, final ProjectForgeGroup... groups)
  {
    if (isUserMemberOfGroup(user, groups) == false) {
      throw getLoggedInUserNotMemberOfException(groups);
    }
  }

  /**
   * Checks if the user of the ThreadLocalUserContext (logged in user) is member at least of one of the given groups.
   *
   * @param groups
   * @see #isUserMemberOfGroup(boolean, ProjectForgeGroup...)
   */
  @Override
  public boolean isLoggedInUserMemberOfGroup(final ProjectForgeGroup... groups)
  {
    return isLoggedInUserMemberOfGroup(false, groups);
  }

  /**
   * Checks if the user of the ThreadLocalUserContext (logged in user) is member at least of one of the given groups.
   *
   * @param throwException default false.
   * @param groups
   * @see #isUserMemberOfGroup(PFUserDO, ProjectForgeGroup...)
   */
  public boolean isLoggedInUserMemberOfGroup(final boolean throwException, final ProjectForgeGroup... groups)
  {
    return isUserMemberOfGroup(ThreadLocalUserContext.getUser(), throwException, groups);
  }

  /**
   * Checks if the user of the ThreadLocalUserContext (logged in user) is member at least of one of the given groups.
   *
   * @param throwException default false.
   * @param groups
   * @see #isUserMemberOfGroup(PFUserDO, ProjectForgeGroup...)
   */
  @Override
  public boolean isUserMemberOfGroup(final PFUserDO user, final boolean throwException,
      final ProjectForgeGroup... groups)
  {
    Validate.notNull(groups);
    if (user == null) {
      // Before user is logged in.
      if (throwException == true) {
        throw getLoggedInUserNotMemberOfException(groups);
      }
      return false;
    }
    if (throwException == false) {
      return isUserMemberOfGroup(user, groups);
    } else if (isUserMemberOfGroup(user, groups) == true) {
      return true;
    } else {
      throw getLoggedInUserNotMemberOfException(groups);
    }
  }

  /**
   * Checks if the user of the ThreadLocalUserContext (logged in user) is member at least of one of the given groups.
   *
   * @param tenant
   * @param user
   * @param throwException default false.
   * @param groups
   * @see #isUserMemberOfGroup(PFUserDO, ProjectForgeGroup...)
   */
  public boolean isUserMemberOfGroup(final TenantDO tenant, final PFUserDO user, final boolean throwException,
      final ProjectForgeGroup... groups)
  {
    Validate.notNull(groups);
    if (user == null) {
      // Before user is logged in.
      if (throwException == true) {
        throw getLoggedInUserNotMemberOfException(groups);
      }
      return false;
    }
    if (throwException == false) {
      return isUserMemberOfGroup(tenant, user, groups);
    } else if (isUserMemberOfGroup(tenant, user, groups) == true) {
      return true;
    } else {
      throw getLoggedInUserNotMemberOfException(groups);
    }
  }

  private AccessException getLoggedInUserNotMemberOfException(final ProjectForgeGroup... groups)
  {
    final StringBuffer buf = new StringBuffer();
    for (int i = 0; i < groups.length; i++) {
      if (i > 0) {
        buf.append(", ");
      }
      buf.append(groups[i].toString());
    }
    final String str = buf.toString();
    log.error(I18N_KEY_VIOLATION_USER_NOT_MEMBER_OF + ": " + str);
    return new AccessException(I18N_KEY_VIOLATION_USER_NOT_MEMBER_OF, str);
  }

  /**
   * Checks if the given user is at least member of one of the given groups.
   *
   * @param user
   * @param groups
   */
  @Override
  public boolean isUserMemberOfGroup(final PFUserDO user, final ProjectForgeGroup... groups)
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    return userGroupCache.isUserMemberOfGroup(user, groups);
  }

  /**
   * Checks if the given user is at least member of one of the given groups.
   *
   * @param tenant
   * @param user
   * @param groups
   */
  public boolean isUserMemberOfGroup(final TenantDO tenant, final PFUserDO user, final ProjectForgeGroup... groups)
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry(tenant).getUserGroupCache();
    return userGroupCache.isUserMemberOfGroup(user, groups);
  }

  /**
   * Compares the two given users on equality. The pk's will be compared. If one or more user's or pk's are null, false
   * will be returned.
   *
   * @param u1
   * @param u2
   * @return true, if both user pk's are not null and equal.
   */
  @Override
  public boolean userEquals(final PFUserDO u1, final PFUserDO u2)
  {
    if (u1 == null || u2 == null || u1.getId() == null) {
      return false;
    }
    return u1.getId().equals(u2.getId());
  }

  /**
   * Gets the user from the ThreadLocalUserContext and compares the both user.
   *
   * @param user
   * @return
   * @see AccessChecker#userEquals(PFUserDO, PFUserDO)
   */
  @Override
  public boolean userEqualsToContextUser(final PFUserDO user)
  {
    return userEquals(ThreadLocalUserContext.getUser(), user);
  }

  /**
   * Is the current context user in at minimum one group of the groups assigned to the given user?
   *
   * @param user
   * @return
   */
  public boolean isLoggedInUserInSameGroup(final PFUserDO user)
  {
    return areUsersInSameGroup(ThreadLocalUserContext.getUser(), user);
  }

  /**
   * Is the current context user in at minimum one group of the groups assigned to the given user?
   *
   * @param user2
   * @return
   */
  @Override
  public boolean areUsersInSameGroup(final PFUserDO user1, final PFUserDO user2)
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final Collection<Integer> userGroups = userGroupCache.getUserGroups(user2);
    // No groups found.
    if (userGroups == null) {
      final Collection<Integer> userGroups2 = userGroupCache.getUserGroups(user1);
      if (userGroups2 == null) {
        return true;
      } else {
        return false;
      }
    }
    final Collection<Integer> currentUserGroups = userGroupCache.getUserGroups(user1);
    if (currentUserGroups == null) {
      // User has now associated groups.
      return false;
    }
    for (final Integer id : currentUserGroups) {
      if (userGroups.contains(id) == true) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param origUser       Check the access for the given user instead of the logged-in user.
   * @param rightId
   * @param obj
   * @param oldObj
   * @param operationType
   * @param throwException
   */
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public boolean hasAccess(final PFUserDO origUser, final IUserRightId rightId, final Object obj, final Object oldObj,
      final OperationType operationType, final boolean throwException)
  {
    final UserRight right = userRights.getRight(rightId);
    if (right == null) {
      throw new IllegalArgumentException("Cannot find UserRightId: " + rightId);
    }
    Validate.notNull(right);
    boolean result;
    if (right instanceof UserRightAccessCheck<?>) {
      Validate.notNull(origUser);
      final PFUserDO user = getUser(origUser);
      switch (operationType) {
        case SELECT:
          if (obj != null) {
            result = ((UserRightAccessCheck) right).hasSelectAccess(user, obj);
          } else {
            result = ((UserRightAccessCheck) right).hasSelectAccess(user);
          }
          break;
        case INSERT:
          if (obj != null) {
            result = ((UserRightAccessCheck) right).hasInsertAccess(user, obj);
          } else {
            result = ((UserRightAccessCheck) right).hasInsertAccess(user);
          }
          break;
        case UPDATE:
          result = ((UserRightAccessCheck) right).hasUpdateAccess(user, obj, oldObj);
          break;
        case DELETE:
          result = ((UserRightAccessCheck) right).hasDeleteAccess(user, obj, oldObj);
          break;
        default:
          throw new UnsupportedOperationException("Oups, value not supported for OperationType: " + operationType);
      }
      if (result == false && throwException == true) {
        throw new AccessException(user, "access.exception.userHasNotRight", rightId, operationType);
      }
      return result;
    }
    if (UserRightId.ADMIN_CORE.equals(right.getId())) {
      return isUserMemberOfGroup(ThreadLocalUserContext.getUser(), ProjectForgeGroup.ADMIN_GROUP);
    }
    if (obj instanceof EmployeeDO && oldObj instanceof EmployeeDO && ((EmployeeDO) obj).getUser().equals(origUser)
        && ((EmployeeDO) oldObj).getUser()
        .equals(origUser)) {
      return true;
    }
    if (operationType == OperationType.SELECT) {
      return hasRight(origUser, rightId, throwException, UserRightValue.READONLY, UserRightValue.READWRITE);
    } else {
      return hasRight(origUser, rightId, throwException, UserRightValue.READWRITE);
    }
  }

  /**
   * Use context user (logged-in user).
   *
   * @param rightId
   * @param obj
   * @param oldObj
   * @param operationType
   * @param throwException
   * @see #hasAccess(PFUserDO, IUserRightId, Object, Object, OperationType, boolean)
   */
  @Override
  public boolean hasLoggedInUserAccess(final IUserRightId rightId, final Object obj, final Object oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return hasAccess(ThreadLocalUserContext.getUser(), rightId, obj, oldObj, operationType, throwException);
  }

  /**
   * Use context user (logged-in user).
   *
   * @param rightId
   * @param obj
   * @param throwException
   * @see #hasSelectAccess(PFUserDO, IUserRightId, boolean)
   */
  @Override
  public boolean hasLoggedInUserSelectAccess(final IUserRightId rightId, final boolean throwException)
  {
    return hasSelectAccess(ThreadLocalUserContext.getUser(), rightId, throwException);
  }

  /**
   * Calls {@link #hasAccess(IUserRightId, Object, Object, OperationType, boolean)} with {@link OperationType#SELECT}
   * and both Objects as null.
   *
   * @param user           Check the access for the given user instead of the logged-in user.
   * @param rightId
   * @param obj
   * @param throwException
   * @see #hasAccess(IUserRightId, Object, Object, OperationType, boolean)
   */
  public boolean hasSelectAccess(final PFUserDO user, final IUserRightId rightId, final boolean throwException)
  {
    return hasAccess(user, rightId, null, null, OperationType.SELECT, throwException);
  }

  /**
   * Calls {@link #hasAccess(IUserRightId, Object, Object, OperationType, boolean)} with {@link OperationType#SELECT}.
   *
   * @param rightId
   * @param obj
   * @param throwException
   * @see #hasAccess(IUserRightId, Object, Object, OperationType, boolean)
   */
  @Override
  public boolean hasLoggedInUserSelectAccess(final IUserRightId rightId, final Object obj,
      final boolean throwException)
  {
    return hasLoggedInUserAccess(rightId, obj, null, OperationType.SELECT, throwException);
  }

  /**
   * Calls {@link #hasAccess(IUserRightId, Object, Object, OperationType, boolean)} with {@link OperationType#INSERT}.
   *
   * @param rightId
   * @param obj
   * @param throwException
   * @see #hasAccess(IUserRightId, Object, Object, OperationType, boolean)
   */
  @Override
  public boolean hasLoggedInUserInsertAccess(final IUserRightId rightId, final Object obj,
      final boolean throwException)
  {
    return hasLoggedInUserAccess(rightId, obj, null, OperationType.INSERT, throwException);
  }

  /**
   * Calls {@link #hasAccess(IUserRightId, Object, Object, OperationType, boolean)} with {@link OperationType#INSERT}.
   *
   * @param rightId
   * @param obj
   * @param throwException
   * @see #hasAccess(IUserRightId, Object, Object, OperationType, boolean)
   */
  public boolean hasLoggedInUserInsertAccess(final IUserRightId rightId, final boolean throwException)
  {
    return hasLoggedInUserAccess(rightId, null, null, OperationType.INSERT, throwException);
  }

  /**
   * Calls {@link #hasAccess(IUserRightId, Object, Object, OperationType, boolean)} with {@link OperationType#INSERT}.
   *
   * @param rightId
   * @param obj
   * @param throwException
   * @see #hasAccess(IUserRightId, Object, Object, OperationType, boolean)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user, final IUserRightId rightId, final boolean throwException)
  {
    return hasAccess(user, rightId, null, null, OperationType.INSERT, throwException);
  }

  /**
   * Calls {@link #hasAccess(IUserRightId, Object, Object, OperationType, boolean)} with {@link OperationType#UPDATE}.
   *
   * @param rightId
   * @param obj
   * @param oldObj
   * @param throwException
   * @see #hasAccess(IUserRightId, Object, Object, OperationType, boolean)
   */
  public boolean hasLoggedInUserUpdateAccess(final IUserRightId rightId, final Object obj, final Object oldObj,
      final boolean throwException)
  {
    return hasLoggedInUserAccess(rightId, obj, oldObj, OperationType.UPDATE, throwException);
  }

  /**
   * Calls {@link #hasAccess(IUserRightId, Object, Object, OperationType, boolean)} with {@link OperationType#DELETE}.
   *
   * @param rightId
   * @param obj
   * @param oldObj
   * @param throwException
   * @see #hasAccess(IUserRightId, Object, Object, OperationType, boolean)
   */
  public boolean hasLoggedInUserDeleteAccess(final IUserRightId rightId, final Object oldObj, final Object obj,
      final boolean throwException)
  {
    return hasLoggedInUserAccess(rightId, obj, oldObj, OperationType.DELETE, throwException);
  }

  /**
   * Throws now exception if the right check fails.
   *
   * @deprec
   * @see #hasRight(IUserRightId, boolean, UserRightValue...)
   */
  public boolean hasLoggedInUserRight(final IUserRightId rightId, final UserRightValue... values)
  {
    return hasRight(ThreadLocalUserContext.getUser(), rightId, false, values);
  }

  /**
   * Throws now exception if the right check fails.
   *
   * @see #hasRight(IUserRightId, boolean, UserRightValue...)
   */
  @Override
  public boolean hasRight(final PFUserDO user, final IUserRightId rightId, final UserRightValue... values)
  {
    return hasRight(user, rightId, false, values);
  }

  /**
   * Checks the availability and the demanded value of the right for the context user. The right will be checked itself
   * on required constraints, e. g. if assigned groups required.
   *
   * @param rightId
   * @param values         At least one of the values should match.
   * @param throwException
   */
  @Override
  public boolean hasLoggedInUserRight(final IUserRightId rightId, final boolean throwException,
      final UserRightValue... values)
  {
    return hasRight(ThreadLocalUserContext.getUser(), rightId, throwException, values);
  }

  /**
   * Checks the availability and the demanded value of the right for the context user. The right will be checked itself
   * on required constraints, e. g. if assigned groups required.
   *
   * @param user           Check the access for the given user instead of the logged-in user.
   * @param rightId
   * @param values         At least one of the values should match.
   * @param throwException
   * @return
   */
  @Override
  public boolean hasRight(final PFUserDO origUser, final IUserRightId rightId, final boolean throwException,
      final UserRightValue... values)
  {
    Validate.notNull(origUser);
    Validate.notNull(values);
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final PFUserDO user = userGroupCache.getUser(origUser.getId());
    if (user == null) {
      return false;
    }
    final UserRightDO rightDO = user.getRight(rightId);
    final UserRight right = userRights.getRight(rightId);
    for (final UserRightValue value : values) {
      if ((rightDO == null || rightDO.getValue() == null) && right.matches(userGroupCache, user, value) == true) {
        return true;
      }
      if (rightDO != null && rightDO.getValue() == value) {
        if (right != null && right.isAvailable(userGroupCache, user, value) == true) {
          return true;
        }
      }
    }
    if (throwException == true) {
      throw new AccessException("access.exception.userHasNotRight", rightId,
          StringHelper.listToString(", ", (Object[]) values));
    }
    return false;
  }

  /**
   * @param rightId
   * @param throwException
   * @see #hasRight(IUserRightId, boolean, UserRightValue...)
   */
  @Override
  public boolean hasLoggedInUserReadAccess(final IUserRightId rightId, final boolean throwException)
  {
    return hasReadAccess(ThreadLocalUserContext.getUser(), rightId, throwException);
  }

  /**
   * @param rightId
   * @param throwException
   * @see #hasRight(IUserRightId, boolean, UserRightValue...)
   */
  @Override
  public boolean hasReadAccess(final PFUserDO user, final IUserRightId rightId, final boolean throwException)
  {
    return hasRight(user, rightId, throwException, UserRightValue.READONLY, UserRightValue.READWRITE);
  }

  @Override
  public boolean hasWriteAccess(final PFUserDO user, final IUserRightId rightId, final boolean throwException)
  {
    return hasRight(user, rightId, throwException, UserRightValue.READWRITE);
  }

  /**
   * Calls {@link #hasReadAccess(IUserRightId, boolean)} with throwException = true.
   *
   * @param rightId
   * @param value
   * @see #hasReadAccess(IUserRightId, boolean)
   */
  public boolean checkLoggedInUserReadAccess(final IUserRightId rightId)
  {
    return hasLoggedInUserReadAccess(rightId, true);
  }

  /**
   * @param rightId
   * @param throwException
   * @see #hasRight(IUserRightId, boolean, UserRightValue...)
   */
  @Override
  public boolean hasLoggedInUserWriteAccess(final IUserRightId rightId, final boolean throwException)
  {
    return hasLoggedInUserRight(rightId, throwException, UserRightValue.READWRITE);
  }

  /**
   * Calls {@link #hasWriteAccess(IUserRightId, boolean)} with throwException = true.
   *
   * @param rightId
   * @param value
   * @see #hasWriteAccess(IUserRightId, boolean)
   */
  public boolean checkLoggedInUserWriteAccess(final IUserRightId rightId)
  {
    return hasLoggedInUserWriteAccess(rightId, true);
  }

  @Override
  public boolean hasLoggedInUserHistoryAccess(final IUserRightId rightId, final Object obj,
      final boolean throwException)
  {
    return hasHistoryAccess(ThreadLocalUserContext.getUser(), rightId, obj, throwException);
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public boolean hasHistoryAccess(final PFUserDO origUser, final IUserRightId rightId, final Object obj,
      final boolean throwException)
  {
    final UserRight right = userRights.getRight(rightId);
    Validate.notNull(right);
    if (right instanceof UserRightAccessCheck<?>) {
      Validate.notNull(origUser);
      final PFUserDO user = getUserGroupCache().getUser(origUser.getId());
      if (((UserRightAccessCheck) right).hasHistoryAccess(user, obj) == true) {
        return true;
      } else if (throwException == true) {
        throw new AccessException("access.exception.userHasNotRight", rightId, "history");
      } else {
        return false;
      }
    } else {
      return hasRight(origUser, rightId, throwException, UserRightValue.READONLY, UserRightValue.READWRITE);
    }
  }

  public TenantRegistry getTenantRegistry()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  public UserGroupCache getUserGroupCache()
  {
    return getTenantRegistry().getUserGroupCache();
  }

  /**
   * Calls {@link #hasRight(IUserRightId, UserRightValue, boolean)} with throwException = true.
   *
   * @param rightId
   * @param value
   * @see #hasRight(IUserRightId, UserRightValue, boolean)
   */
  @Override
  public boolean checkLoggedInUserRight(final IUserRightId rightId, final UserRightValue... values)
  {
    return hasLoggedInUserRight(rightId, true, values);
  }

  /**
   * Calls {@link #hasRight(IUserRightId, UserRightValue, boolean)} with throwException = true.
   *
   * @param rightId
   * @param value
   * @see #hasRight(IUserRightId, UserRightValue, boolean)
   */
  @Override
  public boolean checkUserRight(final PFUserDO user, final IUserRightId rightId, final UserRightValue... values)
  {
    return hasRight(user, rightId, true, values);
  }

  /**
   * Gets the UserRight and calls {@link UserRight#isAvailable(UserGroupCache, PFUserDO)}.
   *
   * @param rightId
   * @return
   */
  public boolean isAvailable(final IUserRightId rightId)
  {
    return isAvailable(ThreadLocalUserContext.getUser(), rightId);
  }

  /**
   * Gets the UserRight and calls {@link UserRight#isAvailable(UserGroupCache, PFUserDO)}.
   *
   * @param rightId
   * @return
   */
  @Override
  public boolean isAvailable(final PFUserDO user, final IUserRightId rightId)
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final UserRight right = userRights.getRight(rightId);
    return right != null && right.isAvailable(userGroupCache, user) == true;
  }

  @Override
  public boolean isDemoUser()
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (user == null) {
      return false;
    }
    return isDemoUser(user.getId());
  }

  @Override
  public boolean isDemoUser(final Integer userId)
  {
    final PFUserDO user = getUserGroupCache().getUser(userId);
    return AccessChecker.isDemoUser(user);
  }

  @Override
  public boolean isRestrictedUser()
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (user == null) {
      return true;
    }
    return isRestrictedUser(user.getId());
  }

  @Override
  public boolean isRestrictedUser(final Integer userId)
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final PFUserDO user = userGroupCache.getUser(userId);
    return AccessChecker.isDemoUser(user);
  }

  @Override
  public boolean isRestrictedUser(final PFUserDO user)
  {
    if (user == null) {
      return false;
    }
    return user.isRestrictedUser();
  }

  /**
   * Throws an exception if the current logged-in user is a demo user.
   */
  @Override
  public void checkRestrictedUser()
  {
    if (isRestrictedUser() == true) {
      throw new AccessException("access.exception.demoUserHasNoAccess");
    }
  }

  @Override
  public boolean isRestrictedOrDemoUser()
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (user == null) {
      return false;
    }
    return isRestrictedOrDemoUser(user.getId());
  }

  public boolean isRestrictedOrDemoUser(final Integer userId)
  {
    final PFUserDO user = getUserGroupCache().getUser(userId);
    return isRestrictedOrDemoUser(user);
  }

  public boolean isRestrictedOrDemoUser(final PFUserDO user)
  {
    if (user == null) {
      return false;
    }
    return isRestrictedUser(user) || AccessChecker.isDemoUser(user);
  }

  /**
   * Throws an exception if the current logged-in user is a demo user.
   */
  @Override
  public void checkRestrictedOrDemoUser()
  {
    if (isDemoUser() == true) {
      throw new AccessException("access.exception.demoUserHasNoAccess");
    }
    if (isRestrictedUser() == true) {
      throw new AccessException("access.exception.demoUserHasNoAccess");
    }
  }

  /**
   * @return true if logged-in-user is member of {@link ProjectForgeGroup#FINANCE_GROUP},
   * {@link ProjectForgeGroup#CONTROLLING_GROUP} or {@link ProjectForgeGroup#PROJECT_MANAGER}. Returns also true
   * if user is member of {@link ProjectForgeGroup#ORGA_TEAM} and has the
   */
  @Override
  public boolean hasLoggedInUserAccessToTimesheetsOfOtherUsers()
  {
    final PFUserDO loggedInUser = ThreadLocalUserContext.getUser();
    Validate.notNull(loggedInUser);
    if (isUserMemberOfGroup(loggedInUser, ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP,
        ProjectForgeGroup.PROJECT_MANAGER) == true) {
      return true;
    }
    if (isUserMemberOfGroup(loggedInUser, ProjectForgeGroup.ORGA_TEAM) == true
        && hasRight(loggedInUser, UserRightId.PM_HR_PLANNING, UserRightValue.READONLY, UserRightValue.READWRITE)) {
      return true;
    }
    return false;
  }

  /**
   * @param user
   * @return The user from the UserGroupCache instead of e. g. Session for getting the newest access right values of the
   * user.
   */
  private PFUserDO getUser(final PFUserDO user)
  {
    if (user == null) {
      return null;
    }
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final PFUserDO result = userGroupCache.getUser(user.getId());
    return result;
  }

  @Override
  public boolean hasLoggedInUserAccess(Class<?> entClass, OperationType opType)
  {
    BaseDao<ExtendedBaseDO<Integer>> dao = getBaseDao(entClass);
    switch (opType) {
      case INSERT:
        return dao.hasLoggedInUserInsertAccess();
      case SELECT:
        return dao.hasLoggedInUserSelectAccess(false);
      case UPDATE:
      case DELETE:
      default:
        throw new IllegalArgumentException("Cannot determine delete access for class: " + entClass.getName());

    }
  }

  private BaseDao<ExtendedBaseDO<Integer>> getBaseDao(Class<?> entClass)
  {
    return fallbackBaseDaoService.getBaseDaoForEntity((Class) entClass);
  }

  @Override
  public IUserRightId getRightIdFromEntity(Class<?> entClass)
  {
    AUserRightId rightId = entClass.getAnnotation(AUserRightId.class);

    if (rightId != null) {
      IUserRightId ret = userRights.getRightId(rightId.value());
      return ret;
    }
    BaseDao<ExtendedBaseDO<Integer>> dao = getBaseDao(entClass);
    UserRight userRight = dao.getUserRight();
    if (userRight == null) {
      throw new IllegalArgumentException(
          "No UserRight for entity: " + entClass.getName() + " via dao: " + dao.getClass().getName());
    }
    return userRight.getId();

  }

  @Override
  public boolean hasLoggedInUserHistoryAccess(Class<?> entClass)
  {
    BaseDao<ExtendedBaseDO<Integer>> dao = getBaseDao(entClass);
    return dao.hasLoggedInUserHistoryAccess(false);
  }
}
