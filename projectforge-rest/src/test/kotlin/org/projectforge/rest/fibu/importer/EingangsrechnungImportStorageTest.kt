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

package org.projectforge.rest.fibu.importer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.rest.dto.Konto
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Test class for EingangsrechnungImportStorage with PRIVA matching scenario.
 */
class EingangsrechnungImportStorageTest {

    @Test
    fun testCreditorMatchingScenario() {
        // Create PRIVA import invoice (from CSV)
        val importInvoice = EingangsrechnungPosImportDTO(
            referenz = "418236720",
            kreditor = "PRIVA Datenschutz GmbH",
            datum = LocalDate.of(2025, 5, 2),
            grossSum = BigDecimal("612.40"),
            konto = Konto(id = 1),
            betreff = "Some service description",
            faelligkeit = LocalDate.of(2025, 6, 1),
            currency = "EUR",
            positionNummer = 1
        )

        // Create PRIVA database invoice (existing)
        val dbInvoice = EingangsrechnungDO().apply {
            referenz = "4182367-20 / Az.: PR-0042-20/ABC"
            kreditor = "PRIVA GmbH"
            datum = LocalDate.of(2025, 5, 2)
            // Note: grossSum calculation would be through ensuredInfo but we'll skip for this test
        }

        // Test the matching score
        val score = importInvoice.matchScore(dbInvoice)

        // Score should be sufficient for matching (without amount matching due to complexity)
        // Expected: referenz(25) + kreditor(20) + date(20) = 65
        assertTrue(score >= 60, "PRIVA invoices should match with score >= 60, got $score")
    }

    @Test
    fun testHeaderMatchingScenario() {
        // Create PRIVA import invoice (from CSV)
        val importInvoice = EingangsrechnungPosImportDTO(
            referenz = "418236720",
            kreditor = "PRIVA Datenschutz GmbH",
            datum = LocalDate.of(2025, 5, 2),
            grossSum = BigDecimal("612.40"),
            konto = Konto(id = 1),
            betreff = "Some service description",
            faelligkeit = LocalDate.of(2025, 6, 1),
            currency = "EUR",
            positionNummer = 1
        )

        // Create PRIVA database invoice (existing)
        val dbInvoice = EingangsrechnungDO().apply {
            referenz = "4182367-20 / Az.: PR-0042-20/ABC"
            kreditor = "PRIVA GmbH"
            datum = LocalDate.of(2025, 5, 2)
        }

        // Test the header matching score
        val headerScore = importInvoice.matchScore(dbInvoice)

        // Score should be sufficient for matching (without amount matching due to complexity)
        // Expected: referenz(25) + kreditor(20) + date(20) = 65
        assertTrue(headerScore >= 60, "PRIVA invoices should header match with score >= 60, got $headerScore")
    }

    @Test
    fun `no update of an invoice of another date or creditor`() {
        val read = EingangsrechnungPosImportDTO(
            referenz = "2026-10001",
            kreditor = "Example Alerting Inc.",
            datum = LocalDate.of(2026, 9, 4),
        )
        val sameCreditorYearBefore = EingangsrechnungDO().apply {
            referenz = "2025-10654"
            kreditor = "Example Alerting Inc."
            datum = LocalDate.of(2025, 9, 4)
        }
        assertFalse(read.isPlausibleUpdateOf(sameCreditorYearBefore), "A year apart: another invoice.")
        assertFalse(read.isPlausibleMatch(sameCreditorYearBefore))

        val otherCreditor = EingangsrechnungDO().apply {
            referenz = "2026-10001"
            kreditor = "Example Office, Inc"
            datum = LocalDate.of(2026, 9, 4)
        }
        assertFalse(read.isPlausibleUpdateOf(otherCreditor), "Another creditor: another invoice.")

        val corrected = EingangsrechnungDO().apply {
            referenz = "2026-10001"
            kreditor = "Example Alerting Inc."
            datum = LocalDate.of(2026, 9, 9)
        }
        assertTrue(read.isPlausibleMatch(corrected), "Date corrected by some days: same invoice.")
    }
}
