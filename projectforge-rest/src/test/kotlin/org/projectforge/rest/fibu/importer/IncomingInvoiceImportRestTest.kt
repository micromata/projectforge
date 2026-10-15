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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.importer.AbstractImportRest
import org.projectforge.rest.importer.ImportEntry
import org.projectforge.rest.importer.ImportStorage
import org.projectforge.rest.importer.ImportView
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockMultipartFile

/**
 * Round-trips a tiny in-memory DATEV CSV through the layout-free [IncomingInvoiceImportRest]:
 * upload -> reconcile -> commit. No Spring context and no database: the collaborators are mocked, the
 * database side of the reconciliation is a mocked [EingangsrechnungDao] answering an empty date range (so
 * every imported invoice is NEW), and the [JobHandler] simply echoes the job back instead of starting it.
 */
class IncomingInvoiceImportRestTest {

    private var previousApplicationContext: ApplicationContext? = null

    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @BeforeEach
    fun setUp() {
        // The reconcile step loads database invoices via the statically held application context; hand it a
        // mocked DAO. Its getByDateRange returns nothing (the storage treats that as an empty date range),
        // so every imported invoice is NEW. No argument matcher is used, because Kotlin's non-null parameter
        // check rejects Mockito's any() there.
        eingangsrechnungDao = Mockito.mock(EingangsrechnungDao::class.java)
        previousApplicationContext = ApplicationContextProvider.getApplicationContext()
        val applicationContext = Mockito.mock(ApplicationContext::class.java)
        Mockito.`when`(applicationContext.getBean(EingangsrechnungDao::class.java)).thenReturn(eingangsrechnungDao)
        ApplicationContextProvider().setApplicationContext(applicationContext)
    }

    @AfterEach
    fun tearDown() {
        // Restore whatever context was held before, so a following Spring based test is unaffected.
        previousApplicationContext?.let { ApplicationContextProvider().setApplicationContext(it) }
    }

    @Test
    fun `upload, reconcile and commit round-trip`() {
        // Echo the passed job back (so import() returns its auto-assigned id without starting a thread) and
        // record it, using a default answer instead of a matcher based stub to sidestep Kotlin's non-null
        // parameter check on Mockito's any().
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

        val rest = IncomingInvoiceImportRest()
        setField(rest, "accessChecker", Mockito.mock(AccessChecker::class.java))
        setField(rest, "kostCache", Mockito.mock(KostCache::class.java))
        setField(rest, "kontoCache", Mockito.mock(KontoCache::class.java))
        setField(rest, "jobHandler", jobHandler)
        setField(rest, "eingangsrechnungDao", eingangsrechnungDao)

        val request = MockHttpServletRequest()
        request.setSession(MockHttpSession())

        // --- upload ---
        val csv = """
            Rechnungsdatum;Rechnungs-Nr.;Geschäftspartner-Name;Rechnungsbetrag;WKZ
            02.05.2025;INV-001;ACME GmbH;746,13;EUR
        """.trimIndent()
        val file = MockMultipartFile("file", "invoices.csv", "text/csv", csv.toByteArray(Charsets.UTF_8))

        val uploadResponse = rest.upload(request, file)
        assertEquals(HttpStatus.OK, uploadResponse.statusCode, "Upload should succeed.")
        val uploadView = uploadResponse.body as ImportView<*>
        assertEquals("invoices.csv", uploadView.filename)
        assertFalse(uploadView.hasBeenReconciled, "Freshly uploaded storage is not reconciled yet.")
        assertEquals(1, uploadView.entries.size, "The single CSV row yields one entry.")
        assertEquals(1, uploadView.info?.totalNumber)
        // No Periode column -> header-only import.
        assertEquals(false, uploadView.meta?.get("isPositionBasedImport"))

        // --- state (round-trips through the session) ---
        val stateView = rest.state(request)
        assertEquals("invoices.csv", stateView.filename)
        assertEquals(1, stateView.entries.size)

        // --- reconcile ---
        val reconcileResponse = rest.reconcile(request, ImportStorage.DisplayOptions())
        assertEquals(HttpStatus.OK, reconcileResponse.statusCode)
        val reconcileView = reconcileResponse.body as ImportView<*>
        assertTrue(reconcileView.hasBeenReconciled, "After reconcile the flag is set.")
        assertEquals(1, reconcileView.entries.size)
        val entry = reconcileView.entries.first()
        assertEquals(ImportEntry.Status.NEW, entry.status, "No matching DB invoice -> NEW.")
        assertNull(entry.oldDiffValues, "A NEW entry has no stored counterpart, so no diff values.")
        assertEquals(1, reconcileView.info?.numberOfNewEntries)

        // --- commit ---
        val commitData = AbstractImportRest.CommitData(selectedIds = listOf(entry.id))
        val commitResponse = rest.commit(request, commitData)
        assertEquals(HttpStatus.OK, commitResponse.statusCode)
        @Suppress("UNCHECKED_CAST")
        val body = commitResponse.body as Map<String, Any>
        val jobId = body["jobId"] as Int
        assertTrue(jobId > 0, "A job should have been enqueued and its id returned, got $jobId.")
        // The job was handed to the JobHandler exactly once.
        assertEquals(1, enqueuedJobs.size)
        assertEquals(jobId, enqueuedJobs.first().id)
    }

    @Test
    fun `commit without a selection is rejected`() {
        val rest = IncomingInvoiceImportRest()
        setField(rest, "accessChecker", Mockito.mock(AccessChecker::class.java))
        setField(rest, "kostCache", Mockito.mock(KostCache::class.java))
        setField(rest, "kontoCache", Mockito.mock(KontoCache::class.java))
        setField(rest, "jobHandler", Mockito.mock(JobHandler::class.java))
        setField(rest, "eingangsrechnungDao", eingangsrechnungDao)

        val request = MockHttpServletRequest()
        request.setSession(MockHttpSession())

        val csv = """
            Rechnungsdatum;Rechnungs-Nr.;Geschäftspartner-Name;Rechnungsbetrag;WKZ
            02.05.2025;INV-001;ACME GmbH;746,13;EUR
        """.trimIndent()
        rest.upload(request, MockMultipartFile("file", "invoices.csv", "text/csv", csv.toByteArray(Charsets.UTF_8)))

        val commitResponse = rest.commit(request, AbstractImportRest.CommitData(selectedIds = emptyList()))
        assertEquals(HttpStatus.BAD_REQUEST, commitResponse.statusCode, "Nothing selected -> 400.")
    }

    private fun setField(target: Any, name: String, value: Any) {
        val field = target.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(target, value)
    }
}
