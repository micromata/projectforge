/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.rest

import org.apache.commons.text.StringEscapeUtils
import org.projectforge.common.DateFormatType
import org.projectforge.framework.time.PFDateTime
import java.util.*

/**
 * Access to rest services will be logged, including UserAgent string, IP, used type of token and timestamp for
 * checking unauthorized access.
 */
data class UserAccessLogEntry(var userAgent: String? = null,
                              var ip: String? = null) {
    var lastAccess: Date = Date()
    var counter: Int = 1

    /**
     * @param escapeHtml If true, the user-agent string will be html escaped. Default is false.
     */
    @JvmOverloads
    fun asText(escapeHtml: Boolean = false): String {
        val dateTime = PFDateTime.from(lastAccess)
        val userAgentString = if (escapeHtml) {
            StringEscapeUtils.escapeHtml4(userAgent)
        } else {
            userAgent
        }
        return "IP=$ip, User-Agent='$userAgentString', Last-Access=${dateTime.format(DateFormatType.DATE_TIME_MINUTES)}, Counter=$counter"
    }
}
