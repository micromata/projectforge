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

package org.projectforge.business.meb;

import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserGroupsRight;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * 
 * @author Kai Reinhard (k.reinhard@me.de)
 * 
 */
public class MebRight extends UserRightAccessCheck<MebEntryDO>
{

  private static final long serialVersionUID = 2985751765063922520L;

  public MebRight(AccessChecker accessChecker)
  {
    super(accessChecker, UserRightId.MISC_MEB, UserRightCategory.MISC, UserRightValue.TRUE);
  }

  /**
   * Every user can insert new MEB entries.
   * 
   * @see org.projectforge.business.user.UserRightAccessCheck#hasInsertAccess()
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user)
  {
    return true;
  }

  /**
   * @return true.
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(org.projectforge.framework.access.AccessChecker,
   *      org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user)
  {
    return true;
  }

  @Override
  public boolean hasSelectAccess(final PFUserDO user, final MebEntryDO obj)
  {
    if (obj == null) {
      return true;
    }
    if (obj.getOwner() == null) {
      return accessChecker.isUserMemberOfAdminGroup(user);
    } else {
      return accessChecker.userEquals(user, obj.getOwner());
    }
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final MebEntryDO obj, final MebEntryDO oldObj,
      final OperationType operationType)
  {
    if (obj == null) {
      return false;
    }
    if (obj.getOwner() == null) {
      return accessChecker.isUserMemberOfAdminGroup(user);
    } else {
      return accessChecker.userEquals(user, obj.getOwner());
    }
  }

  @Override
  public boolean hasUpdateAccess(final PFUserDO user, MebEntryDO obj, MebEntryDO oldObj)
  {
    if (oldObj != null && accessChecker.isUserMemberOfAdminGroup(user) == true
        && oldObj.getOwner() == null) {
      // Otherwise an admin couldn't assign unassigned entries:
      return true;
    }
    return hasAccess(user, obj, oldObj, OperationType.UPDATE);
  }

  /**
   * @param userGroupCache
   * @param user
   * @param value
   * @return Always true.
   * @see UserGroupsRight#matches(UserGroupCache, PFUserDO, UserRightValue)
   */
  @Override
  public boolean matches(final UserGroupCache userGroupCache, final PFUserDO user, final UserRightValue value)
  {
    return true;
  }
}
