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

package org.projectforge.carddav

import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.carddav.CardDavInit.Companion.cardDavBasePath
import org.projectforge.rest.utils.RequestLog
import org.projectforge.security.SecurityLogging
import org.projectforge.web.rest.BasicAuthenticationData
import org.projectforge.web.rest.RestAuthenticationInfo
import org.projectforge.web.rest.RestAuthenticationUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils
import java.io.IOException

private val log = KotlinLogging.logger {}

/**
 * Ensuring a positive url list for using CardDav services.
 */
class CardDavFilter : Filter {
    private lateinit var springContext: WebApplicationContext

    @Autowired
    private lateinit var cardDavService: CardDavService

    @Throws(ServletException::class)
    override fun init(filterConfig: FilterConfig) {
        super.init(filterConfig)
        springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.servletContext)
        val beanFactory = springContext.autowireCapableBeanFactory
        beanFactory.autowireBean(this)
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        request as HttpServletRequest
        if (log.isDebugEnabled) {
            var username: String? = null
            val authHeader = RestAuthenticationUtils.getHeader(request, "authorization", "Authorization")
            if (authHeader != null) {
                username = BasicAuthenticationData(request, authHeader).username
            }
            log.debug {
                "CardDavFilter.doFilter: ${
                    RequestLog.asString(
                        request,
                        username
                    )
                }"
            }
        }
        if (!handledByCardDavFilter(request)) {
            if (log.isDebugEnabled) {
                log.debug(
                    "Request is not for us (neither CalDAV nor CardDAV-call), processing normal filter chain (${
                        RequestLog.asString(
                            request
                        )
                    })..."
                )
            }
            // Not for us:
            chain.doFilter(request, response)
            return
        }
        if (request.method == "PUT") {
            log.info { "DAV doesn't support PUT method (yet): ${request.requestURI}" }
            response as HttpServletResponse
            response.sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "PUT not (yet) supported by ProjectForge."
            )
            return
        }
        log.info(
            "Request with method=${request.method} for CardDav (${
                RequestLog.asString(
                    request
                )
            })..."
        )
        log.debug { "Request-Info: ${RequestLog.asJson(request, true)}" }
        cardDavService.dispatch(request, response as HttpServletResponse)
    }

    companion object {
        /**
         * @return true if given is handled by CardDavController. Otherwise, false.
         */
        fun handledByCardDavFilter(request: HttpServletRequest): Boolean {
            val uri = request.requestURI
            return when (request.method) {
                "PROPFIND" -> {
                    log.debug { "PROPFIND call detected: $uri" }
                    return urlMatches(uri, "/users/")
                }

                "OPTIONS" -> {
                    log.debug { "OPTIONS call detected: $uri" }
                    return urlMatches(uri, "/users/")
                }

                else -> {
                    false
                }
            }
        }

        /**
         * @param uri The URI to check.
         * @param paths The path to check, must start with /.
         * @return true if given URI is a CardDav URI.
         */
        internal fun urlMatches(uri: String, vararg paths: String): Boolean {
            return if (cardDavBasePath == "/") { // Avoid //users instead of /users:
                paths.any { uri.startsWith(it) }
            } else {
                paths.any { uri.startsWith("$cardDavBasePath$it") }
            }
        }
    }
}
