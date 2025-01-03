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

package org.projectforge.business.fibu.orderbooksnapshots

import org.projectforge.common.extensions.format
import org.projectforge.common.extensions.formatBytes
import org.projectforge.common.extensions.isoString
import org.projectforge.jobs.AbstractJob
import org.projectforge.jobs.JobExecutionContext
import java.util.Date

class OrderbookSnapshotsSanityCheck(val orderbookSnapshotsService: OrderbookSnapshotsService) :
    AbstractJob("Checks the recent order book' snapshots.") {
    override fun execute(jobContext: JobExecutionContext) {
        val entries = orderbookSnapshotsService.selectMetas()
        val fullSnapshots = entries.count { it.incrementalBasedOn == null }
        val incrementalSnapshots = entries.count { it.incrementalBasedOn != null }
        val totalSize = entries.sumOf { it.size ?: 0 }
        jobContext.addMessage("Found ${entries.size} order book snapshots: total-size=${totalSize.formatBytes()}, full=${fullSnapshots.format()}, incremental=${incrementalSnapshots.format()}.")
        // Test all last 10 snapshots:
        entries.take(10).forEach {
            val date = it.date
            if (date == null) {
                jobContext.addError("Date is null for entry: ${it}")
                return@forEach
            }
            try {
                val orders = orderbookSnapshotsService.readSnapshot(date)
                val lastUpdate = orders?.maxOf { it.lastUpdate ?: Date(0L) }
                val highestOrderNumber = orders?.maxOf { it.nummer ?: -1 }
                jobContext.addMessage("Snapshot for date $date (${it.size.formatBytes()}) is readable: ${orders?.size?.format()} orders, highest order number=${highestOrderNumber.format()} lastUpdate=${lastUpdate.isoString()}.")
            } catch (e: Exception) {
                jobContext.addError("Error reading snapshot for date $date: $e")
            }
        }
    }
}
