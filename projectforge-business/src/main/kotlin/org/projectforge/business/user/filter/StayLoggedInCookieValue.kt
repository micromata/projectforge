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

package org.projectforge.business.user.filter

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.projectforge.framework.json.JsonUtils
import org.projectforge.security.SecurityLogging.logSecurityWarn
import java.net.URLDecoder
import java.net.URLEncoder

private val log = KotlinLogging.logger {}

class StayLoggedInCookieValue(
  var userId: String,
  var username: String,
  var stayLoggedInKey: String,
) {

  fun serialize(): String {
    return URLEncoder.encode(JsonUtils.toJson(this), "UTF-8")
  }

  companion object {
    fun deserialize(cookieValue: String?): StayLoggedInCookieValue? {
      cookieValue ?: return null
      if (cookieValue.contains("userId%22")) {
        try {
          val value = URLDecoder.decode(cookieValue, "UTF-8")
          val info = JsonUtils.fromJson(value, StayLoggedInCookieValue::class.java)
          if (info == null) {
            logInvalidCookie(value)
          }
          return info
        } catch (ex: Exception) {
          logInvalidCookie(cookieValue)
          return null
        }
      }
      // Old format:
      val values = cookieValue.split(":".toRegex()).toTypedArray()
      if (values.size != 3) {
        logInvalidCookie(cookieValue)
        return null
      }
      return StayLoggedInCookieValue(values[0], values[1], values[2])
    }

    private fun logInvalidCookie(value: String) {
      val msg = "Invalid cookie found: " + StringUtils.abbreviate(value, 10)
      log.warn(msg)
      logSecurityWarn(this.javaClass, "LOGIN FAILED", msg)
    }
  }
}
