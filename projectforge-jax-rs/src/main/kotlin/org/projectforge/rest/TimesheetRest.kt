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
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Path

@Component
@Path("timesheet")
class TimesheetRest() : AbstractStandardRest<TimesheetDO, TimesheetDao, TimesheetFilter>(TimesheetDao::class.java, TimesheetFilter::class.java, "timesheet.title") {

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
        val startTimeEpochSeconds = restHelper.parseLong(request, "start")
        val endTimeEpochSeconds = restHelper.parseLong(request, "end")
        if (startTimeEpochSeconds != null) {
            sheet.setStartDate(startTimeEpochSeconds * 1000)
        }
        if (endTimeEpochSeconds != null) {
            sheet.setStopDate(endTimeEpochSeconds * 1000)
        }
        val userId: Int? = null // Optional parameter given to edit page
        if (userId != null) {
            baseDao.setUser(sheet, userId)
        }
        val pref = getTimesheetPrefData()
        val entry = pref.getNewesRecentEntry()
        if (sheet.taskId == null && entry != null) {
            baseDao.setTask(sheet, entry.taskId)
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
    override fun createEditLayout(dataObject: TimesheetDO?): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(lc, "task", "kost2", "user", "startTime", "stopTime")
                .add(UICustomized("taskConsumption"))
                .add(lc, "location", "description")
        return LayoutUtils.processEditPage(layout, dataObject)
    }


    private fun getTimesheetPrefData(): TimesheetPrefData {
        val prefKey = "timesheetEditPref";
        var pref: TimesheetPrefData? = userPreferencesService.getEntry(TimesheetPrefData::class.java, prefKey)
        if (pref == null) {
            pref = TimesheetPrefData()
            userPreferencesService.putEntry(prefKey, pref, true)
        }
        return pref
    }

}
