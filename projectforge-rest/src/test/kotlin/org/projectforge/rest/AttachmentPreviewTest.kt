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

package org.projectforge.rest

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.files.AttachmentsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockMultipartFile
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The preview of an attachment (`AttachmentsServicesRest.preview`): who may see it, and which files are shown in
 * the browser at all — a file shown inline runs on the origin of ProjectForge, so the format is taken from the
 * content, and html/svg are never shown.
 */
class AttachmentPreviewTest : AbstractTestBase() {
    @Autowired
    private lateinit var attachmentsServicesRest: AttachmentsServicesRest

    @Autowired
    private lateinit var attachmentsService: AttachmentsService

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @PostConstruct
    private fun postConstruct() {
        initTestFileStore()
    }

    @Test
    fun `the formats are detected by their content, not by their name`() {
        assertEquals(AttachmentPreview.Type.PDF, AttachmentPreview.detectType("a.bin", PDF))
        assertEquals(AttachmentPreview.Type.PNG, AttachmentPreview.detectType("a.pdf", PNG))
        assertEquals(AttachmentPreview.Type.TEXT, AttachmentPreview.detectType("a.csv", "a;b\n1;2\n".toByteArray()))
        // An svg or html file, whatever it is called, is no image and no shown text.
        assertNull(AttachmentPreview.detectType("a.png", SVG))
        assertNull(AttachmentPreview.detectType("a.svg", SVG))
        assertNull(AttachmentPreview.detectType("a.html", "<html><script>alert(1)</script></html>".toByteArray()))
        // A text extension on binary data, e.g. a zip renamed (and every encrypted attachment is a zip).
        assertNull(AttachmentPreview.detectType("a.txt", byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0)))
    }

    @Test
    fun `the policy of a pdf allows the plugin of safari, and nothing else changes`() {
        assertEquals(
            "default-src 'self'; frame-ancestors 'self'; object-src 'self'",
            AttachmentPreview.pdfCsp("default-src 'self'; object-src 'none'; frame-ancestors 'self'"),
        )
        assertEquals("default-src 'self'; object-src 'self'", AttachmentPreview.pdfCsp("default-src 'self';"))
        assertNull(AttachmentPreview.pdfCsp(""))
        assertNull(AttachmentPreview.pdfCsp(null))
    }

    @Test
    fun `a pdf is shown inline, without sandbox, which the pdf viewer of chrome refuses`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Preview pdf")
        val fileId = upload(id, "invoice.pdf", PDF)

        val servletResponse = MockHttpServletResponse()
        val response = attachmentsServicesRest.preview(servletResponse, CATEGORY, id, fileId, null)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(MediaType.APPLICATION_PDF, response.headers.contentType)
        assertTrue(response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)!!.startsWith("inline"))
        assertNull(response.headers.getFirst("Content-Security-Policy"))
        // The global policy replaced for this response, if one is configured (see pdfCsp).
        servletResponse.getHeader("Content-Security-Policy")?.let { csp ->
            assertTrue(csp.contains("object-src 'self'"), csp)
            assertTrue(!csp.contains("object-src 'none'"), csp)
        }
        assertEquals(PDF.toList(), response.body!!.inputStream.use { it.readBytes() }.toList())
    }

    @Test
    fun `images and texts are sandboxed, and texts get their charset`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Preview image and text")

        val image = attachmentsServicesRest.preview(MockHttpServletResponse(), CATEGORY, id, upload(id, "logo.png", PNG), null)
        assertEquals(MediaType.IMAGE_PNG, image.headers.contentType)
        assertEquals("sandbox", image.headers.getFirst("Content-Security-Policy"))
        image.body!!.inputStream.close()

        val utf8 = attachmentsServicesRest.preview(MockHttpServletResponse(), CATEGORY, id, upload(id, "utf8.csv", "Größe;1\n".toByteArray()), null)
        assertEquals("UTF-8", utf8.headers.contentType!!.charset!!.name())
        assertEquals("sandbox", utf8.headers.getFirst("Content-Security-Policy"))
        utf8.body!!.inputStream.close()

        // Excel's csv export on Windows.
        val latin = byteArrayOf('G'.code.toByte(), 0xF6.toByte(), 'n'.code.toByte(), '\n'.code.toByte())
        val windows = attachmentsServicesRest.preview(MockHttpServletResponse(), CATEGORY, id, upload(id, "windows.csv", latin), null)
        assertEquals("windows-1252", windows.headers.contentType!!.charset!!.name())
        windows.body!!.inputStream.close()
    }

    @Test
    fun `an svg is refused, the client offers the download instead`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Preview svg")
        val fileId = upload(id, "logo.svg", SVG)

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, attachmentsServicesRest.preview(MockHttpServletResponse(), CATEGORY, id, fileId, null).statusCode)
    }

    @Test
    fun `a user without access to the invoice sees none of its attachments`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Preview access")
        val fileId = upload(id, "invoice.pdf", PDF)

        logon(TEST_USER)
        assertThrows<AccessException> { attachmentsServicesRest.preview(MockHttpServletResponse(), CATEGORY, id, fileId, null) }
    }

    private fun upload(id: Long, filename: String, content: ByteArray): String {
        attachmentsServicesRest.uploadAttachment(
            CATEGORY, id, AttachmentsService.DEFAULT_NODE, MockMultipartFile("file", filename, null, content),
        )
        val path = attachmentsServicesRest.getPagesRest(CATEGORY, null).attachmentsPath!!
        return attachmentsService.internalGetAttachments(path, id).first { it.name == filename }.fileId!!
    }

    /** A planned invoice, i.e. one that spends no invoice number (see `OutgoingInvoicePdfTest`). */
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
        return rechnungDao.insert(invoice)
    }

    companion object {
        private const val CATEGORY = "outgoingInvoice"
        private val PDF = "%PDF-1.4\n%%EOF\n".toByteArray()
        private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D)
        private val SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".toByteArray()
    }
}
