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

package org.projectforge.business.vacation.service

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.framework.ToStringUtil
import java.math.BigDecimal

/**
 * For calculations of vacations, this class holds all calculated information for further processing, displaying, logging or testing.
 *
 * @author Kai Reinhard
 */
class VacationStats(
        @JsonIgnore
        val employee: EmployeeDO,
        /**
         * Specifies the base year.
         */
        val year: Int) : ToStringUtil.ToJsonStringObject() {

    /**
     * Only for logging purposes.
     */
    var employeeName: String? = employee.user?.getFullname()

    /**
     * The number of vacation days left from the previous year.
     */
    var carryVacationDaysFromPreviousYear: BigDecimal? = null
    /**
     * The number of vacation days left from the previous year, which are already used for vacation.
     */
    var carryVacationDaysFromPreviousYearUsed: BigDecimal? = null
    val carryVacationDaysFromPreviousYearUnused: BigDecimal?
        get() {
            val total = carryVacationDaysFromPreviousYear
            val used = carryVacationDaysFromPreviousYearUsed
            if (total == null)
                return null
            if (used == null)
                return BigDecimal.ZERO
            return maxOf(total - used, BigDecimal.ZERO)
        }
    /**
     * The overlap period defines the beginning of year until the end of the vacation year (after it the carried and unused
     * vacation days of the previous years will be lost.
     */
    var usedDaysInOverlapPeriod: BigDecimal? = null
    /**
     * Number of annual vacation days from contract. If the employee has joined in the same year, a fraction is calculated.
     */
    var vacationDaysInYearFromContract: BigDecimal? = null
    /**
     * Number of vacation days, persisted as VacationDO objects in the data-base for the specified base year.
     */
    var vacationDaysUsedInYear: BigDecimal? = null
    /**
     * The employee has some vacation days left (used less than the annual vacation days of contract, including any
     * carry from previous years.
     */
    var vacationDaysLeftInYear: BigDecimal? = null
    /**
     * Only given, if this year has to be calculated for getting the the carry of vacation days of this year.
     * The year must be the last year (from today).
     */
    var lastYearStats: VacationStats? = null

    /**
     * Internal function calculates vacationDaysLeftInYear after having all other properties.
     */
    internal fun calculateLeftDaysInYear() {
        // carried vacation days or actual used vacation days in overlap period. If the employee has less vacation days
        // than carried, these vacation days will be lost after the end of vacation year (31.3.).
        var leftInYear = minOf(carryVacationDaysFromPreviousYear!!, usedDaysInOverlapPeriod!!)
        leftInYear += vacationDaysInYearFromContract!! // annual vacation days from contract.
        leftInYear -= vacationDaysUsedInYear!!
        this.vacationDaysLeftInYear = leftInYear
        this.carryVacationDaysFromPreviousYearUsed = maxOf(carryVacationDaysFromPreviousYear!!, usedDaysInOverlapPeriod!!)
    }
}
