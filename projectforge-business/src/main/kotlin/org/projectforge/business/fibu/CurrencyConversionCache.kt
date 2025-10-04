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
// with this program; if not, see http://www.gnu.org/licenses/>.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.fibu

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Caches currency pairs and their conversion rates for faster access.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
open class CurrencyConversionCache : AbstractCache(), BaseDOModifiedListener<CurrencyPairDO> {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var currencyPairDao: CurrencyPairDao

    @PostConstruct
    private fun postConstruct() {
        instance = this
        currencyPairDao.register(this)
    }

    override fun afterInsertOrModify(obj: CurrencyPairDO, operationType: OperationType) {
        setExpired()
    }

    /**
     * The key is the currency pair id (database pk). Mustn't be synchronized, because it is only read.
     */
    private var currencyPairMap: Map<Long, CurrencyPairDO> = emptyMap()

    /**
     * Quick lookup map for currency pairs by (sourceCurrency, targetCurrency).
     * The value is the currency pair id.
     */
    private var currencyPairLookupMap: Map<Pair<String, String>, Long> = emptyMap()

    /**
     * Gets a currency pair by id.
     * @param id The currency pair id.
     * @return The currency pair or null if not found.
     */
    fun getCurrencyPair(id: Long?): CurrencyPairDO? {
        id ?: return null
        checkRefresh()
        return currencyPairMap[id]
    }

    /**
     * Finds a currency pair by source and target currency.
     * @param sourceCurrency Source currency code (e.g. "USD").
     * @param targetCurrency Target currency code (e.g. "EUR").
     * @return The currency pair or null if not found.
     */
    fun findCurrencyPair(sourceCurrency: String, targetCurrency: String): CurrencyPairDO? {
        checkRefresh()
        val pairId = currencyPairLookupMap[Pair(sourceCurrency.uppercase(), targetCurrency.uppercase())]
        return pairId?.let { currencyPairMap[it] }
    }

    /**
     * Gets the conversion rate for a currency pair at a specific date.
     * @param currencyPairId The currency pair id.
     * @param validAtDate The date for which to get the rate. Defaults to today.
     * @param useFallbackToOldestRate If true and no rate exists for validAtDate, use the oldest available rate as fallback.
     * @return The conversion rate or null if no rate is valid for the given date.
     */
    fun getConversionRate(
        currencyPairId: Long?,
        validAtDate: LocalDate = LocalDate.now(),
        useFallbackToOldestRate: Boolean = false
    ): BigDecimal? {
        currencyPairId ?: return null
        checkRefresh()
        val currencyPair = currencyPairMap[currencyPairId] ?: return null
        return findActiveRate(currencyPair, validAtDate, useFallbackToOldestRate)?.conversionRate
    }

    /**
     * Gets all rates for a currency pair.
     * @param currencyPairId The currency pair id.
     * @return List of rates sorted by validFrom DESC, or empty list if currency pair not found.
     */
    fun getRates(currencyPairId: Long?): List<CurrencyConversionRateDO> {
        currencyPairId ?: return emptyList()
        checkRefresh()
        return currencyPairMap[currencyPairId]?.rates ?: emptyList()
    }

    /**
     * Finds the active rate for a currency pair at a specific date.
     * The active rate is the one with the latest validFrom date that is not after the given date.
     * @param currencyPair The currency pair (must contain loaded rates).
     * @param validAtDate The date for which to find the active rate.
     * @param useFallbackToOldestRate If true and no rate exists for validAtDate, use the oldest available rate as fallback.
     * @return The active rate or null if no rate is valid for the given date.
     */
    private fun findActiveRate(
        currencyPair: CurrencyPairDO,
        validAtDate: LocalDate,
        useFallbackToOldestRate: Boolean = false
    ): CurrencyConversionRateDO? {
        // Rates are sorted by validFrom DESC, so we find the first one where validFrom <= validAtDate
        val rate = currencyPair.rates?.firstOrNull { rate ->
            rate.validFrom != null && !rate.validFrom!!.isAfter(validAtDate)
        }

        // If no rate found and fallback is enabled, use the oldest rate (last in DESC sorted list)
        if (rate == null && useFallbackToOldestRate) {
            val oldestRate = currencyPair.rates?.lastOrNull()
            if (oldestRate != null) {
                log.warn {
                    "No rate found for ${currencyPair.sourceCurrency}→${currencyPair.targetCurrency} " +
                            "at date $validAtDate. Using oldest rate from ${oldestRate.validFrom} as fallback."
                }
            }
            return oldestRate
        }

        return rate
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    public override fun refresh() {
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            log.info("Initializing CurrencyConversionCache...")
            // This method must not be synchronized because it works with a new copy of maps.
            val pairMap = mutableMapOf<Long, CurrencyPairDO>()
            val lookupMap = mutableMapOf<Pair<String, String>, Long>()

            // Load all currency pairs
            persistenceService.executeQuery(
                "from CurrencyPairDO t where deleted=false",
                CurrencyPairDO::class.java,
            ).forEach { currencyPair ->
                pairMap[currencyPair.id!!] = currencyPair

                // Build lookup map
                if (currencyPair.sourceCurrency != null && currencyPair.targetCurrency != null) {
                    lookupMap[Pair(
                        currencyPair.sourceCurrency!!.uppercase(),
                        currencyPair.targetCurrency!!.uppercase()
                    )] = currencyPair.id!!
                }
            }

            // Load all rates grouped by currency pair
            persistenceService.executeQuery(
                "from CurrencyConversionRateDO t where deleted=false order by t.currencyPair.id, t.validFrom desc",
                CurrencyConversionRateDO::class.java,
            ).groupBy { it.currencyPair?.id } // Group by currency pair id
                .forEach { (currencyPairId, rates) ->
                    pairMap[currencyPairId]?.let { currencyPair ->
                        currencyPair.rates = rates.toMutableList()
                    }
                }

            this.currencyPairMap = pairMap
            this.currencyPairLookupMap = lookupMap
            log.info { "CurrencyConversionCache.refresh done. Loaded ${pairMap.size} currency pairs. ${context.formatStats()}" }
        }
    }

    companion object {
        @JvmStatic
        lateinit var instance: CurrencyConversionCache
            private set
    }
}
