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

import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import java.time.LocalDate
import java.util.*

class FullCalendarEvent(val title: String?,
                        val start: Date,
                        val end: Date,
                        val allDay: Boolean? = null,
                        val desc: String? = null,
                        val location: String? = null,
                        val tooltip: String? = null,
                        val formattedDuration: String? = null,
                        val outOfRange: Boolean? = null,
                        val textColor: String? = null,
                        val backgroundColor: String? = null,
                        val classNames: String? = null,
                        val category: String,
                        val editable: Boolean = true,
                        /**
                        * For subscribed events.
                        */
                       val uid: String? = null,
                        /**
                        * The db id of the object (team event, address (birthday) etc.)
                        */
                       val dbId: Int? = null) {

    constructor(title: String?,
                start: LocalDate,
                end: LocalDate,
                allDay: Boolean? = null,
                desc: String? = null,
                location: String? = null,
                tooltip: String? = null,
                formattedDuration: String? = null,
                outOfRange: Boolean? = null,
                fgColor: String? = null,
                bgColor: String? = null,
                cssClass: String? = null,
                category: String,
                readOnly: Boolean = false,
                /**
                 * For subscribed events.
                 */
                uid: String? = null,
                /**
                 * The db id of the object (team event, address (birthday) etc.)
                 */
                dbId: Int? = null)
            : this(title, asStartDate(start), asEndDate(end),
            allDay, desc, location, tooltip, formattedDuration, outOfRange, fgColor, bgColor, cssClass, category, readOnly, uid, dbId)

    /**
     * Must be unique in the list of events. The index of the list will be used: 'e-1', 'e-2', ...
     * Will be set by [CalendarServicesRest].
     */
    internal var key: String? = null

    companion object {
        fun asStartDate(start: LocalDate): Date {
            return PFDay.from(start).utilDate
        }

        fun asEndDate(end: LocalDate): Date {
            return PFDateTime.from(end).endOfDay.utilDate
        }

        fun samePeriod(event: FullCalendarEvent, start: LocalDate?, end: LocalDate?): Boolean {
            start ?: return false
            end ?: return false
            return event.start == asStartDate(start) && event.end == asEndDate(end)
        }
    }
}
