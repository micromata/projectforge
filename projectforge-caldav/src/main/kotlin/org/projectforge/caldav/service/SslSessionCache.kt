/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.caldav.service

import mu.KotlinLogging
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.core.AbstractSessionCache
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Caches the session id's of the clients (for up to 5 Minutes). Every 10 Minutes, expired sessions will be removed.
 */
@Service
open class SslSessionCache
    : AbstractSessionCache<PFUserDO>(
        expireTimeInMillis = 5 * TICKS_PER_MINUTE,
        clearEntriesIntervalInMillis = 10 * TICKS_PER_MINUTE,
        sessionType = "SSL session id") {

    override fun entryAsString(entry: PFUserDO): String {
        return "'${entry.username}' with id ${entry.id}"
    }

    override fun equals(entry: PFUserDO, other: PFUserDO): Boolean {
        return entry.id == other.id
    }

    override fun getSessionId(request: HttpServletRequest): String? {
        val sslSessionId = request.getAttribute(REQUEST_ATTRIBUTE_SSL_SESSION_ID) ?: return null
        if (sslSessionId is String) {
            return sslSessionId
        }
        log.warn { "Oups, Attribute '$REQUEST_ATTRIBUTE_SSL_SESSION_ID' isn't of type String. Ignoring." }
        return null
    }
}

private const val REQUEST_ATTRIBUTE_SSL_SESSION_ID = "javax.servlet.request.ssl_session_id"
