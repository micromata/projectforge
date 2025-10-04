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
import org.projectforge.framework.persistence.history.HistoryFormatService
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
     * @param currencyPairId The id of the currency pair.
     * @param deleted If true, only deleted rates will be returned, if false, only not deleted rates will be returned. If null, deleted and not deleted rates will be returned.
     * @param checkAccess If true, the logged-in user must have access to the currency pair.
     * @return List of rates sorted by validFrom desc.
     */
    fun selectAllRates(
        currencyPairId: Long,
        deleted: Boolean? = false,
        checkAccess: Boolean = true,
    ): List<CurrencyConversionRateDO> {
        val currencyPair = currencyPairDao.find(currencyPairId, checkAccess = checkAccess) ?: return emptyList()
        return selectAllRates(currencyPair, deleted, checkAccess)
    }

    /**
     * Selects all rates for a currency pair.
     * @param currencyPair The currency pair.
     * @param deleted If true, only deleted rates will be returned, if false, only not deleted rates will be returned. If null, deleted and not deleted rates will be returned.
     * @param checkAccess If true, the logged-in user must have access to the currency pair.
     * @return List of rates sorted by validFrom desc.
     */
    internal fun selectAllRates(
        currencyPair: CurrencyPairDO,
        deleted: Boolean? = false,
        checkAccess: Boolean = true,
    ): List<CurrencyConversionRateDO> {
        return serviceSupport.selectAllRates(currencyPair, deleted, checkAccess)
    }

    /**
     * Finds the active rate for the given date from a list of rates.
     * The active rate is the one with the latest validFrom date that is not after the given date.
     * @param rates List of rates sorted by validFrom desc.
     * @param validAtDate The date for which to find the active rate. Defaults to today.
     * @return The active rate or null if no rate is valid for the given date.
     */
    fun findActiveRate(
        rates: List<CurrencyConversionRateDO>,
        validAtDate: LocalDate? = null,
    ): CurrencyConversionRateDO? {
        return serviceSupport.getActiveRate(rates, validAtDate)
    }

    /**
     * Gets the conversion rate for a currency pair at a specific date.
     * @param currencyPair The currency pair.
     * @param validAtDate The date for which to get the rate. Defaults to today.
     * @param checkAccess If true, the logged-in user must have access to the currency pair.
     * @return The conversion rate or null if no rate is valid for the given date.
     */
    fun getConversionRate(
        currencyPair: CurrencyPairDO?,
        validAtDate: LocalDate? = null,
        checkAccess: Boolean = true
    ): BigDecimal? {
        return serviceSupport.getConversionRate(currencyPair, validAtDate, checkAccess)
    }

    /**
     * Gets the conversion rate for a currency pair at a specific date.
     * @param currencyPairId The id of the currency pair.
     * @param validAtDate The date for which to get the rate. Defaults to today.
     * @param checkAccess If true, the logged-in user must have access to the currency pair.
     * @return The conversion rate or null if no rate is valid for the given date.
     */
    fun getConversionRate(
        currencyPairId: Long?,
        validAtDate: LocalDate? = null,
        checkAccess: Boolean = true
    ): BigDecimal? {
        currencyPairId ?: return null
        val currencyPair = currencyPairDao.find(currencyPairId, checkAccess = checkAccess) ?: return null
        return getConversionRate(currencyPair, validAtDate, checkAccess)
    }

    /**
     * Finds a currency pair by source and target currency.
     * @param sourceCurrency The source currency (e.g. "USD").
     * @param targetCurrency The target currency (e.g. "EUR").
     * @param checkAccess If true, the logged-in user must have access to the currency pair.
     * @return The currency pair or null if not found.
     */
    fun findCurrencyPair(
        sourceCurrency: String?,
        targetCurrency: String?,
        checkAccess: Boolean = true
    ): CurrencyPairDO? {
        if (sourceCurrency.isNullOrBlank() || targetCurrency.isNullOrBlank()) {
            return null
        }
        val list = currencyPairDao.persistenceService.executeQuery(
            "from CurrencyPairDO where sourceCurrency = :source and targetCurrency = :target",
            CurrencyPairDO::class.java,
            Pair("source", sourceCurrency.uppercase()),
            Pair("target", targetCurrency.uppercase())
        )
        if (checkAccess) {
            list.forEach { currencyPairDao.checkLoggedInUserSelectAccess(it) }
        }
        return list.firstOrNull()
    }

    /**
     * Converts an amount from one currency to another using the rate valid at the given date.
     * @param amount The amount to convert.
     * @param sourceCurrency The source currency (e.g. "USD").
     * @param targetCurrency The target currency (e.g. "EUR").
     * @param validAtDate The date for which to get the rate. Defaults to today.
     * @param scale The number of decimal places in the result. Defaults to 2.
     * @param roundingMode The rounding mode. Defaults to HALF_UP.
     * @param checkAccess If true, the logged-in user must have access to the currency pair.
     * @return The converted amount or null if no rate is available.
     */
    fun convert(
        amount: BigDecimal?,
        sourceCurrency: String?,
        targetCurrency: String?,
        validAtDate: LocalDate? = null,
        scale: Int = 2,
        roundingMode: RoundingMode = RoundingMode.HALF_UP,
        checkAccess: Boolean = true
    ): BigDecimal? {
        if (amount == null || sourceCurrency.isNullOrBlank() || targetCurrency.isNullOrBlank()) {
            return null
        }
        // If source and target are the same, return the amount unchanged
        if (sourceCurrency.uppercase() == targetCurrency.uppercase()) {
            return amount.setScale(scale, roundingMode)
        }
        val currencyPair = findCurrencyPair(sourceCurrency, targetCurrency, checkAccess) ?: return null
        val rate = getConversionRate(currencyPair, validAtDate, checkAccess) ?: return null
        return amount.multiply(rate).setScale(scale, roundingMode)
    }

    /**
     * Saves or updates a currency conversion rate.
     * @param rate The rate to save or update.
     * @param checkAccess If true, the logged-in user must have insert/update access.
     * @return The saved or updated rate.
     */
    fun saveOrUpdateRate(
        rate: CurrencyConversionRateDO,
        checkAccess: Boolean = true
    ): CurrencyConversionRateDO {
        return serviceSupport.saveOrUpdate(rate, checkAccess)
    }

    /**
     * Deletes a currency conversion rate.
     * @param rate The rate to delete.
     * @param checkAccess If true, the logged-in user must have delete access.
     */
    fun deleteRate(
        rate: CurrencyConversionRateDO,
        checkAccess: Boolean = true
    ) {
        serviceSupport.delete(rate, checkAccess)
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
        cache.setExpired()
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
        cache.setExpired()
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
        cache.setExpired()
    }

    companion object {
        lateinit var instance: CurrencyConversionService
            private set
    }
}
