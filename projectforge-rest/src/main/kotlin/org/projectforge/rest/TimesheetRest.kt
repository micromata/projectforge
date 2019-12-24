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

package org.projectforge.rest

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.projectforge.Const
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.systeminfo.SystemInfoCache
import org.projectforge.business.task.TaskTree
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.business.timesheet.*
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.user.service.UserService
import org.projectforge.common.DateFormatType
import org.projectforge.favorites.Favorites
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.calendar.CalEventRest
import org.projectforge.rest.calendar.TeamEventRest
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
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

@RestController
@RequestMapping("${Rest.URL}/timesheet")
class TimesheetRest : AbstractDORest<TimesheetDO, TimesheetDao>(TimesheetDao::class.java, "timesheet.title",
        cloneSupported = true) {

    companion object {
        private const val PREF_AREA = "timesheet"
        private const val PREF_EDIT_NAME = "edit.recent"

        init {
            JacksonConfiguration.registerAllowedUnknownProperties(Kost2DO::class.java, "title")
        }
    }

    @Value("\${calendar.useNewCalendarEvents}")
    private var useNewCalendarEvents: Boolean = false

    private val dateTimeFormatter = DateTimeFormatter.instance()

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var teamEventRest: TeamEventRest

    @Autowired
    private lateinit var calendarEventRest: CalEventRest

    @Autowired
    private lateinit var timesheetFavoritesService: TimesheetFavoritesService

    private val taskTree: TaskTree
        /** Lazy init, because test cases failed due to NPE in TenantRegistryMap. */
        get() = TaskTreeHelper.getTaskTree()

    /**
     * For exporting list of timesheets.
     */
    private class Timesheet4ListExport(val timesheet: TimesheetDO,
                                       val id: Int, // Needed for history Service
                                       val weekOfYear: String,
                                       val dayName: String,
                                       val timePeriod: String,
                                       val duration: String)

    /**
     * For exporting recent timesheets for copying for new time sheets.
     */
    class RecentTimesheets(val timesheets: List<Timesheet>,
                           val cost2Visible: Boolean)

    override fun getInitialList(request: HttpServletRequest): InitialListData {
        val taskId = NumberHelper.parseInteger(request.getParameter("taskId")) ?: return super.getInitialList(request)
        val filter = MagicFilter()
        filter.entries.add(MagicFilterEntry("task", "$taskId"))
        return super.getInitialList(filter)
    }

    /**
     * Initializes new timesheets for adding.
     */
    override fun newBaseDO(request: HttpServletRequest?): TimesheetDO {
        val sheet = super.newBaseDO(request)
        val startTimeEpochSeconds = RestHelper.parseLong(request, "start")
        val endTimeEpochSeconds = RestHelper.parseLong(request, "end")
        if (startTimeEpochSeconds != null) {
            val start = PFDateTime.from(startTimeEpochSeconds, nowIfNull = true)!!
            sheet.startTime = start.sqlTimestamp
        }
        if (endTimeEpochSeconds != null) {
            val stop = PFDateTime.from(endTimeEpochSeconds, nowIfNull = true)!!
            sheet.stopTime = stop.sqlTimestamp
        }
        val userId: Int? = null // Optional parameter given to edit page
        if (userId != null) {
            baseDao.setUser(sheet, userId)
        }
        val pref = getTimesheetPrefData()
        val entry = pref.recentEntry
        if (entry != null) {
            if (entry.taskId != null) {
                baseDao.setTask(sheet, entry.taskId)
                if (entry.kost2Id != null) {
                    baseDao.setKost2(sheet, entry.kost2Id)
                }
            }
            sheet.location = entry.location
            sheet.description = entry.description
        }
        if (entry?.userId != null) {
            baseDao.setUser(sheet, entry.userId)
        } else {
            baseDao.setUser(sheet, ThreadLocalUserContext.getUserId()) // Use current user.
        }
        return sheet
    }

    override fun afterEdit(obj: TimesheetDO, dto: TimesheetDO): ResponseAction {
        return ResponseAction("/${Const.REACT_APP_PATH}calendar")
                .addVariable("date", obj.startTime)
                .addVariable("id", obj.id ?: -1)
    }

    override fun processResultSetBeforeExport(resultSet: ResultSet<TimesheetDO>): ResultSet<*> {
        val list: List<Timesheet4ListExport> = resultSet.resultSet.map {
            Timesheet4ListExport(it,
                    id = it.id,
                    weekOfYear = DateTimeFormatter.formatWeekOfYear(it.startTime),
                    dayName = dateTimeFormatter.getFormattedDate(it.startTime,
                            DateFormats.getFormatString(DateFormatType.DAY_OF_WEEK_SHORT)),
                    timePeriod = dateTimeFormatter.getFormattedTimePeriodOfDay(it.timePeriod),
                    duration = dateTimeFormatter.getFormattedDuration(it.timePeriod))
        }
        return ResultSet(list, list.size)
    }

    override fun isAutocompletionPropertyEnabled(property: String): Boolean {
        return property == "location"
    }

    override fun getAutoCompletionForProperty(@RequestParam("property") property: String, @RequestParam("search") searchString: String?)
            : List<String> {
        if (property == "location") {
            return baseDao.getLocationAutocompletion(searchString)
        }
        return super.getAutoCompletionForProperty(property, searchString)
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        lc.idPrefix = "timesheet."
        val layout = super.createListLayout()
                .add(UILabel("'${translate("timesheet.totalDuration")}: tbd.")) // See TimesheetListForm
                .add(UITable.UIResultSetTable()
                        .add(lc, "user")
                        .add(UITableColumn("timesheet.kost2.project.customer", "fibu.kunde", formatter = Formatter.CUSTOMER))
                        .add(UITableColumn("timesheet.kost2.project", "fibu.projekt", formatter = Formatter.PROJECT))
                        .add(lc, "task")
                        .add(UITableColumn("timesheet.kost2", "fibu.kost2", formatter = Formatter.COST2))
                        .add(UITableColumn("weekOfYear", "calendar.weekOfYearShortLabel"))
                        .add(UITableColumn("dayName", "calendar.dayOfWeekShortLabel"))
                        .add(UITableColumn("timePeriod", "timePeriod"))
                        .add(UITableColumn("duration", "timesheet.duration"))
                        .add(lc, "location", "description"))
        layout.getTableColumnById("timesheet.user").formatter = Formatter.USER
        layout.getTableColumnById("timesheet.task").formatter = Formatter.TASK_PATH
        LayoutUtils.addListFilterContainer(layout, "longFormat", "recursive",
                filterClass = TimesheetFilter::class.java)
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: TimesheetDO, userAccess: UILayout.UserAccess): UILayout {
        val dayRange = UICustomized("dayRange")
        dayRange.add("startDateId", "startTime")
        dayRange.add("endDateId", "stopTime")
        dayRange.add("label", translate("timePeriod"))
        val layout = super.createEditLayout(dto, userAccess)
                .add(UICustomized("timesheet.edit.templatesAndRecents"))
                .add(UICustomized("timesheet.edit.taskAndKost2", values = mutableMapOf("id" to "kost2")))
                .add(lc, "user")
                .add(dayRange)
                .add(UICustomized("task.consumption"))
                .add(UIInput("location", lc).enableAutoCompletion(this))
                .add(lc, "description")
                .addTranslations("until", "fibu.kost2", "task")
        Favorites.addTranslations(layout.translations)
        layout.addAction(UIButton("switch",
                title = translate("plugins.teamcal.switchToTeamEventButton"),
                color = UIColor.DARK,
                responseAction = ResponseAction(getRestRootPath("switch2CalendarEvent"), targetType = TargetType.POST)))
        layout.addTranslations("templates", "search.search", "fibu.kunde", "fibu.projekt", "timesheet.description", "timesheet.location")
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    /**
     * @return The list fo recent edited time sheets of the current logged in user.
     */
    @GetMapping("recents")
    fun getRecents(): RecentTimesheets {
        val pref = getTimesheetPrefData()
        val timesheets = pref.recents.map {
            val ts = Timesheet()
            ts.location = it.location
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
                val kost2 = kost2Dao.internalGetById(it.kost2Id)
                if (kost2 != null) {
                    ts.kost2 = Kost2()
                    ts.kost2!!.copyFromMinimal(kost2)
                }
            }
            val hcb = HashCodeBuilder()
            hcb.append(ts.kost2?.id)
                    .append(ts.task?.id)
                    .append(ts.user?.id)
                    .append(ts.location)
                    .append(ts.description)
            ts.hashKey = hcb.toHashCode()
            ts
        }
        return RecentTimesheets(timesheets, SystemInfoCache.instance().isCost2EntriesExists())
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
    fun switch2CalendarEvent(request: HttpServletRequest, @RequestBody timesheet: TimesheetDO)
            : ResponseAction {
        return if (useNewCalendarEvents) calendarEventRest.cloneFromTimesheet(request, timesheet)
        else teamEventRest.cloneFromTimesheet(request, timesheet)
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
                subject = teamEvent.subject)
        return cloneFromCalendarEvent(request, calendarEvent)
    }

    fun cloneFromCalendarEvent(request: HttpServletRequest, calendarEvent: CalEvent): ResponseAction {
        val timesheet = newBaseDO(request)
        timesheet.startTime = calendarEvent.startDate
        timesheet.stopTime = calendarEvent.endDate
        if (!calendarEvent.location.isNullOrBlank())
            timesheet.location = calendarEvent.location
        if (!calendarEvent.subject.isNullOrBlank() || !calendarEvent.note.isNullOrBlank())
            timesheet.description = "${calendarEvent.subject ?: ""} ${calendarEvent.note ?: ""}"
        val editLayoutData = getItemAndLayout(request, timesheet, UILayout.UserAccess(false, true))
        return ResponseAction(url = "/${Const.REACT_APP_PATH}calendar/${getRestPath(RestPaths.EDIT)}", targetType = TargetType.UPDATE)
                .addVariable("data", editLayoutData.data)
                .addVariable("ui", editLayoutData.ui)
                .addVariable("variables", editLayoutData.variables)
    }

    override fun onGetItemAndLayout(request: HttpServletRequest, dto: TimesheetDO, editLayoutData: EditLayoutData) {
        val startDateAsSeconds = NumberHelper.parseLong(request.getParameter("startDate"))
        if (startDateAsSeconds != null) dto.setStartDate(startDateAsSeconds * 1000)
        val endDateSeconds = NumberHelper.parseLong(request.getParameter("endDate"))
        if (endDateSeconds != null) dto.setStopDate(endDateSeconds * 1000)
        super.onGetItemAndLayout(request, dto, editLayoutData)
    }

    /**
     * Puts the task information such as path, consumption etc. as additional variable for the client, because the
     * origin task of the timesheet is of type TaskDO and doesn't contain such data.
     */
    override fun addVariablesForEditPage(dto: TimesheetDO): MutableMap<String, Any>? {
        val task = TaskServicesRest.createTask(dto.taskId) ?: return null
        return mutableMapOf("task" to task,
                "timesheetFavorites" to timesheetFavoritesService.getList())
    }

    private fun getTimesheetPrefData(): TimesheetPrefData {
        var pref: TimesheetPrefData? = userPrefService.getEntry(PREF_AREA, PREF_EDIT_NAME, TimesheetPrefData::class.java)
        if (pref == null) {
            val oldPrefKey = "org.projectforge.web.timesheet.TimesheetEditPage" // From Wicket version.
            pref = userPrefService.userXmlPreferencesService.getEntry(TimesheetPrefData::class.java, oldPrefKey)
            if (pref == null) {
                pref = TimesheetPrefData()
            }
            userPrefService.putEntry(PREF_AREA, PREF_EDIT_NAME, pref)
        }
        return pref
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        val element = UIFilterElement("kost2.nummer")
        element.label = element.id // Default label if no translation will be found below.
        element.label = LayoutListFilterUtils.getLabel(ElementInfo("nummer",
                i18nKey = "fibu.kost2.nummer",
                parent = ElementInfo("kost2", i18nKey = "fibu.kost2")))
        elements.add(element)
    }
}
