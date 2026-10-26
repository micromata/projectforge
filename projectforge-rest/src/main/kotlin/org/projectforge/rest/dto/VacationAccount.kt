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

package org.projectforge.rest.dto

import org.projectforge.business.vacation.service.VacationStatsFormatted
import java.time.LocalDate

/**
 * The vacation account ("Urlaubskonto") of an employee, for the page of projectforge-next
 * (`org.projectforge.rest.VacationAccountRest`): the statistics of the previous, current and next year, the
 * vacations of these years, the leave account entries of the previous and current year and the availability entries of
 * the three years.
 *
 * Every value is formatted for the logged-in user already; the urls are paths of the app (`/next/…`).
 */
class VacationAccount(
    /** The employee shown. Null if the logged-in user has no employee (then everything else is empty). */
    val employee: VacationAccountEmployee?,
    /** The logged-in user is HR staff: may choose any employee and recalculate the remaining leave. */
    val hrAccess: Boolean,
    /** HR staff only, and only if the carry of the current year differs from what was left of the previous one. */
    val canRecalculate: Boolean = false,
    /** Previous, current and next year, in this order. */
    val statistics: List<VacationStatsFormatted> = emptyList(),
    /** The label of the row of the unused carry: it names the day the carry expires on. */
    val previousYearLeaveUnusedLabel: String? = null,
    /** Next, current and previous year, in this order (as the old page showed them). */
    val years: List<VacationAccountYear> = emptyList(),
    /** Newest first. */
    val leaveAccountEntries: List<VacationAccountLeaveEntry> = emptyList(),
    /** The booked hours of the last three months divided by the working days, as a localized text. */
    val workingHoursStatistics: String? = null,
    /** A new vacation of this employee; its form returns here. */
    val newVacationUrl: String? = null,
    /**
     * The availability entries of the employee's user overlapping the previous, current and next year, newest first:
     * the other absences, apart from the vacations.
     */
    val availabilities: List<VacationAccountAvailability> = emptyList(),
    /** A new availability entry of the employee's user; its form returns here. Null for an employee without a user. */
    val newAvailabilityUrl: String? = null,
    /** A new leave account entry of this employee; its form returns here. HR staff with write access only. */
    val newLeaveAccountEntryUrl: String? = null,
    val vacationListUrl: String,
    val exportUrl: String,
)

class VacationAccountEmployee(
    val id: Long?,
    val displayName: String?,
    /** Whether the employee is no longer active (deleted, left): the name is struck through. */
    val deleted: Boolean = false,
    val deactivated: Boolean = false,
)

class VacationAccountYear(
    val year: Int,
    val vacations: List<VacationAccountVacation>,
)

class VacationAccountVacation(
    val id: Long?,
    val startDateFormatted: String?,
    val endDateFormatted: String?,
    val statusString: String?,
    val workingDaysFormatted: String?,
    val specialFormatted: String?,
    /** The replacements and the manager as references, so a no longer active one is struck through. */
    val replacement: Employee?,
    val otherReplacements: List<Employee>?,
    val manager: Employee?,
    val vacationModeString: String?,
    val comment: String?,
    /** At least one day of it without any replacement on duty (`ConflictingVacationsCache`). */
    val conflict: Boolean,
    /** The period and its days for the days bar, as in the vacation list. */
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val halfDayBegin: Boolean?,
    val halfDayEnd: Boolean?,
    /** Its weekends and holidays; null for a period the bar doesn't draw. */
    val nonWorkingDays: List<LocalDate>?,
    /** Its days without any replacement on duty, only on a conflict. */
    val uncoveredDays: List<LocalDate>?,
    /** Its form, which returns here. */
    val editUrl: String,
)

class VacationAccountLeaveEntry(
    val id: Long?,
    val dateFormatted: String?,
    val amountFormatted: String?,
    val description: String?,
    /** Its form, which returns here. */
    val editUrl: String,
)

class VacationAccountAvailability(
    val id: Long?,
    val startDateFormatted: String?,
    val endDateFormatted: String?,
    /** For the user and HR staff only (AvailabilityDao.isInternalCommentVisibleFor), as everywhere. */
    val type: String?,
    val statusString: String?,
    val locationString: String?,
    val comment: String?,
    /** The substitutes as references, so a no longer active one is struck through. */
    val substitutes: List<User>?,
    /** At least one day from today on without any substitute on duty (`ConflictingAvailabilitiesCache`). */
    val conflict: Boolean,
    /** Its working days (weekends and holidays not counted), as of a vacation. */
    val workingDaysFormatted: String?,
    /** The period and its days for the days bar, as in the availability list. */
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    /** Its weekends and holidays; null for a period the bar doesn't draw. */
    val nonWorkingDays: List<LocalDate>?,
    /** Its days without any substitute on duty, only on a conflict (from today on for anybody but HR staff). */
    val uncoveredDays: List<LocalDate>?,
    /** Its form, which returns here. */
    val editUrl: String,
)
