/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Kai Reinhard (k.reinhard@micromata.de)
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
import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter
import org.projectforge.business.teamcal.filter.ViewType
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTimeUtils
import java.time.LocalDate

/**
 * The calendar settings of the user to Display.
 */
class CalendarDisplaySettings {
    /**
     * Will be persisted manually separately.
     */
    class ListOfDisplayFilters(val list: MutableList<CalendarsDisplayFilter> = mutableListOf())

    @Transient
    var displayFilters = ListOfDisplayFilters()

    /**
     * The user may define several display filters of Type [CalendarsDisplayFilter]. This index
     * marks the current active filter to use.
     */
    var activeDisplayFilterIndex = 0

    @XStreamAsAttribute
    var startDate: LocalDate? = null

    //@XStreamAsAttribute
    //var firstHour: Int? = 8

    //@XStreamAsAttribute
    //var slot30: Boolean? = null

    @XStreamAsAttribute
    var viewType: CalendarServicesRest.CalendarViewType? = null

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(CalendarDisplaySettings::class.java)
        val DEFAULT_COLOR = "#FAAF26"
        private val USERPREF_KEY_FILTERS = "calendar.listOfDisplayFilters";
    }

    internal fun loadDisplayFilters(userPreferenceService: UserPreferencesService) {
        var filter = userPreferenceService.getEntry(ListOfDisplayFilters::class.java, USERPREF_KEY_FILTERS)
        if (filter == null)
        // No filters stored yet.
            return
        displayFilters = ListOfDisplayFilters(filter.list)
    }

    internal fun saveDisplayFilters(userPreferenceService: UserPreferencesService) {
        userPreferenceService.putEntry(USERPREF_KEY_FILTERS, displayFilters, true)
    }


    // LEGACY STUFF:

    /**
     * For re-using legacy filters (from ProjetForge version up to 6, Wicket-Calendar).
     */
    internal fun copyFrom(oldFilter: TeamCalCalendarFilter?) {
        if (oldFilter != null) {
            activeDisplayFilterIndex = oldFilter.activeTemplateEntryIndex
            //firstHour = oldFilter.firstHour
            //slot30 = oldFilter.isSlot30
            startDate = PFDateTimeUtils.convertToLocalDate(oldFilter.startDate)
            viewType = convert(oldFilter.viewType)
            oldFilter.templateEntries?.forEach { templateEntry ->
                val displayFilter = CalendarsDisplayFilter()
                displayFilter.defaultCalendarId = templateEntry.defaultCalendarId
                displayFilter.name = templateEntry.name
                displayFilter.showBirthdays = templateEntry.isShowBirthdays
                displayFilter.showBreaks = templateEntry.isShowBreaks
                displayFilter.showPlanning = templateEntry.isShowPlanning
                displayFilter.showStatistics = templateEntry.isShowStatistics
                displayFilter.timesheetUserId = templateEntry.timesheetUserId
                displayFilter.showTimesheets = templateEntry.isShowTimesheets
                templateEntry.calendarProperties?.forEach {
                    displayFilter.setColorMapping(it.calId, it.colorCode)
                }
                displayFilters.list.add(displayFilter)
            }
        }
    }

    /**
     * For re-using legacy filters (from ProjetForge version up to 6, Wicket-Calendar).
     */
    private fun convert(oldViewType: ViewType?): CalendarServicesRest.CalendarViewType? {
        return when (oldViewType) {
            ViewType.BASIC_WEEK -> CalendarServicesRest.CalendarViewType.WEEK
            ViewType.BASIC_DAY -> CalendarServicesRest.CalendarViewType.DAY
            else -> CalendarServicesRest.CalendarViewType.MONTH
        }
    }
}
