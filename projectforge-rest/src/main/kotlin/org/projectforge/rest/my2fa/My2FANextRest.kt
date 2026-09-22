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

package org.projectforge.rest.my2fa

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.projectforge.framework.i18n.Duration
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.pub.next.NextTwoFactorMethodsService
import org.projectforge.rest.pub.next.NextTwoFactorPostData
import org.projectforge.rest.pub.next.NextTwoFactorResult
import org.projectforge.rest.pub.next.NextTwoFactorState
import org.projectforge.rest.pub.next.NextTwoFactorSupport
import org.projectforge.rest.pub.next.NextWebAuthnPostData
import org.projectforge.security.My2FAService
import org.projectforge.security.dto.WebAuthnPublicKeyCredentialCreationOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Second factor of an already logged-in user of projectforge-next, required by [org.projectforge.security.My2FARequestHandler]
 * for protected actions.
 *
 * Same purpose as [My2FAPageRest] / [My2FAServicesRest], but with plain JSON: next shows its own dialog instead of the
 * UILayout based page.
 *
 * This path has to be part of My2FARequestHandler.NO_2FA_URLS, otherwise the 2FA would require a 2FA itself.
 */
@RestController
@RequestMapping("${Rest.URL}/${My2FANextRest.REST_PATH}")
open class My2FANextRest {
    @Autowired
    private lateinit var twoFactorMethodsService: NextTwoFactorMethodsService

    @Autowired
    private lateinit var twoFactorSupport: NextTwoFactorSupport

    /**
     * @param expiryMillis The expiry period of the protected action (only for the message shown to the user), see
     * [My2FAPageRest.getForm].
     */
    @GetMapping("status")
    fun getStatus(@RequestParam expiryMillis: Long? = null): NextTwoFactorState {
        val expiryMessage = if (expiryMillis != null && expiryMillis > 0) {
            translateMsg("user.My2FA.expired", Duration.getMessage(expiryMillis))
        } else {
            null
        }
        return NextTwoFactorState(
            methods = twoFactorMethodsService.getMethods(
                ThreadLocalUserContext.userContext,
                lastSuccessful2FA = My2FAService.getLastSuccessful2FAAsTimeAgo(),
            ),
            expiryMessage = expiryMessage,
        )
    }

    @PostMapping("checkOTP")
    fun checkOtp(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestBody postData: NextTwoFactorPostData,
    ): NextTwoFactorResult {
        return twoFactorSupport.checkOtp(request, response, postData)
    }

    @GetMapping("sendSmsCode")
    fun sendSmsCode(request: HttpServletRequest): NextTwoFactorResult {
        return twoFactorSupport.sendSmsCode(request)
    }

    @GetMapping("sendMailCode")
    fun sendMailCode(request: HttpServletRequest): NextTwoFactorResult {
        return twoFactorSupport.sendMailCode(request)
    }

    @GetMapping("webAuthn")
    fun webAuthn(request: HttpServletRequest): WebAuthnPublicKeyCredentialCreationOptions? {
        return twoFactorSupport.webAuthn(request)
    }

    @PostMapping("webAuthnFinish")
    fun webAuthnFinish(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestBody postData: NextWebAuthnPostData,
    ): ResponseEntity<NextTwoFactorResult> {
        val webAuthnFinishRequest = postData.webAuthnFinishRequest
            ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        return ResponseEntity.ok(twoFactorSupport.webAuthnFinish(request, response, webAuthnFinishRequest))
    }

    companion object {
        /**
         * Also listed in My2FARequestHandler.NO_2FA_URLS (the business module can't reference this class).
         */
        const val REST_PATH = "next2FA"
    }
}
