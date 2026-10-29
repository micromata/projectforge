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

package org.projectforge.business.fibu.kost.reallocation

import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.timesheet.TimesheetDO
import java.math.BigDecimal

/**
 * The result of the calculation of a [Kost2ReallocationDO]: the new kost2 shares of the records and the time sheets
 * to move. It is shown as preview and persisted by apply (re-apply: the shares only). A plan with [errors] is shown,
 * but refused by apply and re-apply.
 */
class Kost2ReallocationPlan(
    val run: Kost2ReallocationDO,
    val records: List<RecordChange>,
    val timesheets: List<TimesheetMove>,
    val warnings: List<String>,
    val errors: List<String> = emptyList(),
) {
    /**
     * A record with its effective amounts by kost2 id before ([oldAmounts]) and after the run ([newAmounts]). [salary]
     * is true for a salary record (split by the hours of the moved time sheets).
     */
    class RecordChange(
        val record: BuchungssatzDO,
        val oldAmounts: Map<Long, BigDecimal>,
        val newAmounts: Map<Long, BigDecimal>,
        val salary: Boolean,
    )

    /**
     * A time sheet moved from [oldKost2Id] (and [oldTaskId]) to [newKost2Id] (and [newTaskId]). [millis] is its
     * attendance duration counted by the salary export in [year]/[month]. [timesheet] is null for a re-apply (the
     * time sheet was moved already, the move is taken from the log). [manual] for a time sheet selected by hand.
     */
    class TimesheetMove(
        val timesheet: TimesheetDO?,
        val timesheetId: Long,
        val userId: Long,
        val year: Int,
        val month: Int,
        val oldTaskId: Long,
        val newTaskId: Long,
        val oldKost2Id: Long,
        val newKost2Id: Long,
        val millis: Long,
        val manual: Boolean,
    )

    /** The sums of the effective amounts of the changed records by kost2 id, before and after the run. */
    val sumsByKost2: Map<Long, Pair<BigDecimal, BigDecimal>>
        get() {
            val result = linkedMapOf<Long, Pair<BigDecimal, BigDecimal>>()
            records.forEach { change ->
                change.oldAmounts.forEach { (kost2Id, amount) ->
                    val sums = result[kost2Id] ?: (BigDecimal.ZERO to BigDecimal.ZERO)
                    result[kost2Id] = (sums.first + amount) to sums.second
                }
                change.newAmounts.forEach { (kost2Id, amount) ->
                    val sums = result[kost2Id] ?: (BigDecimal.ZERO to BigDecimal.ZERO)
                    result[kost2Id] = sums.first to (sums.second + amount)
                }
            }
            return result
        }
}
