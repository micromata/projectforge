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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.vacation.model.VacationDO
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

  private fun createJuneVacation(employee: EmployeeDO, fromDay: Int, toDay: Int): VacationDO {
    val vacation = VacationDO()
    vacation.employee = employee
    vacation.startDate = LocalDate.of(2022, Month.JUNE, fromDay)
    vacation.endDate = LocalDate.of(2022, Month.JUNE, toDay)
    return vacation
  }

  private fun createEmployee(id: Int): EmployeeDO {
    val employee = EmployeeDO()
    employee.id = id
    return employee
  }
}
