/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import org.projectforge.Constants
import org.projectforge.business.common.ListStatisticsSupport
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.systeminfo.SystemInfoCache
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFavoritesService
import org.projectforge.business.timesheet.TimesheetRecentService
import org.projectforge.business.user.service.UserService
import org.projectforge.favorites.Favorites
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.*
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.calendar.CalEventPagesRest
import org.projectforge.rest.calendar.TeamEventPagesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.RestButtonEvent
import org.projectforge.rest.core.RestHelper
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.*
import org.projectforge.rest.task.TaskServicesRest
import org.projectforge.ui.*
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("${Rest.URL}/timesheet")
class TimesheetPagesRest : AbstractDTOPagesRest<TimesheetDO, Timesheet, TimesheetDao>(
  TimesheetDao::class.java, "timesheet.title",
  cloneSupport = CloneSupport.AUTOSAVE
) {

  @Value("\${calendar.useNewCalendarEvents}")
  private var useNewCalendarEvents: Boolean = false

  private val dateTimeFormatter = DateTimeFormatter.instance()

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var kost2Dao: Kost2Dao

  @Autowired
  private lateinit var kundeDao: KundeDao

  @Autowired
  private lateinit var projektDao: ProjektDao

  @Autowired
  private lateinit var teamEventRest: TeamEventPagesRest

  @Autowired
  private lateinit var calendarEventRest: CalEventPagesRest

  @Autowired
  private lateinit var taskTree: TaskTree

  @Autowired
  private lateinit var timesheetFavoritesService: TimesheetFavoritesService

  @Autowired
  private lateinit var timesheetRecentService: TimesheetRecentService

  @Autowired
  private lateinit var timesheetDao: TimesheetDao

  /**
   * For exporting list of timesheets.
   */
  @Suppress("unused")
  private class Timesheet4ListExport(
    val timesheet: Timesheet,
    val id: Int, // Needed for history Service
    val weekOfYear: String,
    val dayName: String,
    val timePeriod: String,
    val duration: String,
    val deleted: Boolean? = null,
  )

  /**
   * For exporting recent timesheets for copying for new time sheets.
   */
  @Suppress("unused")
  class RecentTimesheets(
    val timesheets: List<Timesheet>,
    val cost2Visible: Boolean
  )

  override fun transformFromDB(obj: TimesheetDO, editMode: Boolean): Timesheet {
    val timesheet = Timesheet()
    timesheet.copyFrom(obj)
    val day = PFDay.fromOrNull(timesheet.startTime)
    return timesheet
  }

  override fun transformForDB(dto: Timesheet): TimesheetDO {
    val timesheetDO = TimesheetDO()
    dto.copyTo(timesheetDO)
    if (timesheetDO.kost2 != null && baseDao.getKost2List(timesheetDO).isNullOrEmpty()) {
      // Work arround: if kost 2 was selected in client before new task without kost2 assignments was chosen,
      // the former kost2 selection will be sent by the client.
      timesheetDO.kost2 = null
    }
    return timesheetDO
  }


  override fun getInitialList(request: HttpServletRequest): InitialListData {
    val taskId = NumberHelper.parseInteger(request.getParameter("taskId")) ?: return super.getInitialList(request)
    val filter = MagicFilter()
    filter.entries.add(MagicFilterEntry("task", "$taskId"))
    return super.getInitialList(request, filter)
  }

  override fun newBaseDTO(request: HttpServletRequest?): Timesheet {
    val sheet = Timesheet()
    val startTimeEpochSeconds = RestHelper.parseLong(request, "start")
    val endTimeEpochSeconds = RestHelper.parseLong(request, "end")
    if (startTimeEpochSeconds != null) {
      val start = PFDateTime.fromOrNow(startTimeEpochSeconds)
      sheet.startTime = start.sqlTimestamp
    }
    if (endTimeEpochSeconds != null) {
      val stop = PFDateTime.fromOrNow(endTimeEpochSeconds)
      sheet.stopTime = stop.sqlTimestamp
    }
    val userId = RestHelper.parseInt(request, "userId") // Optional parameter given to edit page
    sheet.user = User.getUser(userId)
    val recentEntry = timesheetRecentService.getRecentTimesheet()
    if (recentEntry != null) {
      if (recentEntry.taskId != null) {
        sheet.task = Task.getTask(recentEntry.taskId)
        if (recentEntry.kost2Id != null) {
          sheet.kost2 = Kost2.getkost2(recentEntry.kost2Id)
        }
      }
      sheet.location = recentEntry.location
      sheet.reference = recentEntry.reference
      sheet.tag = recentEntry.tag
      sheet.description = recentEntry.description
      if (sheet.user == null && recentEntry.userId != null) {
        sheet.user = User.getUser(recentEntry.userId)
      }
    }
    if (sheet.user == null) {
      sheet.user = User.getUser(ThreadLocalUserContext.getUserId()) // Use current user.
    }
    return sheet
  }

  override fun onAfterEdit(obj: TimesheetDO, postData: PostData<Timesheet>, event: RestButtonEvent): ResponseAction {
    // Save time sheet as recent time sheet
    val timesheet = postData.data
    timesheetRecentService.addRecentTimesheet(transformForDB(timesheet))

    return ResponseAction("/${Constants.REACT_APP_PATH}calendar")
      .addVariable("date", obj.startTime)
      .addVariable("id", obj.id ?: -1)
  }

  override fun processResultSetBeforeExport(
    resultSet: ResultSet<TimesheetDO>,
    request: HttpServletRequest,
    magicFilter: MagicFilter,
  ): ResultSet<*> {
    val list: List<Timesheet4ListExport> = resultSet.resultSet.map {
      val timesheet = Timesheet()
      timesheet.copyFrom(it)
      val day = PFDay.fromOrNull(it.startTime)
      Timesheet4ListExport(
        timesheet,
        id = it.id,
        weekOfYear = DateTimeFormatter.formatWeekOfYear(it.startTime),
        dayName = day?.dayOfWeekAsShortString ?: "??",
        timePeriod = dateTimeFormatter.getFormattedTimePeriodOfDay(it.timePeriod),
        duration = dateTimeFormatter.getFormattedDuration(it.timePeriod),
        deleted = timesheet.deleted,
      )
    }
    val myResultSet = ResultSet(list, resultSet, list.size, magicFilter = magicFilter)
    var duration = 0L
    resultSet.resultSet.forEach { timesheet ->
      duration += timesheet.getDuration()
    }
    val stats = ListStatisticsSupport()
    stats.append("timesheet.totalDuration", dateTimeFormatter.getPrettyFormattedDuration(duration), ListStatisticsSupport.Color.BLUE)
    myResultSet.addResultInfo(stats.asMarkdown)

    return myResultSet
  }

  override fun isAutocompletionPropertyEnabled(property: String): Boolean {
    return property == "location"
  }

  override fun getAutoCompletionForProperty(
    @RequestParam("property") property: String,
    @RequestParam("search") searchString: String?
  )
      : List<String> {
    if (property == "location") {
      val toLowerSearch = searchString?.lowercase()
      if (toLowerSearch.isNullOrBlank()) {
        return timesheetRecentService.getRecentLocations()
      }
      return timesheetRecentService.getRecentLocations().filter { it.lowercase().contains(toLowerSearch) }
    }
    return super.getAutoCompletionForProperty(property, searchString)
  }

  @GetMapping("acReference")
  fun getReferences(@RequestParam("search") search: String?, @RequestParam("taskId") taskId: Int?): List<String> {
    taskId ?: return emptyList()
    return timesheetDao.getUsedReferences(taskId, search)
  }


  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
    lc.idPrefix = "timesheet."
    val table = agGridSupport.prepareUIGrid4ListPage(
      request,
      layout,
      magicFilter,
      this,
      TimesheetMultiSelectedPageRest::class.java,
      userAccess,
    )
      .add(lc, "user")
    //.add(lc, "kost2.project.customer", lcField = "kost2.projekt.kunde")
    //.add(lc, "kost2.project", lcField = "kost2.projekt")
    if (Configuration.instance.isCostConfigured) {
      table.add(lc, "kost2")
    }
    table.add(lc, "task")
      .add("weekOfYear", headerName = "calendar.weekOfYearShortLabel", width = 30)
      .add("dayName", headerName = "calendar.dayOfWeekShortLabel", width = 30)
      .add("timePeriod", headerName = "timePeriod", width = 140)
      .add("duration", headerName = "timesheet.duration", width = 50)
      .add(lc, "location", "reference")
      .withMultiRowSelection(request, magicFilter)
    if (!baseDao.getTags().isNullOrEmpty()) {
      table.add(lc, "tag", width = 100)
    }
    table.add(lc, "description", width = 1000)
    layout.add(UILabel("'${translate("timesheet.totalDuration")}: tbd.")) // See TimesheetListForm
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: Timesheet, userAccess: UILayout.UserAccess): UILayout {
    val dayRange = UICustomized("dayRange")
    dayRange.add("startDateId", "startTime")
    dayRange.add("endDateId", "stopTime")
    dayRange.add("label", translate("timePeriod"))
    val descriptionArea = UITextArea("description", lc, rows = 5)
    val referenceField = UIInput(
      "reference", lc,
      label = "timesheet.reference",
      tooltip = "timesheet.reference.info"
    ).setAutoCompletion("timesheet/acReference?search=:search", mapOf("taskId" to "task.id"))
    val layout = super.createEditLayout(dto, userAccess)
      .add(UICustomized("timesheet.edit.templatesAndRecent"))
      .add(UICustomized("timesheet.edit.taskAndKost2", values = mutableMapOf("id" to "kost2")))
      .add(lc, "user")
      .add(dayRange)
      .add(UICustomized("task.consumption"))
      .add(UIInput("location", lc).enableAutoCompletion(this))
    val row = UIRow()
    layout.add(row)
    createTagUISelect(dto)?.let { select ->
      row.add(UICol(md = 6).add(select))
    }
    row.add(UICol(md = 6).add(referenceField))
    layout.add(descriptionArea)
    JiraSupport.createJiraElement(dto.description, descriptionArea)?.let { layout.add(UIRow().add(UICol().add(it))) }
    Favorites.addTranslations(layout.translations)
    layout.addAction(
      UIButton.createSecondaryButton(
        id = "switch",
        title = "plugins.teamcal.switchToTeamEventButton",
        responseAction = ResponseAction(getRestRootPath("switch2CalendarEvent"), targetType = TargetType.POST)
      )
    )
    layout.addTranslations(
      "templates",
      "templates.new",
      "task.favorite.new",
      "search.search",
      "fibu.kost2",
      "fibu.kunde",
      "fibu.projekt",
      "task",
      "timesheet.description",
      "timesheet.location",
      "timesheet.reference",
      "timesheet.recent",
      "timesheet.tag",
      "until"
    )
    return LayoutUtils.processEditPage(layout, dto, this)
  }

  /**
   * @return The list fo recent edited time sheets of the current logged in user.
   */
  @GetMapping("recentList")
  fun getRecentList(): RecentTimesheets {
    val recentTimesheets = timesheetRecentService.getRecentTimesheets()
    var counter = 1
    val timesheets = recentTimesheets.map {
      val ts = Timesheet()
      ts.location = it.location
      ts.tag = it.tag
      ts.reference = it.reference
      ts.description = it.description
      val task = taskTree.getTaskById(it.taskId)
      if (task != null) {
        ts.task = Task()
        ts.task!!.copyFromMinimal(task)
      }
      val user = userService.getUser(it.userId)
      if (user != null) {
        ts.user = User()
        ts.user!!.copyFromMinimal(user)
      }
      if (it.kost2Id != null) {
        val kost2DO = kost2Dao.internalGetById(it.kost2Id)
        if (kost2DO != null) {
          val kost2 = Kost2()
          ts.kost2 = kost2
          kost2.copyFromMinimal(kost2DO)
          kost2DO.projektId?.let { projektId ->
            val projektDO = projektDao.internalGetById(projektId)
            if (projektDO != null) {
              val projekt = Project(projektId, name = projektDO.name)
              kost2.project = projekt
              projektDO.kundeId?.let { kundeId ->
                val kundeDO = kundeDao.internalGetById(kundeId)
                if (kundeDO != null) {
                  val kunde = Customer(kundeId, name = kundeDO.name)
                  projekt.customer = kunde
                }
              }
            }
          }
        }
      }
      ts.counter = counter++
      ts
    }
    return RecentTimesheets(timesheets, SystemInfoCache.instance().isCost2EntriesExists)
  }

  @PostMapping("selectRecent")
  fun selectRecent(@RequestBody timesheet: Timesheet): ResponseAction {
    val task = TaskServicesRest.createTask(timesheet.task?.id)
    timesheet.tag = timesheet.tag ?: "" // "" Needed for overwriting clients data.tag if already defined.
    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("task", task)
      .addVariable("data", timesheet)
  }

  /**
   * Will be called by clone button. Sets the id of the form data object to null and deleted to false.
   * @return ResponseAction with [TargetType.UPDATE] and variable "initial" with all the initial data of [getItemAndLayout] as given for new objects.
   */
  @RequestMapping("switch2CalendarEvent")
  fun switch2CalendarEvent(request: HttpServletRequest, @Valid @RequestBody postData: PostData<Timesheet>)
      : ResponseAction {
    return if (useNewCalendarEvents) calendarEventRest.cloneFromTimesheet(request, postData.data)
    else teamEventRest.cloneFromTimesheet(request, postData.data)
  }

  override fun getRestEditPath(): String {
    return "calendar/${super.getRestEditPath()}"
  }

  @Deprecated("Will be replaced by cloneFromCalendarEvent(request, calendarEvent).")
  fun cloneFromTeamEvent(request: HttpServletRequest, teamEvent: TeamEvent): ResponseAction {
    val calendarEvent = CalEvent(
      startDate = teamEvent.startDate,
      endDate = teamEvent.endDate,
      location = teamEvent.location,
      subject = teamEvent.subject
    )
    return cloneFromCalendarEvent(request, calendarEvent)
  }

  fun cloneFromCalendarEvent(request: HttpServletRequest, calendarEvent: CalEvent): ResponseAction {
    val timesheet = newBaseDTO(request)
    timesheet.startTime = calendarEvent.startDate
    timesheet.stopTime = calendarEvent.endDate
    if (!calendarEvent.location.isNullOrBlank())
      timesheet.location = calendarEvent.location
    if (!calendarEvent.subject.isNullOrBlank() || !calendarEvent.note.isNullOrBlank())
      timesheet.description = "${calendarEvent.subject ?: ""} ${calendarEvent.note ?: ""}"
    val editLayoutData = getItemAndLayout(request, timesheet, UILayout.UserAccess(false, true))
    return ResponseAction(
      url = "/${Constants.REACT_APP_PATH}calendar/${getRestPath(RestPaths.EDIT)}",
      targetType = TargetType.UPDATE
    )
      .addVariable("data", editLayoutData.data)
      .addVariable("ui", editLayoutData.ui)
      .addVariable("serverData", editLayoutData.serverData)
      .addVariable("variables", editLayoutData.variables)
  }

  /**
   * Supports request parameters startDate and endDate for creating new time sheet entries.
   *
   * Supports different date formats: long number of epoch seconds
   * or iso date time including any time zone offset.
   * @see PFDateTimeUtils.parse for supported date formats.
   */
  override fun onGetItemAndLayout(request: HttpServletRequest, dto: Timesheet, formLayoutData: FormLayoutData) {
    val startTime = PFDateTimeUtils.parseAndCreateDateTime(
      request.getParameter("startDate"),
      numberFormat = PFDateTime.NumberFormat.EPOCH_SECONDS
    )
    if (startTime != null) {
      dto.startTime = startTime.withPrecision(DatePrecision.MINUTE_5).sqlTimestamp
    }
    val stopTime = PFDateTimeUtils.parseAndCreateDateTime(
      request.getParameter("endDate"),
      numberFormat = PFDateTime.NumberFormat.EPOCH_SECONDS
    )
    if (stopTime != null) {
      dto.stopTime = stopTime.withPrecision(DatePrecision.MINUTE_5).sqlTimestamp
    }
    super.onGetItemAndLayout(request, dto, formLayoutData)
  }

  /**
   * Puts the task information such as path, consumption etc. as additional variable for the client, because the
   * origin task of the timesheet is of type TaskDO and doesn't contain such data.
   */
  override fun addVariablesForEditPage(dto: Timesheet): MutableMap<String, Any>? {
    val task = TaskServicesRest.createTask(dto.task?.id) ?: return null
    return mutableMapOf(
      "task" to task,
      "timesheetFavorites" to timesheetFavoritesService.getList()
    )
  }

  override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
    val element = UIFilterElement("kost2.nummer")
    element.label = element.id // Default label if no translation will be found below.
    element.label = LayoutListFilterUtils.getLabel(
      ElementInfo(
        "nummer",
        i18nKey = "fibu.kost2.nummer",
        parent = ElementInfo("kost2", i18nKey = "fibu.kost2")
      )
    )
    elements.add(element)
  }

  /**
   * @param timesheet Only needed, if the tag of the given timesheet should be added to the tag list and is not
   * configured (after changing configuration of tag list).
   * @param id Field (id) is "tag" as default.
   * @return UISelect or null, if no tags exist (neither configured nor given in timesheet).
   */
  fun createTagUISelect(timesheet: Timesheet? = null, id: String = "tag"): UISelect<String>? {
    val tags = timesheetDao.getTags(timesheet?.tag)
    if (tags.isNullOrEmpty()) {
      return null
    }
    return UISelect(id, label = "timesheet.tag", required = false, values = tags.map { UISelectValue(it, it) })
  }
}
