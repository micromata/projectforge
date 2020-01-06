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
import java.time.LocalDate

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
        val year: Int,
        /**
         * The base date is used for rejecting carry vacation days. If after end of vacation year, the carry is rejected.
         * This date is normally now, but might be differ from for test cases.
         */
        val baseDate: LocalDate = LocalDate.now())
    : ToStringUtil.ToJsonStringObject() {

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
    var carryVacationDaysFromPreviousYearAllocated: BigDecimal? = null
    val carryVacationDaysFromPreviousYearUnused: BigDecimal?
        get() {
            val total = carryVacationDaysFromPreviousYear
            val allocated = allocatedDaysInOverlapPeriod
            if (total == null)
                return null
            if (allocated == null)
                return BigDecimal.ZERO
            return maxOf(total - allocated, BigDecimal.ZERO)
        }
    /**
     * The overlap period defines the beginning of year until the end of the vacation year (after it the carried and unused
     * vacation days of the previous years will be lost.
     */
    var allocatedDaysInOverlapPeriod: BigDecimal? = null
    /**
     * Number of annual vacation days from contract. If the employee has joined in the same year, a fraction is calculated.
     */
    var vacationDaysInYearFromContract: BigDecimal? = null
    /**
     * Number of approved vacation days or vacation days in progress, persisted as VacationDO objects in the data-base for the specified base year.
     */
    var vacationDaysInProgressAndApproved: BigDecimal? = null
    /**
     * The left vacation days of the year including any carry from previous years (if base date is before 31.03.). For
     * any date after end of vacation year (31.03.), this value is equal to [vacationDaysLeftInYearWithoutCarry].
     */
    var vacationDaysLeftInYear: BigDecimal? = null
    /**
     * The left vacation days of the year without any carry.
     */
    var vacationDaysLeftInYearWithoutCarry: BigDecimal? = null
    /**
     * Number of vacation days in progress, not yet approved.
     */
    var vacationDaysInProgress: BigDecimal? = null
    /**
     * Number of approved vacation days.
     */
    var vacationDaysApproved: BigDecimal? = null
    /**
     * Number of special vacation days in progress, not yet approved.
     */
    var specialVacationDaysInProgress: BigDecimal? = null
    /**
     * Number of approved special vacation days.
     */
    var specialVacationDaysApproved: BigDecimal? = null
    /**
     * Only given, if this year has to be calculated for getting the the carry of vacation days of this year.
     * The year must be the last year (from today).
     */
    var lastYearStats: VacationStats? = null
    var endOfVacationYear: LocalDate? = null

    /**
     * Internal function calculates vacationDaysLeftInYear after having all other properties.
     */
    internal fun calculateLeftDaysInYear() {
        // carried vacation days or actual used vacation days in overlap period. If the employee has less vacation days
        // than carried, these vacation days will be lost after the end of vacation year (31.3.).
        var leftInYear = minOf(carryVacationDaysFromPreviousYear!!, allocatedDaysInOverlapPeriod!!)
        leftInYear += vacationDaysInYearFromContract!! // annual vacation days from contract.
        leftInYear -= vacationDaysInProgressAndApproved!!
        this.vacationDaysLeftInYearWithoutCarry = leftInYear
        if (baseDate.isBefore(endOfVacationYear)) {
            leftInYear += carryVacationDaysFromPreviousYearUnused ?: BigDecimal.ZERO
        }
        this.vacationDaysLeftInYear = leftInYear
        this.carryVacationDaysFromPreviousYearAllocated = minOf(carryVacationDaysFromPreviousYear!!, allocatedDaysInOverlapPeriod!!)
    }
}
