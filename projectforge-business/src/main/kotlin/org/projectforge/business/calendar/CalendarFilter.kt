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

import org.projectforge.business.teamcal.filter.TemplateEntry
import org.projectforge.favorites.AbstractFavorite
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.util.*

/**
 * Persist the settings of one named filter entry. The user may configure a list of filters and my switch the active
 * calendar filter.
 *
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
class CalendarFilter(name: String? = null,
                     id: Int? = null,
                     /**
                      * New items created in the calendar will be assumed as entries of this calendar. If null, then the creation
                      * page for new time sheets is instantiated.
                      */
                     var defaultCalendarId: Int? = null,

                     /**
                      * Grid size of the calendar to display in minutes (60 should be dividable by step).
                      */
                     var gridSize: Int = 30,

                     var showBirthdays: Boolean? = null,

                     /**
                      * Not yet supported: Statistics will always be displayed, if time sheets will be displayed.
                      */
                     var showStatistics: Boolean? = true,

                     var otherTimesheetUsersEnabled: Boolean = false,
                     /**
                      * Display the time sheets of the user with this id. If null, no time sheets are displayed.
                      */
                     var timesheetUserId: Int? = null,

                     /**
                      * Not yet supported.
                      */
                     var showBreaks: Boolean? = false,

                     /**
                      * Not yet supported.
                      */
                     var showPlanning: Boolean? = null)
    : AbstractFavorite(name, id) {

    var calendarIds = mutableSetOf<Int>()

    var invisibleCalendars = mutableSetOf<Int>()

    /**
     * Makes a deep copy of all values.
     * @return this for chaining.
     */
    fun copyFrom(src: CalendarFilter): CalendarFilter {
        this.name = src.name
        this.id = src.id
        this.defaultCalendarId = src.defaultCalendarId
        this.showBirthdays = src.showBirthdays
        this.showStatistics = src.showStatistics
        this.timesheetUserId = src.timesheetUserId
        this.gridSize = src.gridSize
        this.showBreaks = src.showBreaks
        this.showPlanning = src.showPlanning
        this.calendarIds = mutableSetOf()
        this.calendarIds.addAll(src.calendarIds)
        this.invisibleCalendars = mutableSetOf()
        this.invisibleCalendars.addAll(src.invisibleCalendars)
        return this
    }

    fun addCalendarId(calendarId: Int) {
        calendarIds.add(calendarId)
        invisibleCalendars.remove(calendarId) // New added calendars should be visible.
    }

    fun removeCalendarId(calendarId: Int) {
        calendarIds.remove(calendarId)
        invisibleCalendars.remove(calendarId)
    }

    fun setVisibility(calendarId: Int, visible: Boolean) {
        if (visible) {
            invisibleCalendars.remove(calendarId)
        } else {
            invisibleCalendars.add(calendarId)
        }
        tidyUp()
    }

    fun isVisible(calendarId: Int): Boolean {
        return calendarIds.contains(calendarId) && !invisibleCalendars.contains(calendarId)
    }

    /**
     * This method tidies up the list of invisible calendars by
     * removing invisible calendars not contained in the main calendar set.
     */
    @Suppress("SENSELESS_COMPARISON")
    fun tidyUp() {
        invisibleCalendars.removeIf { !calendarIds.contains(it) } // Tidy up: remove invisible ids if not in main list.
    }

    /**
     * The sets [calendarIds] and [invisibleCalendars] may contain a null value after deserialization. This will be removed by calling this
     * function. [tidyUp] will also be called.
     */
    @Suppress("SENSELESS_COMPARISON")
    fun afterDeserialization() {
        val nullValue: Int? = null
        if (calendarIds == null) {
            calendarIds = mutableSetOf() // Might be null after deserialization.
        } else {
            calendarIds.remove(nullValue) // Might occur after deserialization.
        }
        if (invisibleCalendars == null) {
            invisibleCalendars = mutableSetOf() // Might be null after deserialization.
        } else {
            invisibleCalendars.remove(nullValue) // Might occur after deserialization.
        }
        tidyUp()
    }

    fun isModified(other: CalendarFilter): Boolean {
        if (!Objects.equals(this.name, other.name)) return true
        if (this.id != other.id) return true
        if (this.defaultCalendarId != other.defaultCalendarId) return true
        if (this.showBirthdays != other.showBirthdays) return true
        if (this.showStatistics != other.showStatistics) return true
        if (this.timesheetUserId != other.timesheetUserId) return true
        if (this.gridSize != other.gridSize) return true
        if (this.showBreaks != other.showBreaks) return true
        if (this.showPlanning != other.showPlanning) return true
        if (isModified(this.calendarIds, other.calendarIds)) return true
        if (isModified(this.invisibleCalendars, other.invisibleCalendars)) return true
        return false
    }

    private fun isModified(set1: Set<Int>, set2: Set<Int>): Boolean {
        set1.forEach {
            if (!set2.contains(it))
                return true
        }
        set2.forEach {
            if (!set1.contains(it))
                return true
        }
        return false
    }

    companion object {
        // LEGACY STUFF:

        /**
         * For re-using legacy filters (from ProjectForge version up to 6, Wicket-Calendar).
         */
        internal fun copyFrom(templateEntry: TemplateEntry?): CalendarFilter {
            val filter = CalendarFilter()
            if (templateEntry != null) {
                filter.defaultCalendarId = templateEntry.defaultCalendarId
                filter.name = templateEntry.name
                filter.showBirthdays = templateEntry.isShowBirthdays
                filter.showBreaks = templateEntry.isShowBreaks
                filter.showPlanning = templateEntry.isShowPlanning
                filter.showStatistics = templateEntry.isShowStatistics
                filter.timesheetUserId = templateEntry.timesheetUserId
                if (templateEntry.isShowTimesheets)
                    filter.timesheetUserId = ThreadLocalUserContext.getUserId()
                templateEntry.calendarProperties?.forEach {
                    filter.addCalendarId(it.calId)
                }
                filter.calendarIds.forEach {
                    filter.setVisibility(it, templateEntry.isVisible(it))
                }
            }
            return filter
        }
    }
}
