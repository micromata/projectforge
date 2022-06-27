/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.LeaveAccountEntryDao
import org.projectforge.business.vacation.repository.RemainingLeaveDao
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.LocalDatePeriod
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.time.TimePeriod
import org.projectforge.framework.utils.NumberFormatter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.time.Year


/**
 * Each employee has a fixed maximum number of vacation days per year. If the employee uses less than this maximum number,
 * the remaining vacation days will be carried to the next year and may be used until the defined end of vacation year (e. g. 31.03.
 * of the following year). After this date, the carried vacation days are lost.
 *
 * In this class, the period from 1.1. until the end of the vacation year is named OverlapPeriod.
 *
 * @author Florian Blumenstein, Kai Reinhard
 */
@Service
open class VacationService {
  @Autowired
  private lateinit var vacationDao: VacationDao

  @Autowired
  private lateinit var configService: ConfigurationService

  @Autowired
  private lateinit var conflictingVacationsCache: ConflictingVacationsCache

  @Autowired
  private lateinit var employeeDao: EmployeeDao

  @Autowired
  private lateinit var employeeService: EmployeeService

  @Autowired
  private lateinit var remainingLeaveDao: RemainingLeaveDao

  @Autowired
  private lateinit var leaveAccountEntryDao: LeaveAccountEntryDao

  class AverageWorkingTime(
    var fromMonth: PFDay? = null,
    var toMonth: PFDay? = null,
    var workingHours: BigDecimal = BigDecimal.ZERO,
    var workingDays: BigDecimal = BigDecimal.ZERO
  ) {
    val average: BigDecimal
      get() = if (workingDays <= BigDecimal.ZERO) {
        BigDecimal.ZERO
      } else {
        workingHours.divide(workingDays, 2, RoundingMode.HALF_UP)
      }

    val localizedMessage: String // vacation.stats.averageWorkingTime={0}-{1}: {2} worked hours in {3} working days, average per working day: {4}h
      get() =
        if (fromMonth == null || toMonth == null) {
          "--"
        } else {
          translateMsg(
            "vacation.stats.averageWorkingTime",
            fromMonth?.format() ?: "--",
            toMonth?.format() ?: "--",
            NumberFormatter.format(workingHours),
            NumberFormatter.format(workingDays),
            NumberFormatter.format(average)
          )
        }
  }

  class VacationsByEmployee(val employee: EmployeeDO, val vacations: List<VacationDO>)

  class VacationOverlaps(
    /**
     * Vacations of all substitutes (replacement and otherReplacement) overlapping requested vacation.
     */
    val otherVacations: List<VacationDO> = emptyList(),
    /**
     * If at least one day of the vacation isn't covered by any substitute (all substitutes are left on at least one
     * day).
     */
    val conflict: Boolean = false,
  )

  /**
   * Gets the remaining leave from the previous year. If not exist, it will be calculated and persisted.
   * @param employee
   * @param year This is usually the current year, for which the carried vacation days from previous year will be returned.
   * @return Number of carried vacation days from previous year or 0, if no remaining leave found.
   */
  @JvmOverloads
  open fun getRemainingDaysFromPreviousYear(employee: EmployeeDO, year: Int = Year.now().value): BigDecimal {
    return getVacationStats(employee, year).remainingLeaveFromPreviousYear ?: BigDecimal.ZERO
  }

  /**
   * Method for getting stats for tests, exports and logging.
   * @param calculateRemainingLeaveInFormerYears Only for internal use for recursive calls.
   * @param baseDate Only for testing with fixed simulated now date. Default is today.
   * @param vacationEntries For internal usage by [VacationValidator].
   */
  @JvmOverloads
  open fun getVacationStats(
    employee: EmployeeDO,
    year: Int = Year.now().value,
    calculateRemainingLeaveInFormerYears: Boolean = true,
    baseDate: LocalDate = LocalDate.now(),
    vacationEntries: List<VacationDO>? = null
  ): VacationStats {
    val stats = VacationStats(employee, year, baseDate)
    // Get employee from database if not initialized (user not given).
    val employeeDO = if (employee.userId == null) employeeDao.internalGetById(employee.id) else employee
    stats.vacationDaysInYearFromContract = getAnnualLeaveDays(employeeDO, year)
    stats.endOfVacationYear = getEndOfCarryVacationOfPreviousYear(year)
    // If date of joining not given, assume 1900...
    val dateOfJoining = employeeDO.eintrittsDatum ?: LocalDate.of(1900, Month.JANUARY, 1)
    // Calculate remaining vacation days from previous year:
    val yearPeriod = LocalDatePeriod.wholeYear(year)
    val allVacationsOfYear = vacationEntries
      ?: getVacationsListForPeriod(employeeDO, yearPeriod.begin, yearPeriod.end, true)
    stats.vacationDaysInProgressAndApproved = sum(allVacationsOfYear, yearPeriod.begin, yearPeriod.end, false)
    stats.vacationDaysInProgress =
      sum(allVacationsOfYear, yearPeriod.begin, yearPeriod.end, false, VacationStatus.IN_PROGRESS)
    stats.vacationDaysApproved =
      sum(allVacationsOfYear, yearPeriod.begin, yearPeriod.end, false, VacationStatus.APPROVED)
    stats.specialVacationDaysInProgress =
      sum(allVacationsOfYear, yearPeriod.begin, yearPeriod.end, true, VacationStatus.IN_PROGRESS)
    stats.specialVacationDaysApproved =
      sum(allVacationsOfYear, yearPeriod.begin, yearPeriod.end, true, VacationStatus.APPROVED)

    stats.allocatedDaysInOverlapPeriod = getNumberValidVacationDaysInOverlapPeriod(allVacationsOfYear, year)

    stats.remainingLeaveFromPreviousYear = remainingLeaveDao.getRemainingLeaveFromPreviousYear(employee.id, year)
    if (stats.remainingLeaveFromPreviousYear == null) {
      if (dateOfJoining.year >= year || year > baseDate.year || year < baseDate.year - 1) {
        // Employee joins in current year or later, no carry of vacation days exist, or
        // the year a future year, so it will not be calculated. Also, only the last year will be calculated, any year
        // before the last year will not anymore.
        stats.remainingLeaveFromPreviousYear = BigDecimal.ZERO
      } else if (year == baseDate.year) {
        // Carry of holidays from last year weren't yet calculated, do it now:
        stats.lastYearStats = getVacationStats(employeeDO, baseDate.year - 1, false)
        stats.remainingLeaveFromPreviousYear = stats.lastYearStats!!.vacationDaysLeftInYear
          ?: BigDecimal.ZERO
        log.info("Calculation of carry for employee: $stats")
        remainingLeaveDao.internalSaveOrUpdate(employeeDO, year, stats.remainingLeaveFromPreviousYear)
      } else {
        // Calculate last year
        stats.remainingLeaveFromPreviousYear = remainingLeaveDao.getRemainingLeaveFromPreviousYear(employee.id, year)
          ?: BigDecimal.ZERO
      }
    }
    stats.leaveAccountEntries = leaveAccountEntryDao.getList(employee.id, year)
    stats.calculateLeftDaysInYear()
    return stats
  }

  @JvmOverloads
  open fun getVacationsListForPeriod(
    employee: EmployeeDO,
    periodBegin: LocalDate,
    periodEnd: LocalDate,
    withSpecial: Boolean = false,
    trimVacations: Boolean = false,
    vararg status: VacationStatus
  )
      : List<VacationDO> {
    return getVacationsListForPeriod(employee.id, periodBegin, periodEnd, withSpecial, trimVacations, *status)
  }

  /**
   * @param trimVacations If true then vacation entries will be reduced for not extending given period.
   * @param status If given, only vacations matching the given status values will be returned. If not given, DEFAULT_VACATION_STATUS_LIST
   * is used.
   */
  @JvmOverloads
  open fun getVacationsListForPeriod(
    employeeId: Int,
    periodBegin: LocalDate,
    periodEnd: LocalDate,
    withSpecial: Boolean = false,
    trimVacations: Boolean = false,
    vararg status: VacationStatus
  )
      : List<VacationDO> {
    var result = vacationDao.getVacationForPeriod(employeeId, periodBegin, periodEnd, withSpecial)
    result = if (status.isNotEmpty()) {
      result.filter { VacationStatus.values().contains(it.status) }
    } else {
      result.filter { DEFAULT_VACATION_STATUS_LIST.contains(it.status) }
    }
    if (trimVacations) {
      result.forEach {
        if (periodBegin.isAfter(it.startDate)) {
          it.startDate = periodBegin
        }
        if (periodEnd.isBefore(it.endDate)) {
          it.endDate = periodEnd
        }
      }
    }
    return result
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
   * Get average working hours per working day for PFUser. For calculating vacation for students or for employees with flexible working hours, only.
   * The number of working hours of the last 3 month will be summarized and divided by number of working days.
   *
   * @param user
   * @param startOfWorkContract Date the user starts to work. Only time sheets and months from startDate or later will be used for calculation.
   * @param date Current date (now is default)
   * @return Number of average working hours per working day.
   */
  open fun getAverageWorkingTimeStats(
    user: PFUserDO,
    startOfWorkContract: PFDay?,
    date: PFDay = PFDay.now()
  ): AverageWorkingTime {
    // var currentMonth = date.beginOfMonth // Start with the last full month.
    var fromMonth = date.beginOfMonth.minusMonths(3)
    if (startOfWorkContract != null) {
      if (startOfWorkContract >= date.beginOfMonth) {
        // Work started after time period to analyse (3 months before date)
        return AverageWorkingTime()
      }
      if (startOfWorkContract > fromMonth) {
        // Start not earlier than begin of month of contract.
        fromMonth = startOfWorkContract.beginOfMonth
      }
    }
    val toMonth = date.beginOfMonth.minusDays(1) // End in month before given date (analyze only full months)
    val result = AverageWorkingTime(fromMonth = fromMonth, toMonth = toMonth)
    var currentMonth = fromMonth
    for (paranoiaCounter in 0..3) { // Paranoia counter for avoiding endless loops.
      val reportOfMonth = employeeService.getReportOfMonth(currentMonth.year, currentMonth.month.value, user)
      result.workingHours += BigDecimal(reportOfMonth.totalNetDuration).divide(
        TimePeriod.MILLIS_PER_HOUR,
        2,
        RoundingMode.HALF_UP
      )
      result.workingDays += reportOfMonth.numberOfWorkingDays
      currentMonth = currentMonth.plusMonths(1)
      if (currentMonth > toMonth) {
        break // here the loop should be ended (not via paranoia setting).
      }
    }
    return result
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
  open fun validate(
    vacation: VacationDO,
    dbVacation: VacationDO? = null,
    throwException: Boolean = false
  ): VacationValidator.Error? {
    var dbVal = dbVacation
    if (dbVacation == null && vacation.id != null) {
      dbVal = vacationDao.internalGetById(vacation.id)
    }
    return VacationValidator.validate(this, vacation, dbVal, throwException)
  }

  open fun getOpenLeaveApplicationsForUser(user: PFUserDO): Int {
    val employee = employeeService.getEmployeeByUserId(user.id) ?: return 0
    return vacationDao.getOpenLeaveApplicationsForEmployee(employee)
  }

  /**
   * Check, if user is able to use vacation services, meaning, has configured annual vacation days (urlaubstage).
   * @see EmployeeService.getAnnualLeaveDays
   */
  open fun hasAccessToVacationService(user: PFUserDO?, throwException: Boolean): Boolean {
    if (user?.id == null)
      return false
    val employee = employeeService.getEmployeeByUserId(user.id)
    val annualLeaveDays = employeeService.getAnnualLeaveDays(employee) ?: BigDecimal.ZERO
    return when {
      employee == null -> {
        if (throwException) {
          throw AccessException("access.exception.noEmployeeToUser")
        }
        false
      }
      annualLeaveDays <= BigDecimal.ZERO -> {
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
   * Method for detecting vacation overlaps between employees and their substitutes (replacement) or to get
   * an vacation list for selected employees or group of employees.
   */
  open fun getVacationOfEmployees(
    employees: Set<EmployeeDO>,
    periodBegin: LocalDate, periodEnd: LocalDate, withSpecial: Boolean = false,
    trimVacations: Boolean = false,
    vararg status: VacationStatus,
  )
      : List<VacationsByEmployee> {
    val result = mutableListOf<VacationsByEmployee>()
    employees.forEach { employee ->
      val vacations =
        getVacationsListForPeriod(employee.id, periodBegin, periodEnd, withSpecial, trimVacations, *status)
      result.add(VacationsByEmployee(employee, vacations))
    }
    return result
  }

  open fun getVacationOverlaps(vacation: VacationDO): VacationOverlaps {
    val periodBegin = vacation.startDate ?: return VacationOverlaps() // Should not occur on db entries.
    val periodEnd = vacation.endDate ?: return VacationOverlaps() // Should not occur on db entries.
    val employees = vacation.allReplacements
    if (employees.isEmpty()) {
      return VacationOverlaps()
    }
    val vacationOverlaps = mutableListOf<VacationDO>()
    getVacationOfEmployees(
      employees,
      periodBegin,
      periodEnd,
      withSpecial = true,
      trimVacations = true,
    ).forEach { employeeVacations ->
      employeeVacations.vacations.forEach { otherVacation ->
        if (vacation.hasOverlap(otherVacation)) {
          vacationOverlaps.add(otherVacation)
        }
      }
    }
    val conflict = checkConflict(vacation, vacationOverlaps)
    conflictingVacationsCache.updateVacation(vacation, conflict)
    return VacationOverlaps(vacationOverlaps.sortedBy { it.startDate }, conflict)
  }

  internal fun checkConflict(vacation: VacationDO, otherVacations: List<VacationDO>): Boolean {
    if (otherVacations.isEmpty()) {
      return false
    }
    val employees = vacation.allReplacements
    employees.forEach { employeeDO ->
      if (otherVacations.none { it.employeeId == employeeDO.id }) {
        return false // one replacement employee found without any vacation in the vacation period -> no conflict.
      }
    }

    val startDate = vacation.startDate ?: return false // return should not occur on db entries.
    val endDate = vacation.endDate ?: return false // return should not occur on db entries.
    if (startDate > endDate) {
      return false // startDate after endDate shouldn't occur fore db entries.
    }
    var date = startDate
    var paranoiaCounter = 10000
    val replacements = vacation.allReplacements
    if (replacements.isEmpty()) {
      return false // return should not occur
    }
    while (date <= endDate) {
      if (--paranoiaCounter <= 0) {
        // Paranoia counter for avoiding endless loops
        break
      }
      var substituteAvailable = false
      // No check either at least one substitute is on duty or not:
      replacements.forEach replacements@{ replacement ->
        otherVacations.filter { it.employeeId == replacement.id }.forEach { other ->
          if (!other.isInBetween(date)) {
            substituteAvailable = true
            return@replacements
          }
        }
      }
      if (!substituteAvailable) {
        // No substitute found for at least one day.
        return true
      }
      date = date.plusDays(1)
    }
    return false
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
   * @return [EmployeeService.getAnnualLeaveDays] of the end of given year if employee joined before given year, 0 if employee joined later than given year, otherwise fraction (joined in given year).
   */
  internal fun getAnnualLeaveDays(employee: EmployeeDO, year: Int): BigDecimal {
    val joinDate = employee.eintrittsDatum ?: LocalDate.of(1900, Month.JANUARY, 1)
    val leaveDate = employee.austrittsDatum ?: LocalDate.of(2999, Month.DECEMBER, 31)
    val endOfYear = PFDay.of(year, Month.JANUARY, 1).endOfYear.date
    val annualLeaveDays = employeeService.getAnnualLeaveDays(employee, endOfYear) ?: BigDecimal.ZERO
    /*if (joinDate == null || joinDate.year < year) {
        return BigDecimal(vacationDaysPerYear)
    }*/
    if (joinDate.year > year || leaveDate.year < year) {
      return BigDecimal.ZERO
    }
    var employedMonths = 12
    if (joinDate.year == year) {
      employedMonths = Month.DECEMBER.value - joinDate.month.value
      if (joinDate.dayOfMonth < 15)
        employedMonths++ // Month counts only if the employee joined latest at 14th of month.
    }
    if (leaveDate.year == year) {
      employedMonths -= Month.DECEMBER.value - leaveDate.month.value
      if (leaveDate.dayOfMonth < 15)
        employedMonths-- // Month counts only if the employee leaved not earlier than 15th of month.
    }
    if (employedMonths == 12) {
      return annualLeaveDays
    }
    return (annualLeaveDays.divide(TWELVE, 2, RoundingMode.HALF_UP) * BigDecimal(employedMonths)).setScale(
      0,
      RoundingMode.HALF_UP
    )
  }

  private fun sum(
    list: List<VacationDO?>,
    periodBegin: LocalDate,
    periodEnd: LocalDate,
    withSpecial: Boolean,
    vararg status: VacationStatus
  ): BigDecimal {
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
          if (vacationEnd.isBefore(periodBegin) || vacationStart.isAfter(periodEnd)) {
            // Ignore entry out of period.
          } else {
            if (vacationStart.isBefore(periodBegin))
              vacationStart = periodBegin
            if (vacationEnd.isAfter(periodEnd))
              vacationEnd = periodEnd
            val numberOfDays =
              getVacationDays(vacationStart, vacationEnd, it.halfDayBegin, it.halfDayEnd, periodBegin, periodEnd)
            sum += numberOfDays
          }
        }
      }
    }
    return sum
  }

  companion object {
    private val log = LoggerFactory.getLogger(VacationService::class.java)
    private val HALF_DAY = BigDecimal(0.5)
    private val TWELVE = BigDecimal(12)
    private val DEFAULT_VACATION_STATUS_LIST = arrayOf(VacationStatus.APPROVED, VacationStatus.IN_PROGRESS)

    @JvmStatic
    @JvmOverloads
    fun getVacationDays(
      vacation: VacationDO,
      periodBegin: LocalDate? = null,
      periodEnd: LocalDate? = null
    ): BigDecimal {
      val startDate = vacation.startDate
      val endDate = vacation.endDate
      if (startDate == null || endDate == null) {
        /// log.warn("from=${startDate}, to=${endDate}. Both mustn't be null!")
        return BigDecimal.ZERO
      }
      return getVacationDays(startDate, endDate, vacation.halfDayBegin, vacation.halfDayEnd, periodBegin, periodEnd)
    }

    /**
     * @param vacationStart
     * @param vacationEnd
     * @param halfDayBegin Should the first day (if working day) counted as half day?
     * @param halfDayEnd Should the last day (if working day) counted as half day?
     * @param periodBegin Optional value to detect number of vacation days inside a specified period (e. g. vacation days in overlap period).
     * @param periodEnd Optional value to detect number of vacation days inside a specified period (e. g. vacation days in overlap period).
     * @return The number of vacation days for the given period (will call [PFDayUtils.getNumberOfWorkingDays].
     */
    @JvmStatic
    @JvmOverloads
    fun getVacationDays(
      startDate: LocalDate,
      endDate: LocalDate,
      halfDayBegin: Boolean? = false,
      halfDayEnd: Boolean? = false,
      periodBegin: LocalDate? = null,
      periodEnd: LocalDate? = null
    ): BigDecimal {
      var useHalfDayBegin = halfDayBegin == true
      var useHalfDayEnd = halfDayEnd == true
      val from = if (periodBegin != null && startDate.isBefore(periodBegin)) {
        useHalfDayBegin = false // begin is outside of period, can't be counted as half day anymore.
        periodBegin
      } else {
        startDate
      }
      val until = if (periodEnd != null && endDate.isAfter(periodEnd)) {
        useHalfDayEnd = false // end is outside of period, can't be counted as half day anymore.
        periodEnd
      } else {
        endDate
      }
      var numberOfWorkingDays = PFDayUtils.getNumberOfWorkingDays(from, until)
      if (numberOfWorkingDays > BigDecimal.ZERO) {
        if (useHalfDayBegin) {
          val workingHours = PFDayUtils.getNumberOfWorkingDays(from, from)
          if (workingHours == BigDecimal.ONE) {
            numberOfWorkingDays -= HALF_DAY
          } else {
            log.warn(
              "User tried to get an half day-off at $from, but this date is not a full working day (${
                VacationStats.format(
                  numberOfWorkingDays
                )
              }. Ignoring half-day switch."
            )
          }
        }
        if (useHalfDayEnd && (!useHalfDayBegin || from != until)) {
          // Don't reduce working days if halfDayBegin and halfDayEnd is given for same date.
          val workingHours = PFDayUtils.getNumberOfWorkingDays(until, until)
          if (workingHours == BigDecimal.ONE) {
            numberOfWorkingDays -= HALF_DAY
          } else {
            log.warn(
              "User tried to get an half day-off at $until, but this date is not a full working day (${
                VacationStats.format(
                  numberOfWorkingDays
                )
              }. Ignoring half-day switch."
            )
          }
        }
      }
      return numberOfWorkingDays
    }
  }
}
