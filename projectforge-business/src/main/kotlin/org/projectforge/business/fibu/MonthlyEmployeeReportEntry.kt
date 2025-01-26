/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.PfCaches.Companion.instance
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.business.timesheet.AITimeSavings
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.common.extensions.isZeroOrNull
import java.io.Serializable
import java.math.BigDecimal

/**
 * Repräsentiert einen Eintrag innerhalb eines Wochenberichts eines Mitarbeiters zu einem Kostenträger (Anzahl Stunden).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MonthlyEmployeeReportEntry : Serializable {
    /**
     * Only given, if task is not given and vice versa.
     */
    var kost2: Kost2DO? = null
        private set

    /**
     * Only given, if kost2 is not given and vice versa.
     */
    var task: TaskDO? = null
        private set

    var millis: Long = 0
        private set

    var timeimeSavedByAIMillis: Long = 0
        private set

    constructor(kost2: Kost2DO?) {
        this.kost2 = kost2
    }

    constructor(task: TaskDO?) {
        this.task = task
    }

    fun addMillis(entry: MonthlyEmployeeReportEntry) {
        this.millis += entry.workFractionMillis
        this.timeimeSavedByAIMillis += entry.timeimeSavedByAIMillis
    }

    fun addMillis(timesheetDO: TimesheetDO, duration: Long) {
        this.millis += duration
        this.timeimeSavedByAIMillis += AITimeSavings.getTimeSavedByAIMs(timesheetDO, duration)
    }

    /**
     * If this entry has a kost2 with a working time fraction set or a kost2art with a working time fraction set then the fraction of millis
     * will be returned.
     */
    val workFractionMillis: Long
        get() = workFraction.multiply(millis.toBigDecimal()).toLong()

    val workFraction: BigDecimal
        get() {
            val useKost2 = instance.getKost2IfNotInitialized(kost2) ?: return BigDecimal.ONE
            useKost2.workFraction?.let {
                return it
            }
            return instance.getKost2ArtIfNotInitialized(useKost2.kost2Art)?.workFraction ?: BigDecimal.ONE
        }

    val formattedDuration: String
        get() = if (workFraction.isZeroOrNull()) {
            "(${MonthlyEmployeeReport.getFormattedDuration(millis)})" // Not working time.
        } else {
            MonthlyEmployeeReport.getFormattedDuration(millis)
        }

    val getFormattedTimeSavedByAI: String
        get() = MonthlyEmployeeReport.getFormattedDuration(timeimeSavedByAIMillis)


    companion object {
        private const val serialVersionUID = 7290000602224467755L
    }
}
