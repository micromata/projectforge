/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.business.poll.PollDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import org.projectforge.rest.poll.types.Question
import java.time.LocalDate

class Poll(
    var title: String? = null,
    var description: String? = null,
    var owner: PFUserDO? = null,
    var location: String? = null,
    var deadline: LocalDate? = null,
    var state: PollDO.State? = PollDO.State.RUNNING,
    var questionType: String? = null,
    var customemailsubject: String? = null,
    var customemailcontent: String? = null,
    var prequestionType: String? = null,
    var inputFields: MutableList<Question>? = mutableListOf(),
    var fullAccessGroups: List<Group>? = null,
    var fullAccessUsers: List<User>? = null,
    var groupAttendees: List<Group>? = null,
    var attendees: List<User>? = null,
    var delegationUser: User? = null
) : BaseDTO<PollDO>() {
    override fun copyFrom(src: PollDO) {
        super.copyFrom(src)
        fullAccessGroups = Group.toGroupList(src.fullAccessGroupIds)
        fullAccessUsers = User.toUserList(src.fullAccessUserIds)
        groupAttendees = Group.toGroupList(src.groupAttendeeIds)
        attendees = User.toUserList(src.attendeeIds)
        if (src.inputFields != null) {
            val fields = ObjectMapper().readValue(src.inputFields, MutableList::class.java)
            inputFields = fields.map { Question().toObject(ObjectMapper().writeValueAsString(it)) }.toMutableList()
        }
    }

    override fun copyTo(dest: PollDO) {
        super.copyTo(dest)
        dest.fullAccessGroupIds = Group.toLongList(fullAccessGroups)
        dest.fullAccessUserIds = User.toLongList(fullAccessUsers)
        dest.groupAttendeeIds = Group.toLongList(groupAttendees)
        dest.attendeeIds = User.toLongList(attendees)
        if (inputFields != null) {
            dest.inputFields = ObjectMapper().writeValueAsString(inputFields)
        }
    }

    fun isAlreadyCreated(): Boolean {
        return id != null
    }

    fun isFinished(): Boolean {
        return state == PollDO.State.FINISHED
    }
}