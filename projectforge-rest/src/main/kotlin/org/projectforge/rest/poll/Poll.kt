package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.BaseDTO
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
    var fullAccessGroups: List<Group>? = null,
    var fullAccessUsers: List<User>? = null
    ) : BaseDTO<PollDO>() {
    override fun copyFrom(src: PollDO) {
        super.copyFrom(src)
        fullAccessGroups = Group.toGroupList(src.fullAccessGroupIds)
        fullAccessUsers = User.toUserList(src.fullAccessUserIds)
    }

    // The user and group ids are stored as csv list of integers in the data base.
    override fun copyTo(dest: PollDO) {
        super.copyTo(dest)
        dest.fullAccessGroupIds = Group.toIntList(fullAccessGroups)
        dest.fullAccessUserIds = User.toIntList(fullAccessUsers)
    }
}