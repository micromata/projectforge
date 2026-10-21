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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.springframework.context.ApplicationContext
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Parses small DATEV CSVs with [IncomingInvoiceCsvImporter] and reconciles them against mocked database invoices:
 * credit-card payment offsets, credit notes, plausibility warnings and the matching, which must never assign an
 * invoice of another date (the import overwrote older invoices of the same creditor before).
 */
class IncomingInvoiceCsvImporterTest {

    private var previousApplicationContext: ApplicationContext? = null

    private var databaseInvoices = listOf<EingangsrechnungDO>()

    @BeforeEach
    fun setUp() {
        // getByDateRange answers databaseInvoices; a default answer instead of a matcher based stub sidesteps
        // Kotlin's non-null parameter check on Mockito's any().
        val dao = Mockito.mock(EingangsrechnungDao::class.java) { invocation ->
            if (invocation.method.name == "getByDateRange") databaseInvoices else Mockito.RETURNS_DEFAULTS.answer(invocation)
        }
        previousApplicationContext = ApplicationContextProvider.getApplicationContext()
        val applicationContext = Mockito.mock(ApplicationContext::class.java)
        Mockito.`when`(applicationContext.getBean(EingangsrechnungDao::class.java)).thenReturn(dao)
        ApplicationContextProvider().setApplicationContext(applicationContext)
    }

    @AfterEach
    fun tearDown() {
        previousApplicationContext?.let { ApplicationContextProvider().setApplicationContext(it) }
    }

    @Test
    fun `credit-card payment offsets are no positions, their date fills a missing paid date`() {
        val storage = parsePositions(
            """"01.09.2026-30.09.2026";"R";"1.134,35";"EUR";"04.09.";"2026-14171";"OpsGenie Inc.";"6837";"04.09.2026"""",
            """"01.09.2026-30.09.2026";"G";"1.134,35";"EUR";"04.09.";"2026-14171";"OpsGenie Inc.";"1814";"04.09.2026"""",
            """"01.04.2026-30.04.2026";"R";"105,24";"USD";"07.04.";"4633DE7-0007";"Gather Presence, Inc";"6837";""""",
            """"01.06.2026-30.06.2026";"G";"169,33";"EUR";"08.06.";"4633DE7-0007";"Gather Presence, Inc";"1814";"08.06.2026"""",
            """"01.02.2026-28.02.2026";"R";"33,60";"EUR";"02.02.";"20982";"SearchApi LLC";"6837";"02.02.2026"""",
            """"01.02.2026-28.02.2026";"G";"33,60";"EUR";"02.02.";"20982";"SearchApi LLC";"1461";"02.02.2026"""",
        )
        assertEquals(3, storage.readInvoices.size, "Only the invoices are read, not the offsets (1814 and 1461).")
        val opsGenie = storage.readInvoices.single { it.referenz == "2026-14171" }
        assertEquals(0, BigDecimal("1134.35").compareTo(opsGenie.grossSum), "Not doubled by the offset.")
        val gather = storage.readInvoices.single { it.referenz == "4633DE7-0007" }
        assertEquals(LocalDate.of(2026, 6, 8), gather.bezahlDatum, "Missing paid date taken from the offset.")
        assertTrue(storage.errorList.isEmpty(), "No inconsistency anymore, got ${storage.errorList}")
        assertTrue(storage.warningList.any { it.startsWith("3 Zahlungs-Gegenbuchungen") }, "${storage.warningList}")
    }

    @Test
    fun `paid date of the invoice row wins over the offset`() {
        val storage = parsePositions(
            """"01.09.2026-30.09.2026";"R";"1.134,35";"EUR";"04.09.";"2026-14171";"OpsGenie Inc.";"6837";"05.09.2026"""",
            """"01.09.2026-30.09.2026";"G";"1.134,35";"EUR";"04.09.";"2026-14171";"OpsGenie Inc.";"1814";"04.09.2026"""",
        )
        assertEquals(LocalDate.of(2026, 9, 5), storage.readInvoices.single().bezahlDatum)
    }

    /** A position without paid date takes that of another position, not the offset's: else they would differ. */
    @Test
    fun `paid date of another position wins over the offset`() {
        val storage = parsePositions(
            """"01.04.2026-30.04.2026";"R";"100,00";"EUR";"07.04.";"EX-42";"Example Ltd";"6837";"07.04.2026"""",
            """"01.04.2026-30.04.2026";"R";"20,00";"EUR";"07.04.";"EX-42";"Example Ltd";"6837";""""",
            """"01.06.2026-30.06.2026";"G";"120,00";"EUR";"02.06.";"EX-42";"Example Ltd";"1814";"02.06.2026"""",
        )
        assertEquals(listOf(LocalDate.of(2026, 4, 7)), storage.readInvoices.map { it.bezahlDatum }.distinct())
        assertTrue(storage.errorList.isEmpty(), "No inconsistent paid dates, got ${storage.errorList}")
    }

    /** Positions paid on different days: the invoice is paid with the last one, a warning, no error. */
    @Test
    fun `differing paid dates of the positions take the latest`() {
        val storage = parsePositions(
            """"01.04.2026-30.04.2026";"R";"100,00";"EUR";"07.04.";"EX-42";"Example Ltd";"6837";"02.06.2026"""",
            """"01.04.2026-30.04.2026";"R";"20,00";"EUR";"07.04.";"EX-42";"Example Ltd";"6837";"07.04.2026"""",
        )
        assertTrue(storage.errorList.isEmpty(), "${storage.errorList}")
        assertTrue(storage.readInvoices.all { it.getErrors().isEmpty() }, "No position is faulty.")
        assertEquals(listOf(LocalDate.of(2026, 6, 2)), storage.readInvoices.map { it.bezahlDatum }.distinct())
        assertTrue(storage.warningList.any { it.contains("Unterschiedliche Zahldaten") }, "${storage.warningList}")
    }

    /** The paid date of a position-based import is taken, the amount paid is the gross sum of the invoice. */
    @Test
    fun `new invoice is paid with its gross sum`() {
        val storage = parsePositions(
            """"01.04.2026-30.04.2026";"R";"100,00";"EUR";"07.04.";"EX-42";"Example Ltd";"6837";"02.06.2026"""",
            """"01.04.2026-30.04.2026";"R";"20,50";"EUR";"07.04.";"EX-42";"Example Ltd";"6837";"02.06.2026"""",
        )
        storage.reconcileImportStorage()
        val reads = storage.pairEntries.mapNotNull { it.read }
        assertEquals(2, reads.size)
        assertTrue(reads.all { it.bezahlDatum == LocalDate.of(2026, 6, 2) }, "Paid date of the import is kept.")
        assertTrue(reads.all { BigDecimal("120.50").compareTo(it.zahlBetrag) == 0 }, "${reads.map { it.zahlBetrag }}")
    }

    /** Booked and cancelled: nothing to pay, and a paid date without amount would be refused by the dao. */
    @Test
    fun `invoice with a gross sum of zero gets no paid date`() {
        val storage = parsePositions(
            """"01.04.2026-30.04.2026";"G";"50,00";"EUR";"07.04.";"EX-43";"Example Ltd";"6837";"07.04.2026"""",
            """"01.04.2026-30.04.2026";"R";"50,00";"EUR";"07.04.";"EX-43";"Example Ltd";"6837";"07.04.2026"""",
        )
        storage.reconcileImportStorage()
        val reads = storage.pairEntries.mapNotNull { it.read }
        assertEquals(2, reads.size)
        assertTrue(reads.all { it.bezahlDatum == null && it.zahlBetrag == null }, "${reads.map { it.bezahlDatum }}")
    }

    @Test
    fun `credit note on an expense account is an invoice with a negative amount`() {
        val storage = parsePositions(
            """"01.11.2025-30.11.2025";"G";"51,78";"EUR";"05.11.";"15978";"OMG.de GmbH";"6846";"30.12.2025"""",
        )
        val creditNote = storage.readInvoices.single()
        assertEquals(0, BigDecimal("-51.78").compareTo(creditNote.grossSum))
        assertTrue(creditNote.netSum!!.signum() < 0)
    }

    @Test
    fun `paid date before invoice date is a warning`() {
        val storage = parsePositions(
            """"01.09.2026-30.09.2026";"R";"1.134,35";"EUR";"04.09.";"2026-14171";"OpsGenie Inc.";"6837";"09.04.2026"""",
        )
        assertTrue(storage.errorList.isEmpty())
        assertTrue(storage.warningList.any { it.contains("liegt vor dem Rechnungsdatum") }, "${storage.warningList}")
    }

    @Test
    fun `currency mixed with EUR is taken as EUR with a warning`() {
        val storage = parsePositions(
            """"01.04.2026-30.04.2026";"R";"105,24";"USD";"07.04.";"4633DE7-0007";"Gather Presence, Inc";"6837";"02.06.2026"""",
            """"01.04.2026-30.04.2026";"R";"169,33";"EUR";"07.04.";"4633DE7-0007";"Gather Presence, Inc";"6837";"02.06.2026"""",
        )
        assertTrue(storage.readInvoices.all { it.currency == "EUR" })
        assertTrue(storage.errorList.isEmpty(), "${storage.errorList}")
        assertTrue(storage.warningList.any { it.contains("EUR übernommen") }, "${storage.warningList}")
    }

    @Test
    fun `invoice of the same creditor a year before isn't matched, overwritten or deleted`() {
        val old = dbInvoice(1, "2025-10654", "OpsGenie Inc.", LocalDate.of(2025, 9, 4))
        val inPeriod = dbInvoice(2, "2026-99999", "Other Ltd.", LocalDate.of(2026, 9, 10))
        databaseInvoices = listOf(old, inPeriod)
        val storage = parsePositions(
            """"01.09.2026-30.09.2026";"R";"1.134,35";"EUR";"04.09.";"2026-14171";"OpsGenie Inc.";"6837";"04.09.2026"""",
        )
        storage.reconcileImportStorage()
        val opsGenie = storage.pairEntries.single { it.read?.referenz == "2026-14171" }
        assertNull(opsGenie.stored, "Must be NEW, not matched to the invoice of 2025.")
        assertTrue(storage.pairEntries.none { it.stored?.id == 1L }, "Outside the periods: not offered for deletion.")
        assertTrue(
            storage.pairEntries.any { it.read == null && it.stored?.id == 2L },
            "Unmatched invoice within the periods is offered for deletion."
        )
    }

    @Test
    fun `period with an en dash limits the deletions too`() {
        databaseInvoices = listOf(dbInvoice(1, "2026-99999", "Other Ltd.", LocalDate.of(2026, 8, 10)))
        val storage = parsePositions(
            """"01.09.2026 – 30.09.2026";"R";"1.134,35";"EUR";"04.09.";"2026-14171";"OpsGenie Inc.";"6837";"04.09.2026"""",
        )
        assertEquals(LocalDate.of(2026, 9, 1), storage.periodFrom)
        storage.reconcileImportStorage()
        assertTrue(storage.pairEntries.none { it.read == null }, "Outside the period: not offered for deletion.")
    }

    @Test
    fun `unparsable period offers no deletions`() {
        databaseInvoices = listOf(dbInvoice(1, "2026-99999", "Other Ltd.", LocalDate.of(2026, 9, 10)))
        val storage = parsePositions(
            """"September 2026";"R";"1.134,35";"EUR";"04.09.";"2026-14171";"OpsGenie Inc.";"6837";"04.09.2026"""",
        )
        assertNull(storage.periodFrom)
        storage.reconcileImportStorage()
        assertTrue(storage.pairEntries.none { it.read == null }, "Without a period nothing is offered for deletion.")
    }

    @Test
    fun `similar invoice number within days is still matched`() {
        databaseInvoices = listOf(dbInvoice(1, "3251246-10 / Az.: IS-0017-10/KSR", "ISICO GmbH", LocalDate.of(2025, 5, 2)))
        val storage = parsePositions(
            """"01.05.2025-31.05.2025";"R";"746,13";"EUR";"05.05.";"325124610";"ISICO Datenschutz GmbH";"6837";"""""
        )
        storage.reconcileImportStorage()
        assertEquals(1L, storage.pairEntries.single { it.read != null }.stored?.id)
    }

    @Test
    fun `header-only import offers no deletions`() {
        databaseInvoices = listOf(dbInvoice(1, "2025-10654", "OpsGenie Inc.", LocalDate.of(2025, 9, 4)))
        val storage = parse(
            "Belegart;Geschäftspartner-Name;Rechnungsbetrag;WKZ;Rechnungs-Nr.;Rechnungsdatum;Konto;BezahltAm",
            """"R";"OpsGenie Inc.";"1.134,35";"EUR";"2026-14171";"04.09.2026";"6837";"04.09.2026"""",
            """"G";"OpsGenie Inc.";"-1.134,35";"EUR";"2026-14171";"04.09.2026";"1814";"04.09.2026"""",
            """"R";"Gather Presence, Inc";"105,24";"USD";"4633DE7-0007";"03.09.2025";"6837";"""""
        )
        assertEquals(2, storage.readInvoices.size, "Offset skipped in header-only exports too.")
        storage.reconcileImportStorage()
        assertTrue(storage.pairEntries.none { it.read == null }, "No DELETED entries in header-only mode.")
        assertTrue(storage.pairEntries.none { it.stored != null }, "Nothing matches the invoice of 2025.")
    }

    private fun parsePositions(vararg rows: String): EingangsrechnungImportStorage {
        return parse("Periode;Belegart;Betrag;Währung;Datum;RENR;LieferantName;Konto;gezahlt_am", *rows)
    }

    private fun parse(header: String, vararg rows: String): EingangsrechnungImportStorage {
        val storage = EingangsrechnungImportStorage(IncomingInvoiceImportRest.DATEV_IMPORT_SETTINGS)
        val csv = (listOf(header) + rows).joinToString("\n")
        IncomingInvoiceCsvImporter(Mockito.mock(KostCache::class.java), Mockito.mock(KontoCache::class.java))
            .parse(csv.byteInputStream(), storage, Charsets.UTF_8)
        return storage
    }

    private fun dbInvoice(id: Long, referenz: String, kreditor: String, datum: LocalDate): EingangsrechnungDO {
        return EingangsrechnungDO().also {
            it.id = id
            it.referenz = referenz
            it.kreditor = kreditor
            it.datum = datum
            it.positionen = mutableListOf()
        }
    }
}
