/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web

import mu.KotlinLogging
import org.projectforge.login.LogoutListener
import org.projectforge.web.session.MySession
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

/**
 * Logout any Wicket session after user's logout if exist.
 */
class WicketLogoutListener : LogoutListener {
  override fun logout(request: HttpServletRequest, response: HttpServletResponse) {
    request.getSession(false)?.let { session ->
      session.getAttribute("wicket:wicket.app:session")?.let { wicketSession ->
        if (wicketSession is MySession) {
          log.debug { "Destroying Wicket session." }
          // Doesn't work without ThreadContext: wicketSession.invalidate()
        }
      }
    }
  }
}
