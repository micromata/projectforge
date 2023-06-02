package org.projectforge.business.poll.filter

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.poll.PollDO
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO

class PollAssignmentFilter(val values: List<PollAssignment>) : CustomResultFilter<PollDO> {

    override fun match(list: MutableList<PollDO>, element: PollDO): Boolean {
        var foundUser: PFUserDO? = null
        if (!element.fullAccessGroupIds.isNullOrEmpty()) {
            val groupIds = element.fullAccessGroupIds!!.split(", ").map { it.toInt() }.toIntArray()
            val accessUsers = groupService.getGroupUsers(groupIds)
            val localUser = ThreadLocalUserContext.userId!!
            foundUser = accessUsers.firstOrNull { user -> user.id == localUser }
        }

        values.forEach { filter ->
            if (element.getPollAssignment()
                    .contains(filter) || (filter == PollAssignment.ACCESS && foundUser != null)
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        private var _groupService: GroupService? = null
        private val groupService: GroupService
            get() {
                if (_groupService == null) {
                    _groupService = ApplicationContextProvider.getApplicationContext().getBean(GroupService::class.java)
                }
                return _groupService!!
            }
    }
}