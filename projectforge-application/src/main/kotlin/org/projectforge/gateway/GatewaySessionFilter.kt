/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.gateway

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.login.LoginService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Sets up ThreadLocalUserContext from the OAuth2 session for authenticated requests.
 * This ensures PF business logic has access to the logged-in user.
 *
 * On the first request after the OAuth2 login, the user is also stored as PF user in the session
 * (see [LoginService.internalLoginInCurrentSession]): REST services such as UserStatusRest, used by the React
 * client, read the logged-in user from there.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewaySessionFilter(
    private val userDao: UserDao,
) : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            val principal = SecurityContextHolder.getContext().authentication
                ?.takeIf { it.isAuthenticated }
                ?.principal as? OidcUser
            if (principal != null && ThreadLocalUserContext.loggedInUser == null) {
                getUserContext(request as HttpServletRequest, principal)?.let {
                    ThreadLocalUserContext.userContext = it
                }
            }
            chain.doFilter(request, response)
        } finally {
            ThreadLocalUserContext.clear()
        }
    }

    private fun getUserContext(request: HttpServletRequest, principal: OidcUser): UserContext? {
        LoginService.getUserContext(request)?.let { userContext ->
            if (userContext.user?.hasSystemAccess() == true) {
                return userContext
            }
            // Deactivated by a sync since the login: log out completely (PF user and Spring's security context).
            log.info { "User '${userContext.user?.username}' isn't active anymore, invalidating gateway session." }
            request.getSession(false)?.invalidate()
            return null
        }
        val pfUser = userDao.getUserByIdpExternalId(principal.subject)
            ?: principal.preferredUsername?.let { userDao.getInternalByName(it) }
        if (pfUser == null || !pfUser.hasSystemAccess()) {
            log.warn { "OAuth2 user '${principal.preferredUsername}' (sub=${principal.subject}) not found or not active on the gateway." }
            return null
        }
        val userContext = UserContext(pfUser)
        LoginService.internalLoginInCurrentSession(request, userContext)
        log.info { "Gateway session established for user '${pfUser.username}'." }
        return userContext
    }
}
