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

package org.projectforge.business.vacation.repository

import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.model.LeaveAccountEntryDO
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDayUtils
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Month

@Service
open class LeaveAccountEntryDao : BaseDao<LeaveAccountEntryDO>(LeaveAccountEntryDO::class.java) {

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    override fun newInstance(): LeaveAccountEntryDO {
        return LeaveAccountEntryDO()
    }

    // Open needed or proxying.
    open fun getList(employeeId: Long?, year: Int): List<LeaveAccountEntryDO>? {
        val beginOfYear = LocalDate.of(year, Month.JANUARY, 1)
        val endOfYear = PFDayUtils.getEndOfYear(beginOfYear)
        return getList(employeeId, beginOfYear, endOfYear)
    }

    // Open needed or proxying.
    open fun getList(employeeId: Long?, periodBegin: LocalDate, periodEnd: LocalDate): List<LeaveAccountEntryDO>? {
        employeeId ?: return null
        return persistenceService.executeNamedQuery(
            LeaveAccountEntryDO.FIND_BY_EMPLOYEE_ID_AND_DATEPERIOD,
            LeaveAccountEntryDO::class.java,
            Pair("employeeId", employeeId),
            Pair("fromDate", periodBegin),
            Pair("toDate", periodEnd),
        )
    }

    override fun select(
        filter: QueryFilter,
        customResultFilters: List<CustomResultFilter<LeaveAccountEntryDO>>?,
        checkAccess: Boolean,
    ): List<LeaveAccountEntryDO> {
        if (filter.sortProperties.isEmpty()) {
            filter.addOrder(desc("date"))
            filter.addOrder(asc("employee.user.firstname"))
        }
        return super.select(filter, customResultFilters, checkAccess)
    }

    override fun hasAccess(
        user: PFUserDO,
        obj: LeaveAccountEntryDO?,
        oldObj: LeaveAccountEntryDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        return if (operationType == OperationType.SELECT) {
            (accessChecker.isLoggedInUserMemberOfGroup(
                throwException,
                ProjectForgeGroup.CONTROLLING_GROUP,
                ProjectForgeGroup.FINANCE_GROUP,
                ProjectForgeGroup.HR_GROUP,
                ProjectForgeGroup.ORGA_TEAM
            )
                    || (user.id != null && user.id == obj?.employee?.user?.id)) // User has select access to his own entries.
        } else {
            accessChecker.hasLoggedInUserRight(
                UserRightId.HR_VACATION,
                throwException,
                UserRightValue.READONLY,
                UserRightValue.READWRITE
            )
        }
    }

    companion object {
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf(
            "employee.user.firstname",
            "employee.user.lastname",
            "employee.user.username",
            "employee.user.organization"
        )
    }
}
