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

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.PfCaches
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.SHType
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.rest.importer.AbstractImportRest
import org.projectforge.rest.importer.ImportEntry
import org.projectforge.rest.importer.ImportStorage
import org.projectforge.rest.importer.ImportView
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockMultipartFile
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Exercises the DATEV import (accounting records and chart of accounts) without a Spring context or a database.
 * The in-memory workbook mimics the tax office's original file: the month sheet `07` (with string dates, the
 * duplicated `S/H` head and string cost units), the report sheets `07_BWA` and `07_SuSa` (whose head row also
 * carries `Konto`/`Beschriftung`, in the second row) and the month-prefixed `07_Kontenplan`. All data is synthetic.
 */
class DatevImportRestTest {

    private var previousApplicationContext: ApplicationContext? = null

    private lateinit var kontoCache: KontoCache
    private lateinit var kostCache: KostCache
    private lateinit var buchungssatzDao: BuchungssatzDao
    private val storedRecords = mutableListOf<BuchungssatzDO>()

    private val konto4400 = konto(1L, 4400, "Erlöse")
    private val konto1200 = konto(2L, 1200, "Bank")
    private val kost1 = Kost1DO().also {
        it.id = 11L
        it.nummernkreis = 1
        it.bereich = 400
        it.teilbereich = 1
        it.endziffer = 0
    }
    private val kost2 = Kost2DO().also {
        it.id = 21L
        it.nummernkreis = 5
        it.bereich = 0
        it.teilbereich = 1
        it.kost2Art = Kost2ArtDO().also { art -> art.id = 1L }
    }

    @BeforeEach
    fun setUp() {
        kontoCache = Mockito.mock(KontoCache::class.java)
        Mockito.`when`(kontoCache.findKontoByNumber(4400)).thenReturn(konto4400)
        Mockito.`when`(kontoCache.findKontoByNumber(1200)).thenReturn(konto1200)
        Mockito.`when`(kontoCache.getKonto(1L)).thenReturn(konto4400)
        Mockito.`when`(kontoCache.getKonto(2L)).thenReturn(konto1200)
        kostCache = Mockito.mock(KostCache::class.java)
        Mockito.`when`(kostCache.getKost1("14000100")).thenReturn(kost1)
        Mockito.`when`(kostCache.getKost2("50000101")).thenReturn(kost2)
        Mockito.`when`(kostCache.getKost1(11L)).thenReturn(kost1)
        Mockito.`when`(kostCache.getKost2(21L)).thenReturn(kost2)
        // Answered by method name, because Kotlin's non-null parameter check rejects Mockito's any().
        buchungssatzDao = Mockito.mock(BuchungssatzDao::class.java) { invocation ->
            if (invocation.method.name == "selectByMonth") storedRecords.toList()
            else Mockito.RETURNS_DEFAULTS.answer(invocation)
        }
        previousApplicationContext = ApplicationContextProvider.getApplicationContext()
        val applicationContext = Mockito.mock(ApplicationContext::class.java)
        Mockito.`when`(applicationContext.getBean(KontoCache::class.java)).thenReturn(kontoCache)
        Mockito.`when`(applicationContext.getBean(KostCache::class.java)).thenReturn(kostCache)
        Mockito.`when`(applicationContext.getBean(BuchungssatzDao::class.java)).thenReturn(buchungssatzDao)
        ApplicationContextProvider().setApplicationContext(applicationContext)
    }

    @AfterEach
    fun tearDown() {
        previousApplicationContext?.let { ApplicationContextProvider().setApplicationContext(it) }
    }

    @Test
    fun `records of the month sheet are read, report sheets are ignored, later dates stay in the batch`() {
        val storage = parseRecords()
        assertEquals(4, storage.readRecords.size, "Only the 4 data rows of sheet 07 are read.")
        storage.readRecords.forEach {
            assertEquals(2026, it.year, "Booking year of record #${it.satznr}")
            assertEquals(7, it.month, "Booking month of record #${it.satznr}")
        }

        val july = storage.readRecords.first { it.satznr == 1 }
        assertTrue(july.getErrors().isEmpty(), "Errors: ${july.getErrors()}")
        assertEquals(LocalDate.of(2026, 7, 15), july.datum)
        assertNull(july.dateHint)
        assertEquals(0, BigDecimal("-1234.56").compareTo(july.betrag), "Debit (S) is stored negative.")
        assertEquals(4400, july.konto)
        assertEquals(1L, july.kontoId)
        assertEquals(1200, july.gegenKonto)
        assertEquals("1.400.01.00", july.kost1)
        assertEquals("5.000.01.01", july.kost2)
        assertEquals("Rechnung 4711", july.text)
        assertEquals("RE-4711", july.beleg)
        assertEquals("88,25", july.menge, "Quantity formatted as the legacy import stored it.")

        val august = storage.readRecords.first { it.satznr == 2 }
        assertTrue(august.getErrors().isEmpty(), "A later voucher date is no error: ${august.getErrors()}")
        assertEquals(LocalDate.of(2026, 8, 13), august.datum, "The real voucher date is kept.")
        assertNotNull(august.dateHint, "A later voucher date carries a hint.")
        assertEquals(0, BigDecimal("250.50").compareTo(august.betrag), "Credit (H) is stored positive.")

        val june = storage.readRecords.first { it.satznr == 3 }
        assertTrue(june.getErrors().isEmpty())
        assertNotNull(june.dateHint)

        val outOfRange = storage.readRecords.first { it.satznr == 4 }
        assertFalse(outOfRange.getErrors().isEmpty(), "A date more than 12 months away is an error.")
    }

    @Test
    fun `booking year falls back to the file name`() {
        assertEquals(2026 to 7, DatevRecordExcelImporter.parseFileMonth("2026-07_-_FiBu_Auswertungen_Steuerbüro.xlsx"))
        assertNull(DatevRecordExcelImporter.parseFileMonth("Buchungssaetze.xlsx"))
    }

    @Test
    fun `lenient cell parsing`() {
        assertEquals(0, BigDecimal("1234.56").compareTo(DatevRecordExcelImporter.parseNumber("1.234,56")))
        assertEquals(0, BigDecimal("1234.56").compareTo(DatevRecordExcelImporter.parseNumber("1234.56")))
        assertEquals(LocalDate.of(2026, 8, 13), DatevRecordExcelImporter.parseDate("13.08.2026"))
        assertEquals(LocalDate.of(2026, 8, 3), DatevRecordExcelImporter.parseDate("3.8.26"))
        assertEquals(LocalDate.of(2026, 8, 13), DatevRecordExcelImporter.parseDate("2026-08-13"))
        assertNull(DatevRecordExcelImporter.parseDate("13/08/2026"))
    }

    @Test
    fun `records upload, reconcile and commit round-trip`() {
        // Record #1 is already stored with the same values, record #2 with another amount.
        storedRecords.add(stored(id = 100L, satznr = 1, betrag = "-1234.56", datum = LocalDate.of(2026, 7, 15)))
        storedRecords.add(stored(id = 101L, satznr = 2, betrag = "99.00", datum = LocalDate.of(2026, 8, 13)))
        val enqueuedJobs = mutableListOf<AbstractJob>()
        val rest = DatevRecordImportRest()
        setField(rest, "accessChecker", Mockito.mock(AccessChecker::class.java))
        setField(rest, "buchungssatzDao", buchungssatzDao)
        setField(rest, "kontoCache", kontoCache)
        setField(rest, "kostCache", kostCache)
        setField(rest, "caches", Mockito.mock(PfCaches::class.java))
        setField(rest, "configurationService", configurationService())
        setField(rest, "jobHandler", echoJobHandler(enqueuedJobs))
        val request = MockHttpServletRequest()
        request.setSession(MockHttpSession())

        val file = MockMultipartFile("file", FILENAME, XLSX_CONTENT_TYPE, buildXlsx())
        val uploadResponse = rest.upload(request, file)
        assertEquals(HttpStatus.OK, uploadResponse.statusCode, "Upload should succeed: ${uploadResponse.body}")
        assertEquals(4, (uploadResponse.body as ImportView<*>).entries.size)

        val reconcileView = rest.reconcile(request, ImportStorage.DisplayOptions(unmodified = true)).body as ImportView<*>
        val bySatznr = reconcileView.entries.associateBy { (it.read as DatevRecordImportDTO).satznr }
        assertEquals(ImportEntry.Status.UNMODIFIED, bySatznr[1]!!.status)
        assertEquals(ImportEntry.Status.MODIFIED, bySatznr[2]!!.status)
        assertEquals(ImportEntry.Status.NEW, bySatznr[3]!!.status)
        assertEquals(ImportEntry.Status.FAULTY, bySatznr[4]!!.status)

        val commitData = AbstractImportRest.CommitData(selectedIds = listOf(bySatznr[2]!!.id, bySatznr[3]!!.id))
        val commitResponse = rest.commit(request, commitData)
        assertEquals(HttpStatus.OK, commitResponse.statusCode)
        assertEquals(1, enqueuedJobs.size)
        assertTrue(enqueuedJobs.first() is DatevRecordImportJob)
    }

    @Test
    fun `chart of accounts is read from the month prefixed sheet, not from the balance list`() {
        val storage = DatevAccountImportStorage { listOf(konto(1L, 4400, "Erlöse alt")) }
        storage.filename = FILENAME
        DatevAccountExcelImporter().parse(buildXlsx().inputStream(), storage)
        assertEquals(listOf(4400, 8400), storage.readAccounts.map { it.nummer })
        assertEquals("Erlöse 19 % USt", storage.readAccounts.first().bezeichnung)

        storage.reconcileImportStorage()
        val byNummer = storage.pairEntries.associateBy { it.read!!.nummer }
        assertEquals(ImportEntry.Status.MODIFIED, byNummer[4400]!!.status)
        assertEquals(1L, byNummer[4400]!!.stored!!.id)
        assertEquals(ImportEntry.Status.NEW, byNummer[8400]!!.status)
    }

    @Test
    fun `chart of accounts upload round-trip`() {
        val persistenceService = Mockito.mock(PfPersistenceService::class.java) { invocation ->
            if (invocation.method.name == "executeQuery") emptyList<KontoDO>()
            else Mockito.RETURNS_DEFAULTS.answer(invocation)
        }
        val applicationContext = ApplicationContextProvider.getApplicationContext()
        Mockito.`when`(applicationContext.getBean(PfPersistenceService::class.java)).thenReturn(persistenceService)
        val enqueuedJobs = mutableListOf<AbstractJob>()
        val rest = DatevAccountImportRest()
        setField(rest, "accessChecker", Mockito.mock(AccessChecker::class.java))
        setField(rest, "kontoDao", Mockito.mock(KontoDao::class.java))
        setField(rest, "configurationService", configurationService())
        setField(rest, "jobHandler", echoJobHandler(enqueuedJobs))
        val request = MockHttpServletRequest()
        request.setSession(MockHttpSession())

        val file = MockMultipartFile("file", FILENAME, XLSX_CONTENT_TYPE, buildXlsx())
        assertEquals(HttpStatus.OK, rest.upload(request, file).statusCode)
        val reconcileView = rest.reconcile(request, ImportStorage.DisplayOptions()).body as ImportView<*>
        assertEquals(2, reconcileView.entries.size)
        assertTrue(reconcileView.entries.all { it.status == ImportEntry.Status.NEW })
        val commitData = AbstractImportRest.CommitData(selectedIds = reconcileView.entries.map { it.id })
        assertEquals(HttpStatus.OK, rest.commit(request, commitData).statusCode)
        assertTrue(enqueuedJobs.single() is DatevAccountImportJob)
    }

    @Test
    fun `a file without chart of accounts is rejected, the balance list is never taken`() {
        val storage = DatevAccountImportStorage { emptyList() }
        storage.filename = FILENAME
        assertThrows(IllegalArgumentException::class.java) {
            DatevAccountExcelImporter().parse(buildXlsx(withKontenplan = false).inputStream(), storage)
        }
    }

    @Test
    fun `cost unit tooltips as in the former Wicket import`() {
        val art = Kost2ArtDO().also {
            it.id = 1L
            it.name = "Entwicklung"
        }
        val described = Kost2DO().also {
            it.id = 22L
            it.description = "Wartung"
            it.kost2Art = art
        }
        val caches = Mockito.mock(PfCaches::class.java)
        Mockito.`when`(caches.getProjektByKost2(22L)).thenReturn(ProjektDO().also { it.name = "Portal" })
        Mockito.`when`(caches.getKundeByKost2(22L)).thenReturn(KundeDO().also { it.identifier = "ACME" })
        Mockito.`when`(caches.getKost2ArtIfNotInitialized(art)).thenReturn(art)
        assertEquals("Wartung\nACME - Portal\n01 - Entwicklung", DatevRecordExcelImporter.kost2Tooltip(described, caches))
        // Without a project only the description, as before; nothing at all gives no tooltip.
        assertEquals("Wartung", DatevRecordExcelImporter.kost2Tooltip(described, null))
        assertNull(DatevRecordExcelImporter.kost2Tooltip(kost2, Mockito.mock(PfCaches::class.java)))
    }

    private fun parseRecords(): DatevRecordImportStorage {
        val storage = DatevRecordImportStorage { _, _ -> emptyList() }
        storage.filename = FILENAME
        DatevRecordExcelImporter(kontoCache, kostCache).parse(buildXlsx().inputStream(), storage)
        return storage
    }

    private fun echoJobHandler(enqueuedJobs: MutableList<AbstractJob>): JobHandler {
        return Mockito.mock(JobHandler::class.java) { invocation ->
            if (invocation.method.name == "addJob") {
                invocation.getArgument<AbstractJob>(0).also { enqueuedJobs.add(it) }
            } else {
                Mockito.RETURNS_DEFAULTS.answer(invocation)
            }
        }
    }

    private fun stored(id: Long, satznr: Int, betrag: String, datum: LocalDate): BuchungssatzDO {
        return BuchungssatzDO().also {
            it.id = id
            it.year = 2026
            it.month = 7
            it.satznr = satznr
            it.betrag = BigDecimal(betrag)
            it.sh = if (betrag.startsWith("-")) SHType.SOLL else SHType.HABEN
            it.datum = datum
            it.konto = konto4400
            it.gegenKonto = konto1200
            it.kost1 = kost1
            it.kost2 = kost2
            it.beleg = if (satznr == 1) "RE-4711" else null
            it.menge = if (satznr == 1) "88,25" else null
            it.text = if (satznr == 1) "Rechnung 4711" else null
        }
    }

    /** Synthetic workbook in the layout of the tax office's original file. */
    private fun buildXlsx(withKontenplan: Boolean = true): ByteArray {
        XSSFWorkbook().use { workbook ->
            val records = workbook.createSheet("07")
            val heads = listOf(
                "Satz-Nr.", "Betrag", "S/H", "Konto", "Kst.", "Menge", "S/H", "Beleg", "Datum", "Gegenkonto",
                "Text", "Alt.-Kst.", "Beleg 2", "KR-BSNr.", "ZI",
            )
            records.createRow(0).let { row -> heads.forEachIndexed { i, head -> row.createCell(i).setCellValue(head) } }
            fun record(rowNum: Int, satznr: Int, betrag: Double, sh: String, datum: String, text: String?, beleg: String?) {
                records.createRow(rowNum).let { row ->
                    row.createCell(0).setCellValue(satznr.toDouble())
                    row.createCell(1).setCellValue(betrag)
                    row.createCell(2).setCellValue(sh)
                    row.createCell(3).setCellValue(4400.0)
                    row.createCell(4).setCellValue("50000101")
                    if (satznr == 1) row.createCell(5).setCellValue(88.25)
                    row.createCell(6).setCellValue("X") // Second S/H column, must not be read.
                    beleg?.let { row.createCell(7).setCellValue(it) }
                    row.createCell(8).setCellValue(datum)
                    row.createCell(9).setCellValue(1200.0)
                    text?.let { row.createCell(10).setCellValue(it) }
                    row.createCell(11).setCellValue("14000100")
                }
            }
            record(1, 1, 1234.56, "S", "15.07.2026", "Rechnung 4711", "RE-4711")
            record(2, 2, 250.5, "H", "13.08.2026", null, null)
            record(3, 3, 10.0, "S", "10.06.2026", null, null)
            record(4, 4, 10.0, "S", "01.01.2024", null, null)
            records.createRow(5) // Empty trailing row.

            workbook.createSheet("07_BWA").createRow(0).createCell(0).setCellValue("BWA Juli 2026")
            workbook.createSheet("07_SuSa").let { susa ->
                susa.createRow(0).createCell(0).setCellValue("Summen- und Saldenliste")
                susa.createRow(1).let { row ->
                    listOf("Konto", "Beschriftung", "EB-Wert", "Saldo").forEachIndexed { i, head ->
                        row.createCell(i).setCellValue(head)
                    }
                }
                susa.createRow(2).let { row ->
                    row.createCell(0).setCellValue(1200.0)
                    row.createCell(1).setCellValue("Bank")
                    row.createCell(2).setCellValue(1.0)
                    row.createCell(3).setCellValue(2.0)
                }
            }
            if (withKontenplan) {
                workbook.createSheet("07_Kontenplan").let { plan ->
                    plan.createRow(0).let { row ->
                        row.createCell(0).setCellValue("Konto")
                        row.createCell(1).setCellValue("Beschriftung")
                    }
                    plan.createRow(1).let { row ->
                        row.createCell(0).setCellValue(4400.0)
                        row.createCell(1).setCellValue("Erlöse 19 % USt")
                    }
                    plan.createRow(2).let { row ->
                        row.createCell(0).setCellValue(8400.0)
                        row.createCell(1).setCellValue("Erlöse neu")
                    }
                }
            }
            return ByteArrayOutputStream().use { out ->
                workbook.write(out)
                out.toByteArray()
            }
        }
    }

    private fun konto(id: Long, nummer: Int, bezeichnung: String): KontoDO {
        return KontoDO().also {
            it.id = id
            it.nummer = nummer
            it.bezeichnung = bezeichnung
        }
    }

    /** The upload limit of both DATEV imports, as configured by default (`projectforge.max-file-size.datev`). */
    private fun configurationService(): ConfigurationService =
        Mockito.mock(ConfigurationService::class.java).also {
            Mockito.`when`(it.maxFileSizeDatev).thenReturn("10MB")
        }

    private fun setField(target: Any, name: String, value: Any) {
        val field = target.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(target, value)
    }

    companion object {
        private const val FILENAME = "2026-07_-_FiBu_Auswertungen_Steuerbuero.xlsx"
        private const val XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
}
