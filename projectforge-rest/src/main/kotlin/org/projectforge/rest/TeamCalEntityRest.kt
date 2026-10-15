/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.admin.right.TeamCalRight
import org.projectforge.business.teamcal.service.CalendarFeedService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.calendar.CalendarSubscriptionInfo
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.TeamCal
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the team calendars (menu "Calendars"), serving the hand-built list and edit
 * page of projectforge-next (see components/features/teamCal). Replaces the React UILayout `TeamCalPagesRest`.
 *
 * The url of an external subscription may contain credentials, so it is sent only to users allowed to update the
 * calendar, and only in edit mode; everyone else gets the anonymized url. The subscription info of an existing
 * calendar (feed urls with and without reminders, QR code) travels with the edit DTO.
 */
@RestController
@RequestMapping("${Rest.URL}/teamCal")
class TeamCalEntityRest :
    AbstractDTOEntityRest<TeamCalDO, TeamCal, TeamCalDao>(TeamCalDao::class.java, "plugins.teamcal.title") {

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var calendarFeedService: CalendarFeedService

    override fun transformFromDB(obj: TeamCalDO, editMode: Boolean): TeamCal {
        val teamCal = TeamCal()
        obj.owner = caches.getUser(obj.ownerId)
        teamCal.copyFrom(obj)
        var anonymize = true
        if (editMode) {
            if (obj.id != null) {
                val right = TeamCalRight()
                if (right.hasUpdateAccess(ThreadLocalUserContext.loggedInUser!!, obj, obj)) {
                    // User has update access right, so don't remove externalSubscriptionUrl due to privacy reasons:
                    anonymize = false
                }
                teamCal.subscriptionInfo =
                    CalendarSubscriptionInfo(translate("plugins.teamcal.subscription"), teamCal.accessStatus)
                        .also { it.initUrls(calendarFeedService, obj.id) }
            }
        }
        if (anonymize) {
            // In list view and for users hasn't access to update the current object, the url will be anonymized due to privacy.
            teamCal.externalSubscriptionUrlAnonymized = obj.externalSubscriptionUrlAnonymized
            teamCal.externalSubscriptionUrl = null // Due to privacy reasons! Must be changed for editing mode.
        }

        // Display names needed by the multi autocompletion fields of the form:
        Group.restoreDisplayNames(teamCal.fullAccessGroups)
        Group.restoreDisplayNames(teamCal.readonlyAccessGroups)
        Group.restoreDisplayNames(teamCal.minimalAccessGroups)
        Group.restoreDisplayNames(teamCal.includeLeaveDaysForGroups)

        User.restoreDisplayNames(teamCal.fullAccessUsers)
        User.restoreDisplayNames(teamCal.readonlyAccessUsers)
        User.restoreDisplayNames(teamCal.minimalAccessUsers)
        User.restoreDisplayNames(teamCal.includeLeaveDaysForUsers)

        return teamCal
    }

    override fun transformForDB(dto: TeamCal): TeamCalDO {
        val teamCalDO = TeamCalDO()
        dto.copyTo(teamCalDO)
        return teamCalDO
    }

    override fun newBaseDTO(request: HttpServletRequest?): TeamCal {
        val cal = TeamCal()
        cal.owner = ThreadLocalUserContext.loggedInUser
        return cal
    }
}
