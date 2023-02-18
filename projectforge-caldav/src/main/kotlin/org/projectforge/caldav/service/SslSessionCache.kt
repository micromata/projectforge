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

package org.projectforge.caldav.service

import mu.KotlinLogging
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.core.AbstractSessionCache
import org.projectforge.rest.utils.RequestLog
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Caches the session id's of the clients (for up to 5 Minutes). Every 10 Minutes, expired sessions will be removed.
 */
@Service
open class SslSessionCache
  : AbstractSessionCache<SslSessionData>(
  expireTimeInMillis = 5 * TICKS_PER_MINUTE,
  clearEntriesIntervalInMillis = 10 * TICKS_PER_MINUTE,
  sessionType = "SSL session id",
) {
  fun registerSessionData(request: HttpServletRequest, user: PFUserDO) {
    val session = request.getSession(true)
    val data = SslSessionData(session.id, user)
    super.registerSessionData(request, data)
    session.setAttribute(HTTP_SESSION_ATTRIBUTE, data)
    log.info { "Registering ${getLogInfo(data, getSslSessionId(request))}" }
  }

  override fun entryAsString(entry: SslSessionData): String {
    return getLogInfo(entry)
  }

  override fun getSessionId(request: HttpServletRequest): String? {
    return getSslSessionId(request)
  }

  override fun getSessionData(request: HttpServletRequest): SslSessionData? {
    var data = super.getSessionData(request)
    if (data != null) {
      if (log.isInfoEnabled) {
        log.info("Found registered user in ssl-session-cache: ${getLogInfo(data, getSslSessionId(request))}")
      }
    } else {
      data = request.getSession(false)?.getAttribute(HTTP_SESSION_ATTRIBUTE) as? SslSessionData
      if (data != null && log.isInfoEnabled) {
        log.info("Found registered user in http-session: ${getLogInfo(data, getSslSessionId(request))}")
      }
    }
    return data
  }

  private fun getLogInfo(data: SslSessionData, sslSessionId: String? = null): String {
    val sb = StringBuilder()
    sb.append("user '${data.user.username}' (#${data.user.id}) with ")
    if (sslSessionId != null) {
      sb.append("ssl-session-id='${RequestLog.getTruncatedSessionId(data.httpSessionId)}'")
    }
    sb.append(" and http-session-id='${RequestLog.getTruncatedSessionId(data.httpSessionId)}'")
    return sb.toString()
  }

  companion object {
    private const val HTTP_SESSION_ATTRIBUTE = "sslSessionData"
  }
}
