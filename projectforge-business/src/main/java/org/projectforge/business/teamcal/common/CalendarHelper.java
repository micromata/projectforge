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

package org.projectforge.business.teamcal.common;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.utils.HtmlHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class CalendarHelper
{
  public static String getTitle(final TimesheetDO timesheet)
  {
    final Kost2DO kost2 = timesheet.getKost2();
    final TaskDO task = timesheet.getTask();
    if (kost2 == null) {
      return (task != null && task.getTitle() != null) ? HtmlHelper.escapeXml(task.getTitle()) : "";
    }
    final StringBuffer buf = new StringBuffer();
    final StringBuffer b2 = new StringBuffer();
    final ProjektDO projekt = kost2.getProjekt();
    if (projekt != null) {
      if (StringUtils.isNotBlank(projekt.getIdentifier()) == true) {
        b2.append(projekt.getIdentifier());
      } else {
        b2.append(projekt.getName());
      }
    } else {
      b2.append(kost2.getDescription());
    }
    buf.append(StringUtils.abbreviate(b2.toString(), 30));
    return buf.toString();
  }

  public static int getCalenderData(final Date date, int calendarData)
  {
    if(date != null) {
      Calendar tmp_date = new GregorianCalendar();
      tmp_date.setTime(date);
      return tmp_date.get(calendarData);
    } else {
      return -1;
    }
  }
}
