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

import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * An attachment shown in the browser instead of downloaded (`AttachmentsServicesRest.preview`).
 *
 * Only a whitelist of formats is shown, and the format is taken from the **content**, never from the name or the
 * type the uploader claimed: an html or svg file renamed to `.png` would otherwise run its scripts on the origin
 * of ProjectForge. Anything else (html, svg, office documents, encrypted zips, …) is refused with 415, and the
 * client offers the download.
 *
 * Images and texts are served with `Content-Security-Policy: sandbox` in addition to the global policy (both are
 * enforced), so even a file the detection got wrong can't run anything. PDFs can't have it: Chrome refuses to show
 * a PDF in a sandboxed document. Their scripts run in the PDF viewer of the browser, without access to the page.
 *
 * PDFs need `object-src 'self'` instead of the global `object-src 'none'` as well ([pdfCsp]): Safari shows a PDF
 * through its PDF plugin, which the policy of the PDF response itself governs, and downloads the file if it's
 * forbidden. The global policy is replaced for this response only, so the pages of the application keep
 * `object-src 'none'`.
 */
object AttachmentPreview {
    enum class Type(val mediaType: MediaType, val sandbox: Boolean = true) {
        PDF(MediaType.APPLICATION_PDF, sandbox = false),
        PNG(MediaType.IMAGE_PNG),
        JPEG(MediaType.IMAGE_JPEG),
        GIF(MediaType.IMAGE_GIF),
        WEBP(MediaType.parseMediaType("image/webp")),
        TEXT(MediaType.TEXT_PLAIN),
    }

    /** The extensions of the texts shown; a text is recognized by its name, then checked for being one. */
    val TEXT_EXTENSIONS = setOf("txt", "csv", "tsv", "json", "xml", "log", "md", "properties", "yml", "yaml")

    /** The head of the file the type and the charset are detected from. */
    private const val HEAD_SIZE = 64 * 1024

    /**
     * The response showing the file, or 415 if it isn't of a format shown in the browser.
     * The stream is consumed or closed in either case.
     *
     * @param response For replacing the global policy of a PDF (see [pdfCsp]).
     * @param cspHeaderValue The global policy (`projectforge.security.csp-header-value`), blank if none.
     */
    fun response(
        filename: String,
        inputStream: InputStream,
        response: HttpServletResponse,
        cspHeaderValue: String?,
    ): ResponseEntity<InputStreamResource> {
        val stream = BufferedInputStream(inputStream, HEAD_SIZE)
        stream.mark(HEAD_SIZE)
        val head = stream.readNBytes(HEAD_SIZE)
        stream.reset()
        val type = detectType(filename, head)
        if (type == null) {
            stream.close()
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build()
        }
        val mediaType = if (type == Type.TEXT) {
            MediaType(MediaType.TEXT_PLAIN, if (isUtf8(head)) StandardCharsets.UTF_8 else WINDOWS_1252)
        } else {
            type.mediaType
        }
        val builder = ResponseEntity.ok()
            .contentType(mediaType)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build().toString(),
            )
        if (type.sandbox) {
            builder.header(CSP_HEADER, "sandbox")
        } else if (type == Type.PDF) {
            // setHeader, not a header of the ResponseEntity: SecurityHeaderFilter has set the global policy already,
            // and a second header would be enforced in addition to it, relaxing nothing.
            pdfCsp(cspHeaderValue)?.let { response.setHeader(CSP_HEADER, it) }
        }
        return builder.body(InputStreamResource(stream))
    }

    /**
     * The global policy with `object-src 'self'` instead of whatever object-src it has (appended if none), so Safari
     * may show the PDF with its plugin. Null for no global policy: then there is nothing to replace.
     */
    fun pdfCsp(cspHeaderValue: String?): String? {
        if (cspHeaderValue.isNullOrBlank()) return null
        val directives = cspHeaderValue.split(';').map { it.trim() }.filter { it.isNotEmpty() }
        val others = directives.filterNot { it.substringBefore(' ').equals("object-src", ignoreCase = true) }
        return (others + "object-src 'self'").joinToString("; ")
    }

    private const val CSP_HEADER = "Content-Security-Policy"

    /** The type of the file by its content, null for any format not shown in the browser. */
    fun detectType(filename: String, head: ByteArray): Type? {
        fun startsWith(vararg bytes: Int, offset: Int = 0): Boolean =
            head.size >= offset + bytes.size && bytes.indices.all { head[offset + it] == bytes[it].toByte() }
        return when {
            startsWith(0x25, 0x50, 0x44, 0x46, 0x2D) -> Type.PDF // %PDF-
            startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> Type.PNG
            startsWith(0xFF, 0xD8, 0xFF) -> Type.JPEG
            startsWith(0x47, 0x49, 0x46, 0x38, 0x37, 0x61) || startsWith(0x47, 0x49, 0x46, 0x38, 0x39, 0x61) -> Type.GIF
            startsWith(0x52, 0x49, 0x46, 0x46) && startsWith(0x57, 0x45, 0x42, 0x50, offset = 8) -> Type.WEBP
            filename.substringAfterLast('.', "").lowercase() in TEXT_EXTENSIONS && isText(head) -> Type.TEXT
            else -> null
        }
    }

    /** No NUL and hardly any other control character: binary data isn't shown as text. */
    private fun isText(head: ByteArray): Boolean {
        var control = 0
        for (b in head) {
            val c = b.toInt() and 0xFF
            if (c == 0) return false
            if (c < 0x20 && c != 0x09 && c != 0x0A && c != 0x0D && c != 0x0C) control++
        }
        return control * 100 <= head.size
    }

    /** Valid UTF-8. A full head may end in the middle of a character, so its last character is left out. */
    private fun isUtf8(head: ByteArray): Boolean {
        val length = if (head.size == HEAD_SIZE) lastCharStart(head) else head.size
        return try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(head, 0, length))
            true
        } catch (_: CharacterCodingException) {
            false
        }
    }

    /** The index of the first byte of the last UTF-8 character (continuation bytes are `10xxxxxx`). */
    private fun lastCharStart(head: ByteArray): Int {
        var i = head.size - 1
        while (i > 0 && head.size - i < 4 && (head[i].toInt() and 0xC0) == 0x80) i--
        return i
    }

    /** Texts that aren't UTF-8 are mostly from Windows (Excel's csv export). */
    private val WINDOWS_1252 = Charset.forName("windows-1252")
}
