/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.business.calendar.*
import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.favorites.Favorites
import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import org.projectforge.ui.LayoutUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Rest services for the user's settings of calendar filters.
 */
@RestController
@RequestMapping("${Rest.URL}/calendar")
class CalendarFilterServicesRest {
  class CalendarInit(
    var date: LocalDate? = null,
    @Suppress("unused")
    var view: String? = CalendarView.WEEK.key,
    var alternateHoursBackground: Boolean? = null,
    var teamCalendars: List<StyledTeamCalendar>? = null,
    var filterFavorites: List<Favorites.FavoriteIdTitle>? = null,
    /**
     * The current filter.
     */
    var filter: CalendarFilter? = null,
    var timesheetUser: User? = null,
    var activeCalendars: MutableList<StyledTeamCalendar>? = null,
    var vacationGroups: List<Group>? = null,
    var vacationUsers: List<User>? = null,
    /**
     * This is the list of possible default calendars (with full access). The user may choose one which is
     * used as default if creating a new event. The pseudo calendar -1 for own time sheets is
     * prepended. If chosen, new time sheets will be created at default.
     */
    var listOfDefaultCalendars: List<TeamCalendar>? = null,
    var styleMap: CalendarStyleMap? = null,
    var translations: Map<String, String>? = null,
    /**
     * If true, the client should provide an save button for syncing the current filter to the data base.
     */
    var isFilterModified: Boolean = false,
  )

  companion object {
    internal const val PREF_AREA = "calendar"
    private const val PREF_NAME_STATE = "state"
    private const val PREF_NAME_STYLES = "styles"

    internal fun getCurrentFilter(userPrefService: UserPrefService): CalendarFilter? {
      return userPrefService.getEntry(PREF_AREA, Favorites.PREF_NAME_CURRENT, CalendarFilter::class.java)
        ?: migrateFromLegacyFilter(userPrefService)?.current
    }

    private fun migrateFromLegacyFilter(userPrefService: UserPrefService): CalendarLegacyFilter? {
      val legacyFilter = CalendarLegacyFilter.migrate(userPrefService.userXmlPreferencesService) ?: return null
      log.info("User's legacy calendar filter migrated.")
      userPrefService.putEntry(PREF_AREA, Favorites.PREF_NAME_LIST, legacyFilter.list)
      userPrefService.putEntry(PREF_AREA, Favorites.PREF_NAME_CURRENT, legacyFilter.current)
      // Filter state is now separately stored:
      userPrefService.putEntry(PREF_AREA, PREF_NAME_STATE, legacyFilter.state)
      // Filter styles are now separately stored:
      userPrefService.putEntry(PREF_AREA, PREF_NAME_STYLES, legacyFilter.styleMap)
      return legacyFilter
    }
  }

  @Autowired
  private lateinit var calendarSettingService: CalendarSettingsService

  @Autowired
  private lateinit var userPrefService: UserPrefService

  @Autowired
  private lateinit var teamCalCache: TeamCalCache

  @Autowired
  private lateinit var timesheetDao: TimesheetDao

  @Autowired
  private lateinit var userGroupCache: UserGroupCache

  @GetMapping("initial")
  fun getInitialCalendar(): CalendarInit {
    val initial = CalendarInit()
    val calendars = getCalendars()
    val currentFilter = getCurrentFilter()
    initial.filter = currentFilter

    currentFilter.otherTimesheetUsersEnabled = timesheetDao.showTimesheetsOfOtherUsers()
    val timesheetUser = userGroupCache.getUser(currentFilter.timesheetUserId)
    if (timesheetUser != null) {
      initial.timesheetUser = User()
      initial.timesheetUser!!.copyFromMinimal(timesheetUser)
    }

    val styleMap = getStyleMap()
    initial.styleMap = styleMap

    initial.teamCalendars =
      StyledTeamCalendar.map(calendars, styleMap) // Add the styles of the styleMap to the exported calendars.

    val state = getFilterState()
    initial.date = state.startDate
    initial.view = state.view?.key ?: CalendarView.MONTH.key
    initial.alternateHoursBackground = calendarSettingService.getSettings().alternateHoursBackground

    initial.activeCalendars = getActiveCalendars(currentFilter, calendars, styleMap)
    initial.vacationGroups = currentFilter.vacationGroupIds?.map {
      val group = Group()
      val dbGroup = userGroupCache.getGroup(it)
      if (dbGroup != null) {
        group.copyFromMinimal(dbGroup)
      }
      group
    }?.filter { it.id != null }
    initial.vacationUsers = currentFilter.vacationUserIds?.map {
      val user = User()
      val dbUser = userGroupCache.getUser(it)
      if (dbUser != null) {
        user.copyFromMinimal(dbUser)
      }
      user
    }?.filter { it.id != null }

    val favorites = getFilterFavorites()
    initial.filterFavorites = favorites.idTitleList

    initial.isFilterModified = isCurrentFilterModified(currentFilter, favorites.get(currentFilter.id))

    val listOfDefaultCalendars = mutableListOf<TeamCalendar>()
    initial.activeCalendars?.forEach { activeCal ->
      val cal = calendars.find { it.id == activeCal.id }
      if (cal != null && (cal.access == TeamCalendar.ACCESS.OWNER || cal.access == TeamCalendar.ACCESS.FULL)
        && !cal.externalSubscription
      ) {
        // Calendar with full access:
        listOfDefaultCalendars.add(TeamCalendar(id = cal.id, title = cal.title))
      }
    }

    listOfDefaultCalendars.sortBy { it.title?.lowercase() }
    listOfDefaultCalendars.add(
      0,
      TeamCalendar(id = -1, title = translate("calendar.option.timesheets"))
    ) // prepend time sheet pseudo calendar
    initial.listOfDefaultCalendars = listOfDefaultCalendars

    val translations = addTranslations(
      "select.placeholder",
      "calendar.filter.dialog.title",
      "calendar.filter.showCalendarEntries",
      "calendar.filter.vacation.groups",
      "calendar.filter.vacation.groups.tooltip",
      "calendar.filter.vacation.users",
      "calendar.filter.vacation.users.tooltip",
      "calendar.filter.visible",
      "calendar.defaultCalendar",
      "calendar.defaultCalendar.tooltip",
      "calendar.navigation.today",
      "calendar.newEntry",
      "calendar.option.firstHour",
      "calendar.option.firstHour.tooltip",
      "calendar.option.gridSize",
      "calendar.option.gridSize.tooltip",
      "calendar.option.showBreaks",
      "calendar.option.showBreaks.tooltip",
      "calendar.option.timesheets",
      "calendar.settings",
      "calendar.settings.tooltip",
      "calendar.showMore",
      "calendar.title",
      "calendar.templates.new",
      "calendar.templates.new.tooltip",
      "calendar.templates.tooltip",
      "calendar.view.agenda",
      "calendar.view.settings.tooltip",
      "calendar.view.overview",
      "calendar.view.workDays",
      "calendar.week",
      "menu.plugins.teamcal",
      "more",
      "plugins.teamcal.calendar.listAndIcsExport.tooltip",
      "plugins.teamcal.calendar.refresh.tooltip",
      "reload",
      "settings",
      "tooltip.selectMe"
    )
    Favorites.addTranslations(translations)
    LayoutUtils.addCommonTranslations(translations)
    initial.translations = translations
    return initial
  }

  private fun getCalendars(): MutableList<TeamCalendar> {
    val list = teamCalCache.allAccessibleCalendars
    val userId = ThreadLocalUserContext.loggedInUserId!!
    val calendars = list.map { teamCalDO ->
      TeamCalendar(teamCalDO, userId, teamCalCache)
    }.toMutableList()
    calendars.removeIf { it.access == TeamCalendar.ACCESS.NONE } // Don't annoy admins.

    calendars.add(0, TeamCalendar.createFavoritesBirthdaysPseudoCalendar())
    calendars.add(0, TeamCalendar.createAllBirthdaysPseudoCalendar())
    return calendars
  }

  private fun getActiveCalendars(
    currentFilter: CalendarFilter,
    calendars: List<TeamCalendar>,
    styleMap: CalendarStyleMap
  ): MutableList<StyledTeamCalendar> {
    val activeCalendars = currentFilter.calendarIds.map { id ->
      StyledTeamCalendar(
        calendars.find { it.id == id }, // Might be not accessible / null, see below.
        style = styleMap.get(id), // Add the styles of the styleMap to the exported calendar.
        visible = !currentFilter.isInvisible(id)
      )
    }.toMutableList()
    activeCalendars.removeIf { it.id == null } // Access to this calendars is not given (anymore).

    activeCalendars.sortWith(compareBy(ThreadLocalUserContext.localeComparator) { it.title })
    return activeCalendars
  }

  private fun isCurrentFilterModified(currentFilter: CalendarFilter): Boolean {
    val favorite = getFilterFavorites().get(currentFilter.id)
    return isCurrentFilterModified(currentFilter, favorite)
  }

  private fun isCurrentFilterModified(currentFilter: CalendarFilter, favoriteFilter: CalendarFilter?): Boolean {
    if (favoriteFilter == null)
      return false
    return currentFilter.isModified(favoriteFilter)
  }

  @GetMapping("changeStyle")
  fun changeCalendarStyle(
    @RequestParam("calendarId", required = true) calendarId: Long,
    @RequestParam("bgColor") bgColor: String?
  ): Map<String, Any> {
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
    val calendars = getCalendars()
    val styleMap = getStyleMap()
    return mapOf(
      "activeCalendars" to getActiveCalendars(getCurrentFilter(), calendars, styleMap),
      "teamCalendars" to StyledTeamCalendar.map(calendars, styleMap),
      "styleMap" to styleMap
    )
  }

  /**
   * @return The currentFilter with changed set of invisibleCalendars.
   */
  @GetMapping("setVisibility")
  fun setVisibility(
    @RequestParam("calendarId", required = true) calendarId: Long,
    @RequestParam("visible", required = true) visible: Boolean
  ): Map<String, Any> {
    val currentFilter = getCurrentFilter()
    currentFilter.setVisibility(calendarId, visible)
    val calendars = getCalendars()
    val styleMap = getStyleMap()
    return mapOf(
      "filter" to currentFilter,
      "activeCalendars" to getActiveCalendars(currentFilter, calendars, styleMap),
      "isFilterModified" to isCurrentFilterModified(currentFilter)
    )
  }

  @GetMapping("changeDefaultCalendar")
  fun changeDefaultCalendar(@RequestParam("id", required = true) id: String): Map<String, Any> {
    val currentFilter = getCurrentFilter()
    currentFilter.defaultCalendarId = NumberHelper.parseLong(id)
    return mapOf("isFilterModified" to isCurrentFilterModified(currentFilter))
  }

  /**
   * @param userIdString Change user for displaying time sheets if allowed. If null, no time sheets will be displayed.
   */
  @GetMapping("changeTimesheetUser")
  fun changeTimesheetUser(@RequestParam("userId") userIdString: String?): Map<String, Any> {
    val currentFilter = getCurrentFilter()
    val userId = NumberHelper.parseLong(userIdString)
    if (timesheetDao.showTimesheetsOfOtherUsers()) {
      currentFilter.timesheetUserId = userId
    } else {
      currentFilter.timesheetUserId = if (userId != null && userId >= 0) {
        ThreadLocalUserContext.loggedInUserId
      } else {
        null
      }
    }
    return mapOf("isFilterModified" to isCurrentFilterModified(currentFilter))
  }

  /**
   * @param userIdString Change user for displaying time sheets if allowed. If null, no time sheets will be displayed.
   */
  @GetMapping("changeShowBreaks")
  fun changeShowBreaks(@RequestParam("showBreaks") showBreaks: Boolean?): Map<String, Any> {
    val currentFilter = getCurrentFilter()
    currentFilter.showBreaks = showBreaks
    return mapOf("isFilterModified" to isCurrentFilterModified(currentFilter))
  }

  @PostMapping("changeVacationGroups")
  fun changeVacationGroups(@RequestBody groupIds: Set<Long>?): Map<String, Any> {
    val currentFilter = getCurrentFilter()
    currentFilter.vacationGroupIds = groupIds
    return mapOf("isFilterModified" to isCurrentFilterModified(currentFilter))
  }

  @PostMapping("changeVacationUsers")
  fun changeVacationUsers(@RequestBody userIds: Set<Long>?): Map<String, Any> {
    val currentFilter = getCurrentFilter()
    currentFilter.vacationUserIds = userIds
    return mapOf("isFilterModified" to isCurrentFilterModified(currentFilter))
  }

  @GetMapping("changeGridSize")
  fun changeGridSize(@RequestParam("size", required = true) size: Int): Map<String, Any> {
    val currentFilter = getCurrentFilter()
    if (size in intArrayOf(5, 10, 15, 30, 60)) {
      currentFilter.gridSize = size
    }
    return mapOf("isFilterModified" to isCurrentFilterModified(currentFilter))
  }

  @GetMapping("changeFirstHour")
  fun changeFirstHour(@RequestParam("hour", required = true) hour: Int): Map<String, Any> {
    val currentFilter = getCurrentFilter()
    if (hour >= 0 && hour < 24) {
      currentFilter.firstHour = hour
    }
    return mapOf("isFilterModified" to isCurrentFilterModified(currentFilter))
  }

  /**
   * @return The currentFilter with changed name and defaultCalendarId and the new list of filterFavorites (id's with titles).
   */
  @GetMapping("createNewFilter")
  fun createFavoriteFilter(@RequestParam("newFilterName", required = true) newFilterName: String): Map<String, Any> {
    val currentFilter = getCurrentFilter()
    currentFilter.name = newFilterName
    val favorites = getFilterFavorites()
    val newFavorite = CalendarFilter().copyFrom(currentFilter)
    favorites.add(newFavorite) // Favorite must be a copy of current filter (new instance).
    currentFilter.id = newFavorite.id // Id is set by function favorites.add
    return mapOf(
      "filter" to currentFilter,
      "filterFavorites" to favorites.idTitleList,
      "isFilterModified" to false
    )
  }

  /**
   * Updates the named Filter with the values of the current filter.
   * @return The current filter with flag modified=false.
   */
  @GetMapping("updateFilter")
  fun updateFavoriteFilter(@RequestParam("id", required = true) id: Long): Map<String, Any> {
    val currentFilter = getCurrentFilter()
    getFilterFavorites().get(id)?.copyFrom(currentFilter)
    return mapOf("isFilterModified" to false)
  }

  /**
   * @return The new list of filterFavorites (id's with titles) without the deleted filter.
   */
  @GetMapping("deleteFilter")
  fun deleteFavoriteFilter(@RequestParam("id", required = true) id: Long): Map<String, Any> {
    val favorites = getFilterFavorites()
    favorites.remove(id)
    return mapOf("filterFavorites" to getFilterFavorites().idTitleList)
  }

  @GetMapping("selectFilter")
  fun selectFilter(@RequestParam("id", required = true) id: Long): CalendarInit {
    val favorites = getFilterFavorites()
    val currentFilter = favorites.get(id)
    if (currentFilter != null)
    // Puts a deep copy of the current filter. Without copying, the favorite filter of the list will
    // be synchronized with the current filter.
      userPrefService.putEntry(PREF_AREA, Favorites.PREF_NAME_CURRENT, CalendarFilter().copyFrom(currentFilter))
    else
      log.warn("Can't select filter $id, because it's not found in favorites list.")
    return getInitialCalendar()
  }

  /**
   * @return new filterFavorites
   */
  @GetMapping("renameFilter")
  fun renameFavoriteFilter(
    @RequestParam("id", required = true) id: Long,
    @RequestParam("newName", required = true) newName: String
  ): Map<String, Any> {
    val favorites = getFilterFavorites()
    favorites.rename(id, newName)
    return mapOf("filterFavorites" to favorites.idTitleList)
  }


  // Ensures filter list (stored one, restored from legacy filter or a empty new one).
  private fun getFilterFavorites(): Favorites<CalendarFilter> {
    var favorites: Favorites<CalendarFilter>? = null
    try {
      @Suppress("UNCHECKED_CAST", "USELESS_ELVIS")
      favorites = userPrefService.getEntry(
        PREF_AREA,
        Favorites.PREF_NAME_LIST,
        Favorites::class.java
      ) as? Favorites<CalendarFilter>
        ?: migrateFromLegacyFilter(userPrefService)?.list
    } catch (ex: Exception) {
      log.error("Exception while getting user preferred favorites: ${ex.message}. This might be OK for new releases. Ignoring filter.")
    }
    if (favorites == null) {
      // Creating empty filter list (user has no filter list yet):
      favorites = Favorites()
      userPrefService.putEntry(PREF_AREA, Favorites.PREF_NAME_LIST, favorites)
    }
    return favorites
  }

  internal fun getCurrentFilter(): CalendarFilter {
    var currentFilter = getCurrentFilter(userPrefService)
    if (currentFilter == null) {
      // Creating empty filter (user has no filter list yet):
      currentFilter = CalendarFilter()
      userPrefService.putEntry(PREF_AREA, Favorites.PREF_NAME_CURRENT, currentFilter)
    }
    currentFilter.afterDeserialization()
    return currentFilter
  }

  internal fun getFilterState(): CalendarFilterState {
    var state = userPrefService.getEntry(PREF_AREA, PREF_NAME_STATE, CalendarFilterState::class.java)
      ?: migrateFromLegacyFilter(userPrefService)?.state
    if (state == null) {
      state = CalendarFilterState()
      userPrefService.putEntry(PREF_AREA, PREF_NAME_STATE, state)
    }
    if (state.startDate == null)
      state.startDate = LocalDate.now()
    if (state.view == null)
      state.view = CalendarView.MONTH
    return state
  }


  internal fun getStyleMap(): CalendarStyleMap {
    var styleMap = userPrefService.getEntry(PREF_AREA, PREF_NAME_STYLES, CalendarStyleMap::class.java)
      ?: migrateFromLegacyFilter(userPrefService)?.styleMap
    if (styleMap == null) {
      styleMap = CalendarStyleMap()
      userPrefService.putEntry(PREF_AREA, PREF_NAME_STYLES, styleMap)
    }
    return styleMap
  }
}

