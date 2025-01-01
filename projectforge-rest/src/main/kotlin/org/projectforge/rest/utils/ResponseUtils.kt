/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.utils

import jakarta.servlet.http.HttpServletResponse
import org.projectforge.framework.json.JsonUtils
import org.springframework.http.HttpStatus
import java.nio.charset.StandardCharsets

object ResponseUtils {
    /**
     * Set the response values.
     * @param response The response.
     * @param status The status to set, if given.
     * @param contentType The content type to set, if given. Examples: MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE.
     * @param content The content to set, if given.
     */
    fun setValues(
        response: HttpServletResponse,
        status: HttpStatus? = null,
        contentType: String? = null,
        content: String? = null
    ) {
        setByteArrayContent(response, status, contentType, content?.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Set the response values.
     * @param response The response.
     * @param status The status to set, if given.
     * @param contentType The content type to set, if given. Examples: MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE.
     * @param content The content to set, if given.
     */
    fun setByteArrayContent(
        response: HttpServletResponse,
        status: HttpStatus? = null,
        contentType: String? = null,
        content: ByteArray? = null
    ) {
        contentType?.let {
            if (it.contains("charset", ignoreCase = true)) {
                response.contentType = it
            } else {
                response.contentType = "$it; charset=UTF-8"
            }
        }
        status?.let {
            response.status = it.value()
        }
        content?.let {
            response.characterEncoding = "UTF-8"
            if (contentType == null) {
                response.contentType = "charset=UTF-8"
            }
            response.setContentLength(content.size)
            response.outputStream.use { it.write(content) }
        }
    }

    fun asJson(response: HttpServletResponse): String {
        return JsonUtils.toJson(Response(response))
    }

    class Response(response: HttpServletResponse) {
        val headers: MutableMap<String, String> = mutableMapOf()
        var status: Int = response.status
        var contentType: String = response.contentType

        init {
            for (header in response.headerNames) {
                headers[header] = response.getHeader(header)
            }
        }
    }
}
