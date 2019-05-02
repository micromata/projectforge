package org.projectforge.rest

import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.TeamCalFilter
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/teamCal")
class TeamCalRest() : AbstractStandardRest<TeamCalDO, TeamCalDO, TeamCalDao, TeamCalFilter>(TeamCalDao::class.java, TeamCalFilter::class.java, "plugins.teamcal.title") {

    override fun validate(validationErrors: MutableList<ValidationError>, obj: TeamCalDO) {
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "", "titel")
                        .add(UITableColumn("kost2.project.customer", "fibu.kunde", formatter = Formatter.CUSTOMER))
                        .add(UITableColumn("kost2.project", "fibu.projekt", formatter = Formatter.PROJECT))
                        .add(UITableColumn("kost2", "fibu.kost2", formatter = Formatter.COST2))
                        .add(lc,  "location", "description"))
        layout.getTableColumnById("user").formatter = Formatter.USER
        layout.getTableColumnById("task").formatter = Formatter.TASK_PATH
        LayoutUtils.addListFilterContainer(layout, "longFormat", "recursive",
                filterClass = TimesheetFilter::class.java)
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: TeamCalDO): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(lc, "task", "kost2", "user", "startTime", "stopTime")
                .add(UICustomized("taskConsumption"))
                .add(lc, "location", "description")
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
