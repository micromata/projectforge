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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.importer.ImportPairEntry
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The deletions of [EingangsrechnungImportJob]: an import with fewer positions than the stored invoice selects only
 * the surplus positions (the others are unmodified). They must be removed from the invoice; the job deleted the
 * whole invoice before, so it was missing after the import.
 */
class EingangsrechnungImportJobTest : AbstractTestBase() {
    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Test
    fun `surplus positions are removed, the invoice stays`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice(positions = 3)
        newJob().importInvoiceGroup(listOf(storedPosition(id, 2), storedPosition(id, 3)))
        val invoice = eingangsrechnungDao.find(id)!!
        assertFalse(invoice.deleted, "Only the positions are gone, not the invoice.")
        assertEquals(listOf<Short>(1), invoice.positionen!!.filter { !it.deleted }.map { it.number })
    }

    @Test
    fun `invoice without any position left is deleted`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice(positions = 2)
        newJob().importInvoiceGroup(listOf(storedPosition(id, 1), storedPosition(id, 2)))
        assertTrue(eingangsrechnungDao.find(id)!!.deleted)
    }

    @Test
    fun `invoice missing in the import is deleted`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice(positions = 2)
        val header = EingangsrechnungPosImportDTO().also { it.id = id } // Header dto, no position number.
        newJob().importInvoiceGroup(listOf(ImportPairEntry(read = null, stored = header)))
        assertTrue(eingangsrechnungDao.find(id)!!.deleted)
    }

    private fun insertInvoice(positions: Int): Long {
        val invoice = EingangsrechnungDO()
        invoice.datum = LocalDate.now()
        invoice.faelligkeit = LocalDate.now()
        invoice.kreditor = "Import job test"
        repeat(positions) {
            invoice.addPosition(EingangsrechnungsPositionDO().also { pos ->
                pos.menge = BigDecimal.ONE
                pos.einzelNetto = BigDecimal.TEN
            })
        }
        return eingangsrechnungDao.insert(invoice)
    }

    private fun storedPosition(invoiceId: Long, number: Int): ImportPairEntry<EingangsrechnungPosImportDTO> {
        val dto = EingangsrechnungPosImportDTO().also {
            it.id = invoiceId
            it.positionNummer = number
            it.isPositionBasedImport = true
        }
        return ImportPairEntry(read = null, stored = dto)
    }

    private fun newJob(): EingangsrechnungImportJob {
        val storage = EingangsrechnungImportStorage(IncomingInvoiceImportRest.DATEV_IMPORT_SETTINGS)
        storage.isPositionBasedImport = true
        return EingangsrechnungImportJob(eingangsrechnungDao, emptyList(), storage)
    }
}
