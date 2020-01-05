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
import org.projectforge.business.vacation.model.VacationAttrProperty
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
import org.projectforge.framework.time.PFDayUtils.Companion.getNumberOfWorkingDays
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


/**
 * Standard implementation of the vacation service interface.
 *
 * @author Florian Blumenstein
 */
// Must be open for usage by SpringBeans (Wicket).
@Service
open class VacationService : CorePersistenceServiceImpl<Int, VacationDO>(), IPersistenceService<VacationDO>, IDao<VacationDO> {
    @Autowired
    private lateinit var vacationDao: VacationDao
    @Autowired
    private lateinit var configService: ConfigurationService
    @Autowired
    private lateinit var employeeDao: EmployeeDao
    @Autowired
    private lateinit var employeeService: EmployeeService

    /**
     * Getting the number of used and planned vacation days
     *
     * @param employee
     * @param year
     * @return number of used vacation days
     */
    open fun getApprovedAndPlannedVacationdaysForYear(employee: EmployeeDO?, year: Int): BigDecimal? {
        val approved = getApprovedVacationdaysForYear(employee, year)
        val planned = getPlannedVacationdaysForYear(employee, year)
        return approved.add(planned)
    }

    /**
     * Returns the date for ending usage of vacation from last year
     */
    open val endDateVacationFromLastYear: LocalDate
        get() = configService.endDateVacationFromLastYear

    /**
     * Updates the used days from last year
     *
     * @param vacationData
     * @return new value for used days
     */
    open fun updateUsedVacationDaysFromLastYear(vacationData: VacationDO?): BigDecimal? {
        if (vacationData == null || vacationData.employee == null || vacationData.startDate == null || vacationData.endDate == null) {
            return BigDecimal.ZERO
        }
        if (vacationData.special!!) {
            if (vacationData.id != null) {
                val vacation = vacationDao.getById(vacationData.id)
                if (!vacation.special!!) {
                    return deleteUsedVacationDaysFromLastYear(vacation)
                }
            }
            return BigDecimal.ZERO
        }
        val startDate = vacationData.startDate
        val endDateVacationFromLastYear = endDateVacationFromLastYear
        if (startDate!!.year > Year.now().value && !startDate.isBefore(endDateVacationFromLastYear)) {
            return BigDecimal.ZERO
        }
        val endDate = if (vacationData.endDate!!.isBefore(endDateVacationFromLastYear)) vacationData.endDate else endDateVacationFromLastYear
        val neededDaysForVacationFromLastYear = getVacationDays(startDate, endDate, vacationData.halfDay)
        val employee = vacationData.employee
        val actualUsedDaysOfLastYear = getVacationFromPreviousYearUsed(employee)
        val vacationFromPreviousYear = getVacationFromPreviousYear(employee)
        val freeDaysFromLastYear = vacationFromPreviousYear.subtract(actualUsedDaysOfLastYear)
        val remainValue = if (freeDaysFromLastYear.subtract(neededDaysForVacationFromLastYear).compareTo(BigDecimal.ZERO) < 0) BigDecimal.ZERO else freeDaysFromLastYear.subtract(neededDaysForVacationFromLastYear)
        val newValue = vacationFromPreviousYear.subtract(remainValue)
        employee!!.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.propertyName, newValue)
        employeeDao.internalUpdate(employee)
        return newValue
    }

    /**
     * Calculates the vacationsdays from last year and updates it.
     *
     * @param employee
     * @param year
     */
    open fun updateVacationDaysFromLastYearForNewYear(employee: EmployeeDO?, year: Int) {
        val availableVacationdaysFromActualYear = getAvailableVacationdaysForGivenYear(employee, year, false)
        employee!!.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.propertyName, availableVacationdaysFromActualYear)
        // find approved vacations in new year
        val from = LocalDate.of(year + 1, Month.JANUARY, 1)
        val to = endDateVacationFromLastYear.withYear(year + 1)
        val vacationNewYear = vacationDao.getVacationForPeriod(employee, from, to, false)
        var usedInNewYear = BigDecimal.ZERO
        for (vacation in vacationNewYear) {
            if (vacation.status != VacationStatus.APPROVED) {
                continue
            }
            // compute used days until EndDateVacationFromLastYear
            val days = this
                    .getVacationDays(vacation.startDate, if (vacation.endDate!!.isAfter(to)) to else vacation.endDate, vacation.halfDay)
            usedInNewYear = usedInNewYear.add(days)
        }
        // compute used days
        val usedDays = if (availableVacationdaysFromActualYear.compareTo(usedInNewYear) < 1) availableVacationdaysFromActualYear else usedInNewYear
        employee.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.propertyName, usedDays)
        employeeDao.internalUpdate(employee)
    }

    private fun getAvailableVacationdaysForGivenYear(currentEmployee: EmployeeDO?, year: Int, b: Boolean): BigDecimal {
        val vacationdays = if (currentEmployee!!.urlaubstage != null) BigDecimal(currentEmployee.urlaubstage!!) else BigDecimal.ZERO
        val vacationdaysPreviousYear = currentEmployee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.propertyName, BigDecimal::class.java)
                ?: BigDecimal.ZERO
        val subtotal1 = vacationdays.add(vacationdaysPreviousYear)
        val approvedVacationdays = getApprovedVacationdaysForYear(currentEmployee, year)
        var availableVacation = subtotal1.subtract(approvedVacationdays)
        //Needed for left and middle part
        val vacationdaysPreviousYearUsed = currentEmployee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.propertyName, BigDecimal::class.java)
                ?: BigDecimal.ZERO
        val vacationdaysPreviousYearUnused = vacationdaysPreviousYear.subtract(vacationdaysPreviousYearUsed)
        //If previousyearleaveunused > 0, then extend left area and display new row
        if (vacationdaysPreviousYearUnused.compareTo(BigDecimal.ZERO) > 0) {
            availableVacation = availableVacation.subtract(vacationdaysPreviousYearUnused)
        }
        return availableVacation
    }

    /**
     * Delete the used days from vacation to last year
     *
     * @param vacationData
     * @return new value for used days
     */
    open fun deleteUsedVacationDaysFromLastYear(vacationData: VacationDO?): BigDecimal? {
        val employee = vacationData?.employee
        if (employee == null || vacationData.special!! || vacationData.startDate == null || vacationData.endDate == null) {
            return BigDecimal.ZERO
        }
        val actualUsedDaysOfLastYear = getVacationFromPreviousYearUsed(employee)
        val vacationFromPreviousYear = getVacationFromPreviousYear(employee)
        val startDate = vacationData.startDate
        val endDateVacationFromLastYear = configService.endDateVacationFromLastYear
        val vacationList = getVacationForDate(employee, startDate, endDateVacationFromLastYear, false)
        // sum vacation days until "configured end date vacation from last year"
        val dayCount = vacationList!!.stream()
                .map { vacation: VacationDO? ->
                    val endDate = if (!vacation!!.endDate!!.isAfter(endDateVacationFromLastYear)) vacation.endDate // before or equal to endDate
                    else endDateVacationFromLastYear
                    getVacationDays(vacation.startDate, endDate, vacation.halfDay)
                }
                .reduce(BigDecimal.ZERO) { obj: BigDecimal?, augend: BigDecimal? -> obj!!.add(augend) }
        var newDays = BigDecimal.ZERO
        if (dayCount!!.compareTo(vacationFromPreviousYear) < 0) // dayCount < vacationFromPreviousYear
        {
            newDays = if (vacationData.endDate!!.compareTo(endDateVacationFromLastYear) <= 0) {
                actualUsedDaysOfLastYear.subtract(getVacationDays(vacationData))
            } else {
                actualUsedDaysOfLastYear.subtract(getVacationDays(vacationData.startDate, endDateVacationFromLastYear, vacationData.halfDay))
            }
            if (newDays.compareTo(BigDecimal.ZERO) < 0) {
                newDays = BigDecimal.ZERO
            }
        }
        employee.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.propertyName, newDays)
        employeeDao.internalUpdate(employee)
        return newDays
    }

    /**
     * Check, if user is able to use vacation services.
     *
     * @param user
     * @param throwException
     * @return
     */
    open fun couldUserUseVacationService(user: PFUserDO?, throwException: Boolean): Boolean {
        var result = true
        if (user == null || user.id == null) {
            return false
        }
        val employee = employeeService.getEmployeeByUserId(user.id)
        if (employee == null) {
            if (throwException) {
                throw AccessException("access.exception.noEmployeeToUser")
            }
            result = false
        } else if (employee.urlaubstage == null) {
            if (throwException) {
                throw AccessException("access.exception.employeeHasNoVacationDays")
            }
            result = false
        }
        return result
    }

    /**
     * Returns the number of approved vacation days
     *
     * @param employee
     * @param year
     * @return
     */
    open fun getApprovedVacationdaysForYear(employee: EmployeeDO?, year: Int): BigDecimal {
        return getVacationDaysForYearByStatus(employee, year, VacationStatus.APPROVED)
    }

    /**
     * Returns the number of planed vacation days
     *
     * @param employee
     * @param year
     * @return
     */
    open fun getPlannedVacationdaysForYear(employee: EmployeeDO?, year: Int): BigDecimal {
        return getVacationDaysForYearByStatus(employee, year, VacationStatus.IN_PROGRESS)
    }

    private fun getVacationDaysForYearByStatus(employee: EmployeeDO?, year: Int, status: VacationStatus): BigDecimal {
        return sum(getActiveVacationForYear(employee, year, false).filter { it.status == status })
    }

    private fun sum(list: List<VacationDO?>, until: LocalDate? = null): BigDecimal {
        var sum = BigDecimal.ZERO
        list.forEach {
            if (it != null) {
                val numberOfDays = getVacationDays(it, until)
                if (numberOfDays != null)
                    sum += numberOfDays
            }
        }
        return sum
    }

    /**
     * Returns the number of available vacation for user object. If user has no employee, it returns 0.
     *
     * @param user
     * @param year
     * @param checkLastYear
     * @return number of available vacation
     */
    open fun getAvailableVacationdaysForYear(user: PFUserDO?, year: Int, checkLastYear: Boolean): BigDecimal? {
        if (user == null) {
            return BigDecimal.ZERO
        }
        val employee = employeeService.getEmployeeByUserId(user.pk) ?: return BigDecimal.ZERO
        return getAvailableVacationdaysForYear(employee, year, checkLastYear)
    }

    /**
     * Returns the number of available vacation
     *
     * @param employee
     * @param year
     * @param checkLastYear
     * @return number of available vacation
     */
    open fun getAvailableVacationdaysForYear(employee: EmployeeDO?, year: Int, checkLastYear: Boolean): BigDecimal? {
        if (employee?.urlaubstage == null) {
            return BigDecimal.ZERO
        }
        val vacationDays = BigDecimal(employee.urlaubstage!!)
        val now = LocalDate.now()
        val endDateVacationFromLastYear = configService.endDateVacationFromLastYear
        val vacationFromPreviousYear: BigDecimal
        vacationFromPreviousYear = if (year != now.year) {
            BigDecimal.ZERO
        } else if (!checkLastYear || now.isAfter(endDateVacationFromLastYear)) {
            getVacationFromPreviousYearUsed(employee)
        } else { // before or same day as endDateVacationFromLastYear
            getVacationFromPreviousYear(employee)
        }
        val approvedVacation = getApprovedVacationdaysForYear(employee, year)
        val planedVacation = getPlannedVacationdaysForYear(employee, year)
        return vacationDays
                .add(vacationFromPreviousYear)
                .subtract(approvedVacation)
                .subtract(planedVacation)
    }

    /**
     * Returns the number of available vacation days for the given employee at the given date.
     * For example: If date is 2017-04-30, then the approved vacation between 2017-01-01 and 2017-04-30 is regarded.
     * Also the (used) vacation from the previous year is regarded depending on the given date.
     *
     * @param employee
     * @param queryDate
     * @return
     */
    open fun getPlandVacationDaysForYearAtDate(employee: EmployeeDO, queryDate: LocalDate?): BigDecimal? {
        val endDate = queryDate!!.withMonth(12).withDayOfMonth(31)
        return getApprovedVacationDaysForYearUntilDate(employee, queryDate, endDate)
    }

    open fun getAvailableVacationDaysForYearAtDate(employee: EmployeeDO?, queryDate: LocalDate): BigDecimal? {
        val urlaubstage = employee?.urlaubstage ?: return BigDecimal.ZERO
        val startDate = queryDate.withMonth(1).withDayOfMonth(1)
        val vacationDays = BigDecimal(urlaubstage)
        val vacationDaysPrevYear = getVacationDaysFromPrevYearDependingOnDate(employee, queryDate)
        val approvedVacationDays = getApprovedVacationDaysForYearUntilDate(employee, startDate, queryDate)
        return vacationDays + vacationDaysPrevYear - approvedVacationDays
    }

    private fun getVacationDaysFromPrevYearDependingOnDate(employee: EmployeeDO, queryDate: LocalDate): BigDecimal {
        val endDateVacationFromLastYear = configService.endDateVacationFromLastYear
        if (queryDate.year != endDateVacationFromLastYear.year) {
            // year of query is different form the year of endDateVacationFromLastYear
            // therefore the vacation from previous year values are from the wrong year
            // therefore we don't know the right values and return zero
            return BigDecimal.ZERO
        }
        return if (queryDate.isAfter(endDateVacationFromLastYear))
            getVacationFromPreviousYearUsed(employee)
        else
            getVacationFromPreviousYear(employee)
    }

    private fun getApprovedVacationDaysForYearUntilDate(employee: EmployeeDO, from: LocalDate, until: LocalDate?): BigDecimal {
        val vacations = getVacationForDate(employee, from, until, false)
        return sum(vacations!!.filter { it?.status == VacationStatus.APPROVED }, until)
    }

    private fun getVacationFromPreviousYearUsed(employee: EmployeeDO?): BigDecimal {
        val prevYearLeaveUsed = employee!!.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.propertyName, BigDecimal::class.java)
        return prevYearLeaveUsed ?: BigDecimal.ZERO
    }

    private fun getVacationFromPreviousYear(employee: EmployeeDO?): BigDecimal {
        val prevYearLeave = employee!!.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.propertyName, BigDecimal::class.java)
        return prevYearLeave ?: BigDecimal.ZERO
    }

    /**
     * Getting all not deleted vacations for given employee of the current year.
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
     * Load all active vacations (not marked as deleted)
     *
     * @param employee
     * @param withSpecial
     * @return
     */
    open fun getAllActiveVacation(employee: EmployeeDO?, withSpecial: Boolean): List<VacationDO?>? {
        return vacationDao.getAllActiveVacation(employee, withSpecial)
    }

    override fun getList(filter: BaseSearchFilter): List<VacationDO> {
        return vacationDao.getList(filter)
    }

    /**
     * Getting vacation for given ids.
     *
     * @param idList
     * @return List of vacations
     */
    open fun getVacation(idList: List<Serializable?>?): List<VacationDO?>? {
        return vacationDao.internalLoad(idList)
    }

    /**
     * Getting all vacations for given employee and a time period.
     *
     * @param employee
     * @param startDate
     * @param endDate
     * @return List of vacations
     */
    // Must be open for mocking.
    open fun getVacationForDate(employee: EmployeeDO, startDate: LocalDate?, endDate: LocalDate?, withSpecial: Boolean): List<VacationDO?>? {
        return vacationDao.getVacationForPeriod(employee, startDate, endDate, withSpecial)
    }

    /**
     * Returns number of open applications for leave for users employee
     *
     * @param user
     * @return
     */
    open fun getOpenLeaveApplicationsForUser(user: PFUserDO): BigDecimal {
        val employee = employeeService.getEmployeeByUserId(user.id) ?: return BigDecimal.ZERO
        return vacationDao.getOpenLeaveApplicationsForEmployee(employee)
    }

    /**
     * Returns number of special vacations for an employee
     *
     * @param employee
     * @param year
     * @param status
     * @return
     */
    open fun getSpecialVacationCount(employee: EmployeeDO, year: Int, status: VacationStatus?): BigDecimal? {
        return sum(vacationDao.getSpecialVacation(employee, year, status))
    }

    open fun getVacationDays(vacationData: VacationDO): BigDecimal? {
        return getVacationDays(vacationData, null)
    }

    private fun getVacationDays(vacationData: VacationDO, until: LocalDate?): BigDecimal? {
        val startDate = vacationData.startDate
        val endDate = vacationData.endDate
        if (startDate != null && endDate != null) {
            val endDateToUse: LocalDate = if (until != null && until.isBefore(endDate)) until else endDate
            return getVacationDays(startDate, endDateToUse, vacationData.halfDay)
        }
        return null
    }

    /**
     * Returns the number of vacation days for the given period.
     *
     * @param from
     * @param to
     * @param isHalfDayVacation
     * @return
     */
    // Must be open for mocking in tests.
    open fun getVacationDays(from: LocalDate?, to: LocalDate?, isHalfDayVacation: Boolean?): BigDecimal? {
        if (from == null || to == null) {
            log.warn("from=$from, to=$to. Both mustn't be null!")
            return null
        }
        val numberOfWorkingDays = getNumberOfWorkingDays(from, to)
        // don't return HALF_DAY if there is no working day
        return if (numberOfWorkingDays != BigDecimal.ZERO && java.lang.Boolean.TRUE == isHalfDayVacation // null evaluates to false
        ) HALF_DAY else numberOfWorkingDays
    }

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

    /**
     * Checks, if logged in User has HR vacation access.
     *
     * @return
     */
    open fun hasLoggedInUserHRVacationAccess(): Boolean {
        return vacationDao.hasLoggedInUserHRVacationAccess()
    }

    override fun getAutocompletion(property: String, searchString: String): List<String> {
        return vacationDao.getAutocompletion(property, searchString)
    }

    override fun getDisplayHistoryEntries(obj: VacationDO): List<DisplayHistoryEntry> {
        return vacationDao.getDisplayHistoryEntries(obj)
    }

    override fun rebuildDatabaseIndex4NewestEntries() {
        vacationDao.rebuildDatabaseIndex4NewestEntries()
    }

    override fun rebuildDatabaseIndex() {
        vacationDao.rebuildDatabaseIndex()
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
    open fun getVacationCount(fromYear: Int, fromMonth: Int, toYear: Int, toMonth: Int, user: PFUserDO?): String? {
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

    companion object {
        private val log = LoggerFactory.getLogger(VacationService::class.java)
        private val HALF_DAY = BigDecimal(0.5)
    }
}
