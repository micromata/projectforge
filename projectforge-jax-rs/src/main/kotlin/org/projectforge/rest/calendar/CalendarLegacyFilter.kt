package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter
import org.projectforge.business.teamcal.filter.ViewType
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.framework.time.PFDateTimeUtils

/**
 * For re-using legacy filters (from ProjectForge version up to 6, Wicket-Calendar).
 */
internal class CalendarLegacyFilter(val state: CalendarFilterState,
                                    val list: CalendarFilterList,
                                    val styleMap: CalendarStyleMap) {
    companion object {
        // LEGACY STUFF:
        private const val OLD_USERPREF_KEY = "TeamCalendarPage.userPrefs"

        /**
         * For re-using legacy filters (from ProjectForge version up to 6, Wicket-Calendar).
         */
        internal fun migrate(userPreferenceService: UserPreferencesService): CalendarLegacyFilter? {
            // No current user filters available. Try the old one (from release 6.* / Wicket Calendarpage):
            val oldFilter = userPreferenceService.getEntry(TeamCalCalendarFilter::class.java, OLD_USERPREF_KEY)
                    ?: return null

            val state = CalendarFilterState()
            val filterList = CalendarFilterList()
            val styleMap = CalendarStyleMap()
            state.activeFilterIndex = oldFilter.activeTemplateEntryIndex
            //firstHour = oldFilter.firstHour
            //slot30 = oldFilter.isSlot30
            state.startDate = PFDateTimeUtils.convertToLocalDate(oldFilter.startDate)
            state.view = convert(oldFilter.viewType)
            oldFilter.templateEntries?.forEach { templateEntry ->
                val displayFilter = CalendarFilter.copyFrom(templateEntry)
                filterList.list.add(displayFilter)
            }
            oldFilter.templateEntries?.forEach { templateEntry ->
                templateEntry.calendarProperties?.forEach {
                    if (!styleMap.styles.containsKey(it.calId)) {
                        styleMap.styles.put(it.calId, CalendarStyle(bgColor = it.colorCode)) // Only bgColor was stored for ProjectForge earlier than 7.0.
                    }
                }
            }
            return CalendarLegacyFilter(state, filterList, styleMap)
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
