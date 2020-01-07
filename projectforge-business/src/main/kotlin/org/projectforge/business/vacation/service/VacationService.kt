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

import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.api.EmployeeService
import org.projectforge.business.vacation.model.RemainingDaysOfVactionDao
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.IDao
import org.projectforge.framework.persistence.api.IPersistenceService
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.LocalDatePeriod
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.time.Year
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext


/**
 * Each employee has a fixed maximum number of vacation days per year. If the employee uses less than this maximum number,
 * the remaining vacation days will be carried to the next year and may be used until the defined end of vacation year (e. g. 31.03.
 * of the following year). After this date, the carried vacation days are lost.
 *
 * In this class, the period from 1.1. until the end of the vacation year is name OverlapPeriod.
 *
 * @author Florian Blumenstein, Kai Reinhard
 */
@Service
open class VacationService : CorePersistenceServiceImpl<Int, VacationDO>(), IPersistenceService<VacationDO>, IDao<VacationDO> {
    // CorePersistenceServiceImpl, IPersistenceService and IDao is needed for VacationListPage (Wicket)
    @Autowired
    private lateinit var vacationDao: VacationDao
    @Autowired
    private lateinit var configService: ConfigurationService
    @Autowired
    private lateinit var employeeDao: EmployeeDao
    @Autowired
    private lateinit var employeeService: EmployeeService
    @Autowired
    private lateinit var remainingDaysOfVactionDao: RemainingDaysOfVactionDao
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /**
     * Gets the carry vacation days from the previous year. If not exist, it will be calculated and persisted.
     * @param employee
     * @param year This is usually the current year, for which the carried vacation days from previous year will be returned.
     * @return Number of carried vacation days from previous year or 0, if no carry found.
     */
    @JvmOverloads
    open fun getRemainingDaysFromPreviousYear(employee: EmployeeDO, year: Int = Year.now().value): BigDecimal {
        return getVacationStats(employee, year).carryVacationDaysFromPreviousYear ?: BigDecimal.ZERO
    }

    /**
     * Method for getting stats for tests, exports and logging.
     * @param nowYear Only for test cases (so they will run in further years).
     */
    @JvmOverloads
    open fun getVacationStats(employee: EmployeeDO,
                              year: Int = Year.now().value,
                              /** Only for internal use for recursive calls. */
                              calculateCarryInFormerYears: Boolean = true,
                              /**
                               * Only for testing with fixed simulated now date. Default is today.
                               */
                              baseDate: LocalDate = LocalDate.now(),
                              /**
                               * For internal usage of [VacationValidator].
                               */
                              vacationEntries: List<VacationDO>? = null): VacationStats {
        val stats = VacationStats(employee, year, baseDate)
        stats.vacationDaysInYearFromContract = getYearlyVacationDays(employee, year)
        stats.carryVacationDaysFromPreviousYear = remainingDaysOfVactionDao.getCarryVacationDaysFromPreviousYear(employee.id, year)
        stats.endOfVacationYear = getEndOfCarryVacationOfPreviousYear(year)
        val dateOfJoining = employee.eintrittsDatum
        if (dateOfJoining == null) {
            log.warn("Employee has now joining date, can't calculate vacation days.")
            stats.carryVacationDaysFromPreviousYear = BigDecimal.ZERO
            return stats
        }
        // Calculate remaining vacation days from previous year:
        val yearPeriod = LocalDatePeriod.wholeYear(year)
        val allVacationsOfYear = vacationEntries ?: getVacationsListForPeriod(employee, yearPeriod.begin, yearPeriod.end, true)
        stats.vacationDaysInProgressAndApproved = sum(allVacationsOfYear, yearPeriod.begin, yearPeriod.end, false)
        stats.vacationDaysInProgress = sum(allVacationsOfYear, yearPeriod.begin, yearPeriod.end, false, VacationStatus.IN_PROGRESS)
        stats.vacationDaysApproved = sum(allVacationsOfYear, yearPeriod.begin, yearPeriod.end, false, VacationStatus.APPROVED)
        stats.specialVacationDaysInProgress = sum(allVacationsOfYear, yearPeriod.begin, yearPeriod.end, true, VacationStatus.IN_PROGRESS)
        stats.specialVacationDaysApproved = sum(allVacationsOfYear, yearPeriod.begin, yearPeriod.end, true, VacationStatus.APPROVED)

        stats.allocatedDaysInOverlapPeriod = getNumberValidVacationDaysInOverlapPeriod(allVacationsOfYear, year)

        stats.carryVacationDaysFromPreviousYear = remainingDaysOfVactionDao.getCarryVacationDaysFromPreviousYear(employee.id, year)
        if (stats.carryVacationDaysFromPreviousYear == null) {
            if (dateOfJoining.year >= year || year > baseDate.year || year < baseDate.year - 1) {
                // Employee joins in current year or later, no carry of vacation days exist, or
                // the year a future year, so it will not be calculated. Also, only the last year will be calculated, any year
                // before the last year will not anymore.
                stats.carryVacationDaysFromPreviousYear = BigDecimal.ZERO
            } else if (year == baseDate.year) {
                // Carry of holidays from last year weren't yet calculated, do it now:
                stats.lastYearStats = getVacationStats(employee, baseDate.year - 1, false)
                stats.carryVacationDaysFromPreviousYear = stats.lastYearStats!!.vacationDaysLeftInYear
                        ?: BigDecimal.ZERO
                log.info("Calculation of carry for employee: $stats")
                remainingDaysOfVactionDao.internalSaveOrUpdate(employee, year, stats.carryVacationDaysFromPreviousYear)
            } else {
                // Calculate last year
                stats.carryVacationDaysFromPreviousYear = remainingDaysOfVactionDao.getCarryVacationDaysFromPreviousYear(employee.id, year)
                        ?: BigDecimal.ZERO
            }
        }
        stats.calculateLeftDaysInYear()
        return stats
    }

    @JvmOverloads
    open fun getVacationsListForPeriod(employee: EmployeeDO, periodBegin: LocalDate, periodEnd: LocalDate, withSpecial: Boolean = false, vararg status: VacationStatus)
            : List<VacationDO> {
        val result = vacationDao.getVacationForPeriod(employee, periodBegin, periodEnd, withSpecial)
        if (status.isNotEmpty()) {
            return result.filter { VacationStatus.values().contains(it.status) }
        }
        return result.filter { DEFAULT_VACATION_STATUS_LIST.contains(it.status) }
    }

    /**
     * Getting vacation for given ids. Calls [VacationDao.internalLoad].
     *
     * @param idList
     * @return List of vacations
     */
    open fun getVacation(idList: List<Serializable?>?): List<VacationDO?>? {
        return vacationDao.internalLoad(idList)
    }

    /**
     * Getting all not deleted vacations for given employee of the current year. Calls [VacationDao.getActiveVacationForYear]
     *
     * @param employee
     * @param year
     * @param withSpecial
     * @return List of vacations
     */
    // Must be open for mocking.
    open fun getActiveVacationForYear(employee: EmployeeDO?, year: Int, withSpecial: Boolean): List<VacationDO> {
        return vacationDao.getActiveVacationForYear(employee, year, withSpecial)
    }

    /**
     * Get VacationCount for PFUser. For calculating vacation for students, only.
     *
     * @param fromYear
     * @param fromMonth 1-based: 1 - January, ..., 12 - December
     * @param toYear
     * @param toMonth   1-based: 1 - January, ..., 12 - December
     * @param user
     * @return
     */
    open fun getStudentsVacationCount(fromYear: Int, fromMonth: Int, toYear: Int, toMonth: Int, user: PFUserDO?): String? {
        var hours: Long = 0
        var days = BigDecimal.ZERO
        if (fromYear == toYear) {
            for (month in fromMonth..toMonth) {
                val reportOfMonth = employeeService.getReportOfMonth(fromYear, month, user)
                hours += reportOfMonth.totalNetDuration
                days = days.add(reportOfMonth.numberOfWorkingDays)
            }
        } else {
            for (month in fromMonth..12) {
                val reportOfMonth = employeeService.getReportOfMonth(fromYear, month, user)
                hours += reportOfMonth.totalNetDuration
                days += reportOfMonth.numberOfWorkingDays
            }
            for (month in 1..toMonth) {
                val reportOfMonth = employeeService.getReportOfMonth(toYear, month, user)
                hours += reportOfMonth.totalNetDuration
                days += reportOfMonth.numberOfWorkingDays
            }
        }
        val big_hours = BigDecimal(hours).divide(BigDecimal(1000 * 60 * 60), 2, RoundingMode.HALF_UP)
        return NumberHelper.formatFraction2(big_hours.toDouble() / days.toDouble())
    }

    open fun getEndOfCarryVacationOfPreviousYear(year: Int): LocalDate {
        return configService.getEndOfCarryVacationOfPreviousYear(year)
    }

    /**
     * Checks for collissions, enough left days etc.
     * @param vacation The vacation entry to check.
     * @param dbVacation If modified, the previous entry (data base entry).
     * @param throwException If true, an exception is thrown if validation failed. Default is false.
     * @return null if no validation error was detected, or i18n-key of error, if validation failed.
     */
    @JvmOverloads
    fun validate(vacation: VacationDO, dbVacation: VacationDO? = null, throwException: Boolean = false): VacationValidator.Error? {
        return VacationValidator.validate(this, vacation, dbVacation, throwException)
    }

    open fun getOpenLeaveApplicationsForUser(user: PFUserDO): BigDecimal {
        val employee = employeeService.getEmployeeByUserId(user.id) ?: return BigDecimal.ZERO
        return vacationDao.getOpenLeaveApplicationsForEmployee(employee)
    }

    /**
     * Check, if user is able to use vacation services, meaning, has configured annual vacation days (urlaubstage).
     * @see [EmployeeDO.urlaubstage]
     */
    open fun hasAccessToVacationService(user: PFUserDO?, throwException: Boolean): Boolean {
        if (user?.id == null)
            return false
        val employee = employeeService.getEmployeeByUserId(user.id)
        return when {
            employee == null -> {
                if (throwException) {
                    throw AccessException("access.exception.noEmployeeToUser")
                }
                false
            }
            employee.urlaubstage == null -> {
                if (throwException) {
                    throw AccessException("access.exception.employeeHasNoVacationDays")
                }
                false
            }
            else -> true
        }
    }

    /**
     * Checks, if logged in User has HR vacation access.
     * @see [VacationDao.hasLoggedInUserHRVacationAccess]
     */
    open fun hasLoggedInUserHRVacationAccess(): Boolean {
        return vacationDao.hasLoggedInUserHRVacationAccess()
    }

    /**
     * Determine the vacation days (of the database) from 1.1. until 31.3. of the given year.
     */
    private fun getNumberValidVacationDaysInOverlapPeriod(vacations: List<VacationDO>, year: Int): BigDecimal {
        val periodBegin = LocalDate.of(year, Month.JANUARY, 1)
        val periodEnd = configService.getEndOfCarryVacationOfPreviousYear(year)
        return sum(vacations, periodBegin, periodEnd, false, *DEFAULT_VACATION_STATUS_LIST)
    }

    /**
     * Please note: If number of yearly vacation days are modified over time, this method assumes the current value also for previous years!!!!!!!!!!!!
     * @return [EmployeeDO.urlaubstage] if employee joined before given year, 0 if employee joined later than given year, otherwise fraction (joined in given year).
     */
    private fun getYearlyVacationDays(employee: EmployeeDO, year: Int): BigDecimal {
        val joinDate = employee.eintrittsDatum
        val vacationDaysPerYear = employee.urlaubstage ?: return BigDecimal.ZERO
        if (joinDate == null || joinDate.year < year) {
            return BigDecimal(vacationDaysPerYear)
        }
        if (joinDate.year > year) {
            return BigDecimal.ZERO
        }
        var employedMonths = Month.DECEMBER.value - joinDate.month.value
        if (joinDate.dayOfMonth < 15)
            employedMonths++ // Month counts only if the employee joined latest at 14th of month.
        return (BigDecimal(vacationDaysPerYear).divide(TWELVE, 2, RoundingMode.HALF_UP) * BigDecimal(employedMonths)).setScale(0, RoundingMode.HALF_UP)
    }

    private fun sum(list: List<VacationDO?>, periodBegin: LocalDate, periodEnd: LocalDate, withSpecial: Boolean, vararg status: VacationStatus): BigDecimal {
        var sum = BigDecimal.ZERO
        val statusValues = if (status.isNullOrEmpty()) {
            DEFAULT_VACATION_STATUS_LIST
        } else {
            status
        }

        list.forEach {
            if (it != null && it.special == withSpecial && statusValues.contains(it.status)) {
                var vacationStart = it.startDate
                var vacationEnd = it.endDate
                if (vacationStart == null || vacationEnd == null) {
                    log.warn("Illegal state of vacation entry of employee ${it.employee?.id}: start ($vacationStart) and end date ($vacationEnd) must be given.")
                } else {
                    if (vacationStart.isBefore(periodBegin))
                        vacationStart = periodBegin
                    if (vacationEnd.isAfter(periodEnd))
                        vacationEnd = periodEnd
                    val numberOfDays = getVacationDays(vacationStart, vacationEnd, it.halfDay, periodBegin, periodEnd)
                    if (numberOfDays != null)
                        sum += numberOfDays
                }
            }
        }
        return sum
    }

    /**
     * @param vacationStart
     * @param vacationEnd
     * @param isHalfDayVacation If number of vacation days is 1 or less and halfDay is chosen, 0.5 will be returned. Default is false.
     * @param periodBegin Optional value to detect number of vacation days inside a specified period (e. g. vacation days in overlap period).
     * @param periodEnd Optional value to detect number of vacation days inside a specified period (e. g. vacation days in overlap period).
     * @return The number of vacation days for the given period (will call [PFDayUtils.getNumberOfWorkingDays].
     */
    @JvmOverloads
    open fun getVacationDays(vacationStart: LocalDate?, vacationEnd: LocalDate?, isHalfDayVacation: Boolean? = false, periodBegin: LocalDate? = null, periodEnd: LocalDate? = null): BigDecimal? {
        if (vacationStart == null || vacationEnd == null) {
            log.warn("from=$vacationStart, to=$vacationEnd. Both mustn't be null!")
            return null
        }
        val from = if (periodBegin != null && vacationStart.isBefore(periodBegin)) periodBegin else vacationStart
        val until = if (periodEnd != null && vacationEnd.isAfter(periodEnd)) periodEnd else vacationEnd
        val numberOfWorkingDays = PFDayUtils.getNumberOfWorkingDays(from, until)
        // don't return HALF_DAY if there is no working day
        return if (numberOfWorkingDays > BigDecimal.ZERO && java.lang.Boolean.TRUE == isHalfDayVacation) // null evaluates to false
            HALF_DAY
        else numberOfWorkingDays
    }

    companion object {
        private val log = LoggerFactory.getLogger(VacationService::class.java)
        private val HALF_DAY = BigDecimal(0.5)
        private val TWELVE = BigDecimal(12)
        private val DEFAULT_VACATION_STATUS_LIST = arrayOf(VacationStatus.APPROVED, VacationStatus.IN_PROGRESS)
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // CorePersistenceServiceImpl, IPersistenceService and IDao stuff (needed by VacationListPage/Wicket).
    //
    // To be removed after migration from Wicket to React:

    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return true
    }

    override fun hasLoggedInUserInsertAccess(): Boolean {
        return vacationDao.hasLoggedInUserInsertAccess()
    }

    override fun hasLoggedInUserInsertAccess(obj: VacationDO, throwException: Boolean): Boolean {
        return vacationDao.hasLoggedInUserInsertAccess(obj, throwException)
    }

    override fun hasLoggedInUserUpdateAccess(obj: VacationDO, dbObj: VacationDO, throwException: Boolean): Boolean {
        return vacationDao.hasLoggedInUserUpdateAccess(obj, dbObj, throwException)
    }

    override fun hasLoggedInUserDeleteAccess(obj: VacationDO, dbObj: VacationDO, throwException: Boolean): Boolean {
        return vacationDao.hasLoggedInUserDeleteAccess(obj, dbObj, throwException)
    }

    override fun hasDeleteAccess(user: PFUserDO, obj: VacationDO, dbObj: VacationDO, throwException: Boolean): Boolean {
        return vacationDao.hasDeleteAccess(user, obj, dbObj, throwException)
    }

    override fun rebuildDatabaseIndex4NewestEntries() {
        vacationDao.rebuildDatabaseIndex4NewestEntries()
    }

    override fun rebuildDatabaseIndex() {
        vacationDao.rebuildDatabaseIndex()
    }

    override fun getAutocompletion(property: String, searchString: String): List<String> {
        return vacationDao.getAutocompletion(property, searchString)
    }

    override fun getList(filter: BaseSearchFilter): List<VacationDO> {
        return vacationDao.getList(filter)
    }

    override fun getDisplayHistoryEntries(obj: VacationDO): List<DisplayHistoryEntry> {
        return vacationDao.getDisplayHistoryEntries(obj)
    }
}
