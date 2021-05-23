/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.api.EmployeeService
import org.projectforge.business.user.UserDao
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

private val log = KotlinLogging.logger {}

class VacationValidatorTest : AbstractTestBase() {
  @Autowired
  private lateinit var employeeDao: EmployeeDao

  @Autowired
  private lateinit var employeeService: EmployeeService

  @Autowired
  private lateinit var userDao: UserDao

  @Autowired
  private lateinit var vacationService: VacationService

  @Test
  fun validatorTest() {
    logon(TEST_EMPLOYEE_USER)
    val employee = createEmployee("2019-joiner-for-validation", LocalDate.of(2019, Month.MAY, 1))
    val manager = createEmployee("VacationValidatorTest-manager", LocalDate.of(2019, Month.MAY, 1))
    var vacation =
      createVacation(employee, manager, 2020, Month.JANUARY, 1, Month.JANUARY, 10, true, VacationStatus.IN_PROGRESS)
    vacation.startDate = null
    Assertions.assertEquals(VacationValidator.Error.DATE_NOT_SET, vacationService.validate(vacation))

    vacation.startDate = vacation.endDate!!.plusDays(1)
    Assertions.assertEquals(VacationValidator.Error.END_DATE_BEFORE_START_DATE, vacationService.validate(vacation))

    vacation.startDate = LocalDate.now().minusMonths(1)
    vacation.endDate = vacation.startDate!!.plusDays(2)
    log.error { "Test-output: $vacation" }
    Assertions.assertEquals(VacationValidator.Error.START_DATE_BEFORE_NOW, vacationService.validate(vacation))

    vacation.startDate = PFDayUtils.getNextWorkingDay(LocalDate.now().plusDays(1))
    vacation.endDate = vacation.startDate
    vacation.halfDayBegin = true
    Assertions.assertNull(vacationService.validate(vacation))
    vacation.endDate = vacation.endDate!!.plusDays(1)
    Assertions.assertNull(vacationService.validate(vacation))
    vacation.halfDayBegin = false

    vacation.startDate = LocalDate.now().with(Month.DECEMBER).withDayOfMonth(24)
    vacation.endDate = vacation.startDate!!.plusMonths(1).withDayOfMonth(6)
    Assertions.assertNull(vacationService.validate(vacation))

    // Collisions and check of enough left vacation days is tested in VacationServiceTest.
  }

  /**
   * If endMonth is before startMonth, the next year will be used as endYear.
   * @return Number of vacation days (equals to working days between startDate and endDate)
   */
  private fun createVacation(
    employee: EmployeeDO,
    manager: EmployeeDO,
    startYear: Int,
    startMonth: Month,
    startDay: Int,
    endMonth: Month,
    endDay: Int,
    special: Boolean,
    status: VacationStatus
  ): VacationDO {
    val endYear = if (startMonth > endMonth)
      startYear + 1 // Vacations over years.
    else
      startYear
    return createVacation(
      employee,
      manager,
      LocalDate.of(startYear, startMonth, startDay),
      LocalDate.of(endYear, endMonth, endDay),
      special,
      status
    )
  }

  /**
   * Ensures vacation days only after join date of this employee.
   * @return Number of vacation days (equals to working days between startDate and endDate)
   */
  private fun createVacation(
    employee: EmployeeDO,
    manager: EmployeeDO,
    startDate: LocalDate,
    endDate: LocalDate,
    special: Boolean = false,
    status: VacationStatus
  ): VacationDO {
    val vacation = VacationDO()
    vacation.employee = employee
    vacation.startDate = if (startDate.isBefore(employee.eintrittsDatum)) employee.eintrittsDatum else startDate
    vacation.endDate = endDate
    vacation.halfDayBegin = false
    vacation.special = false
    vacation.status = status
    vacation.manager = manager // OK for tests...
    vacation.special = special
    return vacation
  }

  private fun createEmployee(name: String, joinDate: LocalDate?, leaveDate: LocalDate? = null): EmployeeDO {
    val user = PFUserDO()
    user.firstname = name
    user.lastname = name
    user.username = "$name.$name"
    userDao.internalSave(user)
    val employee = EmployeeDO()
    employee.user = user
    employee.eintrittsDatum = joinDate
    employee.austrittsDatum = leaveDate
    employeeService.addNewAnnualLeaveDays(employee, joinDate, BigDecimal(30));
    employeeDao.internalSave(employee)
    return employee
  }
}
