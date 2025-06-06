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

package org.projectforge.framework.persistence.database

import mu.KotlinLogging
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor
import org.projectforge.common.extensions.format
import org.projectforge.common.extensions.formatMillis
import org.projectforge.framework.utils.NumberFormatter

private val log = KotlinLogging.logger {}

class IndexProgressMonitor(val entityClass: Class<*>) : MassIndexingMonitor {
    private var totalEntities: Long = 0
    private var indexedEntities: Long = 0
    private var lastReportedProgress = 0
    private var step = 50 // 50% steps at default
    private var started = System.currentTimeMillis()

    init {
        log.info { "${entityClass.simpleName}: Starting indexing..." }
    }

    override fun documentsAdded(increment: Long) {
        synchronized(this) {
            indexedEntities += increment
        }
        printProgress()
    }

    override fun entitiesLoaded(increment: Long) {
        // Diese Methode wird aufgerufen, wenn Entitäten aus der Datenbank geladen werden
    }

    override fun addToTotalCount(count: Long) {
        synchronized(this) {
            totalEntities += count
        }
        step = when { // 2.500 Entities per seconde:
            totalEntities > 5_000_000 -> 1    // 1% steps
            totalEntities > 2_000_000 -> 2    // 2% steps
            totalEntities > 1_000_000 -> 5    // 5% steps
            totalEntities > 500_000 -> 10     // 10% steps
            totalEntities > 250_000 -> 20     // 20% steps
            totalEntities > 200_000 -> 25     // 25% steps
            totalEntities > 100_000 -> 50     // 50% steps
            else -> 100                       // 100% steps
        }
    }

    override fun indexingCompleted() {
        val duration = System.currentTimeMillis() - started
        val speed = totalEntities * 1000L / duration
        log.info { "${entityClass.simpleName}: Indexing completed (${duration.formatMillis()}, ${speed.format()}/s)." }
    }

    override fun documentsBuilt(increment: Long) {
        // Diese Methode wird aufgerufen, wenn Dokumente für die Indizierung erstellt werden
    }

    private fun printProgress() {
        if (totalEntities > 0) {
            // Berechne den aktuellen Fortschritt als ganzzahligen Wert
            val progress = (indexedEntities * 100 / totalEntities / step).toInt() // Fortschritt in step-%-Schritten

            // Logge nur, wenn sich der Fortschritt geändert hat
            if (progress > lastReportedProgress) {
                lastReportedProgress = progress
                log.info(
                    "${entityClass.simpleName}: Indexing ${progress * step}% (${
                        NumberFormatter.format(
                            indexedEntities
                        )
                    }/${NumberFormatter.format(totalEntities)})..."
                )
            }
        }
    }
}
