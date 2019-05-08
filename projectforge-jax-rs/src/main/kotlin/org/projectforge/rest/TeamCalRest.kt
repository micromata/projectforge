package org.projectforge.rest

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.TeamCalFilter
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.business.user.service.UserService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.TeamCal
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/teamCal")
class TeamCalRest() : AbstractDTORest<TeamCalDO, TeamCal, TeamCalDao, TeamCalFilter>(TeamCalDao::class.java, TeamCalFilter::class.java, "plugins.teamcal.title") {

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    override fun transformDO(obj: TeamCalDO): TeamCal {
        val kunde = TeamCal()
        kunde.copyFrom(obj)
        return kunde
    }

    override fun transformDTO(dto: TeamCal): TeamCalDO {
        val kundeDO = TeamCalDO()
        dto.copyTo(kundeDO)
        return kundeDO
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: TeamCalDO) {
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "title", "externalSubscriptionUrl", "description", "owner",
                                "accessright", "last_update", "externalSubscription"))
        layout.getTableColumnById("owner").formatter = Formatter.USER
        layout.getTableColumnById("last_update").formatter = Formatter.TIMESTAMP_MINUTES
        LayoutUtils.addListFilterContainer(layout, "longFormat", "recursive",
                filterClass = TimesheetFilter::class.java)
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: TeamCalDO): UILayout {
        val allGroups = mutableListOf<UISelectValue<Int>>()
        groupService.sortedGroups?.forEach {
            allGroups.add(UISelectValue(it.id, it.name))
        }

        val allUsers = mutableListOf<UISelectValue<Int>>()
        userService.sortedUsers?.forEach {
            allUsers.add(UISelectValue(it.id, it.fullname))
        }

        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "title")
                                .add(lc, "description"))
                        .add(UICol()
                                .add(lc, "owner")))
                .add(UIRow()
                        .add(UICol()
                                .add(UIMultiSelect("fullAccessUsers", lc,
                                        label = "plugins.teamcal.fullAccess",
                                        additionalLabel = "access.users",
                                        values = allUsers,
                                        labelProperty = "fullname",
                                        valueProperty = "id"))
                                .add(UIMultiSelect("readonlyAccessUsers", lc,
                                        label = "plugins.teamcal.readonlyAccess",
                                        additionalLabel = "access.users",
                                        values = allUsers,
                                        labelProperty = "fullname",
                                        valueProperty = "id"))
                                .add(UIMultiSelect("minimalAccessUsers", lc,
                                        label = "plugins.teamcal.minimalAccess",
                                        additionalLabel = "access.users",
                                        values = allUsers,
                                        labelProperty = "fullname",
                                        valueProperty = "id")))
                        .add(UICol()
                                .add(UIMultiSelect("fullAccessGroups", lc,
                                        label = "plugins.teamcal.fullAccess",
                                        additionalLabel = "access.groups",
                                        values = allGroups,
                                        labelProperty = "name",
                                        valueProperty = "id"))
                                .add(UIMultiSelect("readonlyAccessGroups", lc,
                                        label = "plugins.teamcal.readonlyAccess",
                                        additionalLabel = "access.groups",
                                        values = allGroups,
                                        labelProperty = "name",
                                        valueProperty = "id"))
                                .add(UIMultiSelect("minimalAccessUsers", lc,
                                        label = "plugins.teamcal.minimalAccess",
                                        additionalLabel = "access.groups",
                                        values = allUsers,
                                        labelProperty = "name",
                                        valueProperty = "id"))))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
