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

package org.projectforge.business.fibu

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.common.extensions.format
import org.projectforge.common.logging.LogDuration
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.cache.CacheListener
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.TreeSet
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

/**
 * Caches the order positions assigned to invoice positions.
 * Separate cache for incoming invoices due to performance reasons.
 *
 * @author Kai Reinhard
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

    /**
     * [AuftragsCache.refresh] uses this cache via [OrderPositionInfo] and vice versa, so neither cache can be
     * calculated correctly in a single pass: whichever runs first sees the other one empty. Therefore every refresh of
     * this cache triggers a re-calculation of [AuftragsCache] (see [auftragsCacheListener]), which in turn re-reads
     * this cache. This counter breaks that ping-pong after [MAX_COUPLED_REFRESH_ROUNDS] rounds, which is all it takes:
     * once both caches have been calculated on top of a filled counterpart, the result is stable.
     *
     * Reset whenever this cache is invalidated from the outside, so a later invalidation heals the order sums again
     * (before, a one-shot flag made this work on startup only, leaving OrderPositionInfo.invoicedSum at 0 forever if
     * AuftragsCache happened to be refreshed while this cache was still empty).
     */
    private val coupledRefreshRounds = AtomicInteger(0)

    /**
     * True, if [refresh] couldn't resolve all invoice positions to their orders, because [AuftragsCache] wasn't
     * available at that moment. [invoicePositionMapByAuftragId] is then incomplete and this cache must run again after
     * [AuftragsCache] is done.
     */
    @Volatile
    private var orderResolutionIncomplete = false

    /**
     * True while this cache is re-read only to complete [invoicePositionMapByAuftragId] (see
     * [orderResolutionIncomplete]). [AuftragsCache] is up-to-date in that case and must not be refreshed again.
     */
    @Volatile
    private var suppressCoupledRefresh = false

    @PostConstruct
    private fun init() {
        rechnungDao.register(rechnungListener)
        auftragsCache.register(auftragsCacheListener)
    }

    override fun setExpired() {
        // New data: the coupled refresh of AuftragsCache has to be done again.
        coupledRefreshRounds.set(0)
        super.setExpired()
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
        orderResolutionIncomplete = false
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
            if (auftrag == null) {
                // AuftragsCache doesn't know this position (yet): it is refreshing right now and calls this refresh
                // nested (see auftragsCacheListener), so its order map isn't available. invoicePositionMapByAuftragId
                // stays incomplete and has to be rebuilt afterwards:
                orderResolutionIncomplete = true
            }
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
        // The invoice positions of the orders are known now, so the order sums (OrderPositionInfo.invoicedSum) have to
        // be re-calculated on top of them:
        refreshAuftragsCacheIfNeeded()
    }

    /**
     * Re-calculates [AuftragsCache], because its order sums were calculated while this cache was still empty or
     * outdated. Limited to [MAX_COUPLED_REFRESH_ROUNDS] rounds, see [coupledRefreshRounds].
     */
    private fun refreshAuftragsCacheIfNeeded() {
        if (suppressCoupledRefresh) {
            log.debug { "AuftragsCache is up-to-date, only this cache had to be completed." }
            return
        }
        if (coupledRefreshRounds.incrementAndGet() > MAX_COUPLED_REFRESH_ROUNDS) {
            log.debug { "AuftragsCache already re-calculated with this invoice data, nothing to do." }
            return
        }
        log.info { "Forcing AuftragsCache to refresh, so the invoiced sums of the orders are calculated correctly." }
        auftragsCache.forceReload()
    }

    private val rechnungListener = object : BaseDOModifiedListener<RechnungDO> {
        /**
         * Set order as expired, if any invoice on this order was changed.
         */
        override fun afterInsertOrModify(obj: RechnungDO, operationType: OperationType) {
            setExpired()
        }
    }

    /**
     * [AuftragsCache] resolves its order positions to invoice positions via this cache, so this cache must be filled
     * whenever the order cache is (re-)calculated.
     */
    private val auftragsCacheListener = object : CacheListener {
        override fun onBeforeCacheRefresh() {
            if (!initialized) {
                log.info { "AuftragsCache is refreshing, but the invoices aren't read yet. Reading them first." }
                // Fills this cache. The coupled refresh of AuftragsCache triggered afterwards is a no-op while
                // AuftragsCache is refreshing anyway (guarded in AbstractCache.performRefresh).
                forceReload()
            }
        }

        override fun onAfterCacheRefresh() {
            if (orderResolutionIncomplete) {
                // Our order map was built without AuftragsCache (nested refresh). Now that the orders are known, it
                // can be completed. AuftragsCache is up-to-date, so this must not trigger it again:
                log.info { "AuftragsCache is available now, re-reading the invoices of the orders." }
                suppressCoupledRefresh = true
                try {
                    forceReload()
                } finally {
                    suppressCoupledRefresh = false
                }
            }
        }
    }

    companion object {
        /**
         * How often a refresh of this cache may trigger a re-calculation of [AuftragsCache] before the ping-pong
         * between both caches is stopped. One round is enough: [AuftragsCache] then calculates its order sums on top
         * of a completely filled invoice cache. See [coupledRefreshRounds].
         */
        private const val MAX_COUPLED_REFRESH_ROUNDS = 1
    }
}
