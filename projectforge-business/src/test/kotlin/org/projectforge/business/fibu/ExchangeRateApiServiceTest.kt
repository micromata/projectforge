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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ExchangeRateApiServiceTest {
    private val service = ExchangeRateApiService()

    @Test
    fun `parses rate of v2 response`() {
        val json = """[{"date":"2025-03-01","base":"USD","quote":"EUR","rate":0.95958}]"""
        assertEquals(BigDecimal("0.95958"), service.parseRate(json, "EUR"))
    }

    @Test
    fun `picks the requested quote of several`() {
        val json = """[{"date":"2025-03-01","base":"USD","quote":"CHF","rate":0.9},""" +
                """{"date":"2025-03-01","base":"USD","quote":"EUR","rate":0.95958}]"""
        assertEquals(BigDecimal("0.95958"), service.parseRate(json, "EUR"))
    }

    @Test
    fun `returns null for missing quote, empty array, v1 format and invalid json`() {
        assertNull(service.parseRate("""[{"base":"USD","quote":"CHF","rate":0.9}]""", "EUR"))
        assertNull(service.parseRate("[]", "EUR"))
        assertNull(service.parseRate("""{"amount":1.0,"base":"USD","rates":{"EUR":0.85}}""", "EUR"))
        assertNull(service.parseRate("<html>301 Moved Permanently</html>", "EUR"))
    }

    @Test
    fun `builds v2 url with optional date`() {
        assertEquals(
            "https://api.frankfurter.dev/v2/rates?base=USD&quotes=EUR",
            service.buildUrl("USD", "EUR", null),
        )
        assertEquals(
            "https://api.frankfurter.dev/v2/rates?base=USD&quotes=EUR&date=2025-03-01",
            service.buildUrl("USD", "EUR", LocalDate.of(2025, 3, 1)),
        )
    }

    @Test
    fun `same currency returns one without api call`() {
        assertEquals(BigDecimal.ONE, service.fetchRateForDate("eur", "EUR", null))
    }
}
