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


package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.CurrencyConversionService
import org.projectforge.business.fibu.CurrencyPairDO
import org.projectforge.business.fibu.CurrencyPairDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.rest.dto.CurrencyConversionRate
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The conversion rates of a currency pair, written through [CurrencyPairEntityRest] (`currencyPair/rate/...`):
 * access, the pair a rate belongs to and the checks of a rate.
 */
class CurrencyPairEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var currencyConversionService: CurrencyConversionService

    @Autowired
    private lateinit var currencyPairDao: CurrencyPairDao

    @Autowired
    private lateinit var currencyPairEntityRest: CurrencyPairEntityRest

    /** Rates are written by the users with write access to the currency pairs only. */
    @Test
    fun `rates are written by users with access to the currency pairs only`() {
        val pairId = createPair("XQA", "XQB")
        val rate = rate(pairId, LocalDate.of(2025, 1, 1), "0.5", "2")
        logon(TEST_USER)
        assertThrows<AccessException> { currencyPairEntityRest.insertRate(post(rate)) }
        logon(TEST_FINANCE_USER)
        val entries = entries(currencyPairEntityRest.insertRate(post(rate)))
        assertEquals(1, entries.size)
        assertEquals(BigDecimal("0.5"), entries.single().conversionRate?.stripTrailingZeros())

        val stored = entries.single()
        stored.comment = "changed"
        logon(TEST_USER)
        assertThrows<AccessException> { currencyPairEntityRest.updateRate(post(stored)) }
        assertThrows<AccessException> { currencyPairEntityRest.deleteRate(post(stored)) }
        logon(TEST_FINANCE_USER)
        assertEquals("changed", entries(currencyPairEntityRest.updateRate(post(stored))).single().comment)
        assertTrue(entries(currencyPairEntityRest.deleteRate(post(stored))).isEmpty())
    }

    /** The pair of a rate comes from the database: a posted pair id doesn't move the rate to another pair. */
    @Test
    fun `an update keeps the stored pair of the rate`() {
        logon(TEST_FINANCE_USER)
        val pairId = createPair("XQC", "XQD")
        val otherPairId = createPair("XQE", "XQF")
        val stored = entries(
            currencyPairEntityRest.insertRate(post(rate(pairId, LocalDate.of(2025, 1, 1), "0.5", "2")))
        ).single()
        stored.currencyPairId = otherPairId
        stored.comment = "moved?"
        val entries = entries(currencyPairEntityRest.updateRate(post(stored)))
        assertEquals(pairId, entries.single().currencyPairId)
        assertEquals("moved?", entries.single().comment)
        assertTrue(currencyConversionService.selectAllRates(otherPairId, deleted = false).isEmpty())
    }

    /** A rate needs positive values, a date not in the future and no other rate of the pair on the same date. */
    @Test
    fun `invalid rates are refused at their field`() {
        logon(TEST_FINANCE_USER)
        val pairId = createPair("XQG", "XQH")
        val date = LocalDate.of(2025, 1, 1)
        assertEquals(
            listOf("conversionRate", "inverseConversionRate"),
            validationFields(currencyPairEntityRest.insertRate(post(rate(pairId, date, "0", "-1")))),
        )
        assertEquals(
            listOf("validFrom"),
            validationFields(
                currencyPairEntityRest.insertRate(post(rate(pairId, LocalDate.now().plusDays(1), "1", "1")))
            ),
        )
        entries(currencyPairEntityRest.insertRate(post(rate(pairId, date, "0.5", "2"))))
        assertEquals(
            listOf("validFrom"),
            validationFields(currencyPairEntityRest.insertRate(post(rate(pairId, date, "0.6", "1.6")))),
        )
    }

    private fun createPair(source: String, target: String): Long {
        logon(TEST_FINANCE_USER)
        return currencyPairDao.insert(CurrencyPairDO().also {
            it.sourceCurrency = source
            it.targetCurrency = target
        })
    }

    private fun post(rate: CurrencyConversionRate) = PostData(rate, watchFieldsTriggered = null, serverData = null)

    private fun rate(pairId: Long, validFrom: LocalDate, rate: String, inverseRate: String) =
        CurrencyConversionRate(
            validFrom = validFrom,
            conversionRate = BigDecimal(rate),
            inverseConversionRate = BigDecimal(inverseRate),
            currencyPairId = pairId,
        )

    private fun entries(response: ResponseEntity<*>): List<CurrencyConversionRate> {
        assertEquals(HttpStatus.OK, response.statusCode)
        return (response.body as CurrencyPairEntityRest.RateEntries).entries
    }

    private fun validationFields(response: ResponseEntity<*>): List<String> {
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.statusCode)
        return (response.body as ResponseAction).validationErrors!!.mapNotNull { it.fieldId }.sorted()
    }
}
