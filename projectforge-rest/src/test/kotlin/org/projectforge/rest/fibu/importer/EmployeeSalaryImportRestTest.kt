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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeSalaryDao
import org.projectforge.business.fibu.EmployeeSalaryService
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.framework.persistence.user.entities.PFUserDO
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
import java.time.Month

/**
 * Exercises the layout-free employee-salary (Gehaltsimport) xlsx import without a Spring context or a
 * database: the [EmployeeSalaryExcelImporter] parses a tiny in-memory .xlsx built to mirror the real
 * tax-office export (a sheet whose name is truncated to 31 chars, the `Abrechnungsmonat` / `Pers.Nr.` /
 * `*Gesamtbrutto m. bAV AG-Anteil` head row, one resolvable staff number and the synthetic `99998`
 * aggregate row), the collaborators that turn a staff number into an employee are mocked, and the
 * reconcile's database side ([EmployeeSalaryService]) is a mocked bean answering an empty accounting month
 * (so the resolvable row is NEW). The [JobHandler] echoes the job back instead of starting a thread.
 */
class EmployeeSalaryImportRestTest {

    private var previousApplicationContext: ApplicationContext? = null

    private lateinit var employeeSalaryDao: EmployeeSalaryDao
    private lateinit var employeeCache: EmployeeCache
    private lateinit var employeeService: EmployeeService
    private lateinit var employeeSalaryService: EmployeeSalaryService

    @BeforeEach
    fun setUp() {
        employeeSalaryDao = Mockito.mock(EmployeeSalaryDao::class.java)
        // The cache resolves staff number 1001 to an employee; every other number (the 99998 aggregate row)
        // falls through to the service, which finds nothing, so the row stays FAULTY.
        employeeCache = Mockito.mock(EmployeeCache::class.java)
        Mockito.`when`(employeeCache.findByStaffNumber(1001)).thenReturn(employee(id = 42L, staffNumber = "1001"))
        employeeService = Mockito.mock(EmployeeService::class.java)

        // Reconcile loads the month's database salaries via the statically held application context; hand it a
        // mocked service answering an empty month, so the resolvable row is NEW. A default answer is used
        // instead of a matcher based stub because Kotlin's non-null parameter check rejects Mockito's any().
        employeeSalaryService = Mockito.mock(EmployeeSalaryService::class.java) { invocation ->
            if (invocation.method.name == "selectByMonth") emptyList<Any>() else Mockito.RETURNS_DEFAULTS.answer(invocation)
        }
        previousApplicationContext = ApplicationContextProvider.getApplicationContext()
        val applicationContext = Mockito.mock(ApplicationContext::class.java)
        Mockito.`when`(applicationContext.getBean(EmployeeSalaryService::class.java)).thenReturn(employeeSalaryService)
        // Reconcile resolves the stored salary's employee display from the cache, not the lazy association.
        Mockito.`when`(applicationContext.getBean(EmployeeCache::class.java)).thenReturn(employeeCache)
        ApplicationContextProvider().setApplicationContext(applicationContext)
    }

    @AfterEach
    fun tearDown() {
        // Restore whatever context was held before, so a following Spring based test is unaffected.
        previousApplicationContext?.let { ApplicationContextProvider().setApplicationContext(it) }
    }

    @Test
    fun `parse reads the accounting month and gross, and flags the unresolved aggregate row`() {
        val storage = EmployeeSalaryImportStorage()
        storage.filename = "salaries.xlsx"
        EmployeeSalaryExcelImporter(employeeCache, employeeService).parse(buildXlsx().inputStream(), storage)

        assertEquals(2, storage.readSalaries.size, "Both data rows are read; only the empty trailing row is skipped.")

        val resolved = storage.readSalaries.first { it.staffNumber == 1001 }
        assertEquals(2026, resolved.year, "Year is read from the Abrechnungsmonat cell '2026/08'.")
        assertEquals(8, resolved.month, "Month is read from the Abrechnungsmonat cell '2026/08'.")
        assertEquals(
            0,
            BigDecimal("5817.30").compareTo(resolved.bruttoMitAgAnteil),
            "The imported value is the Gesamtkosten column (5817.30), not the *Gesamtbrutto column (4200.50).",
        )
        assertEquals(42L, resolved.employeeId, "The staff number resolved to the mocked employee's id.")
        assertNotNull(resolved.employee, "The resolved employee's display name is kept for the preview.")
        assertTrue(resolved.getErrors().isEmpty(), "A fully resolved row carries no error.")

        val aggregate = storage.readSalaries.first { it.staffNumber == 99998 }
        assertNull(aggregate.employeeId, "The 99998 aggregate row resolves to no employee.")
        assertFalse(aggregate.getErrors().isEmpty(), "The unresolved aggregate row carries an error, so it is FAULTY.")
    }

    @Test
    fun `upload, reconcile and commit round-trip`() {
        val enqueuedJobs = mutableListOf<AbstractJob>()
        val jobHandler = Mockito.mock(JobHandler::class.java) { invocation ->
            if (invocation.method.name == "addJob") {
                val job = invocation.getArgument<AbstractJob>(0)
                enqueuedJobs.add(job)
                job
            } else {
                Mockito.RETURNS_DEFAULTS.answer(invocation)
            }
        }

        val rest = EmployeeSalaryImportRest()
        setField(rest, "employeeSalaryDao", employeeSalaryDao)
        setField(rest, "employeeCache", employeeCache)
        setField(rest, "employeeService", employeeService)
        setField(rest, "jobHandler", jobHandler)

        val request = MockHttpServletRequest()
        request.setSession(MockHttpSession())

        // --- upload ---
        val file = MockMultipartFile("file", "salaries.xlsx", XLSX_CONTENT_TYPE, buildXlsx())
        val uploadResponse = rest.upload(request, file)
        assertEquals(HttpStatus.OK, uploadResponse.statusCode, "Upload should succeed.")
        val uploadView = uploadResponse.body as ImportView<*>
        assertEquals("salaries.xlsx", uploadView.filename)
        assertFalse(uploadView.hasBeenReconciled, "Freshly uploaded storage is not reconciled yet.")
        assertEquals(2, uploadView.entries.size, "The two data rows yield two entries.")

        // --- state (round-trips through the session) ---
        val stateView = rest.state(request)
        assertEquals("salaries.xlsx", stateView.filename)
        assertEquals(2, stateView.entries.size)

        // --- reconcile ---
        val reconcileResponse = rest.reconcile(request, ImportStorage.DisplayOptions())
        assertEquals(HttpStatus.OK, reconcileResponse.statusCode)
        val reconcileView = reconcileResponse.body as ImportView<*>
        assertTrue(reconcileView.hasBeenReconciled, "After reconcile the flag is set.")
        assertEquals(2, reconcileView.entries.size)
        val newEntry = reconcileView.entries.first { it.status == ImportEntry.Status.NEW }
        val faultyEntry = reconcileView.entries.first { it.status == ImportEntry.Status.FAULTY }
        assertNull(newEntry.oldDiffValues, "A NEW entry has no stored counterpart, so no diff values.")
        assertNotNull(faultyEntry.error, "The unresolved aggregate row is FAULTY through its carried error.")

        // --- commit (only the importable NEW row is selected; the FAULTY row is left out) ---
        val commitData = AbstractImportRest.CommitData(selectedIds = listOf(newEntry.id))
        val commitResponse = rest.commit(request, commitData)
        assertEquals(HttpStatus.OK, commitResponse.statusCode)
        @Suppress("UNCHECKED_CAST")
        val body = commitResponse.body as Map<String, Any>
        val jobId = body["jobId"] as Int
        assertTrue(jobId > 0, "A job should have been enqueued and its id returned, got $jobId.")
        assertEquals(1, enqueuedJobs.size, "The job was handed to the JobHandler exactly once.")
        assertEquals(jobId, enqueuedJobs.first().id)
    }

    /**
     * Builds a tiny .xlsx mirroring the real tax-office export: a sheet whose name is the 31-char-truncated
     * `Aktueller Monat NB im Abrechnun`, and — beside `Abrechnungsmonat` / `Pers.Nr.` — both value columns
     * of the real file: the narrower `*Gesamtbrutto m. bAV AG-Anteil` (which must be ignored) and the
     * `Gesamtkosten` total (the one imported into bruttoMitAgAnteil). One resolvable row (staff number
     * 1001), the synthetic 99998 aggregate row, and one empty trailing row that must be skipped.
     */
    private fun buildXlsx(): ByteArray {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Aktueller Monat NB im Abrechnun")
            sheet.createRow(0).let { head ->
                head.createCell(0).setCellValue("Abrechnungsmonat")
                head.createCell(1).setCellValue("Pers.Nr.")
                head.createCell(2).setCellValue("*Gesamtbrutto m. bAV AG-Anteil")
                head.createCell(3).setCellValue("Gesamtkosten")
            }
            sheet.createRow(1).let { row ->
                row.createCell(0).setCellValue("2026/08")
                row.createCell(1).setCellValue(1001.0)
                row.createCell(2).setCellValue(4200.50) // *Gesamtbrutto — must NOT be the imported value.
                row.createCell(3).setCellValue(5817.30) // Gesamtkosten — the imported value.
            }
            sheet.createRow(2).let { row ->
                row.createCell(0).setCellValue("2026/08")
                row.createCell(1).setCellValue(99998.0)
                row.createCell(2).setCellValue(1200000.00)
                row.createCell(3).setCellValue(1234567.89)
            }
            sheet.createRow(3) // Empty trailing row, must be skipped.
            return ByteArrayOutputStream().use { out ->
                workbook.write(out)
                out.toByteArray()
            }
        }
    }

    private fun employee(id: Long, staffNumber: String): EmployeeDO {
        return EmployeeDO().also {
            it.id = id
            it.staffNumber = staffNumber
            it.user = PFUserDO().apply {
                firstname = "Test"
                lastname = "Employee $staffNumber"
            }
        }
    }

    private fun setField(target: Any, name: String, value: Any) {
        val field = target.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(target, value)
    }

    companion object {
        private const val XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
}
