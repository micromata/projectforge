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

package org.projectforge.caldav.service

import mu.KotlinLogging
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.core.AbstractSessionCache
import org.springframework.stereotype.Service
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Work around for authenticate carddav and caldav clients. Expires after 1 minute.
 */
@Service
open class DavSessionCache
  : AbstractSessionCache<DavSessionCache.DavSessionData>(
  expireTimeInMillis = 1 * TICKS_PER_MINUTE,
  clearEntriesIntervalInMillis = 2 * TICKS_PER_MINUTE,
  sessionType = "DavSessionCache",
) {
  class DavSessionData(val user: PFUserDO)

  fun registerSessionData(request: HttpServletRequest, user: PFUserDO) {
    val data = DavSessionData(user)
    super.registerSessionData(request, data)
    log.info { "Registering ${getLogInfo(data, getSessionId(request))}" }
  }

  override fun entryAsString(entry: DavSessionData): String {
    return getLogInfo(entry)
  }

  override fun getSessionId(request: HttpServletRequest): String? {
    request.requestURI.let { uri ->
      if (uri.isNullOrBlank() || !uri.startsWith("/users/")) {
        log.info { "uri doesn't start with /users/: $uri" }
        return null
      }
      val username = uri.removePrefix("/users/").substringBefore('/')
      if (username.isBlank()) {
        log.info { "uri doesn't contain username after /users/: $uri" }
        return null
      }
      val sb = StringBuilder()
      sb.append("user=[").append(username).append("]")
      headerParams.forEach { sb.append(",").append(it).append("=[").append(request.getHeader(it)).append("]") }
      return sb.toString()
    }
  }

  override fun getTruncatedSessionId(sessionId: String?): String? {
    return sessionId // No trunc
  }

  override fun getSessionData(request: HttpServletRequest): DavSessionCache.DavSessionData? {
    val data = super.getSessionData(request)
    if (data != null) {
      if (log.isInfoEnabled) {
        log.info("Found registered user in cache: ${getLogInfo(data, getSessionId(request))}")
      }
    }
    return data
  }

  private fun getLogInfo(data: DavSessionData, sessionId: String? = null): String {
    val sb = StringBuilder()
    sb.append("user '${data.user.username}' (#${data.user.id}) with ")
    if (sessionId != null) {
      sb.append("session-id='$sessionId'")
    }
    return sb.toString()
  }

  companion object {
    private val headerParams = arrayOf("x-real-ip", "x-forward-for", "user-agent")
  }
}
