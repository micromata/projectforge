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

package org.projectforge.rest.poll

import org.projectforge.business.common.BaseUserGroupRightService
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollJson
import org.projectforge.business.poll.PollQuestion
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.EntityAccessSupport
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import java.time.LocalDate

/**
 * A poll as the next pages see it (list and edit, see PollEntityRest). The users and groups are stored as csv lists
 * of ids, the questions as json ([PollDO.questions]).
 */
class Poll(
    var title: String? = null,
    var description: String? = null,
    var owner: User? = null,
    var location: String? = null,
    var deadline: LocalDate? = null,
    var state: PollDO.State = PollDO.State.RUNNING,
    var template: Boolean = false,
    var questions: MutableList<PollQuestion> = mutableListOf(),
    var attendeeUsers: List<User>? = null,
    var attendeeGroups: List<Group>? = null,
    var fullAccessUsers: List<User>? = null,
    var fullAccessGroups: List<Group>? = null,
    /** Subject of the invitation mail, only used when the poll is created (not stored). */
    var invitationSubject: String? = null,
    /** Text of the invitation mail, only used when the poll is created (not stored). */
    var invitationText: String? = null,
    /** Whether the logged-in user has full access (edits the poll and sees all answers); set for list and edit. */
    var fullAccess: Boolean? = null,
    /** Whether the logged-in user takes part in the poll; set for list and edit. */
    var attendee: Boolean? = null,
    override var writeAccess: Boolean? = null,
    override var deleteAccess: Boolean? = null,
) : BaseDTO<PollDO>(), EntityAccessSupport {

    override fun copyFrom(src: PollDO) {
        super.copyFrom(src)
        questions = PollJson.questions(src.questions)
        attendeeUsers = User.toUserList(src.attendeeUserIds)
        attendeeGroups = Group.toGroupList(src.attendeeGroupIds)
        fullAccessUsers = User.toUserList(src.fullAccessUserIds)
        fullAccessGroups = Group.toGroupList(src.fullAccessGroupIds)
    }

    override fun copyTo(dest: PollDO) {
        super.copyTo(dest)
        dest.questions = PollJson.toJson(questions)
        dest.attendeeUserIds = BaseUserGroupRightService.asSortedIdStrings(attendeeUsers)
        dest.attendeeGroupIds = BaseUserGroupRightService.asSortedIdStrings(attendeeGroups)
        dest.fullAccessUserIds = BaseUserGroupRightService.asSortedIdStrings(fullAccessUsers)
        dest.fullAccessGroupIds = BaseUserGroupRightService.asSortedIdStrings(fullAccessGroups)
    }

    /** Display names of all users and groups, for the list and the multi selects of the form. */
    fun restoreDisplayNames() {
        User.restoreDisplayNames(attendeeUsers)
        User.restoreDisplayNames(fullAccessUsers)
        Group.restoreDisplayNames(attendeeGroups)
        Group.restoreDisplayNames(fullAccessGroups)
        owner?.let { User.restoreDisplayNames(listOf(it)) }
    }
}
