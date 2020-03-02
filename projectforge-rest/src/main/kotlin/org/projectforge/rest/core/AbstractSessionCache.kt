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
import org.projectforge.framework.cache.AbstractCache
import javax.servlet.http.HttpServletRequest

/**
 * Caches the session id's of the clients (for up to 5 Minutes). Every 10 Minutes, expired sessions will be removed.
 */
abstract class AbstractSessionCache<T : Any>(
        /**
         * Entries will be expired after this time after last access.
         */
        val expireTimeInMillis: Long,
        /**
         * Expired entries will be tidied up after this interval length.
         */
        clearEntriesIntervalInMillis: Long,
        /**
         * For logging purposes.
         */
        val sessionType: String = "Http session id")
    : AbstractCache(clearEntriesIntervalInMillis) {

    class Entry<T : Any>(val sessionId: String, data: T) {
        var lastAccess: Long = System.currentTimeMillis()
        val _data = data
        val data: T
            get() {
                this.lastAccess = System.currentTimeMillis()
                return _data
            }
    }

    private val log = KotlinLogging.logger {}

    private val cache = mutableListOf<Entry<T>>()

    fun registerSessionData(request: HttpServletRequest, data: T) {
        val sessionId = getSessionId(request) ?: return
        registerSessionData(sessionId, data)
    }

    fun registerSessionData(sessionId: String?, data: T) {
        if (sessionId == null || sessionId.length < 20) {
            log.info { "$sessionType id to short. Usage denied: ${getSessionIdForLogging(sessionId)}" }
            return
        }
        synchronized(cache) {
            val entry = cache.find { it.sessionId == sessionId }
            if (entry != null) {
                val cachedData = entry.data // Updates last access, too.
                if (equals(cachedData, data)) {
                    log.warn { "$sessionType '${getSessionIdForLogging(sessionId)}' of user ${entryAsString(cachedData)} re-used by different user ${entryAsString(data)}!!!! Re-usage denied." }
                } else {
                    log.info { "$sessionType '${getSessionIdForLogging(sessionId)}' is re-used by user ${entryAsString(data)}." }
                }
                // last access is updated by call entry.user.
            } else {
                log.info { "Registering user ${entryAsString(data)} by new $sessionType '${getSessionIdForLogging(sessionId)}'." }
                cache.add(Entry(sessionId, data))
            }
        }
    }

    fun getSessionData(request: HttpServletRequest): T? {
        val sessionId = getSessionId(request) ?: return null
        return getSessionData(sessionId)
    }

    fun getSessionData(sessionId: String?): T? {
        if (sessionId == null || sessionId.length < 20) {
            log.info { "$sessionType to short. Usage denied: ${getSessionIdForLogging(sessionId)}" }
            return null
        }
        synchronized(cache) {
            val entry = cache.find { it.sessionId == sessionId } ?: return null
            if (isExpired(entry)) {
                if (log.isDebugEnabled) {
                    log.debug { "Found expired session user for $sessionType '${getSessionIdForLogging(sessionId)}'." }
                }
                return null
            }
            log.info { "Restore logged-in user ${entryAsString(entry._data)} by $sessionType '${getSessionIdForLogging(sessionId)}'." }
            return entry.data
        }
    }

    protected open fun isExpired(entry: Entry<T>): Boolean {
        return System.currentTimeMillis() - entry.lastAccess > expireTimeInMillis
    }

    /**
     * For logging purposes.
     */
    protected abstract fun entryAsString(entry: T): String

    protected open fun equals(entry: T, other: T): Boolean {
        return entry == other
    }

    protected open fun getSessionId(request: HttpServletRequest): String? {
        return request.session?.id
    }

    /**
     * Show only first 10 chars of ssl session id for security reasons.
     */
    private fun getSessionIdForLogging(sessionId: String?): String {
        sessionId ?: return "null"
        return if (sessionId.length <= 10) "***" else "${sessionId.substring(0..9)}..."
    }

    override fun refresh() {
        synchronized(cache) {
            val size = cache.size
            cache.removeIf {
                isExpired(it)
            }
            if (log.isDebugEnabled) {
                log.debug { "${size - cache.size} expired entries removed from SSL session cache." }
            }
        }

    }
}
