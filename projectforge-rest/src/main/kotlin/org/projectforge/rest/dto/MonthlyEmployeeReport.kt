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

/**
 * The next-facing DTO of a monthly employee report ("Monatsbericht"), the successor of Wicket's
 * `MonthlyEmployeeReportPage`. It carries the already-computed and already-formatted report so the next
 * frontend renders it as a matrix (rows = cost units / tasks, columns = calendar weeks) plus the header
 * block of statistics. The heavy lifting stays in `org.projectforge.business.fibu.MonthlyEmployeeReport`;
 * this only reshapes it into plain data.
 *
 * All durations, percentages and dates arrive **formatted in the user's locale** (the business class's own
 * formatters), so the frontend takes them as-is (see projectforge-next CLAUDE.md: backend-formatted values
 * are not reformatted).
 */
class MonthlyEmployeeReportData(
    /** The user the report is about (defaults to the logged-in user). */
    val userId: Long?,
    val userName: String,
    /** True if the logged-in user may pick another user (access to other users' time sheets). */
    val maySelectOtherUsers: Boolean,
    val year: Int,
    /** 1-based: 1 = January, ..., 12 = December. */
    val month: Int,
    /** Years that have time sheets for [userId], for the year dropdown (descending). */
    val availableYears: List<Int>,
    /** Whether cost accounting is configured (adds the Kost1 header field and Kost2 columns). */
    val costConfigured: Boolean,
    /** Formatted Kost1 number of the employee, or null. */
    val kost1: String?,
    /** Number of working days in the month (formatted). */
    val numberOfWorkingDays: String?,
    /** Working days without time sheets, e.g. "03.11., 08.11." or null. */
    val formattedUnbookedDays: String?,
    /** The localized average-working-time statistics line, or null. */
    val averageWorkingTimeStats: String?,
    /** Whether the "time saved by AI" column/rows are shown. */
    val timeSavingsByAIEnabled: Boolean,
    /** True if at least one Kost2 row exists (drives the four-column head vs. the single task head). */
    val hasKost2Rows: Boolean,
    val weeks: List<MonthlyEmployeeReportWeekDto>,
    val rows: List<MonthlyEmployeeReportRow>,
    /** Net total (working-time fraction applied), the red bold sum. */
    val totalNetDuration: String,
    /** Whether the gross-sum row is shown (gross differs from net). */
    val showGrossRow: Boolean,
    val totalGrossDuration: String,
    val totalTimeSavedByAI: String,
    /** Percentage of gross time saved by AI (only meaningful when [timeSavingsByAIEnabled]). */
    val timeSavedByAIPercentage: String,
    /** Whether the vacation figures may be shown to the logged-in user. */
    val vacationAvailable: Boolean,
    val vacationCount: String?,
    val vacationPlannedCount: String?,
    /** Fakturaquote (invoicing quota), formatted as a percentage, or null if not enabled. */
    val invoicingQuota: String?,
    /** Localized tooltip explaining how [invoicingQuota] was computed, or null. */
    val invoicingQuotaTooltip: String?,
    /** Drill-down window: first day of the month as `yyyy-MM-dd` (the timesheet-list period seed). */
    val startDate: String,
    /** Drill-down window: last day of the month as `yyyy-MM-dd`. */
    val endDate: String,
)

/** One calendar week bucket of the month (a matrix column). */
class MonthlyEmployeeReportWeekDto(
    /** Two-digit first day of month covered by the week, e.g. "01". */
    val fromDay: String,
    /** Two-digit last day of month covered by the week, e.g. "07". */
    val toDay: String,
    /** Weekly net total (formatted). */
    val totalDuration: String,
    /** Weekly gross total (formatted). */
    val grossDuration: String,
    /** Weekly time saved by AI (formatted). */
    val timeSavedByAI: String,
)

/** One matrix row: a Kost2 cost unit, a task, or the pseudo task ("******") for foreign, unreadable sheets. */
class MonthlyEmployeeReportRow(
    /** "kost2", "task" or "pseudoTask". */
    val type: String,
    val kost2Id: Long? = null,
    val taskId: Long? = null,
    /** Kost2 formatted number, or the task path (plain). */
    val label: String,
    val customer: String? = null,
    val project: String? = null,
    /** Kost2 description shown instead of customer/project when the cost unit has no project. */
    val description: String? = null,
    val kost2Art: String? = null,
    /** Formatted duration per week, in [MonthlyEmployeeReportData.weeks] order ("" for an empty cell). */
    val perWeek: List<String>,
    /** Monthly sum for this row (formatted). */
    val sum: String,
    /** Monthly time saved by AI for this row (formatted). */
    val aiTimeSavings: String,
)
