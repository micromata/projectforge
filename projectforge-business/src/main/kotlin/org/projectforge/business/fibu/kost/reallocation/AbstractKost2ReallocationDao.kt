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

package org.projectforge.business.fibu.kost.reallocation

import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * Base of the DAOs of the Kost2 reallocation: writing is only allowed for the finance group, reading for finance (and
 * for controlling, if [controllingMaySelect]). There is no user right, so [hasAccess] and [hasInsertAccess] are both
 * overridden (the defaults of [BaseDao] would allow everybody).
 */
abstract class AbstractKost2ReallocationDao<O : ExtendedBaseDO<Long>>(
    clazz: Class<O>,
    private val controllingMaySelect: Boolean = false,
) : BaseDao<O>(clazz) {

    override fun hasAccess(
        user: PFUserDO, obj: O?, oldObj: O?, operationType: OperationType, throwException: Boolean,
    ): Boolean {
        val allowed = userGroupCache.isUserMemberOfFinanceGroup(user.id) ||
                (operationType == OperationType.SELECT && controllingMaySelect &&
                        userGroupCache.isUserMemberOfControllingGroup(user.id))
        if (!allowed && throwException) {
            throw AccessException(
                user, "access.exception.userHasNotRight", ProjectForgeGroup.FINANCE_GROUP.key, operationType,
            )
        }
        return allowed
    }

    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return userGroupCache.isUserMemberOfFinanceGroup(user.id)
    }
}
