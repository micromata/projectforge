/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.admin.right.TeamCalRight
import org.projectforge.business.teamcal.externalsubscription.SubscriptionUpdateInterval
import org.projectforge.business.teamcal.service.CalendarFeedService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.calendar.CalendarSubscriptionInfo
import org.projectforge.rest.calendar.CalendarSubscriptionInfoPageRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.TeamCal
import org.projectforge.rest.dto.User
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/teamCal")
class TeamCalPagesRest : AbstractDTOPagesRest<TeamCalDO, TeamCal, TeamCalDao>(TeamCalDao::class.java, "plugins.teamcal.title") {

    @Autowired
    private lateinit var calendarFeedService: CalendarFeedService

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var accessChecker: AccessChecker

    override fun transformFromDB(obj: TeamCalDO, editMode: Boolean): TeamCal {
        val teamCal = TeamCal()
        teamCal.copyFrom(obj)
        var anonymize = true
        if (editMode) {
            if (obj.id != null) {
                val right = TeamCalRight(accessChecker)
                if (right.hasUpdateAccess(ThreadLocalUserContext.user!!, obj, obj)) {
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

        // Group names needed by React client (for ReactSelect):
        Group.restoreDisplayNames(teamCal.fullAccessGroups, groupService)
        Group.restoreDisplayNames(teamCal.readonlyAccessGroups, groupService)
        Group.restoreDisplayNames(teamCal.minimalAccessGroups, groupService)
        Group.restoreDisplayNames(teamCal.includeLeaveDaysForGroups, groupService)

        // Usernames needed by React client (for ReactSelect):
        User.restoreDisplayNames(teamCal.fullAccessUsers, userService)
        User.restoreDisplayNames(teamCal.readonlyAccessUsers, userService)
        User.restoreDisplayNames(teamCal.minimalAccessUsers, userService)
        User.restoreDisplayNames(teamCal.includeLeaveDaysForUsers, userService)

        return teamCal
    }

    override fun transformForDB(dto: TeamCal): TeamCalDO {
        val teamCalDO = TeamCalDO()
        dto.copyTo(teamCalDO)
        return teamCalDO
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: TeamCal) {
    }

    override val classicsLinkListUrl: String?
        get() = "wa/wicket/bookmarkable/org.projectforge.web.teamcal.admin.TeamCalListPage"

    override fun newBaseDTO(request: HttpServletRequest?): TeamCal {
        val cal = TeamCal()
        cal.owner = ThreadLocalUserContext.user
        return cal
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
      layout.add(UITable.createUIResultSetTable()
                        .add(lc, "title", "externalSubscriptionUrlAnonymized", "description", "owner",
                                "accessStatusString", "lastUpdate"))//, "externalSubscription"))
        layout.getTableColumnById("owner").formatter = UITableColumn.Formatter.USER
        layout.getTableColumnById("lastUpdate").formatter = UITableColumn.Formatter.TIMESTAMP_MINUTES
        layout.getTableColumnById("accessStatusString").title = "access.title.heading"

        val exportMenu = MenuItem("calendar.export", i18nKey = "export")
        exportMenu.add(MenuItem("calendar.exportTimesheets",
                i18nKey = "plugins.teamcal.export.timesheets",
                type = MenuItemTargetType.MODAL,
                url = CalendarSubscriptionInfoPageRest.getTimesheetUserUrl()))
        exportMenu.add(MenuItem("calendar.exportWeekOfYears",
                i18nKey = "plugins.teamcal.export.weekOfYears",
                tooltip = "plugins.teamcal.export.weekOfYears.tooltip",
                type = MenuItemTargetType.MODAL,
                url = CalendarSubscriptionInfoPageRest.getWeekOfYearUrl()))
        exportMenu.add(MenuItem("calendar.exportHolidays",
                i18nKey = "plugins.teamcal.export.holidays",
                tooltip = "plugins.teamcal.export.holidays.tooltip",
                type = MenuItemTargetType.MODAL,
                url = CalendarSubscriptionInfoPageRest.getHolidaysUrl()))
        layout.add(exportMenu, 0)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: TeamCal, userAccess: UILayout.UserAccess): UILayout {
        val intervals = SubscriptionUpdateInterval.values().map { DisplayObject(it.interval, translate(it.i18nKey)) }
        val subscriptionInfo = CalendarSubscriptionInfo(translate("plugins.teamcal.subscription"), dto.accessStatus)
        subscriptionInfo.initUrls(calendarFeedService, dto.id)
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIFieldset(UILength(md = 12, lg = 12))
                        .add(UIRow()
                                .add(UICol()
                                        .add(UIInput("title", lc))
                                        .add(lc, "description")
                                        .add(UICustomized("calendar.editExternalSubscription",
                                                values = mutableMapOf("intervals" to intervals))))))
                .add(UIFieldset(UILength(md = 12, lg = 12), title = "access.title.heading")
                        .add(UIRow()
                                .add(UICol(UILength(md = 6))
                                        .add(lc, "owner")))
                        .add(UIRow()
                                .add(UIFieldset(6, title = "access.users")
                                        .add(UISelect.createUserSelect(lc, "fullAccessUsers", true, "plugins.teamcal.fullAccess"))
                                        .add(UISelect.createUserSelect(lc, "readonlyAccessUsers", true, "plugins.teamcal.readonlyAccess"))
                                        .add(UISelect.createUserSelect(lc, "minimalAccessUsers", true, "plugins.teamcal.minimalAccess", tooltip = "plugins.teamcal.minimalAccess.users.hint")))
                                .add(UIFieldset(6, title = "access.groups")
                                        .add(UISelect.createGroupSelect(lc, "fullAccessGroups", true, "plugins.teamcal.fullAccess"))
                                        .add(UISelect.createGroupSelect(lc, "readonlyAccessGroups", true, "plugins.teamcal.readonlyAccess"))
                                        .add(UISelect.createGroupSelect(lc, "minimalAccessGroups", true, "plugins.teamcal.minimalAccess", tooltip = "plugins.teamcal.minimalAccess.groups.hint")))))
                .add(UIFieldset(UILength(md = 12, lg = 12), title = "vacation")
                        .add(UIRow()
                                .add(UICol()
                                        .add(UISelect.createUserSelect(lc, "includeLeaveDaysForUsers", true)))
                                .add(UICol()
                                        .add(UISelect.createGroupSelect(lc, "includeLeaveDaysForGroups", true)))))
        if (dto.id != null) {
            // Show subscription barcode and url only for existing entries.
            layout.add(UIFieldset(UILength(md = 12, lg = 12))
                    .add(UIRow()
                            .add(UICol()
                                    .add(UICustomized("calendar.subscriptionInfo",
                                            values = mutableMapOf("subscriptionInfo" to subscriptionInfo))))))
        }
        layout.addTranslations("plugins.teamcal.externalsubscription.label",
                "plugins.teamcal.externalsubscription.label",
                "plugins.teamcal.externalsubscription.url.tooltip",
                "plugins.teamcal.externalsubscription.url",
                "plugins.teamcal.externalsubscription.updateInterval",
                "plugins.teamcal.export.reminder.checkbox",
                "plugins.teamcal.export.reminder.checkbox.tooltip")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
