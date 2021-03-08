/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation

import org.projectforge.business.user.*
import org.projectforge.business.vacation.model.LeaveAccountEntryDO
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class VacationRight(accessChecker: AccessChecker) : UserRightAccessCheck<LeaveAccountEntryDO>(accessChecker, UserRightId.HR_VACATION, UserRightCategory.HR,
        *UserRightServiceImpl.FALSE_READONLY_READWRITE) {

    override fun hasSelectAccess(user: PFUserDO): Boolean {
        return accessChecker.hasRight(user, id, UserRightValue.READONLY, UserRightValue.READWRITE)
    }

    override fun hasAccess(user: PFUserDO, obj: LeaveAccountEntryDO, oldObj: LeaveAccountEntryDO,
                           operationType: OperationType): Boolean {
        if (operationType == OperationType.SELECT) {
            if (accessChecker.hasRight(user, id, UserRightValue.READWRITE)) {
                return true
            }
            return obj.employee?.userId == user.id // User has select access to own entry.
        }
        return accessChecker.hasRight(user, id, UserRightValue.READWRITE)
    }
}
