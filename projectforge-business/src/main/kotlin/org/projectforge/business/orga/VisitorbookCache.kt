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

package org.projectforge.business.orga

import mu.KotlinLogging
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Caches employees with actual status and annual leave days for faster access.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
open class VisitorbookCache : AbstractCache() {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * The key is the visitorbook id (database pk). Must be synchronized, because isn't only read.
     */
    private var visitorbookMap = mutableMapOf<Long, VisitorbookInfo>()

    fun getVisitorbookInfo(id: Long?): VisitorbookInfo? {
        id ?: return null
        checkRefresh()
        synchronized(visitorbookMap) {
            return visitorbookMap[id]
        }
    }

    fun setExpired(id: Long?) {
        id ?: return // Should not happen.
        val info = synchronized(visitorbookMap) {
            visitorbookMap[id] ?: VisitorbookInfo().also { visitorbookMap[id] = it }
        }
        info.let { visitorbookInfo ->
            persistenceService.executeQuery(
                queryVisitEntries,
                VisitorbookEntryDO::class.java,
                Pair("visitorbookId", id)
            ).let { entries ->
                fillVisitorbookInfo(visitorbookInfo, entries)
            }
        }
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    public override fun refresh() {
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            log.info("Initializing VisitorbookCache...")
            // This method must not be synchronized because it works with a new copy of maps.
            val map = mutableMapOf<Long, VisitorbookInfo>()
            persistenceService.executeQuery(
                "from VisitorbookDO t where deleted=false",
                VisitorbookDO::class.java,
            ).forEach { visitorbook ->
                map[visitorbook.id!!] = VisitorbookInfo()
            }
            persistenceService.executeQuery(
                queryAllVisitEntries,
                VisitorbookEntryDO::class.java,
            ).groupBy { it.visitorbook?.id } // Group by visitorbook id
                .forEach { (visitorbookId, entries) ->
                    map[visitorbookId]?.let { visitorbookInfo ->
                        fillVisitorbookInfo(visitorbookInfo, entries) // Fill visitorbook info with entries
                    }
                }
            this.visitorbookMap = map
            log.info { "VisitorbookCache.refresh done. ${context.formatStats()}" }
        }
    }

    private fun fillVisitorbookInfo(visitorbookInfo: VisitorbookInfo, entries: List<VisitorbookEntryDO>) {
        val lastEntry = entries.first()
        visitorbookInfo.lastDateOfVisit = lastEntry.dateOfVisit
        visitorbookInfo.latestArrived = lastEntry.arrived
        visitorbookInfo.latestDeparted = lastEntry.departed
        visitorbookInfo.numberOfVisits = entries.size
    }

    companion object {
        const val queryVisitEntries =
            "SELECT t FROM VisitorbookEntryDO t WHERE t.deleted=false AND t.visitorbook.id=:visitorbookId ORDER BY t.dateOfVisit DESC"
        const val queryAllVisitEntries =
            "SELECT t FROM VisitorbookEntryDO t WHERE t.deleted=false ORDER BY t.visitorbook.id, t.dateOfVisit DESC"
    }
}
