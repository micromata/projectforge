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


package org.projectforge.rest.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.springframework.web.util.UrlPathHelper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

/**
 * The attachment calls of `AttachmentsServicesRest` naming the entity (category) in their JSON body rather than in
 * the path. Used by filters allowing or blocking the attachments of a category (gateway, switched off features).
 */
object AttachmentCategoryRequests {
    const val ATTACHMENTS_URL = "${Rest.URL}/attachments"

    /** The calls carrying the category in the path: `<url>/<category>/...`. */
    val PATH_CATEGORY_CALLS = listOf("upload", "download", "multiDownload")

    /** The calls carrying the category in the body. */
    val BODY_CATEGORY_URLS =
        listOf("modify", "delete", "encrypt", "testDecryption", "multiDelete").map { "$ATTACHMENTS_URL/$it" }.toSet()

    /**
     * The largest body read by the filters (the JSON of an attachment call is small): they run before the
     * authentication, so a body is never read unbounded.
     */
    const val MAX_BODY_SIZE = 1024 * 1024

    private val objectMapper = ObjectMapper()

    private val urlPathHelper = UrlPathHelper().also {
        it.setUrlDecode(true)
        it.setRemoveSemicolonContent(true)
    }

    /**
     * The request path without context path, decoded and without path parameters: the path the controllers are
     * mapped by (`/rs/addr%65ss/list` is `/rs/address/list`). Filters deciding by the path must use this one, not
     * the raw request uri.
     */
    fun pathOf(request: HttpServletRequest): String = urlPathHelper.getPathWithinApplication(request)

    /**
     * @return The body, or null if it exceeds [maxSize] (to be refused with 413).
     */
    fun readBody(request: HttpServletRequest, maxSize: Int = MAX_BODY_SIZE): ByteArray? {
        if (request.contentLengthLong > maxSize) {
            return null
        }
        val body = request.inputStream.readNBytes(maxSize + 1)
        return body.takeIf { it.size <= maxSize }
    }

    /** `PostData<AttachmentData>` and `PostData<FileListData>` both carry it as `data.category`. */
    fun categoryOf(body: ByteArray): String? =
        runCatching { objectMapper.readTree(body)?.path("data")?.path("category")?.asText(null) }.getOrNull()

    /** A request whose body was read already: hands it on to the controller. */
    class CachedBodyRequest(request: HttpServletRequest, private val body: ByteArray) :
        HttpServletRequestWrapper(request) {
        override fun getInputStream(): ServletInputStream {
            val input = ByteArrayInputStream(body)
            return object : ServletInputStream() {
                override fun read(): Int = input.read()
                override fun read(b: ByteArray, off: Int, len: Int): Int = input.read(b, off, len)
                override fun isFinished(): Boolean = input.available() == 0
                override fun isReady(): Boolean = true
                override fun setReadListener(listener: ReadListener?) {
                    throw UnsupportedOperationException()
                }
            }
        }

        override fun getReader(): BufferedReader =
            BufferedReader(InputStreamReader(inputStream, characterEncoding ?: Charsets.UTF_8.name()))
    }
}
