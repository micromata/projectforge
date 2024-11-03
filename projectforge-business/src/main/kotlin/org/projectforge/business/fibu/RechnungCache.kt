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
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Caches the order positions assigned to invoice positions.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
class RechnungCache(persistenceService: PfPersistenceService) :
    AbstractRechnungCache(RechnungDO::class.java, persistenceService) {
    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var auftragsRechnungCache: AuftragsRechnungCache

    @Autowired
    private lateinit var eingangsrechnungCache: EingangsrechnungCache

    @PostConstruct
    private fun postConstruct() {
        instance = this
        AbstractRechnungsStatistik.rechnungCache = this
        RechnungCalculator.rechnungCache = this
        RechnungCalculator.auftragsCache = auftragsCache
    }

    fun getOrderPositionInfos(rechnungId: Long?): Set<OrderPositionInfo>? {
        rechnungId ?: return null
        val info = getRechnungInfo(rechnungId) ?: return null
        val set = mutableSetOf<OrderPositionInfo>()
        info.positions?.forEach { posInfo ->
            posInfo.auftragsPositionId?.let { auftragsPosId ->
                auftragsCache.getOrderPositionInfo(auftragsPosId)?.let {
                    set.add(it)
                }
            }
        }
        return set
    }

    fun getRechnungsPosInfosByAuftragId(auftragId: Long?): Set<RechnungPosInfo>? {
        auftragId ?: return null
        return auftragsRechnungCache.getRechnungsPosInfoByAuftragId(auftragId)
    }

    @JvmOverloads
    fun getRechnungsPosInfosByAuftragsPositionId(
        auftragsPositionId: Long?,
        checkRefresh: Boolean = true
    ): Set<RechnungPosInfo>? {
        auftragsPositionId ?: return null
        return auftragsRechnungCache.getRechnungsPosInfosByAuftragsPositionId(auftragsPositionId, checkRefresh)
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

    fun ensureRechnungInfo(rechnung: EingangsrechnungDO): RechnungInfo {
        return eingangsrechnungCache.ensureRechnungInfo(rechnung)
    }

    fun getEingangsrechnungInfo(rechnungId: Long?): RechnungInfo? {
        return eingangsrechnungCache.getRechnungInfo(rechnungId)
    }

    /**
     * Autodetect Rechnung/Eingangsrechnung.
     */
    override fun getRechnungInfo(rechnung: AbstractRechnungDO?): RechnungInfo? {
        val id = rechnung?.id ?: return null
        return if (rechnung is RechnungDO) {
            super.getRechnungInfo(rechnung)
        } else {
            eingangsrechnungCache.getRechnungInfo(id)
        }
    }

    override fun setExpired() {
        super.setExpired()
        auftragsRechnungCache.setExpired()
    }

    companion object {
        @JvmStatic
        lateinit var instance: RechnungCache
            private set
    }
}
