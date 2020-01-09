/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation.repository

import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.model.LeaveAccountEntryDO
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Repository

@Repository
open class LeaveAccountEntryDao : BaseDao<LeaveAccountEntryDO>(LeaveAccountEntryDO::class.java) {
    override fun newInstance(): LeaveAccountEntryDO {
        return LeaveAccountEntryDO()
    }

    override fun getList(filter: BaseSearchFilter): List<LeaveAccountEntryDO?>? {
        val queryFilter = createQueryFilter(filter)
        queryFilter.addOrder(asc("employee.user.firstname"))
        queryFilter.addOrder(asc("date"))
        return getList(queryFilter)
    }

    override fun hasAccess(user: PFUserDO?, obj: LeaveAccountEntryDO?, oldObj: LeaveAccountEntryDO?, operationType: OperationType?, throwException: Boolean): Boolean {
        if (operationType == OperationType.SELECT) {
            return accessChecker.isLoggedInUserMemberOfGroup(throwException, ProjectForgeGroup.CONTROLLING_GROUP, ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.HR_GROUP, ProjectForgeGroup.ORGA_TEAM)
                    || (user?.id != null && user.id  == obj?.employee?.userId) // User has select access to his own entries.
        } else {
            return accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, throwException, UserRightValue.READONLY, UserRightValue.READWRITE)
        }
    }

}
