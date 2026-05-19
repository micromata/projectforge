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
import mu.KotlinLogging
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Sets up ThreadLocalUserContext from the OAuth2 session for authenticated requests.
 * This ensures PF business logic has access to the logged-in user.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewaySessionFilter(
    private val userDao: UserDao,
) : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication != null && authentication.isAuthenticated) {
                val principal = authentication.principal
                if (principal is OidcUser && ThreadLocalUserContext.loggedInUser == null) {
                    val sub = principal.subject
                    val username = principal.preferredUsername
                    val pfUser = userDao.getUserByIdpExternalId(sub)
                        ?: userDao.getInternalByName(username)
                    if (pfUser != null) {
                        ThreadLocalUserContext.userContext = UserContext(pfUser)
                    }
                }
            }
            chain.doFilter(request, response)
        } finally {
            ThreadLocalUserContext.clear()
        }
    }
}
