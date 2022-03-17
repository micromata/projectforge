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
import org.projectforge.model.rest.RestPaths
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.support.WebApplicationContextUtils
import java.io.IOException
import java.net.URI
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

/**
 * SecurityFilter is a kind of paranoia filter which blocks all urls for unregistered users if not on a positive list.
 * So, if there is any configuration failure (e. g. wrong path settings for Swagger), no access is possible.
 */
class SecurityFilter : Filter {
  @Autowired
  private lateinit var loginService: LoginService

  private val positiveList =
    // Swagger is handled by SwaggerUIFilter
    listOf("/${RestPaths.REST_PUBLIC}/", "/favi", "/react/", "/static/", Rest.CALENDAR_EXPORT_BASE_URI, SwaggerUIFilter.SWAGGER_ROOT)

  init {
    positiveList.forEach { path ->
      if (!path.startsWith("/")) {
        log.warn { "******* Uri '$path' on white list is useless, because it doesn't start with a leading '/'. Please have a look in SecurityFilter definitions." }
      }
    }
  }

  @Throws(ServletException::class)
  override fun init(filterConfig: FilterConfig) {
    WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.servletContext)
      .autowireCapableBeanFactory.autowireBean(this)
  }

  @Throws(IOException::class, ServletException::class)
  override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    request as HttpServletRequest
    val uri = normalizeUri(request)
    positiveList.forEach { path ->
      if (uri.startsWith(path)) {
        // No login check required:
        if (log.isDebugEnabled) {
          log.debug { "No login required. Url matches '$path': $uri" }
        }
        chain.doFilter(request, response)
        return
      }
    }
    if (log.isDebugEnabled) {
      log.debug("doFilter ${request.requestURI}: ${request.getSession(false)?.id}")
    }
    doFilterLoginRequired(loginService, request, response, chain)
  }

  companion object {
    fun doFilterLoginRequired(
      loginService: LoginService,
      request: ServletRequest,
      response: ServletResponse,
      chain: FilterChain
    ) {
      val userContext = loginService.checkLogin(request as HttpServletRequest, response as HttpServletResponse)
      if (userContext?.user != null) {
        chain.doFilter(request, response)
      } else {
        response.sendError(401, "Unauthorized")
      }
    }

    fun normalizeUri(request: HttpServletRequest): String {
      return normalizeUri(request.requestURI)
    }

    /**
     * If an invalid relative url is found, "<invalid>" is returned (e. g. for "../react", because .. cannot
     * be resolved.
     * @return Absolute uri: "" -> "/", "/react" -> "/react", "/react/../rs/" -> "/rs"
     */
    fun normalizeUri(uriString: String): String {
      val path = URI(uriString).normalize().path
      return if (path.contains("..")) {
        "<invalid>" // login required
      } else if (path.startsWith("/")) {
        path
      } else {
        "/$path"
      }
    }
  }
}
