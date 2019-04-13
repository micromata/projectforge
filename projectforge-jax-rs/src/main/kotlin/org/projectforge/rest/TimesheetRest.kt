package org.projectforge.rest

import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.common.DateFormatType
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.ui.*
import org.springframework.stereotype.Component
import javax.ws.rs.Path

@Component
@Path("timesheet")
class TimesheetRest() : AbstractStandardRest<TimesheetDO, TimesheetDao, TimesheetFilter>(TimesheetDao::class.java, TimesheetFilter::class.java, "timesheet.title") {

    private val dateTimeFormatter = DateTimeFormatter.instance();

    /**
     * For exporting list of addresses.
     */
    private class Timesheet(val timesheet: TimesheetDO,
                            val weekOfYearShortLabel: String,
                            val dayOfWeekLabel: String)

    /**
     * Initializes new timesheets for adding.
     */
    override fun newBaseDO(): TimesheetDO {
        val sheet = super.newBaseDO()
        return sheet
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: TimesheetDO) {
    }


    override fun processResultSetBeforeExport(resultSet: ResultSet<Any>) {
        val list: List<TimesheetRest.Timesheet> = resultSet.resultSet.map { it ->
            TimesheetRest.Timesheet(it as TimesheetDO,
                    weekOfYearShortLabel = DateTimeFormatter.formatWeekOfYear(it.startTime),
                    dayOfWeekLabel = dateTimeFormatter.getFormattedDate(it.startTime,
                            DateFormats.getFormatString(DateFormatType.DAY_OF_WEEK_SHORT)))
        }
        resultSet.resultSet = list
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        lc.idPrefix = "timesheet."
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable() // Todo KW, WT, Range, Duration
                        .add(lc, "user")
                        .add(UITableColumn("timesheet.kost2.project.customer", "fibu.kunde", formatter = Formatter.CUSTOMER))
                        .add(UITableColumn("timesheet.kost2.project", "fibu.projekt", formatter = Formatter.PROJECT))
                        .add(lc, "task")
                        .add(UITableColumn("timesheet.kost2", "fibu.kost2", formatter = Formatter.COST2))
                        .add(UITableColumn("weekOfYearShortLabel", "calendar.weekOfYearShortLabel"))
                        .add(UITableColumn("dayName", "calendar.dayOfWeekShortLabel", dataType = UIDataType.CUSTOMIZED))
                        .add(UITableColumn("timePeriod", "timePeriod", dataType = UIDataType.CUSTOMIZED))
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
}
