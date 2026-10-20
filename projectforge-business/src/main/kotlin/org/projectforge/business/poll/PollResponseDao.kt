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

package org.projectforge.business.poll

import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class PollResponseDao : BaseDao<PollResponseDO>(PollResponseDO::class.java) {
    @Autowired
    private lateinit var pollDao: PollDao

    override fun newInstance(): PollResponseDO {
        return PollResponseDO()
    }

    override fun hasAccess(
        user: PFUserDO,
        obj: PollResponseDO?,
        oldObj: PollResponseDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        val result = if (obj == null) {
            // Lists: every entry is checked on its own.
            operationType == OperationType.SELECT
        } else {
            checkAccess(user, obj, oldObj, operationType)
        }
        if (!result && throwException) {
            throw AccessException(AccessException.I18N_KEY_STANDARD, "PollResponseDO", operationType)
        }
        return result
    }

    /**
     * A response belongs to its owner, who must take part in the poll (attendee, owner or full access). A user with
     * full access to the poll reads all responses, answers on behalf of an attendee and deletes responses (e.g.
     * together with the poll).
     */
    private fun checkAccess(
        user: PFUserDO,
        obj: PollResponseDO,
        oldObj: PollResponseDO?,
        operationType: OperationType,
    ): Boolean {
        val poll = obj.poll?.id?.let { pollDao.find(it, checkAccess = false) } ?: return false
        val ownerId = obj.owner?.id ?: return false
        val fullAccess = pollDao.hasFullAccess(poll)
        return when (operationType) {
            OperationType.SELECT -> ownerId == user.id || fullAccess
            OperationType.INSERT, OperationType.UPDATE -> {
                // An update must not move a response to another owner or poll.
                if (oldObj != null && (oldObj.owner?.id != ownerId || oldObj.poll?.id != poll.id)) {
                    false
                } else if (ownerId == user.id) {
                    fullAccess || pollDao.isAttendee(poll, ownerId)
                } else {
                    fullAccess && pollDao.isAttendee(poll, ownerId)
                }
            }
            else -> fullAccess
        }
    }
}
