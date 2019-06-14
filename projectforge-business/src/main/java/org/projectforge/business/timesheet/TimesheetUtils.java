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

package org.projectforge.business.timesheet;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.projectforge.framework.time.DateHolder;

/**
 * Provides some helper methods.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TimesheetUtils
{
  /**
   * Analyzes the given time-sheets. The time-sheets will be filtered (by given day and given user).
   * @param timesheets
   * @param day
   * @param userId
   * @return
   */
  public static TimesheetStats getStats(final Collection<TimesheetDO> timesheets, final Date day, final Integer userId)
  {
    final DateHolder dh = new DateHolder(day).setBeginOfDay();
    final Date startDate = dh.getDate();
    final Date stopDate = dh.add(Calendar.DAY_OF_MONTH, 1).getDate();
    return getStats(timesheets, startDate, stopDate, userId);
  }


  /**
   * Analyzes the given time-sheets. The time-sheets will be filtered (by given time period and given user).
   * @param timesheets
   * @param from
   * @param to
   * @param userId
   * @return
   */
  public static TimesheetStats getStats(final Collection<TimesheetDO> timesheets, final Date from, final Date to, final Integer userId)
  {
    if (timesheets == null || timesheets.size() == 0) {
      return null;
    }
    final TimesheetStats stats = new TimesheetStats(from, to);
    for (final TimesheetDO timesheet : timesheets) {
      if (userId != timesheet.getUserId()) {
        continue;
      }
      stats.add(timesheet);
    }
    return stats;
  }
}
