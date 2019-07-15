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

import org.projectforge.business.task.TaskTree
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.business.timesheet.TimesheetPrefData
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.DateFormatType
import org.projectforge.favorites.Favorites
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.calendar.TeamEventRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractBaseRest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.rest.core.RestHelper
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.task.TaskServicesRest
import org.projectforge.ui.*
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/timesheet")
class TimesheetRest : AbstractDORest<TimesheetDO, TimesheetDao>(TimesheetDao::class.java, "timesheet.title") {

    companion object {
        private const val PREF_AREA = "timesheet"
        private const val PREF_EDIT_NAME = "edit.recent"
    }

    private val dateTimeFormatter = DateTimeFormatter.instance()

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var teamEventRest: TeamEventRest

    private val taskTree: TaskTree
        /** Lazy init, because test cases failed due to NPE in TenantRegistryMap. */
        get() = TaskTreeHelper.getTaskTree()

    /**
     * For exporting list of timesheets.
     */
    private class Timesheet(val timesheet: TimesheetDO,
                            val id: Int, // Needed for history Service
                            val weekOfYear: String,
                            val dayName: String,
                            val timePeriod: String,
                            val duration: String)

    override fun getInitialList(request: HttpServletRequest): AbstractBaseRest.InitialListData {
        val taskId = NumberHelper.parseInteger(request.getParameter("taskId")) ?: return super.getInitialList(request)
        val filter = MagicFilter()
        filter.entries.add(MagicFilterEntry("task", value = "$taskId"))
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
            val start = PFDateTime.from(startTimeEpochSeconds)
            sheet.startTime = start.sqlTimestamp
        }
        if (endTimeEpochSeconds != null) {
            val stop = PFDateTime.from(endTimeEpochSeconds)
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

    override fun validate(validationErrors: MutableList<ValidationError>, dto: TimesheetDO) {
        if (dto.getDuration() < 60000) {// Duration is less than 60 seconds.
            validationErrors.add(ValidationError(translate("timesheet.error.zeroDuration"), fieldId = "stopTime"))
        } else if (dto.getDuration() > TimesheetDao.MAXIMUM_DURATION) {
            validationErrors.add(ValidationError(translate("timesheet.error.maximumDurationExceeded"), fieldId = "stopTime"))
        }
        if (Configuration.getInstance().isCostConfigured) {
            if (dto.kost2 == null) {
                val taskNode = taskTree.getTaskNodeById(dto.taskId)
                if (taskNode != null) {
                    val descendents = taskNode.descendantIds
                    for (taskId in descendents) {
                        if (!taskTree.getKost2List(taskId).isNullOrEmpty()) {
                            // But Kost2 is available for sub task, so user should book his time sheet
                            // on a sub task with kost2s.
                            validationErrors.add(ValidationError(translate("timesheet.error.kost2NeededChooseSubTask"), fieldId = "cost2"))
                            break
                        }
                    }
                }

            }
        }
    }

    override fun afterEdit(obj: TimesheetDO, dto: TimesheetDO): ResponseAction {
        return ResponseAction("/calendar")
                .addVariable("date", obj.startTime)
                .addVariable("id", obj.id ?: -1)
    }

    override fun processResultSetBeforeExport(resultSet: ResultSet<TimesheetDO>): ResultSet<*> {
        val list: List<Timesheet> = resultSet.resultSet.map {
            Timesheet(it,
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
    override fun createEditLayout(dto: TimesheetDO): UILayout {
        val dayRange = UICustomized("dayRange")
        dayRange.add("startDateId", "startTime")
        dayRange.add("endDateId", "stopTime")
        dayRange.add("label", translate("timePeriod"))
        val layout = super.createEditLayout(dto)
                .add(UICustomized("timesheet.edit.taskAndKost2", values = mutableMapOf("id" to "kost2")))
                .add(lc, "user")
                .add(dayRange)
                .add(UICustomized("taskConsumption"))
                .add(UIInput("location", lc).enableAutoCompletion(this))
                .add(lc, "description")
                .add(UIRow().add(UICol().add(UILabel("'ToDo: Validation, resetting Kost2-Combobox after task selection, favorites, templates, Testing..."))))
                .addTranslations("until", "fibu.kost2", "task")
        Favorites.addTranslations(layout.translations)
        layout.addAction(UIButton("switch",
                title = translate("plugins.teamcal.switchToTeamEventButton"),
                color = UIColor.SECONDARY,
                responseAction = ResponseAction(getRestRootPath("switch2CalendarEvent"), targetType = TargetType.POST)))

        return LayoutUtils.processEditPage(layout, dto, this)
    }

    /**
     * Will be called by clone button. Sets the id of the form data object to null and deleted to false.
     * @return ResponseAction with [TargetType.UPDATE] and variable "initial" with all the initial data of [getItemAndLayout] as given for new objects.
     */
    @RequestMapping("switch2CalendarEvent")
    fun switch2CalendarEvent(request: HttpServletRequest, @RequestBody timesheet: TimesheetDO)
            : ResponseAction {
        return teamEventRest.cloneFromTimesheet(request, timesheet)
    }

    fun cloneFromTimesheet(request: HttpServletRequest, teamEvent: TeamEventDO): ResponseAction {
        val timesheet = TimesheetDO()
        timesheet.startTime = teamEvent.startDate
        timesheet.stopTime = teamEvent.endDate
        timesheet.location = teamEvent.location
        timesheet.description = "${teamEvent.subject ?: ""} ${teamEvent.note ?: ""}"
        val editLayoutData = getItemAndLayout(request, timesheet)
        return ResponseAction(url = "/calendar/${getRestPath(RestPaths.EDIT)}", targetType = TargetType.UPDATE)
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

    override fun addVariablesForEditPage(dto: TimesheetDO): Map<String, Any>? {
        val task = TaskServicesRest.createTask(dto.taskId) ?: return null
        return mutableMapOf<String, Any>("task" to task)
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
        element.label = LayoutListFilterUtils.getLabel(ElementsRegistry.ElementInfo("nummer",
                i18nKey = "fibu.kost2.nummer",
                parent = ElementsRegistry.ElementInfo("kost2", i18nKey = "fibu.kost2")))
        elements.add(element)
    }
}
