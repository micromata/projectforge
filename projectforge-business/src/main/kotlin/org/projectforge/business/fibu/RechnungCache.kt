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

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Caches the order positions assigned to invoice positions.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
class RechnungCache : AbstractCache() {
    @Autowired
    private lateinit var auftragsRechnungCache: AuftragsRechnungCache

    @Autowired
    private lateinit var eingangsrechnungCache: EingangsrechnungCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    private var invoiceInfoMap = mutableMapOf<Long, RechnungInfo>()

    private var invoicePosInfoMap = mutableMapOf<Long, RechnungPosInfo>()

    @PostConstruct
    private fun postConstruct() {
        instance = this
        AbstractRechnungsStatistik.rechnungCache = this
        RechnungCalculator.rechnungCache = this
    }

    fun getRechnungsPositionVOSetByAuftragId(auftragId: Long?): Set<RechnungsPositionVO>? {
        return auftragsRechnungCache.getRechnungsPositionVOSetByAuftragId(auftragId)
    }

    fun getRechnungsPositionVOSetByRechnungId(rechnungId: Long?): Set<RechnungsPositionVO>? {
        return auftragsRechnungCache.getRechnungsPositionVOSetByRechnungId(rechnungId)
    }

    fun getRechnungsPositionVOSetByAuftragsPositionId(auftragsPositionId: Long?): Set<RechnungsPositionVO>? {
        return auftragsRechnungCache.getRechnungsPositionVOSetByAuftragsPositionId(auftragsPositionId)
    }

    fun update(invoice: RechnungDO) {
        synchronized(invoiceInfoMap) {
            invoiceInfoMap[invoice.id!!] = RechnungCalculator.calculate(invoice)
        }
        auftragsRechnungCache.setExpired() // Invalidate cache.
    }

    fun update(invoice: EingangsrechnungDO) {
        eingangsrechnungCache.update(invoice)
    }

    fun getOrCalculateRechnungInfo(rechnung: RechnungDO): RechnungInfo {
        checkRefresh()
        synchronized(invoiceInfoMap) {
            return invoiceInfoMap[rechnung.id!!] ?: RechnungCalculator.calculate(rechnung).also {
                invoiceInfoMap[rechnung.id!!] = it
            }
        }
    }

    fun getRechnungInfo(rechnungId: Long?): RechnungInfo? {
        rechnungId ?: return null
        checkRefresh()
        synchronized(invoiceInfoMap) {
            return invoiceInfoMap[rechnungId]
        }
    }

    fun getRechnungPosInfo(rechnungPosId: Long?): RechnungPosInfo? {
        rechnungPosId ?: return null
        checkRefresh()
        synchronized(invoicePosInfoMap) {
            return invoicePosInfoMap[rechnungPosId]
        }
    }

    fun getOrCalculateRechnungInfo(rechnung: EingangsrechnungDO): RechnungInfo {
        return eingangsrechnungCache.getOrCalculateRechnungInfo(rechnung)
    }

    fun getEingangsrechnungInfo(rechnungId: Long?): RechnungInfo? {
        return eingangsrechnungCache.getRechnungInfo(rechnungId)
    }

    /**
     * Autodetect Rechnung/Eingangsrechnung.
     */
    fun getRechnungInfo(rechnung: AbstractRechnungDO?): RechnungInfo? {
        val id = rechnung?.id ?: return null
        return if (rechnung is RechnungDO) {
            synchronized(invoiceInfoMap) {
                invoiceInfoMap[id]
            }
        } else {
            eingangsrechnungCache.getRechnungInfo(id)
        }
    }

    override fun setExpired() {
        super.setExpired()
        auftragsRechnungCache.setExpired()
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing RechnungCache...")
        val saved = persistenceService.saveStatsState()
        try {
            PfPersistenceService.startCallsStatsRecording()
            // This method must not be synchronized because it works with new copies of maps.
            log.info("Getting all invoices (RechnungDO)...")
            val nInvoiceInfoMap = mutableMapOf<Long, RechnungInfo>()
            val nInvoicePosInfoMap = mutableMapOf<Long, RechnungPosInfo>()
            persistenceService.executeQuery(
                "FROM RechnungDO t left join fetch t.positionen p left join fetch t.positionen.kostZuweisungen",
                RechnungDO::class.java,
            ).forEach { rechnung ->
                nInvoiceInfoMap[rechnung.id!!] = RechnungCalculator.calculate(rechnung).also { info ->
                    info.positions?.forEach { pos ->
                        nInvoicePosInfoMap[pos.id!!] = pos
                    }

                }
            }
            this.invoiceInfoMap = nInvoiceInfoMap
            this.invoicePosInfoMap = nInvoicePosInfoMap
            log.info(
                "Initializing of RechnungCache done. stats=${persistenceService.formatStats(saved)}, callsStats=${
                    PfPersistenceService.showCallsStatsRecording()
                }"
            )
        } finally {
            PfPersistenceService.stopCallsStatsRecording()
        }
    }

    companion object {
        lateinit var instance: RechnungCache
            private set
    }
}
