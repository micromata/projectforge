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

package org.projectforge.rest.pub.next

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.projectforge.login.LoginService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.pub.LoginServiceRest
import org.projectforge.security.RegisterUser4Thread
import org.projectforge.security.SecurityLogging
import org.projectforge.security.dto.WebAuthnPublicKeyCredentialCreationOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Second factor directly after the login of projectforge-next. Public service, but the user must be pre-logged-in by
 * username and password (a [org.projectforge.framework.persistence.user.api.UserContext] must exist in the session).
 *
 * Same purpose as [org.projectforge.rest.pub.My2FAPublicServicesRest], but with plain JSON for next.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/${TwoFactorLoginNextRest.REST_PATH}")
open class TwoFactorLoginNextRest {
    @Autowired
    private lateinit var loginServiceRest: LoginServiceRest

    @Autowired
    private lateinit var twoFactorSupport: NextTwoFactorSupport

    @PostMapping("checkOTP")
    fun checkOtp(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestBody postData: NextTwoFactorPostData,
    ): ResponseEntity<NextTwoFactorResult> {
        return authenticated(request) {
            val result = twoFactorSupport.checkOtp(request, response, postData)
            if (!result.success) {
                result
            } else {
                // The user is fully logged-in now, so tell the client where to continue:
                NextTwoFactorResult(
                    success = true,
                    redirectUrl = LoginServiceRest.getRedirectUrl(request, null),
                )
            }
        }
    }

    @GetMapping("sendSmsCode")
    fun sendSmsCode(request: HttpServletRequest): ResponseEntity<NextTwoFactorResult> {
        return authenticated(request) { twoFactorSupport.sendSmsCode(request) }
    }

    @GetMapping("sendMailCode")
    fun sendMailCode(request: HttpServletRequest): ResponseEntity<NextTwoFactorResult> {
        return authenticated(request) { twoFactorSupport.sendMailCode(request) }
    }

    @GetMapping("webAuthn")
    fun webAuthn(request: HttpServletRequest): ResponseEntity<WebAuthnPublicKeyCredentialCreationOptions?> {
        return authenticated(request) { twoFactorSupport.webAuthn(request) }
    }

    @PostMapping("webAuthnFinish")
    fun webAuthnFinish(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestBody postData: NextWebAuthnPostData,
    ): ResponseEntity<NextTwoFactorResult> {
        val webAuthnFinishRequest = postData.webAuthnFinishRequest
            ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        return authenticated(request) {
            val result = twoFactorSupport.webAuthnFinish(request, response, webAuthnFinishRequest)
            if (!result.success) {
                result
            } else {
                NextTwoFactorResult(
                    success = true,
                    redirectUrl = LoginServiceRest.getRedirectUrl(request, null),
                )
            }
        }
    }

    /**
     * Cancels the login process (clears the user's session as well as the stay-logged-in cookie).
     */
    @GetMapping("cancel")
    fun cancel(request: HttpServletRequest, response: HttpServletResponse): NextActionResult {
        loginServiceRest.logout(request, response)
        return NextActionResult(success = true)
    }

    /**
     * The user must be pre-logged-in by username/password, otherwise this public service is denied.
     */
    private fun <T> authenticated(request: HttpServletRequest, doIt: () -> T): ResponseEntity<T> {
        val userContext = LoginService.getUserContext(request)
        if (userContext?.user == null) {
            SecurityLogging.logSecurityWarn(
                request,
                this::class.java,
                "Not logged-in user tried to do a 2FA after login (denied)"
            )
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        try {
            RegisterUser4Thread.registerUser(userContext)
            return ResponseEntity.ok(doIt())
        } finally {
            RegisterUser4Thread.unregister()
        }
    }

    companion object {
        const val REST_PATH = "next2FALogin"
    }
}
