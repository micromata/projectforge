/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.merlin

import org.projectforge.common.StringHelper
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Repository
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class MerlinTemplateDao : BaseDao<MerlinTemplateDO>(MerlinTemplateDO::class.java) {

  override fun hasUserSelectAccess(user: PFUserDO?, throwException: Boolean): Boolean {
    return true // Select access in general for all registered users
  }

  override fun hasAccess(
    user: PFUserDO,
    obj: MerlinTemplateDO,
    oldObj: MerlinTemplateDO?,
    operationType: OperationType,
    throwException: Boolean
  ): Boolean {
    val adminIds = StringHelper.splitToIntegers(obj.adminIds, ",")
    if (adminIds.contains(user.id)) {
      return true
    }
    if (operationType == OperationType.SELECT) {
      em.detach(obj)
      // Select access also for those users:
      StringHelper.splitToIntegers(obj.accessUserIds, ",")?.let {
        if (it.contains(user.id)) {
          return true
        }
      }
      StringHelper.splitToIntegers(obj.accessGroupIds, ",")?.let {
        if (userGroupCache.isUserMemberOfAtLeastOneGroup(user.id, *it)) {
          return true
        }
      }
    }
    if (throwException) {
      throw AccessException(user, "access.exception.userHasNotRight")
    }
    return false
  }

  override fun getDefaultSortProperties(): Array<SortProperty> {
    return arrayOf(SortProperty.desc("lastUpdate"))
  }

  override fun newInstance(): MerlinTemplateDO {
    return MerlinTemplateDO()
  }

  override fun onSave(obj: MerlinTemplateDO) {
    if (!obj.variables.isNullOrBlank() || !obj.dependentVariables.isNullOrBlank()) {
      // Variables were changed:
      obj.lastVariableUpdate = Date()
    }
  }

  override fun onChange(obj: MerlinTemplateDO, dbObj: MerlinTemplateDO) {
    if (obj.variables != dbObj.variables || obj.dependentVariables != dbObj.dependentVariables) {
      // Variables were changed:
      obj.lastVariableUpdate = Date()
    }
  }
}
