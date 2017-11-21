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

package org.projectforge.web.teamcal.rest;

import org.apache.commons.lang3.ObjectUtils;
import org.projectforge.business.converter.DOConverter;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.model.rest.CalendarObject;

/**
 * For conversion of TeamCalDO to CalendarObject.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TeamCalDOConverter
{
  public static CalendarObject getCalendarObject(final TeamCalDO src, UserRightService userRights)
  {
    if (src == null) {
      return null;
    }
    final Integer userId = ThreadLocalUserContext.getUserId();
    final CalendarObject cal = new CalendarObject();
    DOConverter.copyFields(cal, src);
    cal.setTitle(src.getTitle());
    cal.setDescription(src.getDescription());
    cal.setExternalSubscription(src.isExternalSubscription());
    final TeamCalRight right = (TeamCalRight) userRights.getRight(UserRightId.PLUGIN_CALENDAR);
    cal.setMinimalAccess(right.hasMinimalAccess(src, userId));
    cal.setReadonlyAccess(right.hasReadonlyAccess(src, userId));
    cal.setFullAccess(right.hasFullAccess(src, userId));
    cal.setOwner(ObjectUtils.equals(userId, src.getOwnerId()));
    return cal;
  }
}
