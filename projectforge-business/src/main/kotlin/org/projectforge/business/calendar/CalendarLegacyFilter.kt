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

@file:Suppress("DEPRECATION")

package org.projectforge.business.calendar

import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter
import org.projectforge.business.teamcal.filter.ViewType
import org.projectforge.business.user.service.UserXmlPreferencesService
import org.projectforge.favorites.Favorites
import org.projectforge.framework.time.PFDateCompatibilityUtils

/**
 * For re-using legacy filters (from ProjectForge version up to 6, Wicket-Calendar).
 *
 * Some helpful sql statements for testing the migration:
 *
 * select key, serializedSettings from t_user_xml_prefs where user_id=2 and key like 'calendar.%';
 *
 * delete from t_user_pref where user_fk=2 and area='calendar';
 *
 * You may extract settings by using AdminRest.main
 */
class CalendarLegacyFilter(val state: CalendarFilterState,
                           val list: Favorites<CalendarFilter>,
                           val current: CalendarFilter,
                           val styleMap: CalendarStyleMap) {
    companion object {
        // LEGACY STUFF:
        private const val OLD_USERPREF_KEY = "TeamCalendarPage.userPrefs"

        /**
         * For re-using legacy filters (from ProjectForge version up to 6, Wicket-Calendar).
         */
        fun migrate(userXmlPreferenceService: UserXmlPreferencesService): CalendarLegacyFilter? {
            // No current user filters available. Try the old one (from release 6.* / Wicket Calendarpage):
            val oldFilter = userXmlPreferenceService.getEntry(TeamCalCalendarFilter::class.java, OLD_USERPREF_KEY)
                    ?: return null

            val state = CalendarFilterState()
            val filterList = Favorites<CalendarFilter>()
            val styleMap = CalendarStyleMap()
            val currentFilter = CalendarFilter.copyFrom(oldFilter.activeTemplateEntry)
            //firstHour = oldFilter.firstHour
            //slot30 = oldFilter.isSlot30
            state.startDate = PFDateCompatibilityUtils.convertToLocalDate(oldFilter.startDate)
            state.view = convert(oldFilter.viewType)
            oldFilter.templateEntries?.forEach { templateEntry ->
                val filter = CalendarFilter.copyFrom(templateEntry)
                filterList.add(filter)
            }
            currentFilter.id = filterList.get(currentFilter.name)?.id
            oldFilter.templateEntries?.forEach { templateEntry ->
                templateEntry.calendarProperties?.forEach {
                    if (!styleMap.contains(it.calId)) {
                        styleMap.add(it.calId, CalendarStyle(baseBackgroundColor = it.colorCode)) // Only bgColor was stored for ProjectForge earlier than 7.0.
                    }
                }
            }
            return CalendarLegacyFilter(state, filterList, currentFilter, styleMap)
        }

        /**
         * For re-using legacy filters (from ProjetForge version up to 6, Wicket-Calendar).
         */
        private fun convert(oldViewType: ViewType?): CalendarView? {
            return when (oldViewType) {
                ViewType.AGENDA_WEEK -> CalendarView.WEEK
                ViewType.BASIC_WEEK -> CalendarView.WEEK
                ViewType.AGENDA_DAY -> CalendarView.DAY
                ViewType.BASIC_DAY -> CalendarView.DAY
                else -> CalendarView.MONTH
            }
        }
    }
}
