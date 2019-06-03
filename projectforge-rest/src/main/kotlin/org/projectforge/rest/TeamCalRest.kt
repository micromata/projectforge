package org.projectforge.rest

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.TeamCalFilter
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.admin.right.TeamCalRight
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
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

    @Autowired
    private lateinit var accessChecker: AccessChecker

    override fun transformDO(obj: TeamCalDO, editMode: Boolean): TeamCal {
        val teamCal = TeamCal()
        teamCal.copyFrom(obj)
        var anonymize = true
        if (editMode) {
            if (obj.id != null) {
                val right = TeamCalRight(accessChecker)
                if (right.hasUpdateAccess(ThreadLocalUserContext.getUser(), obj, obj)) {
                    // User has update access right, so don't remove externalSubscriptionUrl due to privacy reasons:
                    anonymize = false
                }
            }
        }
        if (anonymize) {
            // In list view and for users hasn't access to update the current object, the url will be anonymized due to privacy.
            teamCal.externalSubscriptionUrlAnonymized = obj.externalSubscriptionUrlAnonymized
            teamCal.externalSubscriptionUrl = null // Due to privacy reasons! Must be changed for editing mode.
        }
        return teamCal
    }

    override fun transformDTO(dto: TeamCal): TeamCalDO {
        val teamCalDO = TeamCalDO()
        dto.copyTo(teamCalDO)
        return teamCalDO
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: TeamCalDO) {
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "title", "externalSubscriptionUrlAnonymized", "description", "owner",
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
            allGroups.add(UISelectValue(it.id, it.name!!))
        }

        val allUsers = mutableListOf<UISelectValue<Int>>()
        userService.sortedUsers?.forEach {
            allUsers.add(UISelectValue(it.id, it.getFullname()))
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
                                .add(UISelect("fullAccessUsers", lc,
                                        multi = true,
                                        label = "plugins.teamcal.fullAccess",
                                        additionalLabel = "access.users",
                                        values = allUsers,
                                        labelProperty = "fullname",
                                        valueProperty = "id"))
                                .add(UISelect("readonlyAccessUsers", lc,
                                        multi = true,
                                        label = "plugins.teamcal.readonlyAccess",
                                        additionalLabel = "access.users",
                                        values = allUsers,
                                        labelProperty = "fullname",
                                        valueProperty = "id"))
                                .add(UISelect("minimalAccessUsers", lc,
                                        multi = true,
                                        label = "plugins.teamcal.minimalAccess",
                                        additionalLabel = "access.users",
                                        values = allUsers,
                                        labelProperty = "fullname",
                                        valueProperty = "id")))
                        .add(UICol()
                                .add(UISelect("fullAccessGroups", lc,
                                        multi = true,
                                        label = "plugins.teamcal.fullAccess",
                                        additionalLabel = "access.groups",
                                        values = allGroups,
                                        labelProperty = "name",
                                        valueProperty = "id"))
                                .add(UISelect("readonlyAccessGroups", lc,
                                        multi = true,
                                        label = "plugins.teamcal.readonlyAccess",
                                        additionalLabel = "access.groups",
                                        values = allGroups,
                                        labelProperty = "name",
                                        valueProperty = "id"))
                                .add(UISelect("minimalAccessUsers", lc,
                                        multi = true,
                                        label = "plugins.teamcal.minimalAccess",
                                        additionalLabel = "access.groups",
                                        values = allUsers,
                                        labelProperty = "name",
                                        valueProperty = "id"))))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
