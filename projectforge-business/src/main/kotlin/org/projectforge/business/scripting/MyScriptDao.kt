/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.scripting

import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Repository

/**
 * For non financial and controlling users.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class MyScriptDao : AbstractScriptDao() {
  /**
   * User must be member of group controlling or finance.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao.hasDeleteAccess
   */
  override fun hasAccess(
    user: PFUserDO, obj: ScriptDO?, oldObj: ScriptDO?,
    operationType: OperationType,
    throwException: Boolean
  ): Boolean {
    if (operationType != OperationType.SELECT) {
      if (throwException) {
        throw AccessException(AccessException.I18N_KEY_STANDARD, "ScriptDO", operationType)
      }
      return false
    }
    if (obj == null) {
      return true
    }
    if (!obj.isDeleted) {
      val userId = ThreadLocalUserContext.userId!!
      val userIdString = "$userId"
      obj.executableByUserIds?.split(",")?.forEach { userId ->
        if (userId.trim() == userIdString) {
          // Logged-in user is listed in executableByUserIds
          return true
        }
      }
      obj.executableByGroupIds?.split(",")?.forEach { groupId ->
        groupId.toIntOrNull()?.let { gid ->
          if (userGroupCache.isUserMemberOfGroup(userId, gid)) {
            // Logged-in user is member of this group listed in executableByGroupIds
            return true
          }
        }
      }
    }
    if (throwException) {
      throw AccessException(AccessException.I18N_KEY_STANDARD, "ScriptDO", operationType)
    }
    return false
  }
}
