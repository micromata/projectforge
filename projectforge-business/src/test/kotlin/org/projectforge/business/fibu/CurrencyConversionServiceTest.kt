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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class CurrencyConversionServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var currencyConversionService: CurrencyConversionService

    @Autowired
    private lateinit var currencyPairDao: CurrencyPairDao

    @Autowired
    private lateinit var cache: CurrencyConversionCache

    override fun beforeAll() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)

            // Create USD → EUR currency pair with rates
            val usdToEur = CurrencyPairDO()
            usdToEur.sourceCurrency = "USD"
            usdToEur.targetCurrency = "EUR"
            usdToEur.comment = "Test USD to EUR"
            val usdToEurPairId = currencyPairDao.insert(usdToEur)

            // Add rate for 2025-01-01: 1 USD = 0.85 EUR, 1 EUR = 1.1765 USD
            addConversionRate(
                usdToEurPairId!!,
                LocalDate.of(2025, 1, 1),
                BigDecimal("0.85"),
                BigDecimal("1.1765")
            )

            // Add rate for 2025-06-01: 1 USD = 0.90 EUR, 1 EUR = 1.1111 USD (rate changed)
            addConversionRate(
                usdToEurPairId,
                LocalDate.of(2025, 6, 1),
                BigDecimal("0.90"),
                BigDecimal("1.1111")
            )

            // Create EUR → GBP currency pair
            val eurToGbp = CurrencyPairDO()
            eurToGbp.sourceCurrency = "EUR"
            eurToGbp.targetCurrency = "GBP"
            eurToGbp.comment = "Test EUR to GBP"
            val eurToGbpPairId = currencyPairDao.insert(eurToGbp)

            // Add rate for 2025-01-01: 1 EUR = 0.87 GBP, 1 GBP = 1.1494 EUR
            addConversionRate(
                eurToGbpPairId!!,
                LocalDate.of(2025, 1, 1),
                BigDecimal("0.87"),
                BigDecimal("1.1494")
            )

            // Force cache refresh to include new test data
            cache.setExpired()
            cache.forceReload()
        }
    }

    @Test
    fun convertNullAmountReturnsNull() {
        val result = currencyConversionService.convert(null, "EUR", "USD")
        assertNull(result)
    }

    @Test
    fun convertNullTargetCurrencyReturnsNull() {
        val result = currencyConversionService.convert(BigDecimal("100"), null, "USD")
        assertNull(result)
    }

    @Test
    fun convertSameCurrencyReturnsScaledAmount() {
        val result = currencyConversionService.convert(
            BigDecimal("100.123456"),
            "EUR",
            "EUR",
            scale = 2
        )
        assertNotNull(result)
        assertEquals(BigDecimal("100.12"), result)
    }

    @Test
    fun convertUsdToEurWithCurrentRate() {
        // Rate from 2025-06-01 (0.90) should be used for dates >= 2025-06-01
        val result = currencyConversionService.convert(
            BigDecimal("100"),
            "EUR",
            "USD",
            LocalDate.of(2025, 7, 1)
        )
        assertNotNull(result)
        assertEquals(BigDecimal("90.00"), result) // 100 * 0.90 = 90.00
    }

    @Test
    fun convertUsdToEurWithHistoricalRate() {
        // Rate from 2025-01-01 (0.85) should be used for dates before 2025-06-01
        val result = currencyConversionService.convert(
            BigDecimal("100"),
            "EUR",
            "USD",
            LocalDate.of(2025, 3, 15)
        )
        assertNotNull(result)
        assertEquals(BigDecimal("85.00"), result) // 100 * 0.85 = 85.00
    }

    @Test
    fun convertEurToGbpWithRate() {
        val result = currencyConversionService.convert(
            BigDecimal("100"),
            "GBP",
            "EUR",
            LocalDate.of(2025, 1, 1)
        )
        assertNotNull(result)
        assertEquals(BigDecimal("87.00"), result) // 100 * 0.87 = 87.00
    }

    @Test
    fun convertCurrencyPairNotFoundReturnsNull() {
        val result = currencyConversionService.convert(
            BigDecimal("100"),
            "JPY", // No currency pair for USD → JPY exists
            "USD"
        )
        assertNull(result)
    }

    @Test
    fun convertNoRateForDateReturnsNull() {
        // Try to get rate for date before first rate entry
        val result = currencyConversionService.convert(
            BigDecimal("100"),
            "EUR",
            "USD",
            LocalDate.of(2024, 12, 1) // Before first rate (2025-01-01)
        )
        assertNull(result)
    }

    @Test
    fun convertWithFallbackToOldestRate() {
        // Request conversion for date before first rate entry, with fallback enabled
        val result = currencyConversionService.convert(
            BigDecimal("100"),
            "EUR",
            "USD",
            LocalDate.of(2024, 12, 1), // Before first rate (2025-01-01)
            useFallbackToOldestRate = true
        )
        // Should use oldest rate (2025-01-01: 0.85) as fallback
        assertNotNull(result)
        assertEquals(BigDecimal("85.00"), result) // 100 * 0.85 = 85.00
    }

    @Test
    fun convertUsesCorrectRateForDate() {
        // This test verifies that the cache correctly selects the most recent valid rate
        // for a given date when multiple rates exist.

        // Look up the EUR→GBP pair from cache
        val eurToGbpPair = cache.findCurrencyPair("EUR", "GBP")
        assertNotNull(eurToGbpPair, "EUR→GBP pair should exist in cache")

        // Add two more rates to have three rates total:
        // 2025-01-01: 0.87 (already exists from setup)
        // 2025-06-01: 0.95 (new)
        // 2025-12-01: 0.90 (new)
        addConversionRate(
            eurToGbpPair!!.id!!,
            LocalDate.of(2025, 6, 1),
            BigDecimal("0.95"),
            BigDecimal("1.0526")
        )
        addConversionRate(
            eurToGbpPair.id!!,
            LocalDate.of(2025, 12, 1),
            BigDecimal("0.90"),
            BigDecimal("1.1111")
        )
        cache.setExpired()
        cache.forceReload()

        // Test 1: Date between first and second rate (should use first rate)
        val result1 = currencyConversionService.convert(
            BigDecimal("100"),
            "GBP",
            "EUR",
            LocalDate.of(2025, 3, 15) // Between 2025-01-01 and 2025-06-01
        )
        assertNotNull(result1)
        assertEquals(BigDecimal("87.00"), result1) // 100 * 0.87 = 87.00

        // Test 2: Date between second and third rate (should use second rate)
        val result2 = currencyConversionService.convert(
            BigDecimal("100"),
            "GBP",
            "EUR",
            LocalDate.of(2025, 8, 20) // Between 2025-06-01 and 2025-12-01
        )
        assertNotNull(result2)
        assertEquals(BigDecimal("95.00"), result2) // 100 * 0.95 = 95.00

        // Test 3: Date after third rate (should use third rate)
        val result3 = currencyConversionService.convert(
            BigDecimal("100"),
            "GBP",
            "EUR",
            LocalDate.of(2025, 12, 15) // After 2025-12-01
        )
        assertNotNull(result3)
        assertEquals(BigDecimal("90.00"), result3) // 100 * 0.90 = 90.00

        // Test 4: Date far in the future (should still use third/latest rate)
        val result4 = currencyConversionService.convert(
            BigDecimal("100"),
            "GBP",
            "EUR",
            LocalDate.of(2026, 6, 1) // Far in the future
        )
        assertNotNull(result4)
        assertEquals(BigDecimal("90.00"), result4) // 100 * 0.90 = 90.00
    }

    @Test
    fun convertWithCustomScale() {
        // 100 * 0.85 = 85.0000 with scale 4
        val result = currencyConversionService.convert(
            BigDecimal("100"),
            "EUR",
            "USD",
            LocalDate.of(2025, 1, 1),
            scale = 4
        )
        assertNotNull(result)
        assertEquals(BigDecimal("85.0000"), result)
    }

    @Test
    fun convertWithRoundingHalfUp() {
        // Look up the USD→EUR pair from cache (since instance variables are reset per test)
        val usdToEurPair = cache.findCurrencyPair("USD", "EUR")
        assertNotNull(usdToEurPair, "USD→EUR pair should exist in cache")

        // 100 * 0.855 = 85.50 with scale 2, HALF_UP
        addConversionRate(
            usdToEurPair!!.id!!,
            LocalDate.of(2025, 12, 1),
            BigDecimal("0.855"),
            BigDecimal("1.1696")
        )
        cache.setExpired()
        cache.forceReload()

        val result = currencyConversionService.convert(
            BigDecimal("100"),
            "EUR",
            "USD",
            LocalDate.of(2025, 12, 1),
            scale = 2,
            roundingMode = RoundingMode.HALF_UP
        )
        assertNotNull(result)
        assertEquals(BigDecimal("85.50"), result)
    }

    @Test
    fun convertDefaultsToSystemCurrency() {
        // When sourceCurrency is null, it should use system default currency
        // We can't predict the system default, but we can verify the method doesn't crash
        val result = currencyConversionService.convert(
            BigDecimal("100"),
            "EUR",
            sourceCurrency = null // Should use system default
        )
        // Result may be null if system currency → EUR pair doesn't exist, which is OK
        // We're just testing that it doesn't throw an exception
        assertTrue(result == null || result > BigDecimal.ZERO)
    }

    @Test
    fun convertCacheIsUsed() {
        // First conversion - cache should be populated
        val result1 = currencyConversionService.convert(
            BigDecimal("100"),
            "EUR",
            "USD",
            LocalDate.of(2025, 1, 1)
        )
        assertNotNull(result1)

        // Get currency pair from cache
        val cachedPair = cache.findCurrencyPair("USD", "EUR")
        assertNotNull(cachedPair)
        assertEquals("USD", cachedPair!!.sourceCurrency)
        assertEquals("EUR", cachedPair.targetCurrency)

        // Get rate from cache
        val cachedRate = cache.getConversionRate(cachedPair.id, LocalDate.of(2025, 1, 1))
        assertNotNull(cachedRate)
        assertEquals(0, BigDecimal("0.85").compareTo(cachedRate))
    }

    /**
     * Helper method to add a conversion rate to a currency pair.
     */
    private fun addConversionRate(
        currencyPairId: Long,
        validFrom: LocalDate,
        conversionRate: BigDecimal,
        inverseConversionRate: BigDecimal
    ) {
        val rate = CurrencyConversionRateDO()
        rate.currencyPair = CurrencyPairDO().apply { id = currencyPairId }
        rate.validFrom = validFrom
        rate.conversionRate = conversionRate
        rate.inverseConversionRate = inverseConversionRate
        rate.comment = "Test rate for ${validFrom}"
        currencyConversionService.insertRate(currencyPairId, rate)
    }
}
