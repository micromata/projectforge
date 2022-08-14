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

import org.projectforge.business.calendar.CalendarStyle

/**
 * Settings for the calendar (independent of filter).
 */
class CalendarSettings {
  // var contrastMode: Boolean? = null,
  var timesheetsColor: String? = TIMESHEETS_DEFAULT_COLOR
  var timesheetsBreaksColor: String? = TIMESHEETS_BREAK_DEFAULT_COLOR
  var timesheetsStatsColor: String? = TIMESHEETS_STATS_DEFAULT_COLOR
  var vacationsColor: String? = VACATIONS_DEFAULT_COLOR

  val timesheetsColorOrDefault: String
    get() = timesheetsColor ?: TIMESHEETS_DEFAULT_COLOR

  val timesheetsBreaksColorOrDefault: String
    get() = timesheetsBreaksColor ?: TIMESHEETS_BREAK_DEFAULT_COLOR

  val timesheetsStatsColorOrDefault: String
    get() = timesheetsStatsColor ?: TIMESHEETS_STATS_DEFAULT_COLOR

  val vacationsColorOrDefault: String
    get() = vacationsColor ?: VACATIONS_DEFAULT_COLOR

  /**
   * Removes default values before saving as user preference.
   */
  fun copyWithoutDefaultsFrom(src: CalendarSettings) {
    this.timesheetsColor = getValueOrNull(src.timesheetsColor, TIMESHEETS_DEFAULT_COLOR)
    this.timesheetsBreaksColor = getValueOrNull(src.timesheetsBreaksColor, TIMESHEETS_BREAK_DEFAULT_COLOR)
    this.timesheetsStatsColor = getValueOrNull(src.timesheetsStatsColor, TIMESHEETS_STATS_DEFAULT_COLOR)
    this.vacationsColor = getValueOrNull(src.vacationsColor, VACATIONS_DEFAULT_COLOR)
  }

  /**
   * Gets a clone of the given (stored) calendar settings. Ensures, that all colors are present. Missing colors
   * will be filled by their defined default colors.
   */
  fun cloneWithDefaultValues(): CalendarSettings {
    val clone = CalendarSettings()
    clone.timesheetsColor = timesheetsColorOrDefault
    clone.timesheetsBreaksColor = timesheetsBreaksColorOrDefault
    clone.timesheetsStatsColor = timesheetsStatsColorOrDefault
    clone.vacationsColor = vacationsColorOrDefault
    return clone
  }

  private fun getValueOrNull(value: String?, default: String): String? {
    value ?: return null
    val color = value.trim().lowercase()
    return if (value == default) {
      null
    } else if (CalendarStyle.validateHexCode(color)) {
      // hex code seems to be valid.
      color
    } else {
      // Syntax error in color, ignoring it.
      null
    }
  }

  companion object {
    private const val TIMESHEETS_DEFAULT_COLOR = "#2f65c8"
    private const val TIMESHEETS_BREAK_DEFAULT_COLOR = "#f9f9f9"
    private const val TIMESHEETS_STATS_DEFAULT_COLOR = "#2f65c8"
    private const val VACATIONS_DEFAULT_COLOR = "#f6d9ab"
  }
}
