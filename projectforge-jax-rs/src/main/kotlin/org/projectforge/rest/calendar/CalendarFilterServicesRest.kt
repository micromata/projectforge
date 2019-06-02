package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.*

/**
 * Rest services for the user's settings of calendar filters.
 */
@RestController
@RequestMapping("${Rest.URL}/calendar")
class CalendarFilterServicesRest {
    class CalendarInit(var date: PFDateTime? = null,
                       @Suppress("unused") var view: CalendarView? = CalendarView.WEEK,
                       var teamCalendars: List<StyledTeamCalendar>? = null,
                       var filterFavorites: List<String>? = null,
                       var currentFilter: CalendarFilter? = null,
                       var activeCalendars: List<StyledTeamCalendar>? = null,
                       /**
                        * This is the list of possible default calendars (with full access). The user may choose one which is
                        * used as default if creating a new event. The pseudo calendar -1 for own time sheets is
                        * prepended. If chosen, new time sheets will be created at default.
                        */
                       var listOfDefaultCalendars: List<TeamCalendar>? = null,
                       var styleMap: CalendarStyleMap? = null,
                       var translations: Map<String, String>? = null)

    class StyledTeamCalendar(teamCalendar: TeamCalendar?,
                             var style: CalendarStyle? = null,
                             val visible: Boolean = true)
        : TeamCalendar(teamCalendar?.id, teamCalendar?.title)

    companion object {
        private const val PREF_KEY_FILTERLIST = "calendar.filter.list"
        internal const val PREF_KEY_CURRENT_FILTER = "calendar.filter.current"
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

        val currentFilter = getCurrentFilter()
        initial.currentFilter = currentFilter

        val styleMap = getStyleMap()
        initial.styleMap = styleMap

        initial.teamCalendars = calendars.map { cal ->
            StyledTeamCalendar(calendars.find { it.id == cal.id },
                    style = styleMap.get(cal.id)) // Add the styles of the styleMap to the exported calendar.
        }
        val state = getFilterState()
        initial.date = PFDateTime.from(state.startDate)
        initial.view = state.view

        initial.activeCalendars = currentFilter.calendarIds.map { id ->
            StyledTeamCalendar(calendars.find { it.id == id },
                    style = styleMap.get(id), // Add the styles of the styleMap to the exported calendar.
                    visible = currentFilter.isVisible(id)
            )
        }

        val favorites = getFilterFavorites()
        initial.filterFavorites = favorites.getFavoriteNames()

        val listOfDefaultCalendars = mutableListOf<TeamCalendar>()
        initial.activeCalendars?.forEach { activeCal ->
            val cal = calendars.find { it.id == activeCal.id }
            if (cal != null && (cal.access == TeamCalendar.ACCESS.OWNER || cal.access == TeamCalendar.ACCESS.FULL)) {
                // Calendar with full access:
                listOfDefaultCalendars.add(TeamCalendar(id = cal.id, title = cal.title))
            }
        }
        listOfDefaultCalendars.sortBy { it.title?.toLowerCase() }
        listOfDefaultCalendars.add(0, TeamCalendar(id = -1, title = translate("calendar.option.timesheeets"))) // prepend time sheet pseudo calendar
        initial.listOfDefaultCalendars = listOfDefaultCalendars

        initial.translations = addTranslations(
                "select.placeholder",
                "calendar.filter.dialog.title",
                "calendar.filter.visible")
        return initial
    }

    @GetMapping("changeStyle")
    fun changeCalendarStyle(@RequestParam("calendarId", required = true) calendarId: Int,
                            @RequestParam("bgColor") bgColor: String?) {
        var style = getStyleMap().get(calendarId)
        if (style == null) {
            style = CalendarStyle()
            getStyleMap().add(calendarId, style)
        }
        if (!bgColor.isNullOrBlank()) {
            if (CalendarStyle.validateHexCode(bgColor)) {
                style.bgColor = bgColor
            } else {
                throw IllegalArgumentException("Hex code of color doesn't fit '#a1b' or '#a1b2c3', can't change background color: '$bgColor'.")
            }
        }
    }

    @GetMapping("setVisibility")
    fun setVisibility(@RequestParam("calendarId", required = true) calendarId: Int,
                      @RequestParam("visible", required = true) visible: Boolean) {
        val currentFilter = getCurrentFilter()
        currentFilter.setVisibility(calendarId, visible)
    }

    // Ensures filter list (stored one, restored from legacy filter or a empty new one).
    private fun getFilterFavorites(): CalendarFilterFavorites {
        var filterList = userPreferenceService.getEntry(CalendarFilterFavorites::class.java, PREF_KEY_FILTERLIST)
                ?: migrateFromLegacyFilter()?.list
        if (filterList == null) {
            // Creating empty filter list (user has no filter list yet):
            filterList = CalendarFilterFavorites()
            userPreferenceService.putEntry(PREF_KEY_FILTERLIST, filterList, true)
        }
        return filterList
    }

    private fun getCurrentFilter(): CalendarFilter {
        var currentFilter = userPreferenceService.getEntry(CalendarFilter::class.java, PREF_KEY_CURRENT_FILTER)
                ?: migrateFromLegacyFilter()?.current
        if (currentFilter == null) {
            // Creating empty filter (user has no filter list yet):
            currentFilter = CalendarFilter()
            userPreferenceService.putEntry(PREF_KEY_CURRENT_FILTER, currentFilter, true)
        }
        return currentFilter
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
        userPreferenceService.putEntry(PREF_KEY_CURRENT_FILTER, legacyFilter.current, true)
        // Filter state is now separately stored:
        userPreferenceService.putEntry(PREF_KEY_STATE, legacyFilter.state, true)
        // Filter styles are now separately stored:
        userPreferenceService.putEntry(PREF_KEY_STYLES, legacyFilter.styleMap, true)
        return legacyFilter
    }

    internal fun updateCalendarFilter(startDate: Date?,
                                      view: CalendarView?,
                                      activeCalendarIds: Set<Int>?) {
        val state = getFilterState()
        if (startDate != null) {
            var startDay = PFDateTime.from(startDate)!!.asLocalDate()
            if (view == CalendarView.MONTH && startDay.dayOfMonth != 1) {
                // Adjusting the begin of month (startDate might be a day of the end of the previous month, if shown.
                startDay = startDay.withDayOfMonth(1).plusMonths(1)
            }
            state.startDate = startDay
        }
        if (view != null) {
            state.view = view
        }
        if (!activeCalendarIds.isNullOrEmpty()) {
            val currentFilter = getCurrentFilter()
            currentFilter.calendarIds = activeCalendarIds.toMutableSet()
            currentFilter.ensureSets()
        }
    }
}

