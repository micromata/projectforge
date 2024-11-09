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
import org.projectforge.common.logging.LogDuration
import org.projectforge.framework.cache.AbstractCache
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

/**
 * Caches the order positions assigned to invoice positions.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractRechnungCache(
    val entityClass: KClass<out AbstractRechnungDO>,
    protected val rechnungJdbcService: RechnungJdbcService,
) : AbstractCache() {
    private val entityName = entityClass.simpleName

    protected var invoiceInfoMap = mutableMapOf<Long, RechnungInfo>()

    protected var invoicePosInfoMap = mutableMapOf<Long, RechnungPosInfo>()

    open fun update(invoice: AbstractRechnungDO) {
        synchronized(invoiceInfoMap) {
            invoiceInfoMap[invoice.id!!] = RechnungCalculator.calculate(invoice)
        }
    }

    /**
     * Ensures that the RechnungInfo is calculated and stored in the cache.
     * If calculated the positions and kostZuweisungen will be fetched, if not existing (rechnung must be attached for lazy loading).
     * @return The RechnungInfo (from cache or calculated).
     */
    fun ensureRechnungInfo(rechnung: AbstractRechnungDO): RechnungInfo {
        val info = getRechnungInfo(rechnung.id)
        if (info != null) {
            rechnung.info = info
            return info
        }
        return RechnungCalculator.calculate(rechnung).also {
            synchronized(invoiceInfoMap) {
                invoiceInfoMap[rechnung.id!!] = it
            }
            // rechnung.info = it // Set by RechnungsCalculator.
        }
    }

    fun getRechnungInfo(rechnungId: Long?): RechnungInfo? {
        rechnungId ?: return null
        checkRefresh()
        synchronized(invoiceInfoMap) {
            return invoiceInfoMap[rechnungId]
        }
    }

    open fun getRechnungInfo(rechnung: AbstractRechnungDO?): RechnungInfo? {
        val rechnungId = rechnung?.id ?: return null
        checkRefresh()
        synchronized(invoiceInfoMap) {
            return invoiceInfoMap[rechnungId]
        }
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
            synchronized(invoiceInfoMap) {
                invoicePosInfoMap[pos.id!!] = it
            }
            // rechnung.info = it // Set by RechnungsCalculator.
        }
    }

    fun getRechnungPosInfo(rechnungPosId: Long?): RechnungPosInfo? {
        rechnungPosId ?: return null
        checkRefresh()
        synchronized(invoicePosInfoMap) {
            return invoicePosInfoMap[rechnungPosId]
        }
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing cache (${entityName})...")
        val duration = LogDuration()
        // This method must not be synchronized because it works with new copies of maps.
        log.info("Getting all invoices ($entityName)...")
        val nInvoiceInfoMap = mutableMapOf<Long, RechnungInfo>()
        val nInvoicePosInfoMap = mutableMapOf<Long, RechnungPosInfo>()
        rechnungJdbcService.selectRechnungInfos(entityClass).forEach { rechnungInfo ->
            nInvoiceInfoMap[rechnungInfo.id] = rechnungInfo.also { info ->
                info.positions?.forEach { pos ->
                    nInvoicePosInfoMap[pos.id!!] = pos
                }
            }
        }
        this.invoiceInfoMap = nInvoiceInfoMap
        this.invoicePosInfoMap = nInvoicePosInfoMap
        log.info { "Initializing cache (${entityName}) done: ${duration.toSeconds()}." }
    }
}
