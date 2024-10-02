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

package org.projectforge.business.vacation.repository

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.model.RemainingLeaveDO
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Year

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class RemainingLeaveDao : BaseDao<RemainingLeaveDO>(RemainingLeaveDO::class.java) {

    /**
     * Mark entry for given employee as deleted (for recalculation), if exist. Otherwise nop.
     * Forces recalculation of remaining leave (carry from previous year).
     */
    open fun internalMarkAsDeleted(employeeId: Long, year: Int) {
        val entry = internalGet(employeeId, year, false) ?: return
        internalMarkAsDeletedInTrans(entry)
    }

    open fun internalSaveOrUpdate(employee: EmployeeDO, year: Int, remainingLeaveFromPreviousYear: BigDecimal?) {
        if (year > Year.now().value) {
            throw IllegalArgumentException("Can't determine remaining vacation days for future year $year.")
        }
        val entry = internalGet(employee.id, year, false) ?: RemainingLeaveDO()
        entry.employee = employee
        entry.year = year
        entry.remainingFromPreviousYear = remainingLeaveFromPreviousYear
        entry.deleted = false
        if (entry.id == null) {
            internalSaveInTrans(entry)
        } else {
            internalUpdateInTrans(entry)
        }
    }

    /**
     * Tries first to get any manual stored value in [RemainingLeaveDO]. If not found then the value of this table
     * will be returned if exists.
     * @see [LeaveAccountEntryDao.getRemainingLeaveFromPreviousYear]
     */
    open fun getRemainingLeaveFromPreviousYear(employeeId: Long?, year: Int): BigDecimal? {
        employeeId ?: return BigDecimal.ZERO
        if (year > Year.now().value) {
            return BigDecimal.ZERO // Can't determine remaining vacation days for future years, assuming 0.
        }
        return internalGet(employeeId, year)?.remainingFromPreviousYear
    }

    @JvmOverloads
    open fun internalGet(employeeId: Long?, year: Int, ignoreDeleted: Boolean = true): RemainingLeaveDO? {
        employeeId ?: return null
        val result = persistenceService.selectNamedSingleResult(
            RemainingLeaveDO.FIND_BY_EMPLOYEE_ID_AND_YEAR,
            RemainingLeaveDO::class.java,
            Pair("employeeId", employeeId),
            Pair("year", year),
        ) ?: return null
        return if (!ignoreDeleted || !result.deleted)
            result
        else
            null
    }

    override fun hasAccess(
        user: PFUserDO,
        obj: RemainingLeaveDO?,
        oldObj: RemainingLeaveDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        if (operationType == OperationType.SELECT) {
            return accessChecker.isLoggedInUserMemberOfGroup(
                throwException,
                ProjectForgeGroup.CONTROLLING_GROUP,
                ProjectForgeGroup.FINANCE_GROUP,
                ProjectForgeGroup.HR_GROUP,
                ProjectForgeGroup.ORGA_TEAM
            )
        } else {
            return accessChecker.hasLoggedInUserRight(
                UserRightId.HR_VACATION,
                throwException,
                UserRightValue.READONLY,
                UserRightValue.READWRITE
            )
        }
    }

    override fun newInstance(): RemainingLeaveDO {
        return RemainingLeaveDO()
    }
}
