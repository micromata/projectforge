package org.projectforge.business.fibu

import mu.KotlinLogging
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Separate cache for incoming invoices due to performance reasons.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
internal class EingangsrechnungCache : AbstractCache() {
    @Autowired
    protected lateinit var persistenceService: PfPersistenceService

    private var invoiceInfoMap = mutableMapOf<Long, RechnungInfo>()


    fun update(invoice: EingangsrechnungDO) {
        synchronized(invoiceInfoMap) {
            invoiceInfoMap[invoice.id!!] = RechnungCalculator.calculate(invoice)
        }
    }

    fun getOrCalculateRechnungInfo(rechnung: EingangsrechnungDO): RechnungInfo {
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

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing EingangrechnungCache...")
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            // This method must not be synchronized because it works with new copies of maps.
            log.info("Getting all incoming invoices (EingangsRechnungDO)...")
            val nInvoiceInfoMap = mutableMapOf<Long, RechnungInfo>()
            persistenceService.executeQuery(
                "FROM EingangsrechnungDO t left join fetch t.positionen left join fetch t.positionen.kostZuweisungen",
                EingangsrechnungDO::class.java,
            ).forEach { rechnung ->
                nInvoiceInfoMap[rechnung.id!!] = RechnungCalculator.calculate(rechnung)
            }
            this.invoiceInfoMap = nInvoiceInfoMap
            log.info(
                "Initializing of RechnungCache done. stats=${persistenceService.formatStats(context.savedStats)}, callsStats=${
                    PfPersistenceService.showCallsStatsRecording()
                }"
            )
        }
    }
}
