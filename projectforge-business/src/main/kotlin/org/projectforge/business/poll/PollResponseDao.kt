package org.projectforge.business.poll

import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Repository

@Repository
open class PollResponseDao : BaseDao<PollResponseDO>(PollResponseDO::class.java) {
    override fun newInstance(): PollResponseDO {
        return PollResponseDO()
    }

    override fun hasAccess(
        user: PFUserDO?,
        obj: PollResponseDO?,
        oldObj: PollResponseDO?,
        operationType: OperationType?,
        throwException: Boolean
    ): Boolean {
        return true
    }
}