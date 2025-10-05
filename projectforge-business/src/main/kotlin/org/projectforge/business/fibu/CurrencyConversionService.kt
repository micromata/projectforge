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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Service for currency conversion and currency pair management.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class CurrencyConversionService {
    @Autowired
    private lateinit var currencyPairDao: CurrencyPairDao

    @Autowired
    private lateinit var serviceSupport: CurrencyConversionServiceSupport

    @Autowired
    private lateinit var cache: CurrencyConversionCache

    @Autowired
    private lateinit var configurationService: org.projectforge.business.configuration.ConfigurationService

    @PostConstruct
    private fun postConstruct() {
        instance = this
        // Register history adapter if needed
        // historyFormatService.register(CurrencyConversionRateDO::class.java, CurrencyConversionRateHistoryAdapter())
    }

    /**
     * Finds a rate by its id.
     * @param id The id of the rate.
     * @param checkAccess If true, the logged-in user must have access to the rate.
     * @return The rate or null if not found.
     */
    fun findRate(id: Long?, checkAccess: Boolean = true): CurrencyConversionRateDO? {
        return serviceSupport.findRate(id, checkAccess)
    }

    /**
     * Selects all rates for a currency pair.
     * Uses cache for better performance when deleted=false.
     * @param currencyPairId The id of the currency pair.
     * @param deleted If true, only deleted rates will be returned, if false, only not deleted rates will be returned. If null, deleted and not deleted rates will be returned.
     * @param checkAccess If true, the logged-in user must have access to the currency pair (only applies when querying DB for deleted rates).
     * @return List of rates sorted by validFrom desc.
     */
    fun selectAllRates(
        currencyPairId: Long,
        deleted: Boolean? = false,
        checkAccess: Boolean = true,
    ): List<CurrencyConversionRateDO> {
        // For deleted rates, use DB (cache only contains active rates)
        if (deleted != false) {
            val currencyPair = currencyPairDao.find(currencyPairId, checkAccess = checkAccess) ?: return emptyList()
            return serviceSupport.selectAllRates(currencyPair, deleted, checkAccess)
        }
        // For active rates, use cache (faster)
        return cache.getRates(currencyPairId)
    }

    /**
     * Gets the conversion rate for a currency pair at a specific date.
     * Uses cache for better performance.
     * @param currencyPair The currency pair.
     * @param validAtDate The date for which to get the rate. Defaults to today.
     * @param inverseRate If true, the inverse rate (target to source) will be returned.
     * @return The conversion rate or null if no rate is valid for the given date.
     */
    fun getConversionRate(
        currencyPair: CurrencyPairDO?,
        validAtDate: LocalDate? = null,
        inverseRate: Boolean = false,
    ): BigDecimal? {
        currencyPair?.id ?: return null
        return cache.getConversionRate(
            currencyPair.id,
            validAtDate ?: LocalDate.now(),
            inverseRate,
            useFallbackToOldestRate = false
        )
    }

    /**
     * Gets the validFrom date of the active conversion rate for a currency pair at a specific date.
     * Uses cache for better performance.
     * @param currencyPair The currency pair.
     * @param validAtDate The date for which to get the rate. Defaults to today.
     * @return The validFrom date of the active rate or null if no rate is valid for the given date.
     */
    fun getConversionRateDate(
        currencyPair: CurrencyPairDO?,
        validAtDate: LocalDate? = null,
    ): LocalDate? {
        currencyPair?.id ?: return null
        return cache.getActiveRateDate(
            currencyPair.id,
            validAtDate ?: LocalDate.now(),
            useFallbackToOldestRate = false
        )
    }

    /**
     * Converts an amount from one currency to another using the rate valid at the given date.
     * Uses cache for better performance.
     * @param amount The amount to convert.
     * @param targetCurrency The target currency (e.g. "EUR").
     * @param sourceCurrency The source currency (e.g. "USD"). Defaults to system default currency.
     * @param validAtDate The date for which to get the rate. Defaults to today.
     * @param scale The number of decimal places in the result. Defaults to 2.
     * @param roundingMode The rounding mode. Defaults to HALF_UP.
     * @param useFallbackToOldestRate If true and no rate exists for validAtDate, use the oldest available rate as fallback.
     *        CAUTION: Using this in financial accounting may violate compliance rules. Use only when approximate conversions are acceptable.
     * @param checkAccess If true, the logged-in user must have access to the currency pair.
     * @return The converted amount or null if no rate is available.
     */
    fun convert(
        amount: BigDecimal?,
        targetCurrency: String?,
        sourceCurrency: String? = null,
        validAtDate: LocalDate = LocalDate.now(),
        scale: Int = 2,
        roundingMode: RoundingMode = RoundingMode.HALF_UP,
        useFallbackToOldestRate: Boolean = false,
    ): BigDecimal? {
        amount ?: return null
        if (targetCurrency.isNullOrBlank()) {
            return null
        }

        // Use system default currency if sourceCurrency is not specified
        val effectiveSourceCurrency = sourceCurrency?.takeIf { it.isNotBlank() }
            ?: configurationService.currency
            ?: "EUR"

        // If source and target are the same, return the amount unchanged
        if (effectiveSourceCurrency.uppercase() == targetCurrency.uppercase()) {
            return amount.setScale(scale, roundingMode)
        }

        // Get currency pair - tries both direct and inverse direction automatically
        val lookup = cache.findCurrencyPairForConversion(effectiveSourceCurrency, targetCurrency) ?: return null

        // Get conversion rate from cache (uses inverse rate if lookup found inverse pair)
        val rate = cache.getConversionRate(
            lookup.pair.id,
            validAtDate,
            inverseRate = lookup.useInverseRate,
            useFallbackToOldestRate = useFallbackToOldestRate
        ) ?: return null

        return amount.multiply(rate).setScale(scale, roundingMode)
    }

    /**
     * Validates a rate for uniqueness.
     * @param rate The rate to validate.
     * @return Error message, if any. Null if given object can be modified or inserted.
     */
    fun validateRate(rate: CurrencyConversionRateDO): String? {
        try {
            serviceSupport.validate(rate)
            return null
        } catch (ex: Exception) {
            return org.projectforge.framework.i18n.translateMsg(
                "attr.validation.error.entryWithDateDoesAlreadyExist",
                rate.validFrom
            )
        }
    }

    /**
     * Inserts a new rate.
     * @param currencyPairId The currency pair id.
     * @param rateDO The rate to insert.
     * @param checkAccess If true, the logged-in user must have insert access.
     * @return The id of the inserted rate.
     */
    fun insertRate(
        currencyPairId: Long,
        rateDO: CurrencyConversionRateDO,
        checkAccess: Boolean = true,
    ): Long? {
        val result = serviceSupport.insert(currencyPairId, rateDO, checkAccess = checkAccess)
        return result
    }

    /**
     * Updates an existing rate.
     * @param currencyPairId The currency pair id (needed for access check).
     * @param rateDO The rate to update.
     * @param checkAccess If true, the logged-in user must have update access.
     */
    fun updateRate(
        currencyPairId: Long?,
        rateDO: CurrencyConversionRateDO,
        checkAccess: Boolean = true,
    ): org.projectforge.framework.persistence.api.EntityCopyStatus {
        val currencyPair = currencyPairDao.find(currencyPairId)!!
        val result = serviceSupport.updateRate(currencyPair, rateDO, checkAccess = checkAccess)
        return result
    }

    /**
     * Marks a rate as deleted.
     * @param currencyPairId The currency pair id (needed for access check).
     * @param rateId The rate id.
     * @param checkAccess If true, the logged-in user must have update access.
     */
    fun markRateAsDeleted(
        currencyPairId: Long?,
        rateId: Long?,
        checkAccess: Boolean = true,
    ) {
        serviceSupport.markRateAsDeleted(currencyPairId, rateId, checkAccess = checkAccess)
    }

    companion object {
        lateinit var instance: CurrencyConversionService
            private set
    }
}
