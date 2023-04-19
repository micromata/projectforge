package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.poll.types.Frage
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
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
    var inputFields: MutableList<Frage>? = null,
    var fullAccessGroups: List<Group>? = null,
    var fullAccessUsers: List<User>? = null,
    var groupAttendees: List<Group>? = null,
    var attendees: List<User>? = null
) : BaseDTO<PollDO>() {
    override fun copyFrom(src: PollDO) {
        super.copyFrom(src)
        fullAccessGroups = Group.toGroupList(src.fullAccessGroupIds)
        fullAccessUsers = User.toUserList(src.fullAccessUserIds)
        groupAttendees = Group.toGroupList(src.groupAttendeesIds)
        attendees = User.toUserList(src.attendeesIds)
    }

    override fun copyTo(dest: PollDO) {
        super.copyTo(dest)
        dest.fullAccessGroupIds = Group.toIntList(fullAccessGroups)
        dest.fullAccessUserIds = User.toIntList(fullAccessUsers)
        dest.groupAttendeesIds = Group.toIntList(groupAttendees)
        dest.attendeesIds = User.toIntList(attendees)
    }

}