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
class CalendarSettings {
  // var contrastMode: Boolean? = null,
  var timesheetsColor: String? = TIMESHEETS_DEFAULT_COLOR
  var timesheetBreaksColor: String? = TIMESHEETS_BREAK_DEFAULT_COLOR
  var timesheetStatsColor: String? = TIMESHEETS_STATS_DEFAULT_COLOR
  var vacationsColor: String? = VACATIONS_DEFAULT_COLOR

  val timesheetsColorOrDefault: String
    get() = timesheetsColor ?: TIMESHEETS_DEFAULT_COLOR

  val timesheetsBreaksColorOrDefault: String
    get() = timesheetBreaksColor ?: TIMESHEETS_BREAK_DEFAULT_COLOR

  val timesheetsStatsColorOrDefault: String
    get() = timesheetStatsColor ?: TIMESHEETS_STATS_DEFAULT_COLOR

  val vacationsColorOrDefault: String
    get() = vacationsColor ?: VACATIONS_DEFAULT_COLOR

  /**
   * Removes default values before saving as user preference.
   */
  fun copyWithoutDefaultsFrom(src: CalendarSettings) {
    this.timesheetsColor = getValueOrNull(src.timesheetsColor, TIMESHEETS_DEFAULT_COLOR)
    this.timesheetBreaksColor = getValueOrNull(src.timesheetBreaksColor, TIMESHEETS_BREAK_DEFAULT_COLOR)
    this.timesheetStatsColor = getValueOrNull(src.timesheetStatsColor, TIMESHEETS_STATS_DEFAULT_COLOR)
    this.vacationsColor = getValueOrNull(src.vacationsColor, VACATIONS_DEFAULT_COLOR)
  }

  private fun getValueOrNull(value: String?, default: String): String? {
    value ?: return null
    return if (value == default) {
      null
    } else {
      value
    }
  }

  companion object {
    private const val TIMESHEETS_DEFAULT_COLOR = "#2F65C8"
    private const val TIMESHEETS_BREAK_DEFAULT_COLOR = "#F9F9F9"
    private const val TIMESHEETS_STATS_DEFAULT_COLOR = "#2F65C8"
    private const val VACATIONS_DEFAULT_COLOR = "#F6D9AB"
  }
}
