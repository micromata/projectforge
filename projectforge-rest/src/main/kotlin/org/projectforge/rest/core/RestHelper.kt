/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.core

import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.converter.DateTimeFormat
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.servlet.http.HttpServletRequest


class RestHelper {
    companion object {

        private val log = org.slf4j.LoggerFactory.getLogger(RestHelper::class.java)

        fun buildUri(request: HttpServletRequest, path: String): URI {
            return URI("${getRootUrl(request)}/$path")
        }

        fun getRootUrl(request: HttpServletRequest): String {
            val serverName = request.serverName
            val portNumber = request.serverPort
            return if (portNumber != 80 && portNumber != 443) "$serverName:$portNumber" else serverName
        }

        fun parseJSDateTime(jsString: String?): PFDateTime? {
            if (jsString.isNullOrBlank())
                return null
            try {
                val length = jsString.length
                val formatter =
                        when {
                            length > 19 -> jsonDateTimeFormatter
                            length > 16 -> jsonDateTimeSecondsFormatter
                            length > 10 -> jsonDateTimeMinutesFormatter
                            else -> jsonDateFormatter
                        }
                if (formatter != jsonDateFormatter)
                    return PFDateTime.parseUTCDate(jsString, formatter)
                val local = LocalDate.parse(jsString, jsonDateFormatter) // Parses UTC as local date.
                return PFDateTime.from(local)
            } catch (ex: DateTimeParseException) {
                log.error("Error while parsing date '$jsString': ${ex.message}.")
                return null
            }
        }

        fun parseLong(request: HttpServletRequest?, parameter: String): Long? {
            try {
                return request?.getParameter(parameter)?.toLong()
            } catch (ex: DateTimeParseException) {
                log.error("Error while parsing date millis '${request?.getParameter(parameter)}': ${ex.message}.")
                return null
            }
        }

        private val jsonDateTimeFormatter = DateTimeFormatter.ofPattern(DateTimeFormat.JS_DATE_TIME_MILLIS.pattern)
        private val jsonDateTimeSecondsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        private val jsonDateTimeMinutesFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        private val jsonDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
