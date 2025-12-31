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

package org.projectforge.business.fibu

import org.projectforge.business.fibu.MonthlyEmployeeReport.Companion.createPseudoTask
import org.projectforge.business.fibu.MonthlyEmployeeReport.Companion.getFormattedDuration
import org.projectforge.business.timesheet.AITimeSavings
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.common.StringHelper
import org.projectforge.framework.time.PFDateTime
import java.io.Serializable

/**
 * Repräsentiert einen Wochenbericht eines Mitarbeiters. Diese Wochenberichte sind dem MonthlyEmployeeReport zugeordnet.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MonthlyEmployeeReportWeek(date: PFDateTime) : Serializable {
    private var fromDate: PFDateTime

    private var toDate: PFDateTime

    /**
     * Summe aller Stunden der Woche in Millis.
     */
    var totalDuration: Long = 0
        private set

    var totalTimeSavedByAI: Long = 0
        private set

    private var totalGrossDuration: Long = 0

    /**
     * Key is kost2 id.
     */
    val kost2Entries = mutableMapOf<Long, MonthlyEmployeeReportEntry>()

    /**
     * Key is task id.
     */
    val taskEntries = mutableMapOf<Long, MonthlyEmployeeReportEntry>()

    /**
     * FromDate will be set to the begin of week but not before first day of month.
     * ToDate will be set to end of week but not after the last day of month.
     */
    init {
        this.fromDate = date.beginOfWeek
        if (fromDate.month != date.month) {
            this.fromDate = date.beginOfMonth
        }
        this.toDate = fromDate.endOfWeek
        if (toDate.month != fromDate.month) {
            this.toDate = fromDate.endOfMonth
        }
    }

    /**
     * Start time of sheet must be fromDate or later and before toDate.
     *
     * @param sheet
     */
    fun matchWeek(sheet: TimesheetDO): Boolean {
        return !sheet.startTime!!.before(fromDate.utilDate) && sheet.startTime!!.before(toDate.utilDate)
    }

    fun addEntry(timesheet: TimesheetDO, hasSelectAccess: Boolean) {
        if (!matchWeek(timesheet)) {
            throw RuntimeException("Oups, given time sheet is not inside the week represented by this week object.")
        }
        var entry: MonthlyEmployeeReportEntry? = null
        if (!hasSelectAccess) {
            entry = taskEntries[MonthlyEmployeeReport.MAGIC_PSEUDO_TASK_ID] // -42 represents timesheets without access.
            if (entry == null) {
                entry = MonthlyEmployeeReportEntry(createPseudoTask())
                taskEntries[MonthlyEmployeeReport.MAGIC_PSEUDO_TASK_ID] = entry
            }
        } else if (timesheet.kost2Id != null) {
            entry = kost2Entries[timesheet.kost2Id]
            if (entry == null) {
                entry = MonthlyEmployeeReportEntry(timesheet.kost2)
                kost2Entries[timesheet.kost2Id!!] = entry
            }
        } else {
            timesheet.taskId?.let { taskId ->
                entry = taskEntries[taskId]
                if (entry == null) {
                    entry = MonthlyEmployeeReportEntry(timesheet.task)
                    taskEntries[taskId] = entry!!
                }
            }
        }
        val duration = timesheet.duration
        entry?.addMillis(timesheet, duration)
        totalDuration += timesheet.workFractionDuration
        if (timesheet.workFractionDuration > 0) {
            // Don't add time sheets with zero working time fraction.
            totalGrossDuration += duration
        }
        totalTimeSavedByAI += AITimeSavings.getTimeSavedByAIMillis(timesheet, duration)
    }

    val formattedFromDayOfMonth: String
        get() = StringHelper.format2DigitNumber(fromDate.dayOfMonth)

    val formattedToDayOfMonth: String
        get() = StringHelper.format2DigitNumber(toDate.dayOfMonth)

    val formattedTotalDuration: String
        get() = getFormattedDuration(totalDuration)

    val formattedGrossDuration: String
        get() = getFormattedDuration(totalGrossDuration)

    val formattedTotalTimeSavedByAI: String
        get() = getFormattedDuration(totalTimeSavedByAI)

    companion object {
        private const val serialVersionUID = 6075755848054540114L
    }
}
