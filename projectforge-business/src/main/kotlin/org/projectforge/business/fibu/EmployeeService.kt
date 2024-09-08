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

package org.projectforge.business.fibu

import org.apache.commons.collections4.CollectionUtils
import org.projectforge.business.fibu.*
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.business.user.UserDao
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.stream.Collectors

/**
 * Standard implementation of the Employee service interface.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Service
class EmployeeService {
    @Autowired
    private val userDao: UserDao? = null

    @Autowired
    private val kost1Dao: Kost1Dao? = null

    @Autowired
    private val employeeDao: EmployeeDao? = null

    @Autowired
    private val vacationService: VacationService? = null

    @Autowired
    private val timesheetDao: TimesheetDao? = null

    fun getList(filter: BaseSearchFilter): List<EmployeeDO> {
        return employeeDao!!.getList(filter)
    }

    fun setPfUser(employee: EmployeeDO, userId: Int?) {
        val user = userDao!!.getOrLoad(userId)
        employee.user = user
    }

    fun getEmployeeByUserId(userId: Int?): EmployeeDO? {
        return employeeDao!!.findByUserId(userId)
    }

    fun setKost1(employee: EmployeeDO, kost1Id: Int?) {
        val kost1 = kost1Dao!!.getOrLoad(kost1Id)
        employee.kost1 = kost1
    }

    fun hasLoggedInUserInsertAccess(): Boolean {
        return employeeDao!!.hasLoggedInUserInsertAccess()
    }

    fun hasLoggedInUserInsertAccess(obj: EmployeeDO, throwException: Boolean): Boolean {
        return employeeDao!!.hasLoggedInUserInsertAccess(obj, throwException)
    }

    fun hasLoggedInUserUpdateAccess(obj: EmployeeDO, dbObj: EmployeeDO, throwException: Boolean): Boolean {
        return employeeDao!!.hasLoggedInUserUpdateAccess(obj, dbObj, throwException)
    }

    fun hasLoggedInUserDeleteAccess(obj: EmployeeDO, dbObj: EmployeeDO, throwException: Boolean): Boolean {
        return employeeDao!!.hasLoggedInUserDeleteAccess(obj, dbObj, throwException)
    }

    fun hasDeleteAccess(user: PFUserDO, obj: EmployeeDO, dbObj: EmployeeDO, throwException: Boolean): Boolean {
        return employeeDao!!.hasDeleteAccess(user, obj, dbObj, throwException)
    }

    @Throws(AccessException::class)
    fun getById(id: Serializable?): EmployeeDO? {
        return employeeDao!!.getById(id)
    }

    fun getAutocompletion(property: String, searchString: String): List<String> {
        return employeeDao!!.getAutocompletion(property, searchString)
    }

    fun getDisplayHistoryEntries(obj: EmployeeDO): List<DisplayHistoryEntry> {
        return employeeDao!!.getDisplayHistoryEntries(obj)
    }

    fun isEmployeeActive(employee: EmployeeDO): Boolean {
        if (employee.austrittsDatum == null) {
            return true
        }
        val now = now()
        val austrittsdatum = from(employee.austrittsDatum!!) // not null
        return now.isBefore(austrittsdatum)
    }

    fun getMonthlySalary(employee: EmployeeDO?, selectedDate: PFDateTime?): BigDecimal? {
        log.error("****** Not yet migrated.")
        /*    final EmployeeTimedDO attribute = timeableService.getAttrRowValidAtDate(employee, "annuity", selectedDate.getUtilDate());
    final BigDecimal annualSalary = attribute != null ? attribute.getAttribute("annuity", BigDecimal.class) : null;
    final BigDecimal weeklyWorkingHours = employee.getWeeklyWorkingHours();

    if (annualSalary != null && weeklyWorkingHours != null && BigDecimal.ZERO.compareTo(weeklyWorkingHours) < 0) {
      // do the multiplication before the division to minimize rounding problems
      // we need a rounding mode to avoid ArithmeticExceptions when the exact result cannot be represented in the result
      return annualSalary
              .multiply(weeklyWorkingHours)
              .divide(MONTHS_PER_YEAR, BigDecimal.ROUND_HALF_UP)
              .divide(FULL_TIME_WEEKLY_WORKING_HOURS, BigDecimal.ROUND_HALF_UP);
    }
*/
        return null
    }

    fun findAllActive(checkAccess: Boolean): List<EmployeeDO> {
        val employeeList: Collection<EmployeeDO> = if (checkAccess) {
            employeeDao!!.getList(EmployeeFilter())
        } else {
            employeeDao!!.internalLoadAll()
        }
        return employeeList.stream()
            .filter { employee: EmployeeDO -> this.isEmployeeActive(employee) }
            .collect(Collectors.toList())
    }

    fun getEmployeeByStaffnumber(staffnumber: String): EmployeeDO? {
        return employeeDao!!.getEmployeeByStaffnumber(staffnumber)
    }

    fun getAll(checkAccess: Boolean): List<EmployeeDO> {
        return if (checkAccess) employeeDao!!.getList(EmployeeFilter()) else employeeDao!!.internalLoadAll()
    }

    fun getEmployeeStatus(employee: EmployeeDO?): EmployeeStatus? {
        log.error("****** Not yet migrated.")
        /*
    final EmployeeTimedDO attrRow = timeableService
            .getAttrRowValidAtDate(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME, new Date());
    if (attrRow != null && !StringUtils.isEmpty(attrRow.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_STATUS_DESC_NAME))) {
      return EmployeeStatus.findByi18nKey(attrRow.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_STATUS_DESC_NAME));
    }*/
        return null
    }

    fun getAnnualLeaveDays(employee: EmployeeDO?): BigDecimal? {
        return getAnnualLeaveDays(employee, LocalDate.now())
    }

    fun getAnnualLeaveDays(employee: EmployeeDO?, validAtDate: LocalDate?): BigDecimal? {
        if (employee == null || validAtDate == null) { // Should only occur in CallAllPagesTest (Wicket).
            return null
        }
        log.error("****** Not yet migrated.")
        return BigDecimal.ZERO
        /*
    Date date = PFDateTime.from(validAtDate).getUtilDate(); // not null
    final EmployeeTimedDO attrRow = timeableService
            .getAttrRowValidAtDate(employee, InternalAttrSchemaConstants.EMPLOYEE_ANNUAL_LEAVEDAYS_GROUP_NAME, date);
    if (attrRow != null) {
      final String str = attrRow.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_ANNUAL_LEAVEDAYS_PROP_NAME);
      if (NumberUtils.isCreatable(str)) {
        return NumberUtils.createBigDecimal(str);
      }
    }
    return BigDecimal.ZERO;*/
    }

    /*
  @Override
  public EmployeeTimedDO addNewAnnualLeaveDays(final EmployeeDO employee, final LocalDate validfrom, final BigDecimal annualLeaveDays) {
    final EmployeeTimedDO newAttrRow = addNewTimeAttributeRow(employee, InternalAttrSchemaConstants.EMPLOYEE_ANNUAL_LEAVEDAYS_GROUP_NAME);
    newAttrRow.setStartTime(PFDay.from(validfrom).getUtilDate());
    newAttrRow.putAttribute(InternalAttrSchemaConstants.EMPLOYEE_ANNUAL_LEAVEDAYS_PROP_NAME, annualLeaveDays);
    return newAttrRow;
  }*/
    fun getReportOfMonth(year: Int, month: Int?, user: PFUserDO): MonthlyEmployeeReport {
        val monthlyEmployeeReport = MonthlyEmployeeReport(this, vacationService, user, year, month)
        monthlyEmployeeReport.init()
        val filter = TimesheetFilter()
        filter.setDeleted(false)
        filter.startTime = monthlyEmployeeReport.fromDate
        filter.stopTime = monthlyEmployeeReport.toDate
        filter.userId = user.id
        val list = timesheetDao!!.getList(filter)
        val loggedInUser = ThreadLocalUserContext.user
        if (CollectionUtils.isNotEmpty(list)) {
            for (sheet in list) {
                monthlyEmployeeReport.addTimesheet(
                    sheet,
                    timesheetDao.hasUserSelectAccess(loggedInUser!!, sheet, false)
                )
            }
        }
        monthlyEmployeeReport.calculate()
        return monthlyEmployeeReport
    }

    fun isFulltimeEmployee(employee: EmployeeDO?, selectedDate: PFDateTime): Boolean {
        val startOfMonth = selectedDate.utilDate
        val dt = selectedDate.plusMonths(1).minusDays(1)
        val endOfMonth = dt.utilDate
        log.error("****** Not yet migrated.")
        return true

        /*
    final List<EmployeeTimedDO> attrRows = timeableService
            .getAttrRowsWithinDateRange(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME, startOfMonth, endOfMonth);

    final EmployeeTimedDO rowValidAtBeginOfMonth = timeableService
            .getAttrRowValidAtDate(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME, selectedDate.getUtilDate());

    if (rowValidAtBeginOfMonth != null) {
      attrRows.add(rowValidAtBeginOfMonth);
    }

    return attrRows
            .stream()
            .map(row -> row.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_STATUS_DESC_NAME))
            .filter(Objects::nonNull)
            .anyMatch(s -> EmployeeStatus.FEST_ANGESTELLTER.getI18nKey().equals(s) || EmployeeStatus.BEFRISTET_ANGESTELLTER.getI18nKey().equals(s));
            */
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(KundeDao::class.java)

        private val FULL_TIME_WEEKLY_WORKING_HOURS = BigDecimal(40)

        private val MONTHS_PER_YEAR = BigDecimal(12)
    }
}
