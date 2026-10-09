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

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.common.StringHelper
import org.projectforge.framework.calendar.Holidays
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*

/**
 * The working days of a month without time sheets ("ohne Zeitberichte"), shared by the monthly employee report
 * and the notifications about missing time sheets, so both show the same days: every working day of the month without any time sheet,
 * except the days of an approved vacation (a half day included).
 */
@Service
open class UnbookedDaysCalculator {
  @Autowired
  private lateinit var caches: PfCaches

  @Autowired
  private lateinit var timesheetDao: TimesheetDao

  @Autowired
  private lateinit var vacationService: VacationService

  /** The days of month of the approved vacations (special ones included) of the given employee. */
  open fun vacationDays(employee: EmployeeDO?, month: YearMonth): Set<Int> {
    employee?.id ?: return emptySet()
    val vacations = vacationService.getVacationsListForPeriod(
      employee.id, month.atDay(1), month.atEndOfMonth(), withSpecial = true
    )
    return vacationDays(month, vacations)
  }

  /**
   * The unbooked days of all given employees, by employee id. Without access checks and without a logged-in user
   * (cron job): the time sheets are read in one query, the days are those of each employee's own time zone, as
   * the employee sees them in the own monthly report.
   */
  open fun unbookedDays(employees: Collection<EmployeeDO>, month: YearMonth): Map<Long, List<Int>> {
    // Widened by a day on both sides: the time zones of the users differ, the days are cut per user below.
    val from = Date.from(month.atDay(1).minusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant())
    val to = Date.from(month.atEndOfMonth().plusDays(2).atStartOfDay(ZoneId.of("UTC")).toInstant())
    val startTimesByUser = timesheetDao.getStartTimesByUser(from, to)
    val result = mutableMapOf<Long, List<Int>>()
    employees.forEach { employee ->
      val employeeId = employee.id ?: return@forEach
      val user = caches.getUser(employee.user?.id) ?: return@forEach
      val zoneId = user.timeZone.toZoneId()
      val bookedDays = startTimesByUser[user.id].orEmpty()
        .map { it.toInstant().atZone(zoneId).toLocalDate() }
        .filter { YearMonth.from(it) == month }
        .map { it.dayOfMonth }
        .toSet()
      result[employeeId] = unbookedDays(month, bookedDays, vacationDays(employee, month))
    }
    return result
  }

  companion object {
    /** The working days of the [month] neither booked nor covered by a vacation, as days of month. */
    @JvmStatic
    fun unbookedDays(month: YearMonth, bookedDays: Set<Int>, vacationDays: Set<Int>): List<Int> {
      return (1..month.lengthOfMonth()).filter { dayOfMonth ->
        Holidays.instance.isWorkingDay(month.atDay(dayOfMonth)) && dayOfMonth !in bookedDays && dayOfMonth !in vacationDays
      }
    }

    /** The days of the [month] covered by the approved ones of the given vacations. */
    @JvmStatic
    fun vacationDays(month: YearMonth, vacations: Collection<VacationDO>): Set<Int> {
      val result = mutableSetOf<Int>()
      vacations.filter { it.status == VacationStatus.APPROVED && !it.deleted }.forEach { vacation ->
        val start = vacation.startDate ?: return@forEach
        val end = vacation.endDate ?: return@forEach
        var day: LocalDate = maxOf(start, month.atDay(1))
        val last = minOf(end, month.atEndOfMonth())
        while (!day.isAfter(last)) {
          result.add(day.dayOfMonth)
          day = day.plusDays(1)
        }
      }
      return result
    }

    /** The days as `05.10., 06.10., …`, null if there are none. */
    @JvmStatic
    fun format(month: YearMonth, days: List<Int>): String? {
      if (days.isEmpty()) {
        return null
      }
      val monthOfYear = StringHelper.format2DigitNumber(month.monthValue)
      return days.joinToString(", ") { "${StringHelper.format2DigitNumber(it)}.$monthOfYear." }
    }
  }
}
