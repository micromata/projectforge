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

package org.projectforge.web.rest

import mu.KotlinLogging
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.utils.RequestLog
import org.projectforge.security.SecurityLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils
import java.io.IOException
import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Does the authentication stuff for restful requests.
 *
 * @author Daniel Ludwig (d.ludwig@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractRestUserFilter(val userTokenType: UserTokenType) : Filter {
  private lateinit var springContext: WebApplicationContext

  @Autowired
  lateinit var restAuthenticationUtils: RestAuthenticationUtils

  @Autowired
  lateinit var userAuthenticationsService: UserAuthenticationsService

  @Autowired
  lateinit var userService: UserService

  @Throws(ServletException::class)
  override fun init(filterConfig: FilterConfig) {
    springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.servletContext)
    val beanFactory = springContext.getAutowireCapableBeanFactory()
    beanFactory.autowireBean(this)
  }

  abstract fun authenticate(authInfo: RestAuthenticationInfo)

  /**
   * @see jakarta.servlet.Filter.doFilter
   */
  @Throws(IOException::class, ServletException::class)
  override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    if (log.isDebugEnabled) {
      log.debug("Processing request ${RequestLog.asString(request as HttpServletRequest)}...")
    }
    ThreadLocalUserContext.userContext?.let { userContext ->
      // Paranoia:
      SecurityLogging.logSecurityWarn(
        request as HttpServletRequest,
        this::class.java,
        "UserContext in ThreadLocal is given on request start!!!!!: ${userContext.user}"
      )
      ThreadLocalUserContext.userContext = null
    }
    restAuthenticationUtils.doFilter(request,
      response,
      userTokenType,
      authenticate = { authInfo -> authenticate(authInfo) },
      doFilter = { -> chain.doFilter(request, response) }
    )
  }

  override fun destroy() { // NOOP
  }
}
