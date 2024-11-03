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

package org.projectforge.business.fibu

import mu.KotlinLogging
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.TreeSet

private val log = KotlinLogging.logger {}

/**
 * Caches the order positions assigned to invoice positions.
 * Separate cache for incoming invoices due to performance reasons.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
class AuftragsRechnungCache : AbstractCache() {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    /**
     * The key is the order id.
     */
    private var invoicePositionMapByAuftragId = mapOf<Long, MutableSet<RechnungPosInfo>>()

    /**
     * The key is the order position id.
     */
    private var invoicePositionMapByAuftragsPositionId = mapOf<Long, MutableSet<RechnungPosInfo>>()

    private var invoicePositionMapByRechnungId = mapOf<Long, MutableSet<RechnungPosInfo>>()

    fun getRechnungsPosInfoByAuftragId(auftragId: Long?): Set<RechnungPosInfo>? {
        auftragId ?: return null
        checkRefresh()
        return invoicePositionMapByAuftragId[auftragId]
    }

    fun getRechnungsPosInfosByAuftragsPositionId(
        auftragsPositionId: Long?,
        checkRefresh: Boolean = true,
    ): Set<RechnungPosInfo>? {
        auftragsPositionId ?: return null
        if (checkRefresh) {
            checkRefresh()
        }
        return invoicePositionMapByAuftragsPositionId[auftragsPositionId]
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing AuftragsRechnungCache...")
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            // This method must not be synchronized because it works with new copies of maps.
            val mapByAuftragId = mutableMapOf<Long, TreeSet<RechnungPosInfo>>()
            val mapByAuftragsPositionId = mutableMapOf<Long, TreeSet<RechnungPosInfo>>()
            val mapByRechnungsPositionMapByRechnungId = mutableMapOf<Long, TreeSet<RechnungPosInfo>>()
            log.info("Analyzing orders in invoices (RechnungsPositionDO.AuftragsPosition)...")
            val list: List<RechnungsPositionDO> = context.executeQuery(
                "from RechnungsPositionDO t left join fetch t.auftragsPosition left join fetch t.auftragsPosition.auftrag where t.auftragsPosition is not null",
                RechnungsPositionDO::class.java,
            )
            for (pos in list) {
                val rechnung = pos.rechnung
                val auftragsPosition = pos.auftragsPosition
                if (auftragsPosition?.auftrag == null) {
                    log.error("Assigned order position expected: $pos")
                    continue
                } else if (pos.deleted || rechnung == null || rechnung.deleted || rechnung.nummer == null) {
                    // Invoice position or invoice is deleted.
                    continue
                }
                val auftrag = auftragsPosition.auftrag
                val rechnungPosInfo = rechnungCache.ensureRechnungPosInfo(pos)
                pos.info = rechnungPosInfo
                mapByAuftragId.getOrPut(auftrag!!.id ?: 0) { TreeSet() }
                    .add(rechnungPosInfo)
                mapByAuftragsPositionId
                    .getOrPut(auftragsPosition.id ?: 0) { TreeSet() }
                    .add(rechnungPosInfo)
                mapByRechnungsPositionMapByRechnungId.getOrPut(rechnung.id ?: 0) { TreeSet() }
                    .add(rechnungPosInfo)
            }
            this.invoicePositionMapByAuftragId = mapByAuftragId
            this.invoicePositionMapByAuftragsPositionId = mapByAuftragsPositionId
            this.invoicePositionMapByRechnungId = mapByRechnungsPositionMapByRechnungId
            log.info { "Initializing of RechnungCache done. ${context.formatStats()}" }
        }
    }
}
