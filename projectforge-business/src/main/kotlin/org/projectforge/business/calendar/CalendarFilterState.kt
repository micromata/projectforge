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

import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import org.projectforge.framework.time.PFDateTime
import java.time.LocalDate
import java.util.*

/**
 * The user's current calendar state to display. This state will be persisted per user. It contain the recent
 * displayed date range viewed by the user.
 */
class CalendarFilterState {
    @XStreamAsAttribute
    var startDate: LocalDate? = null

    //@XStreamAsAttribute
    //var firstHour: Int? = 8

    //@XStreamAsAttribute
    //var slot30: Boolean? = null

    @XStreamAsAttribute
    var view: CalendarView? = null

    /**
     * Updates the fields and ensures, that for month view, the startDate will be the first day of the month:
     * 28/3/19 -> 1/4/19, 1/4/19 -> 1/4/19, etc.
     */
    fun updateCalendarFilter(startDate: Date?,
                             view: CalendarView?) {
        if (startDate != null) {
            var startDay = PFDateTime.from(startDate)!!.localDate
            if (view == CalendarView.MONTH && startDay.dayOfMonth != 1) {
                // Adjusting the begin of month (startDate might be a day of the end of the previous month, if shown.
                startDay = startDay.withDayOfMonth(1).plusMonths(1)
            }
            this.startDate = startDay
        }
        if (view != null) {
            this.view = view
        }
    }
}
