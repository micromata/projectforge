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

package org.projectforge.web.rest

import mu.KotlinLogging
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.StringUtils
import java.nio.charset.StandardCharsets
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

class BasicAuthenticationData(request: HttpServletRequest, authHeader: String, required: Boolean = false) {
  var username: String? = null
  var secret: String? = null

  init {
    val basic = StringUtils.split(authHeader)
    if (basic.size != 2 || !StringUtils.equalsIgnoreCase(basic[0], "Basic")) {
      if (required) {
        logError(
          request,
          "Basic authentication failed, header 'authorization' not in supported format (Basic <base64>)."
        )
      }
    } else {
      val credentials = String(Base64.decodeBase64(basic[1]), StandardCharsets.UTF_8)
      val p = credentials.indexOf(":")
      if (p < 1) {
        logError(request, "Basic authentication failed, credentials not of format 'user:secret'.")
      } else {
        username = credentials.substring(0, p).trim { it <= ' ' }
        secret = credentials.substring(p + 1).trim { it <= ' ' }
      }
    }
  }

  override fun toString(): String {
    username ?: return "invalid"
    return "Basic '$username'/'***'"
  }

  private fun logError(request: HttpServletRequest, msg: String) {
    log.error("$msg (requestUri=${request.requestURI}, ${request.queryString})")
  }
}
