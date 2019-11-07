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

package org.projectforge.framework.access;

import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRight;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;

/**
 * This class contains some helper methods for evaluation of user and group access'.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */

public interface AccessChecker
{
  /**
   * Checks if the user is an admin user (member of admin group). If not, an AccessException will be thrown.
   */
  void checkIsLoggedInUserMemberOfAdminGroup();

  /**
   * @see org.projectforge.business.user.UserGroupCache#isUserMemberOfAdminGroup(java.lang.Integer)
   */
  boolean isUserMemberOfAdminGroup(PFUserDO user);

  /**
   * @see org.projectforge.business.user.UserGroupCache#isUserMemberOfAdminGroup(java.lang.Integer)
   */
  boolean isUserMemberOfAdminGroup(TenantDO tenant, PFUserDO user);

  boolean isUserMemberOfAdminGroup(PFUserDO user, boolean throwException);

  boolean isLoggedInUserMemberOfAdminGroup();

  boolean isRestrictedUser();

  boolean isRestrictedUser(final Integer userId);

  /**
   * Throws an exception if the current logged-in user is a demo user.
   */
  void checkRestrictedUser();

  boolean isRestrictedUser(final PFUserDO user);

  /**
   * Throws an exception if the current logged-in user is a demo user.
   */
  void checkRestrictedOrDemoUser();

  boolean isDemoUser();

  boolean isDemoUser(final Integer userId);

  static boolean isDemoUser(final PFUserDO user)
  {
    if (user == null) {
      return false;
    }
    if (!"demo".equals(user.getUsername())) {
      return false;
    }
    return true;
  }

  public boolean isRestrictedOrDemoUser();

  /**
   * Checks if the user of the ThreadLocalUserContext (logged in user) is member at least of one of the given groups.
   *
   * @param groups
   */
  boolean isLoggedInUserMemberOfGroup(ProjectForgeGroup... groups);

  /**
   * Checks if the user of the ThreadLocalUserContext (logged in user) is member at least of one of the given groups.
   *
   * @param throwException default false.
   * @param groups
   * @see #isUserMemberOfGroup(PFUserDO, ProjectForgeGroup...)
   */
  boolean isUserMemberOfGroup(final PFUserDO user, final boolean throwException,
      final ProjectForgeGroup... groups);

  /**
   * Checks if the user is in one of the given groups. If not, an AccessException will be thrown.
   */
  void checkIsUserMemberOfGroup(final PFUserDO user, final ProjectForgeGroup... groups);

  /**
   * Checks if the given user is at least member of one of the given groups.
   *
   * @param user
   * @param groups
   */
  boolean isUserMemberOfGroup(final PFUserDO user, final ProjectForgeGroup... groups);

  /**
   * Checks if the user is in one of the given groups. If not, an AccessException will be thrown.
   */
  void checkIsLoggedInUserMemberOfGroup(final ProjectForgeGroup... groups);

  /**
   * Checks the availability and the demanded value of the right for the context user. The right will be checked itself
   * on required constraints, e. g. if assigned groups required.
   *
   * @param rightId
   * @param values         At least one of the values should match.
   * @param throwException
   */
  boolean hasLoggedInUserRight(final IUserRightId rightId, final boolean throwException,
      final UserRightValue... values);

  /**
   * Checks the availability and the demanded value of the right for the context user. The right will be checked itself
   * on required constraints, e. g. if assigned groups required.
   *
   * @param origUser           Check the access for the given user instead of the logged-in user.
   * @param rightId
   * @param values         At least one of the values should match.
   * @param throwException
   * @return
   */
  boolean hasRight(final PFUserDO origUser, final IUserRightId rightId, final boolean throwException,
      final UserRightValue... values);

  /**
   * Gets the UserRight and calls {@link UserRight#isAvailable(UserGroupCache, PFUserDO)}.
   *
   * @param rightId
   * @return
   */
  boolean isAvailable(final PFUserDO user, final IUserRightId rightId);

  /**
   * Throws now exception if the right check fails.
   */
  boolean hasRight(final PFUserDO user, final IUserRightId rightId, final UserRightValue... values);

  /**
   *
   * @param rightId
   */
  boolean checkUserRight(final PFUserDO user, final IUserRightId rightId, final UserRightValue... values);

  boolean checkLoggedInUserRight(final IUserRightId rightId, final UserRightValue... values);

  /**
   * Tests for every group the user is assigned to, if the given permission is given.
   *
   * @return true, if the user owns the required permission, otherwise false.
   */
  boolean hasPermission(final PFUserDO user, final Integer taskId, final AccessType accessType,
      final OperationType operationType,
      final boolean throwException);

  /**
   * @param origUser       Check the access for the given user instead of the logged-in user.
   * @param rightId
   * @param obj
   * @param oldObj
   * @param operationType
   * @param throwException
   */

  boolean hasAccess(PFUserDO origUser, IUserRightId rightId, Object obj, Object oldObj,
      OperationType operationType, boolean throwException);

  boolean hasInsertAccess(PFUserDO user, IUserRightId rightId, boolean throwException);

  boolean hasLoggedInUserInsertAccess(final IUserRightId rightId, final Object obj,
      final boolean throwException);

  boolean hasLoggedInUserReadAccess(final IUserRightId rightId, final boolean throwException);

  boolean hasLoggedInUserWriteAccess(final IUserRightId rightId, final boolean throwException);

  boolean hasLoggedInUserAccess(Class<?> entClass, OperationType opType);

  IUserRightId getRightIdFromEntity(Class<?> entClass);

  /**
   * Use context user (logged-in user).
   */
  boolean hasLoggedInUserSelectAccess(final IUserRightId rightId, final boolean throwException);

  boolean hasLoggedInUserSelectAccess(final IUserRightId rightId, final Object obj,
      final boolean throwException);

  boolean hasReadAccess(final PFUserDO user, final IUserRightId rightId, final boolean throwException);

  boolean hasWriteAccess(final PFUserDO user, final IUserRightId rightId, final boolean throwException);

  boolean hasLoggedInUserHistoryAccess(final IUserRightId rightId, final Object obj,
      final boolean throwException);

  boolean hasLoggedInUserHistoryAccess(Class<?> entClass);

  boolean hasHistoryAccess(PFUserDO origUser, IUserRightId rightId, Object obj,
      boolean throwException);

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
  boolean hasLoggedInUserAccess(final IUserRightId rightId, final Object obj, final Object oldObj,
      final OperationType operationType,
      final boolean throwException);

  /**
   * Compares the two given users on equality. The pk's will be compared. If one or more user's or pk's are null, false
   * will be returned.
   *
   * @param u1
   * @param u2
   * @return true, if both user pk's are not null and equal.
   */

  boolean userEquals(PFUserDO u1, PFUserDO u2);

  /**
   * Gets the user from the ThreadLocalUserContext and compares the both user.
   *
   * @param user
   * @return
   * @see AccessChecker#userEquals(PFUserDO, PFUserDO)
   */
  boolean userEqualsToContextUser(final PFUserDO user);

  /**
   * Is the current context user in at minimum one group of the groups assigned to the given user?
   *
   * @param user2
   * @return
   * @deprecated wrong place.
   */
  @Deprecated
  boolean areUsersInSameGroup(final PFUserDO user1, final PFUserDO user2);

  /**
   * @return
   * @deprecated wrong place.
   */
  @Deprecated
  boolean hasLoggedInUserAccessToTimesheetsOfOtherUsers();
}
