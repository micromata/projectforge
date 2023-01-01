/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.timesheet;

import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;

/**
 */
public class TimesheetListFilter extends TimesheetFilter
{
  private static final long serialVersionUID = -7685135320311389741L;

  @Override
  public TimesheetListFilter reset()
  {
    super.reset();
    setUserId(ThreadLocalUserContext.getUserId());
    final DateHolder date = new DateHolder(DatePrecision.DAY);
    date.setBeginOfWeek();
    setStartTime(date.getUtilDate());
    date.setEndOfWeek();
    setStopTime(date.getUtilDate());
    return this;
  }
}
