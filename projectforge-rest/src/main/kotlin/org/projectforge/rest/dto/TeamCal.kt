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

package org.projectforge.rest.dto

import org.projectforge.business.common.BaseUserGroupRightService
import org.projectforge.business.teamcal.CalendarAccessStatus
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.admin.right.TeamCalRight
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO

class TeamCal(
    var title: String? = null,
    var owner: PFUserDO? = null,
    var description: String? = null,
    var accessStatus: CalendarAccessStatus? = null,
    var accessStatusString: String? = null,
    var fullAccessGroups: List<Group>? = null,
    var fullAccessUsers: List<User>? = null,
    var readonlyAccessGroups: List<Group>? = null,
    var readonlyAccessUsers: List<User>? = null,
    var minimalAccessGroups: List<Group>? = null,
    var minimalAccessUsers: List<User>? = null,
    var includeLeaveDaysForUsers: List<User>? = null,
    var includeLeaveDaysForGroups: List<Group>? = null,
    var externalSubscription: Boolean = false,
    var externalSubscriptionUrl: String? = null,
    /**
     * In seconds.
     */
    var externalSubscriptionUpdateInterval: Int? = null,
    var externalSubscriptionUrlAnonymized: String? = null,
    var vacation4Groups: List<Long>? = null,
    var vacation4Users: List<Long>? = null
) : BaseDTO<TeamCalDO>() {
    // The user and group ids are stored as csv list of longs in the database.
    override fun copyFrom(src: TeamCalDO) {
        super.copyFrom(src)
        fullAccessGroups = Group.toGroupList(src.fullAccessGroupIds)
        fullAccessUsers = User.toUserList(src.fullAccessUserIds)
        readonlyAccessGroups = Group.toGroupList(src.readonlyAccessGroupIds)
        readonlyAccessUsers = User.toUserList(src.readonlyAccessUserIds)
        minimalAccessGroups = Group.toGroupList(src.minimalAccessGroupIds)
        minimalAccessUsers = User.toUserList(src.minimalAccessUserIds)

        includeLeaveDaysForGroups = Group.toGroupList(src.includeLeaveDaysForGroups)
        includeLeaveDaysForUsers = User.toUserList(src.includeLeaveDaysForUsers)

        val teamCalDao = ApplicationContextProvider.getApplicationContext().getBean(TeamCalDao::class.java)
        val accessChecker = ApplicationContextProvider.getApplicationContext().getBean(AccessChecker::class.java)
        val right = teamCalDao.userRight as TeamCalRight
        val loggedInUserId = ThreadLocalUserContext.loggedInUserId
        accessStatus = when {
            right.isOwner(loggedInUserId, src) -> CalendarAccessStatus.OWNER
            right.hasFullAccess(src, loggedInUserId) -> CalendarAccessStatus.FULL_ACCESS
            right.hasReadonlyAccess(src, loggedInUserId) -> CalendarAccessStatus.READONLY_ACCESS
            right.hasMinimalAccess(src, loggedInUserId) -> CalendarAccessStatus.MINIMAL_ACCESS
            accessChecker.isLoggedInUserMemberOfAdminGroup -> CalendarAccessStatus.ADMIN_ACCESS
            else -> null
        }
        accessStatus?.let { accessStatusString = translate(it.i18nKey) }
    }

    // The user and group ids are stored as csv list of longs in the database.
    override fun copyTo(dest: TeamCalDO) {
        super.copyTo(dest)
        val svc = BaseUserGroupRightService.instance
        svc.setFullAccessGroups(dest, fullAccessGroups)
        svc.setFullAccessUsers(dest, fullAccessUsers)
        svc.setReadonlyAccessGroups(dest, readonlyAccessGroups)
        svc.setReadonlyAccessUsers(dest, readonlyAccessUsers)
        svc.setMinimalAccessGroups(dest, minimalAccessGroups)
        svc.setMinimalAccessUsers(dest, minimalAccessUsers)

        dest.includeLeaveDaysForGroups = Group.toLongList(includeLeaveDaysForGroups)
        dest.includeLeaveDaysForUsers = User.toLongList(includeLeaveDaysForUsers)
    }
}
