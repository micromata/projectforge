package org.projectforge.rest.calendar

import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * Rest services for the user's settings of calendar filters.
 */
@RestController
@RequestMapping("${Rest.URL}/calendar")
class CalendarConfigServicesRest {
    enum class ACCESS { OWNER, FULL, READ, MINIMAL, NONE }

    class CalendarInit(val date: LocalDate,
                       @Suppress("unused") var view: CalendarView? = CalendarView.WEEK,
                       var teamCalendars: List<StyledTeamCalendar>? = null,
                       var storedFilters: List<String>? = null,
                       var activeFilter: CalendarsDisplayFilter? = null,
                       var activeCalendars: List<StyledTeamCalendar>? = null,
                       var styleMap: CalendarStyleMap? = null,
                       var translations: Map<String, String>? = null)

    class StyledTeamCalendar(teamCalendar: TeamCalendar?, var style: CalendarStyle? = null)
        : TeamCalendar(teamCalendar?.id, teamCalendar?.title)

    companion object {
        const val OLD_USERPREF_KEY = "TeamCalendarPage.userPrefs"
        const val SETTINGS_USERPREF_KEY = "calendar.displaySettings"
        const val CALENDARSTYLE_MAP_USERPREF_KEY = "calendar.styleMap"
    }

    private val log = org.slf4j.LoggerFactory.getLogger(CalendarConfigServicesRest::class.java)

    @Autowired
    private lateinit var teamCalCache: TeamCalCache

    @Autowired
    private lateinit var userPreferenceService: UserPreferencesService

    @GetMapping("initial")
    fun getInitialCalendar(): CalendarInit {
        val initial = CalendarInit(LocalDate.now())
        val list = teamCalCache.allAccessibleCalendars
        val userId = ThreadLocalUserContext.getUserId()
        val calendars = list.map { teamCalDO ->
            TeamCalendar(teamCalDO, userId, teamCalCache)
        }.toMutableList()
        calendars.removeIf { it.access == ACCESS.NONE } // Don't annoy admins.

        val styleMap = getStyleMap()
        initial.styleMap = styleMap

        initial.teamCalendars = calendars.map { cal ->
            StyledTeamCalendar(calendars.find { it.id == cal.id },
                    style = styleMap.get(cal.id)) // Add the styles of the styleMap to the exported calendar.
        }

        initial.activeFilter = getUsersSettings().getActiveFilter()

        initial.activeCalendars = initial.activeFilter?.calendarIds?.map { id ->
            StyledTeamCalendar(calendars.find { it.id == id },
                    style = styleMap.get(id)) // Add the styles of the styleMap to the exported calendar.
        }

        initial.translations = addTranslations("select.placeholder", "plugins.teamcal.calendar.filterDialog.title")
        return initial
    }

    internal fun getUsersSettings(): CalendarDisplaySettings {
        var settings = userPreferenceService.getEntry(CalendarDisplaySettings::class.java, SETTINGS_USERPREF_KEY)
        if (settings == null) {
            // No current user pref entry available. Try the old one (from release 6.* / Wicket Calendarpage):
            val oldFilter = userPreferenceService.getEntry(TeamCalCalendarFilter::class.java, OLD_USERPREF_KEY)
            oldFilter.viewType
            settings = CalendarDisplaySettings.copyFrom(oldFilter)
            log.warn("**** Please remove test mode: User's settings will not be persisted.") // Remove uncomment following line:
            // userPreferenceService.putEntry(SETTINGS_USERPREF_KEY, settings, true)
            settings.saveDisplayFilters(userPreferenceService)
        }
        if (settings.startDate == null)
            settings.startDate = LocalDate.now()
        if (settings.view == null)
            settings.view = CalendarView.MONTH
        return settings
    }

    internal fun getStyleMap(): CalendarStyleMap {
        var styleMap = userPreferenceService.getEntry(CalendarStyleMap::class.java, CALENDARSTYLE_MAP_USERPREF_KEY)
        if (styleMap == null) {
            // No current user pref entry available. Try the old one (from release 6.* / Wicket Calendarpage):
            val oldFilter = userPreferenceService.getEntry(TeamCalCalendarFilter::class.java, OLD_USERPREF_KEY)
            styleMap = CalendarStyleMap.copyFrom(oldFilter)
            log.warn("**** Please remove test mode: User's calendarStyleMap will not be persisted.") // Remove uncomment following line:
            //userPreferenceService.putEntry(CALENDARSTYLE_MAP_USERPREF_KEY, styleMap, true)
        }
        return styleMap
    }
}

