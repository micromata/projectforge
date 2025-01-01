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
 * Team calendar object extended by CalendarStyle and visibility.
 */
class StyledTeamCalendar(teamCalendar: TeamCalendar?,
                         var style: CalendarStyle? = null,
                         val visible: Boolean = true)
    : TeamCalendar(teamCalendar?.id, teamCalendar?.title, teamCalendar?.access) {

    companion object {
        /**
         * Add the styles of the styleMap to the returned calendars.
         */
        fun map(calendars : List<TeamCalendar>, styleMap : CalendarStyleMap) : List<StyledTeamCalendar> {
            return calendars.map { cal ->
                StyledTeamCalendar(calendars.find { it.id == cal.id },
                        style = styleMap.get(cal.id)) // Add the styles of the styleMap to the exported calendar.
            }
        }
    }
}
