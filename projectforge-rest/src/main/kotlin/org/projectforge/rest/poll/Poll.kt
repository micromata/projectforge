package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.poll.types.Question
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
    var inputFields: MutableList<Question>? = mutableListOf(),
    var fullAccessGroups: List<Group>? = null,
    var fullAccessUsers: List<User>? = null,
    var groupAttendees: List<Group>? = null,
    var attendees: List<User>? = null,
    private var frontendState: String? = null
) : BaseDTO<PollDO>() {
    override fun copyFrom(src: PollDO) {
        super.copyFrom(src)
        fullAccessGroups = Group.toGroupList(src.fullAccessGroupIds)
        fullAccessUsers = User.toUserList(src.fullAccessUserIds)
        groupAttendees = Group.toGroupList(src.groupAttendeeIds)
        attendees = User.toUserList(src.attendeeIds)
        frontendState = if (state == PollDO.State.RUNNING)
            "Endet am $deadline"
        else
            "Beendet"
    }

    override fun copyTo(dest: PollDO) {
        super.copyTo(dest)
        dest.fullAccessGroupIds = Group.toIntList(fullAccessGroups)
        dest.fullAccessUserIds = User.toIntList(fullAccessUsers)
        dest.groupAttendeeIds = Group.toIntList(groupAttendees)
        dest.attendeeIds = User.toIntList(attendees)
    }

}