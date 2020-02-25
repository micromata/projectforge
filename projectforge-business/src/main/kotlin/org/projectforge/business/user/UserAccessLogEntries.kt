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

package org.projectforge.business.user

import org.projectforge.framework.ToStringUtil
import org.projectforge.web.rest.UserAccessLogEntry
import org.slf4j.LoggerFactory
import java.util.*
import javax.servlet.http.HttpServletRequest

private const val MAX_SIZE = 20

/**
 * Access to rest services will be logged, including UserAgent string, IP, used type of token and timestamp for
 * checking unauthorized access.
 */
class UserAccessLogEntries(tokenType: UserTokenType) {
    private var entries = mutableSetOf<UserAccessLogEntry>()
    var logAccessName: String
        private set

    init {
        this.logAccessName = tokenType.name
    }

    fun update(request: HttpServletRequest) {
        val ip = request.remoteAddr
        val userAgent = request.getHeader("User-Agent")
        update(userAgent = userAgent, ip = ip)
    }

    /**
     * Clears all access entries (writes current state to log file before clearing). Should be called after renewing a token.
     */
    fun clear() {
        log.info("Clearing entries '$logAccessName'. State was: $this")
        entries.clear()
    }

    fun update(userAgent: String?, ip: String?) {
        entries.find { it.ip == ip && it.userAgent == userAgent }?.let {
            it.lastAccess = Date()
            it.counter++
        } ?: run {
            entries.add(UserAccessLogEntry(userAgent = userAgent, ip = ip))
        }
        if (entries.size > 20) {
            val numberOfItemsToDrop = entries.size - MAX_SIZE
            entries = sortedList().dropLast(numberOfItemsToDrop).toMutableSet()
        }
    }

    /**
     * @return All entries sorted by timeStamp of last access in descending order.
     */
    fun sortedList(): List<UserAccessLogEntry> {
        return entries.sortedByDescending { it.lastAccess }
    }

    fun size(): Int {
        return entries.size
    }

    /**
     * @param escapeHtml If true, the user-agent string will be html escaped. Default is false. The separator string
     * itself will not be escaped.
     */
    @JvmOverloads
    fun asText(separator: String, escapeHtml: Boolean = false): String {
        return sortedList().joinToString(separator) { it.asText(escapeHtml) }
    }

    override fun toString(): String {
        return ToStringUtil.toJsonString(this)
    }

    companion object {
        private val log = LoggerFactory.getLogger(UserAccessLogEntries::class.java)
    }
}
