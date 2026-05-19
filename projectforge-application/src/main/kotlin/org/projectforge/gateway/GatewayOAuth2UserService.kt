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

import mu.KotlinLogging
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
open class GatewayOAuth2UserService(
    private val userDao: UserDao,
) : OidcUserService() {

    @Throws(OAuth2AuthenticationException::class)
    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = super.loadUser(userRequest)
        val sub = oidcUser.subject
        val preferredUsername = oidcUser.preferredUsername

        // Try to find the PF user by idpExternalId (= Authentik sub claim)
        var pfUser = userDao.getUserByIdpExternalId(sub)
        if (pfUser == null && preferredUsername != null) {
            pfUser = userDao.getInternalByName(preferredUsername)
        }
        if (pfUser == null) {
            log.warn { "OAuth2 login rejected: no local user found for sub=$sub, username=$preferredUsername" }
            throw OAuth2AuthenticationException(
                OAuth2Error("user_not_found", "No local user found for this identity. User must be synced first.", null)
            )
        }
        if (pfUser.deactivated == true) {
            log.warn { "OAuth2 login rejected: user '${pfUser.username}' is deactivated" }
            throw OAuth2AuthenticationException(
                OAuth2Error("user_deactivated", "User account is deactivated.", null)
            )
        }

        log.info { "OAuth2 login successful for user '${pfUser.username}' (sub=$sub)" }
        val userContext = UserContext(pfUser)
        ThreadLocalUserContext.userContext = userContext

        return oidcUser
    }
}
