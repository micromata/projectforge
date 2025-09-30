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

package org.projectforge.rest.fibu.importer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.rest.dto.Konto
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Test class for EingangsrechnungImportStorage with ISICO matching scenario.
 */
class EingangsrechnungImportStorageTest {

    @Test
    fun testISICOMatchingScenario() {
        // Create ISICO import invoice (from CSV)
        val importInvoice = EingangsrechnungPosImportDTO(
            referenz = "325124610",
            kreditor = "ISICO Datenschutz GmbH",
            datum = LocalDate.of(2025, 5, 2),
            grossSum = BigDecimal("746.13"),
            konto = Konto(id = 1),
            betreff = "Some service description",
            faelligkeit = LocalDate.of(2025, 6, 1),
            currency = "EUR",
            positionNummer = 1
        )

        // Create ISICO database invoice (existing)
        val dbInvoice = EingangsrechnungDO().apply {
            referenz = "3251246-10 / Az.: IS-0017-10/KSR"
            kreditor = "ISICO GmbH"
            datum = LocalDate.of(2025, 5, 2)
            // Note: grossSum calculation would be through ensuredInfo but we'll skip for this test
        }

        // Test the matching score
        val score = importInvoice.matchScore(dbInvoice)

        // Score should be sufficient for matching (without amount matching due to complexity)
        // Expected: referenz(25) + kreditor(20) + date(20) = 65
        assertTrue(score >= 60, "ISICO invoices should match with score >= 60, got $score")
    }

    @Test
    fun testHeaderMatchingScenario() {
        // Create ISICO import invoice (from CSV)
        val importInvoice = EingangsrechnungPosImportDTO(
            referenz = "325124610",
            kreditor = "ISICO Datenschutz GmbH",
            datum = LocalDate.of(2025, 5, 2),
            grossSum = BigDecimal("746.13"),
            konto = Konto(id = 1),
            betreff = "Some service description",
            faelligkeit = LocalDate.of(2025, 6, 1),
            currency = "EUR",
            positionNummer = 1
        )

        // Create ISICO database invoice (existing)
        val dbInvoice = EingangsrechnungDO().apply {
            referenz = "3251246-10 / Az.: IS-0017-10/KSR"
            kreditor = "ISICO GmbH"
            datum = LocalDate.of(2025, 5, 2)
        }

        // Test the header matching score
        val headerScore = importInvoice.matchScore(dbInvoice)

        // Score should be sufficient for matching (without amount matching due to complexity)
        // Expected: referenz(25) + kreditor(20) + date(20) = 65
        assertTrue(headerScore >= 60, "ISICO invoices should header match with score >= 60, got $headerScore")
    }
}
