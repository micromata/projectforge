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

package org.projectforge.rest.calendar

import java.util.*

/**
 * CalendarFilter to request calendar events as POST param. Dates are required as JavaScript ISO date time strings
 * (start and end).
 */
class CalendarRestFilter(var start: Date? = null,
                         /** Optional, if view is given. */
                         var end: Date? = null,
                         /** Will be ignored if end is given. */
                         var view: String? = null,
                         var timesheetUserId: Int? = null,
                         /** The team calendarIds to display. */
                         var activeCalendarIds: MutableSet<Int>? = null,
                         /**
                          * If true, then this filter updates the fields of the user's calendar state (start date and view).
                          * If the user calls the calendar page next time, this properties are restored.
                          * Default is false (the calendar state will not be updated.
                          * This flag is only used by the React client for restoring the states on later views.
                          */
                         var updateState: Boolean? = false,
                         /**
                          * If true, then calendars in the invisibleCalendarIds set of the current filter will be hidden.
                          * Default is false (all active calendars are displayed).
                          * This flag is only used by the React client for hiding active calendars.
                          */
                         var useVisibilityState: Boolean? = false) {
    /**
     * The set [activeCalendarIds] may contain a null value after deserialization. This will be removed by calling this
     * function.
     */
    fun afterDeserialization() {
        val nullValue: Int? = null
        activeCalendarIds?.remove(nullValue)
    }
}
