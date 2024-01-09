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

package org.projectforge.security

import org.projectforge.common.logging.MDC_USER
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.slf4j.MDC

/**
 * Helper class for registering and unregistering user in thread. Hanlding ThreadLocalUserContext as well as MDC stuff.
 */
object RegisterUser4Thread {
  /**
   * You must use try { registerUser(...) } finally { unregisterUser() }!!!!
   * Please note: ip, session, userAgent is already added by LoggingFilter.
   *
   * @param request
   */
  fun registerUser(userContext: UserContext) {
    ThreadLocalUserContext.userContext = userContext
    MDC.put(MDC_USER, userContext.user!!.username)
  }

  /**
   * You must use try { registerUser(...) } finally { unregisterUser() }!!!!
   * Please note: ip, session, userAgent is already added by LoggingFilter.
   *
   * @param request
   */
  fun registerUser(user: PFUserDO) {
    registerUser(UserContext(user))
    MDC.put(MDC_USER, user.username)
  }

  fun unregister() {
    ThreadLocalUserContext.setUser(null)
    MDC.remove(MDC_USER) // Will be also removed by LoggingFilter, but who knows really?
  }
}
