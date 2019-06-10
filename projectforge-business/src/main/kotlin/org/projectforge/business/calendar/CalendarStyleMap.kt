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

package org.projectforge.business.calendar

import org.projectforge.business.calendar.TeamCalendar.Companion.BIRTHDAYS_ALL_CAL_ID
import org.projectforge.business.calendar.TeamCalendar.Companion.BIRTHDAYS_FAVS_CAL_ID

/**
 * Persist the styles of the calendarIds for the user.
 *
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
class CalendarStyleMap {
    /**
     * Colors for the calendarIds by calendar id.
     */
    private val styles = mutableMapOf<Int, CalendarStyle>()

    val birthdaysFavoritesStyle: CalendarStyle
        get() = get(BIRTHDAYS_FAVS_CAL_ID)!!

    val birthdaysAllStyle: CalendarStyle
        get() = get(BIRTHDAYS_ALL_CAL_ID)!!

    fun contains(calendarId: Int): Boolean {
        return styles.containsKey(calendarId)
    }

    fun add(calendarId: Int, style: CalendarStyle) {
        styles.put(calendarId, style)
    }

    fun get(calendarId: Int?): CalendarStyle? {
        if (calendarId == null) return null
        var style = styles[calendarId]
        if (style == null) {
            if (calendarId == BIRTHDAYS_FAVS_CAL_ID) {
                style = CalendarStyle(bgColor = "#06790e")
                add(BIRTHDAYS_FAVS_CAL_ID, style)
            } else if (calendarId == BIRTHDAYS_ALL_CAL_ID) {
                style = CalendarStyle(bgColor = "#ffffff")
                add(BIRTHDAYS_ALL_CAL_ID, style)
            }
        }
        return style
    }
}
