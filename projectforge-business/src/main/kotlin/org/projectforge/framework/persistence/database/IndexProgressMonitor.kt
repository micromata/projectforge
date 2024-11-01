/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.common.format
import org.projectforge.framework.persistence.jpa.PersistenceStats.Companion.formatMillis
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
        if (totalEntities > 2_000_000) {
            step = 1 // 5% steps for more than 5,000,000 entities
        } else if (totalEntities > 1_000_000) {
            step = 2 // 5% steps for more than 5,000,000 entities
        } else if (totalEntities > 500_000) {
            step = 5 // 5%
        } else if (totalEntities > 200_000) {
            step = 10 // 10%
        } else if (totalEntities > 100_000) {
            step = 20 // 20%
        } else if (totalEntities > 50_000) {
            step = 25 // 25%
        } else if (totalEntities > 10_000) {
            step = 50 // 50%
        } else {
            step = 100
        }
    }

    override fun indexingCompleted() {
        val duration = System.currentTimeMillis() - started
        val speed = totalEntities * 1000L / duration
        log.info { "${entityClass.simpleName}: Indexing completed (${formatMillis(duration)}, ${speed.format()}/s)." }
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
