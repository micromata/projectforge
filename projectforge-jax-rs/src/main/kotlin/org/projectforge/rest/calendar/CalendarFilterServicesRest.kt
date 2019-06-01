package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDate
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.*

/**
 * Rest services for the user's settings of calendar filters.
 */
@RestController
@RequestMapping("${Rest.URL}/calendar")
class CalendarFilterServicesRest {
    class CalendarInit(var date: LocalDate? = null,
                       @Suppress("unused") var view: CalendarView? = CalendarView.WEEK,
                       var teamCalendars: List<StyledTeamCalendar>? = null,
                       var storedFilters: List<String>? = null,
                       var activeFilter: CalendarFilter? = null,
                       var activeCalendars: List<StyledTeamCalendar>? = null,
                       var styleMap: CalendarStyleMap? = null,
                       var translations: Map<String, String>? = null)

    class StyledTeamCalendar(teamCalendar: TeamCalendar?, var style: CalendarStyle? = null)
        : TeamCalendar(teamCalendar?.id, teamCalendar?.title)

    companion object {
        private const val PREF_KEY_FILTERLIST = "calendar.filter.list"
        private const val PREF_KEY_STATE = "calendar.state"
        private const val PREF_KEY_STYLES = "calendar.styles"
    }

    private val log = org.slf4j.LoggerFactory.getLogger(CalendarFilterServicesRest::class.java)

    @Autowired
    private lateinit var teamCalCache: TeamCalCache

    @Autowired
    private lateinit var userPreferenceService: UserPreferencesService

    @GetMapping("initial")
    fun getInitialCalendar(): CalendarInit {
        val initial = CalendarInit()
        val list = teamCalCache.allAccessibleCalendars
        val userId = ThreadLocalUserContext.getUserId()
        val calendars = list.map { teamCalDO ->
            TeamCalendar(teamCalDO, userId, teamCalCache)
        }.toMutableList()
        calendars.removeIf { it.access == TeamCalendar.ACCESS.NONE } // Don't annoy admins.

        val styleMap = getStyleMap()
        initial.styleMap = styleMap

        initial.teamCalendars = calendars.map { cal ->
            StyledTeamCalendar(calendars.find { it.id == cal.id },
                    style = styleMap.get(cal.id)) // Add the styles of the styleMap to the exported calendar.
        }
        val state = getFilterState()
        initial.date = state.startDate ?: LocalDate.now()
        initial.view = state.view
        initial.activeFilter = getFilterList().getActiveFilter(state.activeFilterIndex)

        initial.activeCalendars = initial.activeFilter?.calendarIds?.map { id ->
            StyledTeamCalendar(calendars.find { it.id == id },
                    style = styleMap.get(id)) // Add the styles of the styleMap to the exported calendar.
        }

        initial.translations = addTranslations("select.placeholder", "plugins.teamcal.calendar.filterDialog.title")
        return initial
    }

    // Ensures filter list (stored one, restored from legacy filter or a empty new one).
    private fun getFilterList(): CalendarFilterList {
        var filterList = userPreferenceService.getEntry(CalendarFilterList::class.java, PREF_KEY_FILTERLIST)
                ?: migrateFromLegacyFilter()?.list
        if (filterList == null) {
            // Creating empty filter list (user has no filter list yet):
            filterList = CalendarFilterList()
            userPreferenceService.putEntry(PREF_KEY_FILTERLIST, filterList, true)
        }
        return filterList
    }

    private fun getFilterState(): CalendarFilterState {
        var state = userPreferenceService.getEntry(CalendarFilterState::class.java, PREF_KEY_STATE)
                ?: migrateFromLegacyFilter()?.state
        if (state == null) {
            state = CalendarFilterState()
            userPreferenceService.putEntry(PREF_KEY_STATE, state, true)
        }
        if (state.startDate == null)
            state.startDate = LocalDate.now()
        if (state.view == null)
            state.view = CalendarView.MONTH
        return state
    }


    internal fun getStyleMap(): CalendarStyleMap {
        var styleMap = userPreferenceService.getEntry(CalendarStyleMap::class.java, PREF_KEY_STYLES)
                ?: migrateFromLegacyFilter()?.styleMap
        if (styleMap == null) {
            styleMap = CalendarStyleMap()
            userPreferenceService.putEntry(PREF_KEY_STYLES, styleMap, true)
        }
        return styleMap
    }

    private fun migrateFromLegacyFilter(): CalendarLegacyFilter? {
        val legacyFilter = CalendarLegacyFilter.migrate(userPreferenceService) ?: return null
        log.info("User's legacy calendar filter migrated.")
        userPreferenceService.putEntry(PREF_KEY_FILTERLIST, legacyFilter.list, true)
        // Filter state is now separately stored:
        userPreferenceService.putEntry(PREF_KEY_STATE, legacyFilter.state, true)
        // Filter styles are now separately stored:
        userPreferenceService.putEntry(PREF_KEY_STYLES, legacyFilter.styleMap, true)
        return legacyFilter
    }

    internal fun updateCalendarFilterState(startDate: Date?, view: CalendarView?) {
        val state = getFilterState()
        if (startDate != null) {
            state.startDate = PFDate.from(startDate)?.date
        }
        if (view != null)
            state.view = view
    }
}

