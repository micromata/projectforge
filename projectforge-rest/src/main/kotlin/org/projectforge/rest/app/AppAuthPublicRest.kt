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


package org.projectforge.rest.app

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.user.AppAuthService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.security.SecurityLogging
import org.projectforge.web.WebUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * The public half of the app login (see [AppAuthService]): the app exchanges the one-time code it got from the
 * browser for its token. The server information the app shows before the login is `/rsPublic/systemStatus`.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/appAuth")
class AppAuthPublicRest {
    @Autowired
    private lateinit var appAuthService: AppAuthService

    class TokenRequest(val code: String? = null, val codeVerifier: String? = null)

    class TokenResponse(
        val userId: Long,
        val username: String?,
        val fullname: String?,
        val deviceId: Long,
        /** Sent as `Authorization: PFApp <token>` with every request. Shown nowhere, stored in the key chain. */
        val token: String,
        /**
         * The cookies of an SSO proxy in front of the server (name to value), taken from the browser login: the app
         * sends them with each request and puts them into its WebView. Empty without such a proxy.
         */
        val proxyCookies: Map<String, String>,
    )

    /**
     * Guessing a code is throttled per ip address like the credentials of [org.projectforge.rest.pub.AuthenticationPublicServicesRest];
     * a refusal is a plain 400 without any detail.
     */
    @AccessChecked("PUBLIC: one-time code and PKCE code verifier required")
    @PostMapping("token")
    fun token(request: HttpServletRequest, @RequestBody body: TokenRequest): ResponseEntity<*> {
        val clientIp = WebUtils.getClientIp(request)
        val loginProtection = LoginProtection.instance()
        // The ip address as user key, see AuthenticationPublicServicesRest.getAuthenticationCredentials.
        val offset = loginProtection.getFailedLoginTimeOffsetIfExists(clientIp, null, AUTHENTICATION_TYPE)
        if (offset > 0) {
            SecurityLogging.logSecurityWarn(
                request, this::class.java, "APP AUTHENTICATION DENIED",
                "Access denied for ${offset / 1000} seconds due to failed attempts with an invalid app code.",
            )
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid call.")
        }
        val exchanged = appAuthService.exchange(body.code, body.codeVerifier, request)
        if (exchanged == null) {
            loginProtection.incrementFailedLoginTimeOffset(clientIp, null, AUTHENTICATION_TYPE)
            SecurityLogging.logSecurityWarn(
                request, this::class.java, "APP AUTHENTICATION FAILED",
                "App code unknown, expired or the code verifier doesn't match.",
            )
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid call.")
        }
        loginProtection.clearLoginTimeOffset(clientIp, null, null, AUTHENTICATION_TYPE)
        val user = exchanged.user
        log.info { "App device #${exchanged.device.id} of user '${user.username}' got its token." }
        return ResponseEntity.ok(
            TokenResponse(
                userId = user.id!!,
                username = user.username,
                fullname = user.getFullname(),
                deviceId = exchanged.device.id!!,
                token = exchanged.token,
                proxyCookies = exchanged.proxyCookies,
            )
        )
    }

    companion object {
        private const val AUTHENTICATION_TYPE = "appAuthToken"
    }
}
