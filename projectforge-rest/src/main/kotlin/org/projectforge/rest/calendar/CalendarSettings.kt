/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.calendar

/**
 * Settings for the calendar (independent of filter).
 */
class CalendarSettings(
  // var contrastMode: Boolean? = null,
  var timesheetsColor: String? = null,
  var timesheetBreaksColor: String? = null,
  var timesheetStatsColor: String? = null,
  var vacationColor: String? = null,
) {
  companion object {
    const val TIMESHEETS_DEFAULT_COLOR = "#2F65C8"
    const val TIMESHEETS_BREAKS_DEFAULT_COLOR = "#F9F9F9"
    const val TIMESHEETS_STATS_DEFAULT_COLOR = "#2F65C8"
    const val VACATION_DEFAULT_COLOR = "#F6D9AB"
  }
}
