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

package org.projectforge.business.calendar

/**
 * Vales for Fullcalendar: dayGridMonth, timeGridWeek, timeGridWorkingWeek, timeGridDay, dayGridWeek, listWeek, listMonth
 */
enum class CalendarView(val key: String) {
  MONTH("dayGridMonth"),

  WORK_MONTH("dayGridWorkingMonth"),

  WEEK("timeGridWeek"),

  WORK_WEEK("timeGridWorkingWeek"),

  DAY("timeGridDay"),

  WEEK_LIST("dayGridWeek"),

  AGENDA("listWeek"),

  MONTH_AGENDA("listMonth");

  companion object {
    fun from(name: String?): CalendarView {
      if (name == null) {
        return MONTH // Default view.
      }
      return values().find { it.name == name || it.key == name } ?: MONTH
    }
  }
}
