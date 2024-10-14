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

import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

/**
 * Caches the order positions assigned to invoice positions.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
open class RechnungCache : AbstractCache() {
    @Autowired
    protected lateinit var persistenceService: PfPersistenceService

    /**
     * The key is the order id.
     */
    private var invoicePositionMapByAuftragId: Map<Long?, MutableSet<RechnungsPositionVO>>? = null

    /**
     * The key is the order position id.
     */
    private var invoicePositionMapByAuftragsPositionId: Map<Long?, MutableSet<RechnungsPositionVO>>? = null

    private var invoicePositionMapByRechnungId: Map<Long?, MutableSet<RechnungsPositionVO>>? = null

    fun getRechnungsPositionVOSetByAuftragId(auftragId: Long?): Set<RechnungsPositionVO>? {
        checkRefresh()
        return invoicePositionMapByAuftragId!![auftragId]
    }

    fun getRechnungsPositionVOSetByRechnungId(rechnungId: Long?): Set<RechnungsPositionVO>? {
        checkRefresh()
        return invoicePositionMapByRechnungId!![rechnungId]
    }

    fun getRechnungsPositionVOSetByAuftragsPositionId(auftragsPositionId: Long?): Set<RechnungsPositionVO>? {
        checkRefresh()
        return invoicePositionMapByAuftragsPositionId!![auftragsPositionId]
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing RechnungCache...")
        val saved = persistenceService.saveStatsState()
        // This method must not be synchronized because it works with a new copy of maps.
        val mapByAuftragId: MutableMap<Long?, MutableSet<RechnungsPositionVO>> = HashMap()
        val mapByAuftragsPositionId: MutableMap<Long?, MutableSet<RechnungsPositionVO>> = HashMap()
        val mapByRechnungsPositionMapByRechnungId: MutableMap<Long?, MutableSet<RechnungsPositionVO>> = HashMap()
        val list: List<RechnungsPositionDO> = persistenceService.executeQuery(
            "from RechnungsPositionDO t left join fetch t.auftragsPosition left join fetch t.auftragsPosition.auftrag where t.auftragsPosition is not null",
            RechnungsPositionDO::class.java,
        )
        for (pos in list) {
            val rechnung = pos.rechnung
            val auftragsPosition = pos.auftragsPosition
            if (auftragsPosition == null || auftragsPosition.auftrag == null) {
                log.error("Assigned order position expected: $pos")
                continue
            } else if (pos.deleted || rechnung == null || rechnung.deleted
                || rechnung.nummer == null
            ) {
                // Invoice position or invoice is deleted.
                continue
            }
            val auftrag = auftragsPosition.auftrag
            var setByAuftragId = mapByAuftragId[auftrag!!.id]
            if (setByAuftragId == null) {
                setByAuftragId = TreeSet()
                mapByAuftragId[auftrag.id] = setByAuftragId
            }
            var setByAuftragsPositionId = mapByAuftragsPositionId[auftragsPosition.id]
            if (setByAuftragsPositionId == null) {
                setByAuftragsPositionId = TreeSet()
                mapByAuftragsPositionId[auftragsPosition.id] = setByAuftragsPositionId
            }
            val vo = RechnungsPositionVO(pos)
            if (!setByAuftragId.contains(vo)) {
                setByAuftragId.add(vo)
            }
            if (!setByAuftragsPositionId.contains(vo)) {
                setByAuftragsPositionId.add(vo)
            }
            var positionen = mapByRechnungsPositionMapByRechnungId[rechnung.id]
            if (positionen == null) {
                positionen = TreeSet()
                mapByRechnungsPositionMapByRechnungId[rechnung.id] = positionen
            }
            positionen.add(vo)
        }
        this.invoicePositionMapByAuftragId = mapByAuftragId
        this.invoicePositionMapByAuftragsPositionId = mapByAuftragsPositionId
        this.invoicePositionMapByRechnungId = mapByRechnungsPositionMapByRechnungId
        log.info("Initializing of RechnungCache done. stats=${persistenceService.formatStats(saved)}")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RechnungCache::class.java)
    }
}
