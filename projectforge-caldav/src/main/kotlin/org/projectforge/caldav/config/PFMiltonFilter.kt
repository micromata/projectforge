/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.caldav.config

import io.milton.servlet.MiltonFilter
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.web.rest.RestAuthenticationInfo
import org.projectforge.web.rest.RestAuthenticationUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest

/**
 * Ensuring a white url list for using Milton filter. MiltonFilter at default supports only black list.
 */
class PFMiltonFilter : MiltonFilter() {
    private lateinit var springContext: WebApplicationContext
    @Autowired
    private lateinit var restAuthenticationUtils: RestAuthenticationUtils
    @Autowired
    private lateinit var userAuthenticationsService: UserAuthenticationsService

    companion object {
        private val log = LoggerFactory.getLogger(PFMiltonFilter::class.java)
    }

    @Throws(ServletException::class)
    override fun init(filterConfig: FilterConfig) {
        super.init(filterConfig)
        springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.servletContext)
        val beanFactory = springContext.getAutowireCapableBeanFactory()
        beanFactory.autowireBean(this)
    }

    private fun authenticate(authInfo: RestAuthenticationInfo) {
        return restAuthenticationUtils.basicAuthentication(authInfo, true) { userString, authenticationToken ->
            val authenticatedUser = userAuthenticationsService.getUserByToken(authInfo.request, userString, UserTokenType.DAV_TOKEN, authenticationToken)
            if (authenticatedUser == null) {
                log.error("Can't authenticate user '$userString' by given token. User name and/or token invalid.")
            }
            authenticatedUser
        }
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        request as HttpServletRequest
        if (!DAVMethodsInterceptor.isMethodSupported(request)) {
            // Not for us:
            chain.doFilter(request, response)
        } else {
            log.info("request ${request.requestURI} with method=${request.method} for Milton...")
            restAuthenticationUtils.doFilter(request,
                    response,
                    UserTokenType.DAV_TOKEN,
                    authenticate = { authInfo -> authenticate(authInfo) },
                    doFilter = { -> super.doFilter(request, response, chain) }
            )
        }
    }
}
