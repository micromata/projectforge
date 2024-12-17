/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.carddav

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.rest.utils.RequestLog
import java.io.BufferedReader

private val log = KotlinLogging.logger {}

internal class RequestWrapper(val request: HttpServletRequest) {
    val requestURI = request.requestURI ?: "null"
    val method = request.method ?: "null"
    val basicAuth: Boolean by lazy {
        request.getHeader("authorization") != null ||
                request.getHeader("Authorization") != null
    }

    val body: String by lazy {
        try {
            if (request.contentLength > 0) {
                request.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                ""
            }
        } catch (e: Exception) {
            log.info { "Can't read body of request: ${RequestLog.asJson(request)}" }
            ""
        }
    }
}
