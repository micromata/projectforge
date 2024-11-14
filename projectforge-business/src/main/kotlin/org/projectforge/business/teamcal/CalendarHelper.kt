/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal

import org.apache.commons.lang3.StringUtils
import org.projectforge.business.PfCaches
import org.projectforge.business.timesheet.TimesheetDO

object CalendarHelper {
  @JvmStatic
  fun getTitle(timesheet: TimesheetDO): String {
    PfCaches.instance.initialize(timesheet)
    val kost2 = timesheet.kost2
    val task = timesheet.task
    val buf = StringBuilder()
    if (kost2 == null) {
      buf.append(StringUtils.abbreviate(task?.title ?: "", 60))
    } else {
      val b2 = StringBuilder()
      val projekt = kost2.projekt
      if (projekt != null) {
        if (StringUtils.isNotBlank(projekt.identifier)) {
          b2.append(projekt.identifier)
        } else {
          b2.append(projekt.name)
        }
      } else {
        b2.append(kost2.description)
      }
      buf.append(StringUtils.abbreviate(b2.toString(), 60))
    }
    return buf.toString()
  }

  @JvmStatic
  fun getDescription(timesheet: TimesheetDO): String {
    val location = timesheet.location
    val description = timesheet.getShortDescription()
    val sb = StringBuilder()
    if (StringUtils.isNotBlank(location)) {
      sb.append(StringUtils.abbreviate(location, 60))
      if (StringUtils.isNotBlank(description)) {
        sb.append("\n")
      }
    }
    sb.append(StringUtils.abbreviate(description, 60))
    return sb.toString()
  }
}
