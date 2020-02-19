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
package org.projectforge.web.rest

import org.projectforge.SystemStatus
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.filter.UserFilter
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.api.UserContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Does the authentication stuff for restful requests.
 *
 * @author Daniel Ludwig (d.ludwig@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractRestUserFilter : Filter {
    private lateinit var springContext: WebApplicationContext
    @Autowired
    lateinit var restAuthenticationUtils: RestAuthenticationUtils
    @Autowired
    lateinit var userAuthenticationsService: UserAuthenticationsService
    @Autowired
    lateinit var userService: UserService
    @Autowired
    private lateinit var systemStatus: SystemStatus

    @Throws(ServletException::class)
    override fun init(filterConfig: FilterConfig) {
        springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.servletContext)
        val beanFactory = springContext.getAutowireCapableBeanFactory()
        beanFactory.autowireBean(this)
    }

    abstract fun authenticate(authInfo: RestAuthenticationInfo)

    /**
     * Authentication via request header.
     *
     *  1. Authentication userId (authenticationUserId) and authenticationToken (authenticationToken) or
     *  1. Authentication username (authenticationUsername) and password (authenticationPassword) or
     *
     *
     * @see javax.servlet.Filter.doFilter
     */
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        response as HttpServletResponse
        request as HttpServletRequest
        if (!systemStatus.upAndRunning) {
            log.error("System isn't up and running, all rest calls are denied. The system is may-be in start-up phase or in maintenance mode.")
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
            return
        }
        if (UserFilter.isUpdateRequiredFirst()) {
            log.warn("Update of the system is required first. Login via Rest not available. Administrators login required.")
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
            return
        }
        val authInfo = RestAuthenticationInfo(request, response)
        authenticate(authInfo)
        if (!authInfo.success) {
            LoginProtection.instance().incrementFailedLoginTimeOffset(authInfo.userString, authInfo.clientIpAddress)
            response.sendError(authInfo.resultCode?.value() ?: HttpServletResponse.SC_UNAUTHORIZED)
            return
        }
        try {
            restAuthenticationUtils.registerUser(request, authInfo)
            chain.doFilter(request, response)
        } finally {
            restAuthenticationUtils.unregister(request, response, authInfo)
        }
    }

    override fun destroy() { // NOOP
    }

    companion object {
        private val log = LoggerFactory.getLogger(AbstractRestUserFilter::class.java)

        fun executeLogin(request: HttpServletRequest?, userContext: UserContext?) { // Wicket part: (page.getSession() as MySession).login(userContext, page.getRequest())
            UserFilter.login(request, userContext)
        }
    }
}
