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

import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.framework.i18n.UserException
import org.projectforge.framework.time.LocalDatePeriod
import org.projectforge.framework.time.PFDayUtils
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Validates vacation entries. Use this functionality through [VacationService.validate].
 */
object VacationValidator {
    /**
     * Available and used validation errors.
     */
    enum class Error(val messageKey: String) {
        /**
         * Start and/or end date is not set.
         */
        DATE_NOT_SET("vacation.validate.datenotset"),
        END_DATE_BEFORE_START_DATE("vacation.validate.endbeforestart"),
        DATE_BEFORE_JOINING("vacation.validate.vacationBeforeJoinDate"),
        START_DATE_BEFORE_NOW("vacation.validate.startDateBeforeNow"),
        /**
         * Number of vacation days is zero or less.
         */
        ZERO_NUMBER_OF_DAYS("vacation.validate.daysarenull"),
        /**
         * Another vacation with conflicting date period already exists.
         */
        COLLISION("vacation.validate.leaveapplicationexists"),
        /**
         * Vacation entries over New Years Eve are not yet supported.
         */
        VACATION_IN_2YEARS("vacation.validate.vacationIn2Years"),
        /**
         * Not enough vacation days left in the year for the validated vacations entry.
         */
        NOT_ENOUGH_DAYS_LEFT("vacation.validate.notEnoughVacationDaysLeft"),
        /**
         * The current user is not allowed to approve this entry. Only allowed for the manager of this entry or
         * HR staff members. Checked by [org.projectforge.business.vacation.model.VacationDao].
         */
        NOT_ALLOWED_TO_APPROVE("vacation.validate.notAllowedToSelfApprove")
    }

    /**
     * May be modified for test cases.
     */
    internal var rejectNewVacationEntriesBeforeNow = true

    /**
     * Checks for collisions, enough left days etc. The access checking will be done by [org.projectforge.business.vacation.model.VactionDO].
     * @param vacation The vacation entry to check.
     * @param dbVacation If modified, the previous entry (data base entry).
     * @param throwException If true, an exception is thrown if validation failed. Default is false.
     * @return null if no validation error was detected, or i18n-key of error, if validation failed.
     */
    @JvmStatic
    @JvmOverloads
    internal fun validate(vacationService: VacationService, vacation: VacationDO, dbVacation: VacationDO? = null, throwException: Boolean = false): Error? {
        val startDate = vacation.startDate
        val endDate = vacation.endDate
        val employee = vacation.employee
        if (startDate == null || endDate == null) {
            return returnOrThrow(Error.DATE_NOT_SET, throwException)
        }
        require(employee != null)
        val year = startDate.year
        if (endDate.isBefore(startDate)) {
            return returnOrThrow(Error.END_DATE_BEFORE_START_DATE, throwException)
        }
        val joinDate = employee.eintrittsDatum
        if (joinDate != null && startDate.isBefore(joinDate)) {
            return returnOrThrow(Error.DATE_BEFORE_JOINING, throwException)
        }

        // Is new vacation data
        if (rejectNewVacationEntriesBeforeNow && vacation.id == null && startDate.isBefore(LocalDate.now()) && !vacationService.hasLoggedInUserHRVacationAccess()) {
            return returnOrThrow(Error.START_DATE_BEFORE_NOW, throwException)
        }

        if (startDate.year != endDate.year) {
            return returnOrThrow(Error.VACATION_IN_2YEARS, throwException)
        }
        val status = vacation.status
                ?: throw IllegalStateException("Status of vacation data is required for validation, but not given.")
        if (vacation.isDeleted || !CHECK_VACATION_STATUS_LIST.contains(status)) {
            // No further validations for deleted or REJECTED vacations required.
            return null
        }
        if (vacationService.getVacationsListForPeriod(employee, startDate, endDate).any { it.id != vacation.id }) {
            // Any other entry exist with overlapping time period.
            return returnOrThrow(Error.COLLISION, throwException)
        }
        val numberOfWorkingDays = PFDayUtils.getNumberOfWorkingDays(startDate, endDate)
        //vacationdays <= 0 days
        if (vacation.halfDayBegin == true && numberOfWorkingDays <= BigDecimal.ZERO) {
            return returnOrThrow(Error.ZERO_NUMBER_OF_DAYS, throwException)
        }
        if (vacation.special == true) {
            // No checking of available days.
        } else {
            // Check of available days:

            val yearPeriod = LocalDatePeriod.wholeYear(year)
            var allVacationEntriesOfYear = vacationService.getVacationsListForPeriod(employee, yearPeriod.begin, yearPeriod.end, false)
            if (dbVacation != null) {
                // Remove old entry from list to get statistics without this entry. Otherwise this entry would count twice.
                allVacationEntriesOfYear = allVacationEntriesOfYear.filter { it.id != dbVacation.id }
            }
            val stats = vacationService.getVacationStats(employee, year, vacationEntries = allVacationEntriesOfYear)
            if (numberOfWorkingDays  > stats.vacationDaysLeftInYearWithoutCarry) {
                val endOfVacationYear = vacationService.getEndOfCarryVacationOfPreviousYear(year)
                var enoughDaysLeft = false
                if (startDate.isBefore(endOfVacationYear)) {
                    val overlapDays = if (endDate > endOfVacationYear)
                        PFDayUtils.getNumberOfWorkingDays(startDate, endOfVacationYear)
                    else
                        PFDayUtils.getNumberOfWorkingDays(startDate, endDate)
                    val additionalCarryDays = maxOf(stats.remainingLeaveFromPreviousYearUnused!! - overlapDays, BigDecimal.ZERO)
                    if (numberOfWorkingDays  <= stats.vacationDaysLeftInYearWithoutCarry!! + additionalCarryDays) {
                        // Including unused carry days, it's now enough:
                        enoughDaysLeft = true
                    }
                }
                if (!enoughDaysLeft)
                    return returnOrThrow(Error.NOT_ENOUGH_DAYS_LEFT, throwException)
            }
        }
        return null // No validation error.
    }

    private fun returnOrThrow(error: Error, throwException: Boolean): Error {
        if (throwException)
            throw UserException(error.messageKey)
        return error
    }

    private val CHECK_VACATION_STATUS_LIST = listOf(VacationStatus.APPROVED, VacationStatus.IN_PROGRESS)
}
