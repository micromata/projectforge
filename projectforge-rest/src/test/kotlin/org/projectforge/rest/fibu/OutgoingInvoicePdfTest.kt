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

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.EInvoiceExportService
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.jcr.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The invoice PDF of an outgoing invoice — the three endpoints of [OutgoingInvoiceEntityRest] Wicket has as
 * the `fibu.rechnung.invoicePdf` fieldset of its e-invoice dialog.
 *
 * Three things are worth asserting, and none of them is the JCR itself:
 *
 * - the round trip, i.e. that an uploaded PDF is what the info endpoint answers afterwards and that deleting
 *   it leaves none — the marker (`__INVOICE_PDF__`) is what ties the three calls together, so a mismatch
 *   would show as an invoice PDF that vanishes right after it was stored;
 * - that the marked attachment stays in the *normal* attachment list and that its `fileId` travels with the
 *   info, because that is what the client downloads it by (the generic attachment route serves it, see
 *   AttachmentSection) and a server that hid it would break Wicket's own list;
 * - that a file which is not a PDF is refused with the backend's own text instead of being dropped in
 *   silence, which is what Wicket does (`processInvoicePdfUpload` checks the extension and returns);
 * - that both writes appear in the invoice's change history, since the JCR keeps none of its own: the file is
 *   gone for good, and the entry naming it is what is left afterwards.
 */
class OutgoingInvoicePdfTest : AbstractTestBase() {

    @Autowired
    private lateinit var outgoingInvoiceEntityRest: OutgoingInvoiceEntityRest

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var attachmentsService: AttachmentsService

    @Autowired
    private lateinit var repoService: RepoService

    @PostConstruct
    private fun postConstruct() {
        // The JCR is a repository on disk, and the endpoints under test store into it; without this the
        // application's own one would be used (see AbstractTestBase.initJCRTestRepo).
        initJCRTestRepo(MODULE_NAME, "outgoingInvoicePdfTestRepo")
    }

    /** Releases the repository directory again, as `DataTransferJCRCleanUpJobTest` does. */
    override fun afterAll() {
        repoService.shutdown()
    }

    @Test
    fun `an uploaded pdf is the invoice pdf of that invoice until it is deleted`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Invoice PDF round trip")

        assertNull(outgoingInvoiceEntityRest.getInvoicePdfInfo(id).pdf, "Nothing uploaded yet.")

        val response = outgoingInvoiceEntityRest.uploadInvoicePdf(id, pdf("invoice.pdf"))
        assertEquals(HttpStatus.OK, response.statusCode)
        val uploaded = (response.body as OutgoingInvoiceEntityRest.InvoicePdfState).pdf
        assertNotNull(uploaded, "The write answers the new state, so the client needs no second call.")
        assertEquals("invoice.pdf", uploaded!!.name)

        // The same file through the read endpoint: what the form shows when the page is opened again.
        assertEquals("invoice.pdf", outgoingInvoiceEntityRest.getInvoicePdfInfo(id).pdf?.name)
        // And it is a regular attachment of the invoice, carrying the marker as its description — which is
        // how `exportAsZUGFeRD` finds it and how the client knows to hide it.
        val attachments = attachmentsService.internalGetAttachments(EInvoiceExportService.JCR_PATH, id)
        assertEquals(1, attachments.size)
        assertEquals(EInvoiceExportService.INVOICE_PDF_MARKER, attachments.first().description)
        // The very file id the info answers: it is what the form's download link is built from, so the two
        // must not drift apart — a link with another id leads to a 404.
        assertEquals(
            attachments.first().fileId,
            outgoingInvoiceEntityRest.getInvoicePdfInfo(id).pdf?.fileId,
            "The info answers the file id the attachment route serves this file by.",
        )

        assertNull(outgoingInvoiceEntityRest.deleteInvoicePdf(id).pdf)
        assertNull(outgoingInvoiceEntityRest.getInvoicePdfInfo(id).pdf, "Deleted for good.")
    }

    @Test
    fun `uploading a second pdf replaces the first, since an invoice has exactly one`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Invoice PDF replacement")

        outgoingInvoiceEntityRest.uploadInvoicePdf(id, pdf("first.pdf"))
        outgoingInvoiceEntityRest.uploadInvoicePdf(id, pdf("second.pdf"))

        assertEquals("second.pdf", outgoingInvoiceEntityRest.getInvoicePdfInfo(id).pdf?.name)
        // Not two files: `uploadInvoicePdf` deletes the previous one, or the export would have to pick.
        assertEquals(1, attachmentsService.internalGetAttachments(EInvoiceExportService.JCR_PATH, id).size)
    }

    @Test
    fun `uploading and deleting the pdf is written to the invoice's change history`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Invoice PDF history")

        outgoingInvoiceEntityRest.uploadInvoicePdf(id, pdf("history.pdf"))

        // `attachmentsLastUserAction` is the only historized one of the attachment fields — the counter, the
        // names, the ids and the size are `@NoHistory`, they exist for the search index. So a write that
        // updates only those leaves no trace at all, which is what this asserts against.
        assertTrue(
            lastUserAction(id)?.contains("history.pdf") == true,
            "The upload names the file it stored: ${lastUserAction(id)}",
        )
        assertTrue(
            historyOf(id).any { it.contains("history.pdf") },
            "…and the invoice's history says so, not just its current state.",
        )

        outgoingInvoiceEntityRest.deleteInvoicePdf(id)

        // The delete overwrites the sentence, so the history holds both — the file is gone from the JCR,
        // which keeps no history of its own, and this is all that is left of it.
        assertTrue(
            lastUserAction(id)?.contains("deleted") == true,
            "The delete is an action of its own: ${lastUserAction(id)}",
        )
        assertEquals(
            2,
            historyOf(id).count { it.contains("history.pdf") },
            "Two entries for the one file: the upload and the delete.",
        )
    }

    /** What the invoice currently says about the last write to its attachments. */
    private fun lastUserAction(id: Long): String? {
        return rechnungDao.find(id, checkAccess = false)?.attachmentsLastUserAction
    }

    /** Every value `attachmentsLastUserAction` ever took, as the invoice's history recorded it. */
    private fun historyOf(id: Long): List<String> {
        val invoice = rechnungDao.find(id, checkAccess = false)!!
        return rechnungDao.loadHistory(invoice, checkAccess = false).sortedEntries
            .flatMap { it.attributes ?: emptySet() }
            .filter { it.propertyName == "attachmentsLastUserAction" }
            .mapNotNull { it.value }
    }

    @Test
    fun `a file that is not a pdf is refused, and the answer says why`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Invoice PDF refusal")

        val response = outgoingInvoiceEntityRest.uploadInvoicePdf(
            id,
            MockMultipartFile("file", "invoice.docx", null, "not a pdf".toByteArray()),
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val message = response.body as String
        // `FileCheck`'s own translated text, so the client can show it as it is; Wicket drops such a file
        // without a word.
        assertTrue(message.contains("pdf"), "The refusal names the format it wanted: $message")
        assertNull(outgoingInvoiceEntityRest.getInvoicePdfInfo(id).pdf, "Nothing was stored.")
    }

    @Test
    fun `an empty file is refused as well`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Invoice PDF empty")

        val response = outgoingInvoiceEntityRest.uploadInvoicePdf(
            id,
            MockMultipartFile("file", "invoice.pdf", null, ByteArray(0)),
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNull(outgoingInvoiceEntityRest.getInvoicePdfInfo(id).pdf)
    }

    @Test
    fun `a user outside the finance and orga groups reaches none of the three endpoints`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Invoice PDF access")

        logon(TEST_USER)
        // The e-invoice functions require the groups of `EInvoiceCheckerPageRest`, and the two writing ones
        // additionally the write access to this very invoice: uploading a PDF changes what an e-invoice of
        // it looks like.
        assertThrows<AccessException> { outgoingInvoiceEntityRest.getInvoicePdfInfo(id) }
        assertThrows<AccessException> { outgoingInvoiceEntityRest.uploadInvoicePdf(id, pdf("invoice.pdf")) }
        assertThrows<AccessException> { outgoingInvoiceEntityRest.deleteInvoicePdf(id) }
    }

    @Test
    fun `an unknown invoice is refused rather than answered with an empty state`() {
        logon(TEST_FINANCE_USER)

        // Not a 200 with `pdf: null`: that would read as "this invoice has no PDF" for an invoice that
        // doesn't exist, and the upload would create a JCR node nobody owns.
        assertThrows<AccessException> { outgoingInvoiceEntityRest.getInvoicePdfInfo(-1L) }
        assertThrows<AccessException> { outgoingInvoiceEntityRest.deleteInvoicePdf(-1L) }
    }

    /** A minimal but real PDF, so nothing along the way rejects it for its content. */
    private fun pdf(name: String): MockMultipartFile {
        return MockMultipartFile("file", name, "application/pdf", "%PDF-1.4\n%%EOF\n".toByteArray())
    }

    /** A planned invoice, i.e. one that spends no invoice number (see `OutgoingInvoiceWordExportTest`). */
    private fun insertInvoice(subject: String): Long {
        val invoice = RechnungDO()
        invoice.status = RechnungStatus.GEPLANT
        invoice.typ = RechnungTyp.RECHNUNG
        invoice.datum = LocalDate.of(2026, 3, 2)
        invoice.betreff = subject
        invoice.kundeText = "$subject customer"
        invoice.positionen = mutableListOf(
            RechnungsPositionDO().also { pos ->
                pos.rechnung = invoice
                pos.number = 1.toShort()
                pos.menge = BigDecimal.ONE
                pos.einzelNetto = BigDecimal("1000.00")
                pos.vat = BigDecimal("0.19")
            },
        )
        return rechnungDao.insert(invoice)!!
    }

    companion object {
        /** Module directory this test runs in — `initJCRTestRepo` insists on it (see `TestUtils`). */
        private const val MODULE_NAME = "projectforge-rest"
    }
}
