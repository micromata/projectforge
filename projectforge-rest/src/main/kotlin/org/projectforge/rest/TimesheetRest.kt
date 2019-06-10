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
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.business.timesheet.TimesheetPrefData
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.common.DateFormatType
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.rest.core.RestHelper
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.task.TaskServicesRest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/timesheet")
class TimesheetRest() : AbstractDORest<TimesheetDO, TimesheetDao, TimesheetFilter>(TimesheetDao::class.java, TimesheetFilter::class.java, "timesheet.title") {

    private val dateTimeFormatter = DateTimeFormatter.instance();

    @Autowired
    private lateinit var userPreferencesService: UserPreferencesService

    private val taskTree: TaskTree
        /** Lazy init, because test cases failed due to NPE in TenantRegistryMap. */
        get() = TaskTreeHelper.getTaskTree()

    /**
     * For exporting list of tiesheets.
     */
    private class Timesheet(val timesheet: TimesheetDO,
                            val id: Int, // Needed for history Service
                            val weekOfYear: String,
                            val dayName: String,
                            val timePeriod: String,
                            val duration: String)

    /**
     * Initializes new timesheets for adding.
     */
    override fun newBaseDO(request: HttpServletRequest): TimesheetDO {
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
        val entry = pref.getRecentEntry()
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

    override fun validate(validationErrors: MutableList<ValidationError>, obj: TimesheetDO) {
        if (obj.getDuration() < 60000) {// Duration is less than 60 seconds.
            validationErrors.add(ValidationError(translate("timesheet.error.zeroDuration"), fieldId = "stopTime"))
        } else if (obj.getDuration() > TimesheetDao.MAXIMUM_DURATION) {
            validationErrors.add(ValidationError(translate("timesheet.error.maximumDurationExceeded"), fieldId = "stopTime"))
        }
        if (Configuration.getInstance().isCostConfigured) {
            if (obj.kost2 == null) {
                val taskNode = taskTree.getTaskNodeById(obj.taskId)
                if (taskNode != null) {
                    val descendents = taskNode.getDescendantIds()
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

    override fun afterEdit(obj: TimesheetDO): ResponseAction {
        return ResponseAction("calendar").addVariable("id", obj.id ?: -1)
    }

    override fun processResultSetBeforeExport(resultSet: ResultSet<Any>) {
        val list: List<TimesheetRest.Timesheet> = resultSet.resultSet.map { it ->
            TimesheetRest.Timesheet(it as TimesheetDO,
                    id = it.id,
                    weekOfYear = DateTimeFormatter.formatWeekOfYear(it.startTime),
                    dayName = dateTimeFormatter.getFormattedDate(it.startTime,
                            DateFormats.getFormatString(DateFormatType.DAY_OF_WEEK_SHORT)),
                    timePeriod = dateTimeFormatter.getFormattedTimePeriodOfDay(it.timePeriod),
                    duration = dateTimeFormatter.getFormattedDuration(it.timePeriod))
        }
        resultSet.resultSet = list
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
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: TimesheetDO): UILayout {
        val dayRange = UICustomized("dayRange")
        dayRange.add("startDateId", "startTime")
        dayRange.add("endDateId", "stopTime")
        dayRange.add("label", translate("timePeriod"))
        val layout = super.createEditLayout(dataObject)
                .add(UICustomized("timesheet.edit.taskAndKost2", values = mutableMapOf("id" to "kost2")))
                .add(lc, "user")
                .add(dayRange)
                .add(UICustomized("taskConsumption"))
                .add(UIInput("location", lc).enableAutoCompletion(this))
                .add(lc, "description")
                .add(UIRow().add(UICol().add(UILabel("'ToDo: Validation, resetting Kost2-Combobox after task selection, favorites, templates, Testing..."))))
                .addTranslations("until", "fibu.kost2", "task")
        return LayoutUtils.processEditPage(layout, dataObject)
    }

    override fun addVariablesForEditPage(item: TimesheetDO): Map<String, Any>? {
        val task = TaskServicesRest.createTask(item.taskId)
        if (task == null)
            return null
        val variables = mutableMapOf<String, Any>("task" to task)
        return variables
    }

    private fun getTimesheetPrefData(): TimesheetPrefData {
        val prefKey = "timesheetEditPref";
        var pref: TimesheetPrefData? = userPreferencesService.getEntry(TimesheetPrefData::class.java, prefKey)
        if (pref == null) {
            val oldPrefKey = "org.projectforge.web.timesheet.TimesheetEditPage" // From Wicket version.
            pref = userPreferencesService.getEntry(TimesheetPrefData::class.java, oldPrefKey)
            if (pref == null) {
                pref = TimesheetPrefData()
            }
            userPreferencesService.putEntry(prefKey, pref, true)
        }
        return pref
    }

}
