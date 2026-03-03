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

package org.projectforge.business.fibu.orderbooksnapshots

import java.time.LocalDate

/**
 * Scripting proxy for OrderbookSnapshotsService.
 * Provides read-only access to order book snapshot data for Groovy/Kotlin scripts.
 * Available in scripts as: orderbookSnapshotService
 *
 * Since [OrderbookSnapshotDO] is internal, this proxy exposes snapshot data via
 * the public [SnapshotMeta] data class.
 */
class OrderbookSnapshotScriptingService(private val __service: OrderbookSnapshotsService) {

    /**
     * Public data class for snapshot metadata, usable in scripts.
     */
    data class SnapshotMeta(
        val date: LocalDate?,
        val incrementalBasedOn: LocalDate?,
        val size: Int?,
    ) {
        val incremental: Boolean
            get() = incrementalBasedOn != null
    }

    /**
     * Returns metadata for all snapshots, optionally filtered to full backups only.
     *
     * @param onlyFullBackups If true, returns only full backups (incrementalBasedOn is null).
     * @return List of [SnapshotMeta] sorted by date descending.
     */
    fun selectMetas(onlyFullBackups: Boolean = false): List<SnapshotMeta> {
        return __service.selectMetas(onlyFullBackups).map { snapshot ->
            SnapshotMeta(
                date = snapshot.date,
                incrementalBasedOn = snapshot.incrementalBasedOn,
                size = snapshot.size,
            )
        }
    }

    /**
     * Returns the full serialized snapshot (gzipped JSON bytes) for a given date.
     * This loads the full entry from the database including the BLOB.
     *
     * @param date The date of the snapshot.
     * @return The gzipped byte array of the serialized order book, or null if not found.
     */
    fun getSerializedOrderBook(date: LocalDate): ByteArray? {
        return __service.getSerializedOrderBook(date)
    }
}
