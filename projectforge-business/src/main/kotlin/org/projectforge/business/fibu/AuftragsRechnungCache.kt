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

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.common.extensions.format
import org.projectforge.common.logging.LogDuration
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
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
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var rechnungJdbcService: RechnungJdbcService

    /**
     * The key is the order id, value is the id of the invoice position.
     */
    private var invoicePositionMapByAuftragId = mapOf<Long, MutableSet<Long>>()

    /**
     * The key is the order position id.
     */
    private var invoicePositionMapByAuftragsPositionId = mapOf<Long, MutableSet<RechnungPosInfo>>()

    private var invoicePositionMapByRechnungId = mapOf<Long, MutableSet<RechnungPosInfo>>()

    @PostConstruct
    private fun init() {
        rechnungDao.register(rechnungListener)
    }

    /**
     * Returns the invoice positions assigned to the order.
     * The list is sorted by invoice number and position number.
     * @param auftragId The order id.
     * @return The list of invoice positions or null if the order id is null.
     */
    fun getRechnungsPosInfoByAuftragId(auftragId: Long?): List<RechnungPosInfo>? {
        auftragId ?: return null
        checkRefresh()
        val posIds = invoicePositionMapByAuftragId[auftragId]
        return posIds?.map { rechnungCache.getRechnungPosInfo(it) }?.filterNotNull()
            ?.sortedWith(compareBy<RechnungPosInfo> { it.rechnungInfo?.nummer }.thenBy { it.number })
    }

    fun getRechnungsPosInfosByAuftragsPositionId(
        auftragsPositionId: Long?,
    ): Set<RechnungPosInfo>? {
        auftragsPositionId ?: return null
        checkRefresh()
        return invoicePositionMapByAuftragsPositionId[auftragsPositionId]
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing AuftragsRechnungCache...")
        val duration = LogDuration()
        val list = rechnungJdbcService.selectRechnungsPositionenWithAuftragPosition()
        // This method must not be synchronized because it works with new copies of maps.
        val mapByAuftragId = mutableMapOf<Long, TreeSet<Long>>()
        val mapByAuftragsPositionId = mutableMapOf<Long, TreeSet<RechnungPosInfo>>()
        val mapByRechnungsPositionMapByRechnungId = mutableMapOf<Long, TreeSet<RechnungPosInfo>>()
        log.info("Analyzing orders in invoices (RechnungsPositionDO.AuftragsPosition, ${list.size.format()} entries)...")
        for (pos in list) {
            val rechnungInfo = rechnungCache.getRechnungInfo(pos.rechnung?.id)
            val auftragsPositionId = pos.auftragsPosition?.id
            if (auftragsPositionId == null) {
                log.error("Assigned order position expected: $pos")
                continue
            }
            if (pos.deleted || rechnungInfo == null || rechnungInfo.deleted || rechnungInfo.nummer == null) {
                // Invoice position or invoice is deleted.
                continue
            }
            val auftrag = auftragsCache.getOrderInfoByPositionId(auftragsPositionId)
            //val auftrag = auftragsPosition.auftrag
            val rechnungPosInfo = rechnungCache.ensureRechnungPosInfo(pos)
            pos.info = rechnungPosInfo
            auftrag?.id?.let { auftragId ->
                pos.id?.let { mapByAuftragId.getOrPut(auftragId) { TreeSet() }.add(it) }
            }
            mapByAuftragsPositionId
                .getOrPut(auftragsPositionId) { TreeSet() }
                .add(rechnungPosInfo)
            mapByRechnungsPositionMapByRechnungId.getOrPut(rechnungInfo.id) { TreeSet() }
                .add(rechnungPosInfo)
        }
        this.invoicePositionMapByAuftragId = mapByAuftragId
        this.invoicePositionMapByAuftragsPositionId = mapByAuftragsPositionId
        this.invoicePositionMapByRechnungId = mapByRechnungsPositionMapByRechnungId
        log.info { "Initializing of AuftragsRechnungCache done: ${duration.toSeconds()}." }
    }

    private val rechnungListener = object : BaseDOModifiedListener<RechnungDO> {
        /**
         * Set order as expired, if any invoice on this order was changed.
         */
        override fun afterInsertOrModify(obj: RechnungDO, operationType: OperationType) {
            setExpired()
        }
    }
}
