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

package org.projectforge.business.teamcal.event.right;

import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.WicketSupport;

import java.util.Objects;

/**
 * Every user has access to own to-do's or to-do's he's assigned to. All other users have access if the to-do is
 * assigned to a task and the user has the task access.
 *
 * @author Kai Reinhard (k.reinhard@me.de)
 *
 */
public class TeamEventRight extends UserRightAccessCheck<TeamEventDO>
{
  private static final long serialVersionUID = 4076952301071024285L;

  private final TeamCalRight teamCalRight;

  public TeamEventRight()
  {
    super(UserRightId.PLUGIN_CALENDAR_EVENT, UserRightCategory.PLUGINS,
        UserRightValue.TRUE);
    teamCalRight = new TeamCalRight();
  }

  /**
   * General select access.
   *
   * @return true
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user)
  {
    return true;
  }

  /**
   * @return true if user is assignee or reporter. If not, the task access is checked.
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final TeamEventDO obj)
  {
    final TeamCalDO calendar = obj.getCalendar();
    if (calendar == null) {
      return false;
    }
    if (Objects.equals(user.getId(), calendar.getOwnerId())) {
      // User has full access to it's own calendars.
      return true;
    }
    final Long userId = user.getId();
    if (teamCalRight.hasFullAccess(calendar, userId)
        || teamCalRight.hasReadonlyAccess(calendar, userId)) {
      return true;
    } else if (teamCalRight.hasMinimalAccess(calendar, userId)) {
      // Clear fields for users with minimal access.
      obj.clearFields();
      return true;
    }
    return false;
  }

  /**
   * General insert access.
   *
   * @return true
   * @see org.projectforge.business.user.UserRightAccessCheck#hasInsertAccess(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user)
  {
    return true;
  }

  /**
   * Same as {@link #hasUpdateAccess(PFUserDO, TeamEventDO, TeamEventDO)}
   *
   * @see org.projectforge.business.user.UserRightAccessCheck#hasInsertAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user, final TeamEventDO obj)
  {
    return hasUpdateAccess(user, obj, null);
  }

  /**
   * Same as {@link #hasUpdateAccess(PFUserDO, TeamEventDO, TeamEventDO)}
   *
   * @see org.projectforge.business.user.UserRightAccessCheck#hasDeleteAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean hasDeleteAccess(final PFUserDO user, final TeamEventDO obj, final TeamEventDO oldObj)
  {
    return hasUpdateAccess(user, obj, oldObj);
  }

  /**
   * Owners of the given calendar and users with full access have update access to the given calendar: obj.getCalendar().
   *
   * @see org.projectforge.business.user.UserRightAccessCheck#hasUpdateAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final TeamEventDO obj, final TeamEventDO oldObj)
  {
    if (obj == null) {
      return false;
    }
    final TeamCalDO calendar = obj.getCalendar();
    if (calendar == null) {
      return false;
    }
    return hasUpdateAccess(user, calendar);
  }

  public boolean hasUpdateAccess(final PFUserDO user, final TeamCalDO calendar)
  {
    if (calendar != null && calendar.getExternalSubscription()) {
      return false;
    }
    if (Objects.equals(user.getId(), calendar.getOwnerId())) {
      // User has full access to it's own calendars.
      return true;
    }
    final Long userId = user.getId();
    if (teamCalRight.hasFullAccess(calendar, userId) || WicketSupport.getAccessChecker().isDemoUser()) {
      return true;
    }
    return false;
  }

  /**
   * Owners of the given calendar and users with full and read-only access have update access to the given calendar:
   * obj.getCalendar().
   *
   * @see org.projectforge.business.user.UserRightAccessCheck#hasHistoryAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final TeamEventDO obj)
  {
    if (obj == null) {
      return true;
    }
    final TeamCalDO calendar = obj.getCalendar();
    if (calendar == null) {
      return false;
    }
    if (Objects.equals(user.getId(), calendar.getOwnerId())) {
      // User has full access to it's own calendars.
      return true;
    }
    final Long userId = user.getId();
    if (teamCalRight.hasFullAccess(calendar, userId)
        || teamCalRight.hasReadonlyAccess(calendar, userId)) {
      return true;
    }
    return false;
  }

  public boolean hasMinimalAccess(final TeamEventDO event, final Long userId)
  {
    if (event.getCalendar() == null) {
      return true;
    }
    return teamCalRight.hasMinimalAccess(event.getCalendar(), userId);
  }
}
