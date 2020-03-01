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

package org.projectforge.caldav.service

import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

/**
 * Caches the session id's of the clients (for up to 5 Minutes). Every 10 Minutes, expired sessions will be removed.
 */
@Service
open class SslSessionCache : AbstractCache(10 * TICKS_PER_MINUTE) {
    private val log = LoggerFactory.getLogger(SslSessionCache::class.java)
    private val sslSessionCache = mutableListOf<SslSessionEntry>()

    fun registerSslSessionUser(request: HttpServletRequest, user: PFUserDO) {
        val sslSessionId = getSslSessionId(request) ?: return
        registerSslSessionUser(sslSessionId, user)
    }

    fun registerSslSessionUser(sslSessionId: String?, user: PFUserDO) {
        if (sslSessionId == null || sslSessionId.length < 20) {
            log.info("SSL session id to short. Usage denied: ${getSslSessionIdForLogging(sslSessionId)}")
            return
        }
        synchronized(sslSessionCache) {
            val entry = sslSessionCache.find { it.sslSessionId == sslSessionId }
            if (entry != null) {
                val cacheUser = entry.user // Updates last access, too.
                if (cacheUser.id != user.id) {
                    log.warn("SSL session id '${getSslSessionIdForLogging(sslSessionId)}' of user '${cacheUser.username}' with id ${cacheUser.id} re-used by different user '${user.username}' with id ${user.id}!!!! Re-usage denied.")
                } else {
                    log.info("SSL session id '${getSslSessionIdForLogging(sslSessionId)}' is re-used by user ${user.username} with id ${user.id}.")
                }
                // last access is updated by call entry.user.
            } else {
                log.info("Registering user '${user.username}' with id ${user.id} by new SSL session id '${getSslSessionIdForLogging(sslSessionId)}'.")
                sslSessionCache.add(SslSessionEntry(sslSessionId, user))
            }
        }
    }

    fun getSslSessionUser(request: HttpServletRequest): PFUserDO? {
        val sslSessionId = getSslSessionId(request) ?: return null
        return getSslSessionUser(sslSessionId)
    }

    fun getSslSessionUser(sslSessionId: String?): PFUserDO? {
        if (sslSessionId == null || sslSessionId.length < 20) {
            log.info("SSL session id to short. Usage denied: ${getSslSessionIdForLogging(sslSessionId)}")
            return null
        }
        synchronized(sslSessionCache) {
            val entry = sslSessionCache.find { it.sslSessionId == sslSessionId } ?: return null
            if (entry.isExpired) {
                if (log.isDebugEnabled) {
                    log.debug("Found expired SSL session user for SSL session id '${getSslSessionIdForLogging(sslSessionId)}'.")
                }
                return null
            }
            log.info("Restore logged-in user '${entry._user.username}' with id ${entry._user.id} by SSL session id '${getSslSessionIdForLogging(sslSessionId)}'.")
            return entry.user
        }
    }

    private fun getSslSessionId(request: HttpServletRequest): String? {
        val sslSessionId = request.getAttribute(REQUEST_ATTRIBUTE_SSL_SESSION_ID) ?: return null
        if (sslSessionId is String) {
            return sslSessionId
        }
        log.warn("Oups, Attribute '$REQUEST_ATTRIBUTE_SSL_SESSION_ID' isn't of type String. Ignoring.")
        return null
    }

    /**
     * Show only first 10 chars of ssl session id for security reasons.
     */
    private fun getSslSessionIdForLogging(sslSessionId: String?): String {
        sslSessionId ?: return "null"
        return if (sslSessionId.length <= 10) "***" else "${sslSessionId.substring(0..9)}..."
    }

    override fun refresh() {
        synchronized(sslSessionCache) {
            val size = sslSessionCache.size
            sslSessionCache.removeIf {
                it.isExpired
            }
            if (log.isDebugEnabled) {
                log.debug("${size - sslSessionCache.size} expired entries removed from SSL session cache.")
            }
        }

    }
}

private const val REQUEST_ATTRIBUTE_SSL_SESSION_ID = "javax.servlet.request.ssl_session_id"
private const val EXPIRES_MS = 5 * 60 * 1000

private class SslSessionEntry(val sslSessionId: String, user: PFUserDO) {
    var lastAccess: Long = System.currentTimeMillis()
    val _user = user
    val user: PFUserDO
        get() {
            this.lastAccess = System.currentTimeMillis()
            return _user
        }
    val isExpired: Boolean
        get() = System.currentTimeMillis() - lastAccess > EXPIRES_MS
}
