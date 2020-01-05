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
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.time.LocalDatePeriod
import org.projectforge.framework.time.PFDayUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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
open class VacationServiceNew {
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
    open fun getVacationStats(employee: EmployeeDO, year: Int = Year.now().value, calculateCarryInFormerYears: Boolean = true, nowYear: Int = Year.now().value): VacationStats {
        val stats = VacationStats(employee, year)
        stats.vacationDaysInYearFromContract = getYearlyVacationDays(employee, year)
        stats.carryVacationDaysFromPreviousYear = remainingDaysOfVactionDao.getCarryVacationDaysFromPreviousYear(employee.id, year)
        val dateOfJoining = employee.eintrittsDatum
        if (dateOfJoining == null) {
            log.warn("Employee has now joining date, can't calculate vacation days.")
            stats.carryVacationDaysFromPreviousYear = BigDecimal.ZERO
            return stats
        }
        // Calculate remaining vacation days from previous year:
        val yearPeriod = LocalDatePeriod.wholeYear(year)
        val allVacationsOfYear = vacationDao.getVacationForPeriod(employee, yearPeriod.begin, yearPeriod.end, false)
        stats.vacationDaysUsedInYear = sum(allVacationsOfYear, yearPeriod.begin, yearPeriod.end)

        stats.usedDaysInOverlapPeriod = getNumberOfUsedVacationDaysInOverlapPeriod(allVacationsOfYear, year)

        stats.carryVacationDaysFromPreviousYear = remainingDaysOfVactionDao.getCarryVacationDaysFromPreviousYear(employee.id, year)
        if (stats.carryVacationDaysFromPreviousYear == null) {
            if (dateOfJoining.year >= year || year > nowYear || year < nowYear - 1) {
                // Employee joins in current year or later, no carry of vacation days exist, or
                // the year a future year, so it will not be calculated. Also, only the last year will be calculated, any year
                // before the last year will not anymore.
                stats.carryVacationDaysFromPreviousYear = BigDecimal.ZERO
            } else if (year == nowYear) {
                // Carry of holidays from last year weren't yet calculated, do it now:
                stats.lastYearStats = getVacationStats(employee, nowYear - 1, false)
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

    fun validate() {
        // Overlap
        // carry
    }

    /**
     * Gets the number of used vacation days inside the overlap period from the carry vacation days of the previous year.
     * @return Number of days >= 0 but at maximum the carry value.
     */
    @JvmOverloads
    open fun getUsedCarryVacationDays(employee: EmployeeDO, year: Int = Year.now().value): BigDecimal {
        val yearPeriod = LocalDatePeriod.wholeYear(year)
        val allVacationsOfYear = vacationDao.getVacationForPeriod(employee, yearPeriod.begin, yearPeriod.end, false)
        val carryOfYearBefore = remainingDaysOfVactionDao.getCarryVacationDaysFromPreviousYear(employee.id, year - 1)
                ?: BigDecimal.ZERO
        val usedDaysInOverlapPeriod = getNumberOfUsedVacationDaysInOverlapPeriod(allVacationsOfYear, year)

        return minOf(carryOfYearBefore, usedDaysInOverlapPeriod)
    }

    /**
     * Determine the vacation days (of the database) from 1.1. until 31.3. of the given year.
     */
    private fun getNumberOfUsedVacationDaysInOverlapPeriod(vacations: List<VacationDO>, year: Int): BigDecimal {
        val periodBegin = LocalDate.of(year, Month.JANUARY, 1)
        val periodEnd = configService.getEndOfVacation(year)
        return sum(vacations, periodBegin, periodEnd)
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

    private fun sum(list: List<VacationDO?>, periodBegin: LocalDate, periodEnd: LocalDate): BigDecimal {
        var sum = BigDecimal.ZERO
        list.forEach {
            if (it != null) {
                var vacationStart = it.startDate
                var vacationEnd = it.endDate
                if (vacationStart == null || vacationEnd == null) {
                    log.warn("Illegal state of vacation entry of employee ${it.employee?.id}: start ($vacationStart) and end date ($vacationEnd) must be given.")
                } else {
                    if (vacationStart.isBefore(periodBegin))
                        vacationStart = periodBegin
                    if (vacationEnd.isAfter(periodEnd))
                        vacationEnd = periodEnd
                    val numberOfDays = getVacationDays(vacationStart, vacationEnd, periodBegin, periodEnd, it.halfDay)
                    if (numberOfDays != null)
                        sum += numberOfDays
                }
            }
        }
        return sum
    }

    private fun getVacationDays(vacationStart: LocalDate, vacationEnd: LocalDate, periodBegin: LocalDate, periodEnd: LocalDate, isHalfDayVacation: Boolean?): BigDecimal? {
        val from = if (vacationStart.isBefore(periodBegin)) periodBegin else vacationStart
        val until = if (vacationEnd.isAfter(periodEnd)) periodEnd else vacationEnd
        val numberOfWorkingDays = PFDayUtils.getNumberOfWorkingDays(from, until)
        // don't return HALF_DAY if there is no working day
        return if (numberOfWorkingDays > BigDecimal.ZERO && java.lang.Boolean.TRUE == isHalfDayVacation) // null evaluates to false
            HALF_DAY
        else numberOfWorkingDays
    }

    companion object {
        private val log = LoggerFactory.getLogger(VacationServiceNew::class.java)
        private val HALF_DAY = BigDecimal(0.5)
        private val TWELVE = BigDecimal(12)
    }
}
