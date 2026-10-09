/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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


package org.projectforge.business.timesheet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.MonthlyEmployeeReportDao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.business.vacation.service.VacationValidator
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.*

/**
 * The days without time sheets of the notifications must be the same as "without time sheets" of the monthly
 * report; approved vacation days (also half days) don't count.
 */
class UnbookedDaysCalculatorTest : AbstractTestBase() {
  @Autowired
  private lateinit var employeeDao: EmployeeDao

  @Autowired
  private lateinit var employeeService: EmployeeService

  @Autowired
  private lateinit var monthlyEmployeeReportDao: MonthlyEmployeeReportDao

  @Autowired
  private lateinit var timesheetDao: TimesheetDao

  @Autowired
  private lateinit var unbookedDaysCalculator: UnbookedDaysCalculator

  @Autowired
  private lateinit var vacationDao: VacationDao

  override fun beforeAll() {
    VacationValidator.rejectNewVacationEntriesBeforeNow = false
  }

  override fun afterAll() {
    VacationValidator.rejectNewVacationEntriesBeforeNow = true // Reset to normal value.
  }

  @Test
  fun unbookedDaysTest() {
    persistenceService.runInTransaction { _ ->
      logon(TEST_FINANCE_USER)
      val user = initTestDB.addUser("unbooked-days-user")
      val employee = insertEmployee(user)
      val manager = insertEmployee(initTestDB.addUser("unbooked-days-manager"))
      val task = initTestDB.addTask("unbooked-days-task", "root")
      // March 2032: Good Friday (26th) and Easter Monday (29th) are holidays.
      insertTimesheet(task, user, 1)
      insertTimesheet(task, user, 2)
      insertTimesheet(task, user, 2, hour = 14)
      insertTimesheet(task, user, 6) // Saturday.
      insertVacation(employee, manager, 3, 5)
      insertVacation(employee, manager, 8, 8, halfDay = true)
      insertVacation(employee, manager, 9, 9, VacationStatus.IN_PROGRESS)
      val expected = listOf(9, 10, 11, 12, 15, 16, 17, 18, 19, 22, 23, 24, 25, 30, 31)
      val month = YearMonth.of(2032, 3)
      assertEquals(expected, unbookedDaysCalculator.unbookedDays(listOf(employee), month)[employee.id])
      assertEquals(expected, monthlyEmployeeReportDao.getReport(2032, 3, user, false)!!.getUnbookedDays())
      null
    }
  }

  private fun insertEmployee(user: PFUserDO): EmployeeDO {
    val employee = EmployeeDO()
    employee.user = user
    employee.eintrittsDatum = LocalDate.of(2020, 1, 1)
    employeeDao.insert(employee, checkAccess = false)
    employeeService.insertAnnualLeaveDays(employee, employee.eintrittsDatum!!, BigDecimal(30), checkAccess = false)
    return employee
  }

  private fun insertTimesheet(task: TaskDO, user: PFUserDO, day: Int, hour: Int = 8) {
    val start = LocalDateTime.of(2032, 3, day, hour, 0).atZone(ZoneId.of("UTC"))
    val sheet = TimesheetDO()
    sheet.task = task
    sheet.user = user
    sheet.startTime = Date.from(start.toInstant())
    sheet.stopTime = Date.from(start.plusHours(2).toInstant())
    sheet.description = "Unbooked days test"
    timesheetDao.insert(sheet, checkAccess = false)
  }

  private fun insertVacation(
    employee: EmployeeDO, manager: EmployeeDO, from: Int, until: Int,
    status: VacationStatus = VacationStatus.APPROVED, halfDay: Boolean = false,
  ) {
    val vacation = VacationDO()
    vacation.employee = employee
    vacation.manager = manager
    vacation.replacement = employee
    vacation.startDate = LocalDate.of(2032, 3, from)
    vacation.endDate = LocalDate.of(2032, 3, until)
    vacation.halfDayBegin = halfDay
    vacation.special = false
    vacation.status = status
    vacationDao.insert(vacation, checkAccess = false)
  }
}
