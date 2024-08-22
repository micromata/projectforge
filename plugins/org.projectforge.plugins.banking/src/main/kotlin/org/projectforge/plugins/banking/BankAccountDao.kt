/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.banking

import org.projectforge.business.common.BaseUserGroupRightUtils
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseDaoSupport
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Repository

/**
 * This is the base data access object class. Most functionality such as access checking, select, insert, update, save,
 * delete etc. is implemented by the super class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class BankAccountDao : BaseDao<BankAccountDO>(BankAccountDO::class.java) {

  override fun hasAccess(
    user: PFUserDO,
    obj: BankAccountDO?,
    oldObj: BankAccountDO?,
    operationType: OperationType,
    throwException: Boolean
  ): Boolean {
    if (!accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)) {
      return BaseDaoSupport.returnFalseOrThrowException(throwException, operationType = operationType)
    }
    if (accessChecker.isUserMemberOfAdminGroup(user)) {
      // Admins AND Finance staff have always access.
      return true
    }
    if (!accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)) {
      // Only access for financial staff.
      return BaseDaoSupport.returnFalseOrThrowException(throwException, operationType = operationType)
    }
    if (obj == null && oldObj == null) {
      // Financial staff has general read access.
      return true
    }
    return BaseUserGroupRightUtils.hasAccess(
      obj = obj,
      oldObj = oldObj,
      userId = user?.id,
      operationType = operationType,
      throwException = throwException,
    )
  }

  override fun newInstance(): BankAccountDO {
    return BankAccountDO()
  }
}
