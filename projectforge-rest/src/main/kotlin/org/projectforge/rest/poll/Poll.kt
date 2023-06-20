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
    var date: LocalDate? = null,
    var deadline: LocalDate? = null,
    var state: PollDO.State? = PollDO.State.RUNNING,
    var questionType: String? = null,
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
        groupAttendees = Group.toGroupList(src.groupAttendeesIds)
        attendees = User.toUserList(src.attendeesIds)
        if (src.inputFields != null) {
            val fields = ObjectMapper().readValue(src.inputFields, MutableList::class.java)
            inputFields = fields.map { Question().toObject(ObjectMapper().writeValueAsString(it)) }.toMutableList()
        }
    }

    override fun copyTo(dest: PollDO) {
        super.copyTo(dest)
        dest.fullAccessGroupIds = Group.toIntList(fullAccessGroups)
        dest.fullAccessUserIds = User.toIntList(fullAccessUsers)
        dest.groupAttendeesIds = Group.toIntList(groupAttendees)
        dest.attendeesIds = User.toIntList(attendees)
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