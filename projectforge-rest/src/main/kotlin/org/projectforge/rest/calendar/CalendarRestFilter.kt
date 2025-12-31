/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import java.util.*

/**
 * CalendarFilter to request calendar events as POST param. Dates are required as JavaScript ISO date time strings
 * (start and end).
 */
class CalendarRestFilter(
  var start: Date? = null,
  /** Optional, if view isn't given. */
  var view: String? = null,
  var end: Date? = null,
  var timesheetUserId: Long? = null,
  /**
   * If true, breaks between time sheets of a day will be displayed. If the user clicks on a break, a time sheet
   * with the start and stop time of the break could easily be created.
   */
  var showBreaks: Boolean? = null,
  /**
   * Check box for enabling and disabling vacation entries of employees.
   */
  //var showVacations: Boolean = false,

  /**
   * All vacations of any employee assigned to at least one of this
   * vacationGroups will be displayed.
   */
  var vacationGroupIds: MutableSet<Long?>? = null,

  /**
   * All vacations of the given employees (by user) will be displayed.
   * Null items should only occur on (de)serialization issues.
   */
  var vacationUserIds: MutableSet<Long?>? = null,
  /**
   *  The team calendarIds to display.
   * Null items should only occur on (de)serialization issues.
   */
  var activeCalendarIds: MutableSet<Long?>? = null,
  /**
   * If true, then calendars in the invisibleCalendarIds set of the current filter will be hidden.
   * Default is false (all active calendars are displayed).
   * This flag is only used by the React client for hiding active calendars.
   */
  var useVisibilityState: Boolean? = false,
  /**
   * The browsers time zone is needed for BigCalendar if the user's timezone of the server
   * differs from the browsers timezone. BigCalendar doesn't support the setting of a timezone.
   */
  var timeZone: String? = null
) {
  /**
   * The set [activeCalendarIds] may contain a null value after deserialization. This will be removed by calling this
   * function.
   */
  fun afterDeserialization() {
    activeCalendarIds?.removeIf { it == null }
    vacationGroupIds?.removeIf { it == null }
    vacationUserIds?.removeIf { it == null }
  }
}
