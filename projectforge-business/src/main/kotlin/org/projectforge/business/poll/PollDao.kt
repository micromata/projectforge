package org.projectforge.business.poll

import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.user
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Repository

@Repository
open class PollDao : BaseDao<PollDO>(PollDO::class.java){

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
        /*val loggedInUserId = ThreadLocalUserContext.user?.id
        if (obj == null) {
            return true
        }
        if (obj.fullAccessUserIds!!.contains(loggedInUserId.toString())) {
            return true
        }*/
        if (operationType == OperationType.SELECT){
            return true
        };
        return false
    }


    override fun onSaveOrModify(obj: PollDO) {
        val loggedInUser = user
        val userString = obj.fullAccessUserIds
        if (loggedInUser != null && !obj.fullAccessUserIds!!.contains(loggedInUser.id.toString())) {
            obj.fullAccessUserIds = userString.plus(", ${loggedInUser.id}");
        }
        // Prüfen, ob loggedInUser in accessUsers, wenn nicht, hinuif+gen
    super.onSaveOrModify(obj)
    }
}