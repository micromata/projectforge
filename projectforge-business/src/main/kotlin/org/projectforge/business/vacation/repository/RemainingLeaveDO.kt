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

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.model.RemainingLeaveDO
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.utils.SQLHelper
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Year

/**
 * Not multi tenant proven.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class RemainingLeaveDO : BaseDao<RemainingLeaveDO>(RemainingLeaveDO::class.java) {
    open fun internalSaveOrUpdate(employee: EmployeeDO, year: Int, carryVacationDaysFromPreviousYear: BigDecimal?) {
        if (year > Year.now().value) {
            throw IllegalArgumentException("Can't determine remaining vacation days for future year $year.")
        }
        val entry = internalGet(employee.id, year) ?: RemainingLeaveDO()
        entry.employee = employee
        entry.year = year
        entry.remainingFromPreviousYear = carryVacationDaysFromPreviousYear
        if (entry.id == null) {
            internalSave(entry)
        } else {
            internalUpdate(entry)
        }
    }

    open fun getCarryVacationDaysFromPreviousYear(employeeId: Int, year: Int): BigDecimal? {
        if (year > Year.now().value) {
            throw IllegalArgumentException("Can't determine remaining vacation days for future year $year.")
        }
        return internalGet(employeeId, year)?.remainingFromPreviousYear
    }

    open fun internalGet(employeeId: Int, year: Int): RemainingLeaveDO? {
        val result = SQLHelper.ensureUniqueResult(em.createNamedQuery(RemainingLeaveDO.FIND_BY_EMPLOYEE_ID_AND_YEAR, RemainingLeaveDO::class.java)
                .setParameter("employeeId", employeeId)
                .setParameter("year", year))
        return result
    }

    /**
     * Throws [UnsupportedOperationException]
     */
    override fun delete(obj: RemainingLeaveDO?) {
        throw UnsupportedOperationException("Deletion not supported.")
    }

    override fun hasAccess(user: PFUserDO?, obj: RemainingLeaveDO?, oldObj: RemainingLeaveDO?, operationType: OperationType?, throwException: Boolean): Boolean {
        if (operationType == OperationType.SELECT) {
            return accessChecker.isLoggedInUserMemberOfGroup(throwException, ProjectForgeGroup.CONTROLLING_GROUP, ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.HR_GROUP, ProjectForgeGroup.ORGA_TEAM)
        } else {
            return accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, throwException, UserRightValue.READONLY, UserRightValue.READWRITE)
        }
    }

    override fun newInstance(): RemainingLeaveDO {
        return RemainingLeaveDO()
    }
}
