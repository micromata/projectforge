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

package org.projectforge.security

import mu.KotlinLogging
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.login.LoginService
import org.projectforge.web.WebUtils
import org.slf4j.MDC
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Helper class for registering and unregistering user in thread. Hanlding ThreadLocalUserContext as well as MDC stuff.
 */
object RegisterUser4Thread {
  /**
   * You must use try { registerUser(...) } finally { unregisterUser() }!!!!
   *
   * @param request
   */
  fun registerUser(request: HttpServletRequest, user: PFUserDO): UserContext {
    var userContext = LoginService.getUserContext(request)
    if (userContext != null) {
      userContext.user = user // Replace by fresh user from authentication.
      ThreadLocalUserContext.setUserContext(userContext)
    } else {
      userContext = ThreadLocalUserContext.setUser(user)!!
    }
    val ip = request.getRemoteAddr()
    if (ip != null) {
      MDC.put("ip", ip)
    } else { // Only null in test case:
      MDC.put("ip", "unknown")
    }
    MDC.put("session", request.getSession(false)?.id)
    MDC.put("user", user.username)
    MDC.put("userAgent", request.getHeader("User-Agent"))
    return userContext
  }

  fun unregister() {
    ThreadLocalUserContext.setUser(null)
    MDC.remove("ip")
    MDC.remove("session")
    MDC.remove("user")
    MDC.remove("userAgent")
  }
}
