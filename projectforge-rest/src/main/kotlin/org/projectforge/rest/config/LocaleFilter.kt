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

package org.projectforge.rest.config

import org.projectforge.business.user.UserLocale
import kotlin.Throws
import java.io.IOException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.login.LoginService
import javax.servlet.*
import javax.servlet.http.HttpServletRequest

/**
 * Sets locale to ThreadLocale (used by public services such as login and data transfer).
 */
class LocaleFilter : Filter {
  /**
   * NOP.
   * @see Filter.destroy
   */
  override fun destroy() {}

  /**
   * @see Filter.doFilter
   */
  @Throws(IOException::class, ServletException::class)
  override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    try {
      // LoginService.getUser is only given for public services on 2FA check on login.
      val locale = UserLocale.determineUserLocale(LoginService.getUser(request as HttpServletRequest), request = request)
      ThreadLocalUserContext.locale = locale
      chain.doFilter(request, response)
    } finally {
      ThreadLocalUserContext.clear()
    }
  }

  /**
   * NOP.
   * @see Filter.init
   */
  @Throws(ServletException::class)
  override fun init(fConfig: FilterConfig) {
  }
}
