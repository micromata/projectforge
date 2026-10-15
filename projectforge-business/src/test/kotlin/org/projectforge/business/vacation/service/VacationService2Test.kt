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

package org.projectforge.business.vacation.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityStatus
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.LocalDate
import java.time.Month

class VacationService2Test {
  @Test
  fun checkConflictTest() {
    val employee = createEmployee(1)
    val substitute1 = createEmployee(2)
    val substitute2 = createEmployee(3)
    val vacation = createJuneVacation(employee, 1, 30) // Whole month
    vacation.replacement = substitute1
    val substitute1Vacation1 = createJuneVacation(substitute1, 1, 10)
    val substitute2Vacation1 = createJuneVacation(substitute2, 1, 10)
    val substitute2Vacation2 = createJuneVacation(substitute2, 11, 20)
    Assertions.assertFalse(VacationService().checkConflict(vacation, emptyList()), "No other vacations.")
    Assertions.assertTrue(VacationService().checkConflict(vacation, listOf(substitute1Vacation1)), "No substitute on duty found")

    vacation.otherReplacements = mutableSetOf(substitute2)
    Assertions.assertFalse(VacationService().checkConflict(vacation, listOf(substitute1Vacation1, substitute2Vacation2)), "Substitute 1 and 2 with different leave times.")
    Assertions.assertTrue(VacationService().checkConflict(vacation, listOf(substitute1Vacation1, substitute2Vacation1)), "Substitute 1 and 2 with overlapping leave times.")

    Assertions.assertFalse(VacationService().checkConflict(vacation, listOf(substitute1Vacation1)), "Substitute 2 has no vacations, so no conflict.")
  }

  @Test
  fun uncoveredDaysTest() {
    val employee = createEmployee(1)
    val substitute1 = createEmployee(2)
    val substitute2 = createEmployee(3)
    val vacation = createJuneVacation(employee, 1, 30)
    vacation.replacement = substitute1
    vacation.otherReplacements = mutableSetOf(substitute2)
    val service = VacationService()
    // Substitute 1 away 5th-10th, substitute 2 away 1st-6th and 9th-12th: nobody on duty on 5th, 6th, 9th and 10th.
    val days = service.uncoveredDays(
      vacation, listOf(
        createJuneVacation(substitute1, 5, 10),
        createJuneVacation(substitute2, 1, 6),
        createJuneVacation(substitute2, 9, 12),
      )
    )
    Assertions.assertEquals(listOf(5, 6, 9, 10), days.map { it.dayOfMonth })
    Assertions.assertTrue(
      service.checkConflict(vacation, listOf(createJuneVacation(substitute1, 20, 21), createJuneVacation(substitute1, 1, 3),
        createJuneVacation(substitute2, 2, 2))),
      "A second vacation of a substitute doesn't put them on duty on the days of the first one."
    )
  }

  @Test
  fun absentAvailabilityCountsAsVacationTest() {
    val employee = createEmployee(1)
    val substitute1 = createEmployee(2)
    val substitute2 = createEmployee(3)
    val vacation = createJuneVacation(employee, 1, 30)
    vacation.replacement = substitute1
    vacation.otherReplacements = mutableSetOf(substitute2)
    val service = VacationService()
    val substitute1Vacation = createJuneVacation(substitute1, 5, 10)
    // Substitute 2 not available 8th-12th (by their user): nobody on duty on 8th-10th.
    Assertions.assertEquals(
      listOf(8, 9, 10),
      service.uncoveredDays(
        vacation, listOf(substitute1Vacation),
        listOf(createJuneAvailability(substitute2, 8, 12, AvailabilityStatus.NOT_AVAILABLE)),
      ).map { it.dayOfMonth },
    )
    // Without any vacation, by availability entries only.
    Assertions.assertTrue(
      service.checkConflict(
        vacation, emptyList(), listOf(
          createJuneAvailability(substitute1, 1, 3, AvailabilityStatus.NOT_AVAILABLE),
          createJuneAvailability(substitute2, 2, 4, AvailabilityStatus.NOT_AVAILABLE),
        )
      ),
      "Both substitutes not available on the 2nd and 3rd."
    )
    // Reachable (remote, if needed, if urgent) is on duty: no conflict.
    AvailabilityStatus.entries.filter { !it.absent }.forEach { status ->
      Assertions.assertFalse(
        service.checkConflict(
          vacation, listOf(substitute1Vacation),
          listOf(createJuneAvailability(substitute2, 1, 30, status)),
        ),
        "$status doesn't make a conflict."
      )
    }
  }

  private fun createJuneAvailability(
    employee: EmployeeDO,
    fromDay: Int,
    toDay: Int,
    status: AvailabilityStatus,
  ): AvailabilityDO {
    val availability = AvailabilityDO()
    availability.user = employee.user
    availability.startDate = LocalDate.of(2022, Month.JUNE, fromDay)
    availability.endDate = LocalDate.of(2022, Month.JUNE, toDay)
    availability.status = status
    return availability
  }

  private fun createJuneVacation(employee: EmployeeDO, fromDay: Int, toDay: Int): VacationDO {
    val vacation = VacationDO()
    vacation.employee = employee
    vacation.startDate = LocalDate.of(2022, Month.JUNE, fromDay)
    vacation.endDate = LocalDate.of(2022, Month.JUNE, toDay)
    return vacation
  }

  private fun createEmployee(id: Long): EmployeeDO {
    val employee = EmployeeDO()
    employee.id = id
    employee.user = PFUserDO().also { it.id = 100 + id }
    return employee
  }
}
