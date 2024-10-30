package org.projectforge.business.fibu

import mu.KotlinLogging
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

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
    private var invoicePositionMapByAuftragId = mapOf<Long, MutableSet<RechnungsPositionVO>>()

    /**
     * The key is the order position id.
     */
    private var invoicePositionMapByAuftragsPositionId = mapOf<Long, MutableSet<RechnungsPositionVO>>()

    private var invoicePositionMapByRechnungId = mapOf<Long, MutableSet<RechnungsPositionVO>>()

    fun getRechnungsPositionVOSetByAuftragId(auftragId: Long?): Set<RechnungsPositionVO>? {
        auftragId ?: return null
        checkRefresh()
        return invoicePositionMapByAuftragId[auftragId]
    }

    fun getRechnungsPositionVOSetByRechnungId(rechnungId: Long?): Set<RechnungsPositionVO>? {
        rechnungId ?: return null
        checkRefresh()
        return invoicePositionMapByRechnungId[rechnungId]
    }

    fun getRechnungsPositionVOSetByAuftragsPositionId(auftragsPositionId: Long?): Set<RechnungsPositionVO>? {
        auftragsPositionId ?: return null
        checkRefresh()
        return invoicePositionMapByAuftragsPositionId[auftragsPositionId]
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing AuftragsRechnungCache...")
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            // This method must not be synchronized because it works with new copies of maps.
            val mapByAuftragId = mutableMapOf<Long, MutableSet<RechnungsPositionVO>>()
            val mapByAuftragsPositionId = mutableMapOf<Long, MutableSet<RechnungsPositionVO>>()
            val mapByRechnungsPositionMapByRechnungId = mutableMapOf<Long, MutableSet<RechnungsPositionVO>>()
            log.info("Analyzing orders in invoices (RechnungsPositionDO.AuftragsPosition)...")
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
                    mapByAuftragId[auftrag.id ?: 0] = setByAuftragId
                }
                var setByAuftragsPositionId = mapByAuftragsPositionId[auftragsPosition.id]
                if (setByAuftragsPositionId == null) {
                    setByAuftragsPositionId = TreeSet()
                    mapByAuftragsPositionId[auftragsPosition.id ?: 0] = setByAuftragsPositionId
                }
                pos.info = rechnungCache.getRechnungPosInfo(rechnung.id) ?: RechnungPosInfo(pos)
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
                    mapByRechnungsPositionMapByRechnungId[rechnung.id ?: 0] = positionen
                }
                positionen.add(vo)
            }
            this.invoicePositionMapByAuftragId = mapByAuftragId
            this.invoicePositionMapByAuftragsPositionId = mapByAuftragsPositionId
            this.invoicePositionMapByRechnungId = mapByRechnungsPositionMapByRechnungId
            log.info(
                "Initializing of RechnungCache done. stats=${persistenceService.formatStats(context.savedStats)}, callsStats=${
                    PfPersistenceService.showCallsStatsRecording()
                }"
            )
        }
    }
}
