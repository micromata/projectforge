/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.Const
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.systeminfo.SystemInfoCache
import org.projectforge.business.task.TaskTree
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFavoritesService
import org.projectforge.business.timesheet.TimesheetRecentService
import org.projectforge.business.user.service.UserService
import org.projectforge.common.DateFormatType
import org.projectforge.favorites.Favorites
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
  private lateinit var timesheetFavoritesService: TimesheetFavoritesService

  @Autowired
  private lateinit var timesheetRecentService: TimesheetRecentService

  @Autowired
  private lateinit var timesheetDao: TimesheetDao

  private val taskTree: TaskTree
    /** Lazy init, because test cases failed due to NPE in TenantRegistryMap. */
    get() = TaskTreeHelper.getTaskTree()

  /**
   * For exporting list of timesheets.
   */
  private class Timesheet4ListExport(
    val timesheet: Timesheet,
    val id: Int, // Needed for history Service
    val weekOfYear: String,
    val dayName: String,
    val timePeriod: String,
    val duration: String
  )

  /**
   * For exporting recent timesheets for copying for new time sheets.
   */
  class RecentTimesheets(
    val timesheets: List<Timesheet>,
    val cost2Visible: Boolean
  )

  override fun transformFromDB(obj: TimesheetDO, editMode: Boolean): Timesheet {
    val timesheet = Timesheet()
    timesheet.copyFrom(obj)
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
    return super.getInitialList(filter)
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
    val recentEntry = timesheetRecentService.getRecentTimesheet();
    if (recentEntry != null) {
      if (recentEntry.taskId != null) {
        sheet.task = Task.getTask(recentEntry.taskId, ThreadLocalUserContext.getUser())
        if (recentEntry.kost2Id != null) {
          sheet.kost2 = Kost2.getkost2(recentEntry.kost2Id)
        }
      }
      sheet.location = recentEntry.location
      sheet.reference = recentEntry.reference
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

  override fun onAfterEdit(obj: TimesheetDO, postData: PostData<Timesheet>): ResponseAction {
    // Save time sheet as recent time sheet
    val timesheet = postData.data
    timesheetRecentService.addRecentTimesheet(transformForDB(timesheet))

    return ResponseAction("/${Const.REACT_APP_PATH}calendar")
      .addVariable("date", obj.startTime)
      .addVariable("id", obj.id ?: -1)
  }

  override fun processResultSetBeforeExport(resultSet: ResultSet<TimesheetDO>): ResultSet<*> {
    val list: List<Timesheet4ListExport> = resultSet.resultSet.map {
      val timesheet = Timesheet()
      timesheet.copyFrom(it)
      Timesheet4ListExport(
        timesheet,
        id = it.id,
        weekOfYear = DateTimeFormatter.formatWeekOfYear(it.startTime),
        dayName = dateTimeFormatter.getFormattedDate(
          it.startTime,
          DateFormats.getFormatString(DateFormatType.DAY_OF_WEEK_SHORT)
        ),
        timePeriod = dateTimeFormatter.getFormattedTimePeriodOfDay(it.timePeriod),
        duration = dateTimeFormatter.getFormattedDuration(it.timePeriod)
      )
    }
    return ResultSet(list, list.size)
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
      val toLowerSearch = searchString?.toLowerCase()
      if (toLowerSearch.isNullOrBlank()) {
        return timesheetRecentService.getRecentLocations()
      }
      return timesheetRecentService.getRecentLocations().filter { it.toLowerCase().contains(toLowerSearch) }
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
  override fun createListLayout(): UILayout {
    lc.idPrefix = "timesheet."
    val layout = super.createListLayout()
      .add(UILabel("'${translate("timesheet.totalDuration")}: tbd.")) // See TimesheetListForm
      .add(
        UITable.createUIResultSetTable()
          .add(lc, "user")
          .add(UITableColumn("timesheet.kost2.project.customer", "fibu.kunde", formatter = Formatter.CUSTOMER))
          .add(UITableColumn("timesheet.kost2.project", "fibu.projekt", formatter = Formatter.PROJECT))
          .add(lc, "task")
          .add(UITableColumn("timesheet.kost2", "fibu.kost2", formatter = Formatter.COST2))
          .add(UITableColumn("weekOfYear", "calendar.weekOfYearShortLabel"))
          .add(UITableColumn("dayName", "calendar.dayOfWeekShortLabel"))
          .add(UITableColumn("timePeriod", "timePeriod"))
          .add(UITableColumn("duration", "timesheet.duration"))
          .add(lc, "location", "reference", "description")
      )
    layout.getTableColumnById("timesheet.user").formatter = Formatter.USER
    layout.getTableColumnById("timesheet.task").formatter = Formatter.TASK_PATH
    return LayoutUtils.processListPage(layout, this)
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
      .add(referenceField)
      .add(descriptionArea)
    JiraSupport.createJiraElement(dto.description, descriptionArea)?.let { layout.add(UIRow().add(UICol().add(it))) }
    Favorites.addTranslations(layout.translations)
    layout.addAction(
      UIButton(
        "switch",
        title = translate("plugins.teamcal.switchToTeamEventButton"),
        color = UIColor.DARK,
        responseAction = ResponseAction(getRestRootPath("switch2CalendarEvent"), targetType = TargetType.POST)
      )
    )
    layout.addTranslations(
      "templates",
      "search.search",
      "fibu.kost2",
      "fibu.kunde",
      "fibu.projekt",
      "task",
      "timesheet.description",
      "timesheet.location",
      "timesheet.reference",
      "timesheet.recent",
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
      url = "/${Const.REACT_APP_PATH}calendar/${getRestPath(RestPaths.EDIT)}",
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

  companion object {
    private const val PREF_AREA = "timesheet"
    private const val PREF_EDIT_NAME = "edit.recent"
  }
}
