/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.framework.utils.NumberHelper
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Caches the csrf token per session id's of the clients (for up to 4 Hours). Every hour, expired csrf tokens will be removed.
 */
@Service
open class SessionCsrfCache
    : AbstractSessionCache<String>(
        expireTimeInMillis = 4 * TICKS_PER_HOUR,
        clearEntriesIntervalInMillis = TICKS_PER_HOUR) {

    fun checkToken(request: HttpServletRequest, token: String?): Boolean {
        if (token.isNullOrEmpty() || token.trim().length < 30) {
            log.info { "Token to short, check faild for session id '${request.session.id}'."}
            return false
        }
        return super.getSessionData(request) == token
    }

    fun ensureAndGetToken(request: HttpServletRequest): String {
        var token = super.getSessionData(request)
        if (token != null) {
            return token
        }
        token = NumberHelper.getSecureRandomAlphanumeric(30)
        super.registerSessionData(request, token)
        return token
    }

    override fun entryAsString(entry: String): String {
        return "'${entry.substring(0..9)}...'"
    }
}
