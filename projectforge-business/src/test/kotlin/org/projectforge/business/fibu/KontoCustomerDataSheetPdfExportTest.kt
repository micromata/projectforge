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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class KontoCustomerDataSheetPdfExportTest : AbstractTestBase() {
    @Autowired
    private lateinit var export: KontoCustomerDataSheetPdfExport

    private fun fullKonto() = KontoDO().also {
        it.nummer = 10001
        it.bezeichnung = "Müller & Söhne GmbH"
        it.street = "Hauptstraße 1"
        it.zipCode = "34117"
        it.city = "Kassel"
        it.country = "DE"
        it.vatId = "DE123456789"
        it.contactPerson = "Erika Mustermann"
        it.contactDepartment = "Einkauf"
        it.contactEmail = "einkauf@example.com"
        it.eInvoiceEmail = "rechnung@example.com"
        it.leitwegId = "04011000-1234512345-12"
        it.costCenter = "4711"
    }

    private fun isPdf(bytes: ByteArray) =
        bytes.size > 4 && String(bytes, 0, 4, Charsets.US_ASCII) == "%PDF"

    @Test
    fun `a filled account yields a pdf`() {
        assertTrue(isPdf(export.export(fullKonto())))
    }

    @Test
    fun `a blank account yields a pdf too`() {
        assertTrue(isPdf(export.export(KontoDO())))
    }

    @Test
    fun `the stored values are carried and what the account lacks is listed blank`() {
        val rows = export.sections(fullKonto()).flatMap { it.rows }
        assertTrue(rows.any { it.value == "Müller & Söhne GmbH" })
        assertTrue(rows.any { it.value == "04011000-1234512345-12" })
        // Order number, contract number and payment target belong to the order: always blank here.
        assertEquals(3, rows.count { it.value == null })
        assertNull(export.sections(KontoDO()).flatMap { it.rows }.firstNotNullOfOrNull { it.value })
    }

    @Test
    fun `the file name carries number and name`() {
        val name = export.filename(fullKonto())
        assertTrue(name.endsWith(".pdf") && name.contains("10001"), name)
    }
}
