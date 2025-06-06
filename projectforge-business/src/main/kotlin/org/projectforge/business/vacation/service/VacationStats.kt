/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.vacation.model.LeaveAccountEntryDO
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.utils.NumberFormatter
import java.io.Serializable
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
    : ToStringUtil.ToJsonStringObject(), Serializable { // Needed by Wicket (VacationViewHelper)

    /**
     * Only for logging purposes.
     */
    var employeeName: String? = PfCaches.instance.getUserIfNotInitialized(employee.user)?.getFullname()

    /**
     * The number of vacation days left from the previous year.
     */
    var remainingLeaveFromPreviousYear: BigDecimal? = null
    /**
     * The number of vacation days left from the previous year, which are already used for vacation.
     */
    var remainingLeaveFromPreviousYearAllocated: BigDecimal? = null
    /**
     * The number of vacation days left from the previous year and not allocated (used). They might be lost after end of
     * vacation year ([remainingLeaveFromPreviousYear] - [allocatedDaysInOverlapPeriod]).
     */
    @get:JsonProperty
    val remainingLeaveFromPreviousYearUnused: BigDecimal?
        get() {
            val total = remainingLeaveFromPreviousYear
            val allocated = allocatedDaysInOverlapPeriod
            if (total == null)
                return null
            if (allocated == null)
                return BigDecimal.ZERO
            return maxOf(total - allocated, BigDecimal.ZERO)
        }
    /**
     * @return true, if [baseDate] is after [endOfVacationYear] and the unused remaining days from the previous year is lost.
     */
    @get:JsonProperty
    val endOfVactionYearExceeded: Boolean
        get() = baseDate.isAfter(endOfVacationYear)
    @get:JsonProperty
    val totalLeaveIncludingCarry: BigDecimal?
        get() {
            var subTotal = vacationDaysInYearFromContract ?: BigDecimal.ZERO
            remainingLeaveFromPreviousYear?.let {
                subTotal += it
            }
            return subTotal
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
     * Entries per date for correction values, if the annual leave day statistics have to be corrected,
     * @see LeaveAccountEntryDO
     */
    var leaveAccountEntries: List<LeaveAccountEntryDO>? = null

    @get:JsonProperty
    val leaveAccountEntriesSum: BigDecimal
        get() {
            var result = BigDecimal.ZERO
            leaveAccountEntries?.forEach {
                it.amount?.let { amount ->
                    result += amount
                }
            }
            return result
        }

    /**
     * Internal function calculates vacationDaysLeftInYear after having all other properties.
     */
    internal fun calculateLeftDaysInYear() {
        // carried vacation days or actual used vacation days in overlap period. If the employee has less vacation days
        // than carried, these vacation days will be lost after the end of vacation year (31.3.).
        var leftInYear = minOf(remainingLeaveFromPreviousYear!!, allocatedDaysInOverlapPeriod!!)
        leftInYear += vacationDaysInYearFromContract!! // annual vacation days from contract.
        leftInYear -= vacationDaysInProgressAndApproved!!
        leftInYear += leaveAccountEntriesSum
        this.vacationDaysLeftInYearWithoutCarry = leftInYear
        if (!endOfVactionYearExceeded) {
            // End of vacation year is not reached: full remaining days from previuos year are available:
            leftInYear += remainingLeaveFromPreviousYearUnused ?: BigDecimal.ZERO
        }
        this.vacationDaysLeftInYear = leftInYear
        this.remainingLeaveFromPreviousYearAllocated = minOf(remainingLeaveFromPreviousYear!!, allocatedDaysInOverlapPeriod!!)
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun format(value: Number?, negate: Boolean = false, emptyStringIfZero: Boolean = false, inBraces: Boolean = false): String {
            value ?: return ""
            if (emptyStringIfZero && value == BigDecimal.ZERO) {
                return ""
            }
            val str = if (!negate) {
                NumberFormatter.format(value, 1)
            } else if (value is BigDecimal) {
                NumberFormatter.format(value.negate(), 1)
            } else {
                NumberFormatter.format((0.0 - value.toDouble()), 1)
            }
            if (inBraces) {
                return "($str)"
            } else {
                return str
            }
        }
    }
}

