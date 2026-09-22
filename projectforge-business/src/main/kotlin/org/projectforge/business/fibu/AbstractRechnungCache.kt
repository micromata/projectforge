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

import mu.KotlinLogging
import org.projectforge.common.logging.LogDuration
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

/**
 * Caches the order positions assigned to invoice positions.
 *
 * @author Kai Reinhard
 */
abstract class AbstractRechnungCache(
    val entityClass: KClass<out AbstractRechnungDO>,
    protected val rechnungJdbcService: RechnungJdbcService,
) : AbstractCache() {
    private val entityName = entityClass.simpleName

    protected var invoiceInfoMap = ConcurrentHashMap<Long, RechnungInfo>()

    protected var invoicePosInfoMap = ConcurrentHashMap<Long, RechnungPosInfo>()

    open fun update(invoice: AbstractRechnungDO) {
        invoiceInfoMap[invoice.id!!] = RechnungCalculator.calculate(invoice)
    }

    /**
     * Ensures that the RechnungInfo is calculated and stored in the cache.
     * If calculated the positions and kostZuweisungen will be fetched, if not existing (rechnung must be attached for lazy loading).
     * @return The RechnungInfo (from cache or calculated).
     */
    fun ensureRechnungInfo(rechnung: AbstractRechnungDO): RechnungInfo {
        // Wait only for the initial fill (the startup window); afterwards serve the current cache data as is, even
        // while a refresh is running. This is what avoids the SQL storm: this method runs once per row while building
        // an invoice list (RechnungDao.afterLoad). On a not-yet-filled cache, every miss would fall through to
        // RechnungCalculator.calculate below, lazily loading that invoice's positions and cost assignments - an N+1
        // flood. Waiting once for the single bulk refresh instead is far cheaper.
        //
        // But NOT while this thread is inside a write transaction: the initial fill runs refresh() ->
        // RechnungJdbcService on a *second* DB connection, whose SELECT on t_fibu_rechnung would block on the rows
        // the open transaction has locked - and that transaction can never commit, because the same thread is blocked
        // here waiting for the fill. A single-thread self-deadlock across two connections (it froze
        // RechnungDaoTest.testNextNumber, and would freeze the first invoice load after a cache expiry inside a write
        // transaction in production until the stuck-refresh watchdog interrupts it). Inside a transaction we skip the
        // wait and fall through to the per-entity calculation below, which lazy-loads on the *same* connection - safe.
        // A list load inside a write transaction is unusual, so the N+1 trade-off for that rare case is acceptable.
        //
        // NB: this is the JdbcTemplate variant of the same self-deadlock that
        // [org.projectforge.framework.cache.AbstractCache.runReadOnlyForCacheMaintenance] solves for caches whose
        // second connection is a persistence context (e.g. VacationCache). There the fix reuses the transaction's
        // connection; here the second connection is RechnungJdbcService's own JdbcTemplate connection, which cannot
        // be routed onto the transaction, so we must avoid triggering the refresh at all while a transaction is active.
        if (PfPersistenceService.instance.isTransactionActive()) {
            log.debug {
                "ensureRechnungInfo ($entityName): inside a write transaction, skipping the cache initial-fill wait " +
                        "to avoid a cross-connection self-deadlock; serving cached data or calculating per entity."
            }
        } else {
            waitForInitialization()
        }
        rechnung.id?.let { id ->
            invoiceInfoMap[id]?.let {
                rechnung.info = it
                return it
            }
        }
        // Genuine miss on a filled cache (e.g. an invoice not yet persisted, or created after the last refresh):
        // calculate it from the attached entity - a single invoice, not a list-wide N+1.
        log.warn {
            "ensureRechnungInfo cache MISS ($entityName): id=${rechnung.id}, deleted=${rechnung.deleted}, " +
                    "initialized=$initialized, cacheSize=${invoiceInfoMap.size} - calculating (lazy-loads positions " +
                    "and kostZuweisungen for this invoice)."
        }
        return RechnungCalculator.calculate(rechnung).also {
            rechnung.id?.let { id -> invoiceInfoMap[id] = it }
            // rechnung.info = it // Set by RechnungsCalculator.
        }
    }

    fun getRechnungInfo(rechnungId: Long?): RechnungInfo? {
        rechnungId ?: return null
        checkRefresh()
        return invoiceInfoMap[rechnungId]
    }

    open fun getRechnungInfo(rechnung: AbstractRechnungDO?): RechnungInfo? {
        val rechnungId = rechnung?.id ?: return null
        checkRefresh()
        return invoiceInfoMap[rechnungId]
    }

    /**
     * Ensures that the RechnungPosInfo is calculated and stored in the cache.
     * If calculated the kostZuweisungen will be fetched, if not existing (position must be attached for lazy loading).
     * @return The RechnungPosInfo (from cache or calculated).
     */
    fun ensureRechnungPosInfo(pos: AbstractRechnungsPositionDO): RechnungPosInfo {
        var posInfo = getRechnungPosInfo(pos.id)
        if (posInfo != null) {
            pos.info = posInfo
            return posInfo
        }
        val info = getRechnungInfo(pos.rechnungId)
        posInfo = RechnungPosInfo(info, pos)
        return RechnungCalculator.calculate(posInfo, pos).also {
            pos.id?.let { posId ->
                // pos.id is null for cloned invoices.
                invoicePosInfoMap[posId] = it
            }
            // rechnung.info = it // Set by RechnungsCalculator.
        }
    }

    fun getRechnungPosInfo(rechnungPosId: Long?): RechnungPosInfo? {
        rechnungPosId ?: return null
        checkRefresh()
        return invoicePosInfoMap[rechnungPosId]
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing cache (${entityName})...")
        val duration = LogDuration()
        // This method must not be synchronized because it works with new copies of maps.
        log.info("Getting all invoices ($entityName)...")
        val nInvoiceInfoMap = ConcurrentHashMap<Long, RechnungInfo>()
        val nInvoicePosInfoMap = ConcurrentHashMap<Long, RechnungPosInfo>()
        rechnungJdbcService.selectRechnungInfos(entityClass).forEach { rechnungInfo ->
            nInvoiceInfoMap[rechnungInfo.id] = rechnungInfo.also { info ->
                info.positions?.forEach { pos ->
                    val posId = pos.id
                    if (posId != null) {
                        nInvoicePosInfoMap[posId] = pos
                    } else {
                        log.error { "Position without id found in invoice: ${info.id}." }
                    }
                }
            }
        }
        this.invoiceInfoMap = nInvoiceInfoMap
        this.invoicePosInfoMap = nInvoicePosInfoMap
        log.info { "Initializing cache (${entityName}) done: ${duration.toSeconds()}." }
    }
}
