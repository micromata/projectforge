package org.projectforge.business.poll

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.business.group.service.GroupService
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.user
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class PollDao : BaseDao<PollDO>(PollDO::class.java){

    @Autowired
    private val groupService: GroupService? = null

    override fun newInstance(): PollDO {
        return PollDO()
    }

    override fun hasAccess(
        user: PFUserDO?,
        obj: PollDO?,
        oldObj: PollDO?,
        operationType: OperationType?,
        throwException: Boolean
    ): Boolean {

        if (obj == null && operationType == OperationType.SELECT) {
            return true
        };
        if (obj != null && operationType == OperationType.SELECT){
            if(hasFullAccess(obj) || isAttendee(obj))
                return true
        }
        if(obj != null) {
            return hasFullAccess(obj)
        }
        return false
    }
    fun hasFullAccess(obj: PollDO): Boolean {
        val loggedInUser = user
        if(!obj.fullAccessUserIds.isNullOrBlank() && obj.fullAccessUserIds!!.contains(loggedInUser?.id.toString()))
            return true
        if(obj.owner?.id == loggedInUser?.id)
            return true
        if (!obj.fullAccessGroupIds.isNullOrBlank()){
            val groupIdArray = obj.fullAccessGroupIds!!.split(", ").map { it.toInt() }.toIntArray()
            val groupUsers = groupService?.getGroupUsers(groupIdArray)
            if(groupUsers?.contains(loggedInUser) == true)
                return true
        }
        return false
    }

    private fun isAttendee(obj: PollDO): Boolean {
        val loggedInUser = user
        val listOfAttendeesIds = ObjectMapper().readValue(obj.attendeeIds, IntArray::class.java)
        if (loggedInUser != null) {
            if(listOfAttendeesIds.contains(loggedInUser.id)){
                return true
            }
        }
        obj.attendeeIds= ObjectMapper().writeValueAsString(listOfAttendeesIds)


        return false
    }
}