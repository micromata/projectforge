/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import java.util.*

/**
 * Persist the settings of one named filter entry.
 *
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
class CalendarsDisplayFilter {

    val calendarDisplayProperties = mutableListOf<DisplayedCalendarProperties>()

    @XStreamAsAttribute
    var name: String? = null

    /**
     * New items created in the calendar will be assumed as entries of this calendar. If null, then the creation
     * page for new time sheets is instantiated.
     */
    @XStreamAsAttribute
    var defaultCalendarId: Int? = null

    @XStreamAsAttribute
    var showBirthdays: Boolean? = null

    @XStreamAsAttribute
    var showStatistics: Boolean? = null

    /**
     * Display the time sheets of the user with this id. If null, no time sheets are displayed.
     */
    @XStreamAsAttribute
    var timesheetUserId: Int? = null

    /**
     * If true, own time sheets are displayed. It depends on the user rights if [showTimesheets] or [timesheetUserId] is used.
     */
    @XStreamAsAttribute
    var showTimesheets: Boolean? = null

    @XStreamAsAttribute
    var showBreaks: Boolean? = true

    @XStreamAsAttribute
    var showPlanning: Boolean? = null
}
