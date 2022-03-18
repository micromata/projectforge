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

import mu.KotlinLogging
import org.projectforge.login.LoginService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.support.WebApplicationContextUtils
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * SwaggerFilter is used for /swagger pages (Swagger stuff).
 */
class SwaggerUIFilter : Filter {
  @Autowired
  private lateinit var loginService: LoginService

  @Throws(ServletException::class)
  override fun init(filterConfig: FilterConfig) {
    WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.servletContext)
      .autowireCapableBeanFactory.autowireBean(this)
  }

  @Throws(IOException::class, ServletException::class)
  override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    request as HttpServletRequest
    if (log.isDebugEnabled) {
      log.debug("doFilter ${request.requestURI}: ${request.getSession(false)?.id}")
    }
    SecurityFilter.doFilterLoginRequired(loginService, request, response, chain)
  }

  companion object {
    @JvmField
    val SWAGGER_ROOT_NON_TRAILING_SLASH = "swagger/"

    @JvmField
    val SWAGGER_ROOT = "/$SWAGGER_ROOT_NON_TRAILING_SLASH"

    @JvmStatic
    val enabled: Boolean
      get() = SpringFoxConfig.swaggerEnabled
  }
}
