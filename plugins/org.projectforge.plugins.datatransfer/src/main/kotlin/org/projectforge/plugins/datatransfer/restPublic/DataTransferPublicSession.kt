/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.datatransfer.restPublic

import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.projectforge.framework.ToStringUtil
import org.projectforge.rest.config.RestUtils
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * A minimal session handling for avoiding annoying re-logins for external users of the data transfer tool.
 */
object DataTransferPublicSession {
  class TransferAreaData(var authenticationToken: String, @JsonIgnore var password: String?, var userInfo: String?)

  fun getTransferAreaData(request: HttpServletRequest, authenticationToken: String?): TransferAreaData? {
    authenticationToken ?: return null
    val data = getSessionMap(request)?.get(authenticationToken)
    if (data != null) {
      log.info { "External user info restored from session: ${ToStringUtil.toJsonString(data)}, ip=${RestUtils.getClientIp(request)}" }
    }
    return data
  }

  fun register(request: HttpServletRequest, authenticationToken: String, password: String, userInfo: String?) {
    @Suppress("UNCHECKED_CAST")
    var map = getSessionMap(request)
    if (map == null) {
      map = mutableMapOf()
      request.getSession(true).setAttribute(SESSION_ATTRIBUTE, map)
    }
    val data = TransferAreaData(authenticationToken, password, userInfo)
    if (map[authenticationToken] == null) {
      log.info { "External user logged-in: ${ToStringUtil.toJsonString(data)}, ip=${RestUtils.getClientIp(request)}" }
    }
    map[authenticationToken] = data
  }

  fun logout(request: HttpServletRequest) {
    val map = getSessionMap(request)
    if (map != null) {
      log.info { "External user logged-out: ${ToStringUtil.toJsonString(map)}, ip=${RestUtils.getClientIp(request)}" }
      request.session?.invalidate()
    }
  }

  private fun getSessionMap(request: HttpServletRequest): MutableMap<String, TransferAreaData>? {
    @Suppress("UNCHECKED_CAST")
    val map: MutableMap<String, TransferAreaData>? =
      request.session?.getAttribute(SESSION_ATTRIBUTE) as? MutableMap<String, TransferAreaData>
    return map
  }

  private const val SESSION_ATTRIBUTE = "transferAreas"
}
