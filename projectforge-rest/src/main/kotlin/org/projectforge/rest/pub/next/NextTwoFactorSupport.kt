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
import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.my2fa.My2FAServicesRest
import org.projectforge.security.My2FAService
import org.projectforge.security.OTPCheckResult
import org.projectforge.security.WebAuthnServicesRest
import org.projectforge.security.dto.WebAuthnFinishRequest
import org.projectforge.security.dto.WebAuthnPublicKeyCredentialCreationOptions
import org.projectforge.web.My2FAHttpService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The 2FA itself is identical for all three contexts of projectforge-next (after login, password reset and
 * in-session). Only the user registration differs, so the callers do the [org.projectforge.security.RegisterUser4Thread]
 * handling and the security check, and this service does the check itself.
 *
 * All methods require a registered user in [ThreadLocalUserContext].
 */
@Service
class NextTwoFactorSupport {
    @Autowired
    private lateinit var my2FAServicesRest: My2FAServicesRest

    @Autowired
    private lateinit var my2FAHttpService: My2FAHttpService

    @Autowired
    private lateinit var my2FAService: My2FAService

    @Autowired
    private lateinit var webAuthnServicesRest: WebAuthnServicesRest

    /**
     * Checks the OTP of an authenticator app or of a code sent via sms/mail. On success, the cookie and the user's
     * session are updated (and [org.projectforge.framework.persistence.user.api.UserContext.new2FARequired] is reset).
     */
    fun checkOtp(
        request: HttpServletRequest,
        response: HttpServletResponse,
        postData: NextTwoFactorPostData,
    ): NextTwoFactorResult {
        val result = my2FAServicesRest.internalCheckOTP(request, response, postData.code, postData.password)
        if (result == OTPCheckResult.SUCCESS) {
            return NextTwoFactorResult(success = true)
        }
        return NextTwoFactorResult(success = false, message = getMessage(result))
    }

    /**
     * Sends an OTP as text message to the mobile phone of the registered user.
     */
    fun sendSmsCode(request: HttpServletRequest): NextTwoFactorResult {
        val user = ThreadLocalUserContext.loggedInUser!!
        val mobilePhone = user.mobilePhone
        if (!NumberHelper.matchesPhoneNumber(mobilePhone)) {
            log.error { "User '${user.username}' tried to send 2FA code as text message, but mobile phone isn't available." }
            return NextTwoFactorResult(
                success = false,
                message = translate("address.sendSms.sendMessage.result.wrongOrMissingNumber"),
            )
        }
        if (!my2FAHttpService.smsConfigured) {
            log.error { "User tried to send a text message, but sms isn't configured." }
            return NextTwoFactorResult(
                success = false,
                message = translate("address.sendSms.sendMessage.result.unknownError"),
            )
        }
        return toResult(my2FAHttpService.createAndTextOTP(request, mobilePhone))
    }

    /**
     * Sends an OTP as mail to the registered user. Not allowed for the password reset.
     */
    fun sendMailCode(request: HttpServletRequest): NextTwoFactorResult {
        val user = ThreadLocalUserContext.loggedInUser!!
        if (user.email.isNullOrBlank()) {
            log.error { "User '${user.username}' tried to send 2FA code as mail, but e-mail address isn't available." }
            return NextTwoFactorResult(success = false, message = translate("mail.error.missingToAddress"))
        }
        if (my2FAService.isMail2FADisabledForUser()) {
            // Otherwise My2FAHttpService.createAndMailOTP would throw (require), resulting in a 500 instead of a
            // message. next doesn't offer the button in this case (see NextTwoFactorMethodsService.getMethods).
            log.error { "User '${user.username}' tried to send 2FA code as mail, but mail 2FA is disabled for the user's groups." }
            return NextTwoFactorResult(success = false, message = translate("user.My2FACode.error.validation"))
        }
        return toResult(my2FAHttpService.createAndMailOTP(request))
    }

    /**
     * Step 1 of the WebAuthn authentication: the challenge for navigator.credentials.get().
     */
    fun webAuthn(request: HttpServletRequest): WebAuthnPublicKeyCredentialCreationOptions? {
        return webAuthnServicesRest.webAuthn(request).body
    }

    /**
     * Step 2 of the WebAuthn authentication: the signed response of the authenticator.
     */
    fun webAuthnFinish(
        request: HttpServletRequest,
        response: HttpServletResponse,
        webAuthnFinishRequest: WebAuthnFinishRequest,
    ): NextTwoFactorResult {
        val result = webAuthnServicesRest.doWebAuthnFinish(request, response, webAuthnFinishRequest)
        if (result.success) {
            return NextTwoFactorResult(success = true)
        }
        return NextTwoFactorResult(
            success = false,
            message = translate(result.errorMessage ?: "user.My2FACode.error.validation"),
        )
    }

    /**
     * Same messages as used by [My2FAServicesRest.checkOTP] for the UILayout based clients.
     */
    private fun getMessage(result: OTPCheckResult): String {
        result.userMessage?.let { return it } // Brute force protection: "Please wait x seconds".
        return when (result) {
            OTPCheckResult.CODE_EMPTY -> translate("user.My2FA.setup.check.fail")
            OTPCheckResult.WRONG_LOGIN_PASSWORD -> translate("user.My2FACode.password.wrong")
            else -> translate("user.My2FACode.error.validation")
        }
    }

    private fun toResult(result: My2FAHttpService.Result): NextTwoFactorResult {
        // The message is already localized by My2FAHttpService.
        return NextTwoFactorResult(success = result.success, message = result.message)
    }
}
