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

class BigCalendarEvent(val title: String?,
                       val start: Date,
                       val end: Date,
                       val allDay: Boolean? = null,
                       val desc: String? = null,
                       val location: String? = null,
                       val tooltip: String? = null,
                       val formattedDuration: String? = null,
                       val outOfRange: Boolean? = null,
                       val fgColor: String? = null,
                       val bgColor: String? = null,
                       val cssClass: String? = null,
                       val category: String,
                       val readOnly: Boolean = false,
                       /**
                        * For subscribed events.
                        */
                       val uid: String? = null,
                       /**
                        * The db id of the object (team event, address (birthday) etc.)
                        */
                       val dbId: Int? = null) {
    /**
     * Must be unique in the list of events. The index of the list will be used: 'e-1', 'e-2', ...
     * Will be set by [CalendarServicesRest].
     */
   internal var key : String? = null
}
