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
import org.projectforge.Constants
import org.projectforge.business.user.UserLocale
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.i18n.I18nKeys
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.service.PasswordResetService
import org.projectforge.framework.time.TimeUnit
import org.projectforge.login.LoginService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.my2fa.My2FAServicesRest
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Arrays

private val log = KotlinLogging.logger {}

/**
 * Password forgotten and password reset of projectforge-next. Public service (available without login).
 *
 * Same functionality as [org.projectforge.rest.pub.PasswordForgottenPageRest] and
 * [org.projectforge.rest.pub.PasswordResetPageRest], but with plain JSON for next.
 *
 * The user is assigned by the token of the reset mail (not by a login), and a 2FA is required before the new password
 * may be entered. A OTP via mail is not offered here: the reset link was sent to the same mail account.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/${PasswordResetNextRest.REST_PATH}")
open class PasswordResetNextRest : AbstractDynamicPageRest() {
    @Autowired
    private lateinit var loginServiceRest: LoginServiceRest

    @Autowired
    private lateinit var passwordResetService: PasswordResetService

    @Autowired
    private lateinit var twoFactorMethodsService: NextTwoFactorMethodsService

    @Autowired
    private lateinit var twoFactorSupport: NextTwoFactorSupport

    @Autowired
    private lateinit var userService: UserService

    /**
     * Requests a mail with a password reset link. Doesn't tell the client whether the user exists (no user
     * enumeration): [PasswordResetService.sendMail] sends the mail in an own thread for the very same reason.
     */
    @PostMapping("requestMail")
    fun requestMail(
        request: HttpServletRequest,
        @RequestBody postData: NextPasswordForgottenPostData,
    ): ResponseEntity<NextActionResult> {
        if (LoginService.getUserContext(request) != null) {
            return ResponseEntity.badRequest().body(
                NextActionResult(success = false, message = translate(I18nKeys.ERROR_NOT_AVAILABLE_FOR_LOGGED_IN_USERS))
            )
        }
        val usernameEmail = postData.usernameEmail
        if (usernameEmail.isNullOrBlank()) {
            return ResponseEntity.ok(
                NextActionResult(
                    success = false,
                    message = translate("password.reset.username_email") + ": " + translate("validation.error.fieldRequired"),
                    field = FIELD_ID_USERNAME_EMAIL,
                )
            )
        }
        // "TOKEN" is replaced by the generated token, see PasswordResetService.sendPasswordReset:
        val link = getUrl("/${Constants.NEXT_APP_PATH}$NEXT_RESET_PAGE?token=TOKEN")
        passwordResetService.sendMail(usernameEmail, link)
        return ResponseEntity.ok(
            NextActionResult(
                success = true,
                message = translateMsg("password.forgotten.mailSentTo", usernameEmail),
            )
        )
    }

    /**
     * State of the password reset page.
     *
     * @param token The token sent by mail (mandatory for getting and checking the user).
     * @see PasswordResetService.checkToken
     */
    @GetMapping("status")
    fun getStatus(request: HttpServletRequest, @RequestParam("token") token: String): NextPasswordResetState {
        if (LoginService.getUserContext(request) != null) {
            log.warn { "Logged-in user tried to use the password reset page." }
            return NextPasswordResetState(tokenValid = false)
        }
        passwordResetService.checkToken(token)?.let { user ->
            request.getSession(true).setAttribute(SESSION_ATTRIBUTE_DATA, SessionData(token, user))
            UserLocale.registerLocale(request, user)
        }
        val data = getSessionData(request) ?: return NextPasswordResetState(tokenValid = false)
        val user = data.user
        return NextPasswordResetState(
            tokenValid = true,
            username = user.username,
            twoFactorDone = hasSuccessful2FA(request),
            // A OTP by mail is no second factor here (the link was sent to the same mail account):
            methods = twoFactorMethodsService.getMethods(UserContext(user), mailOTPDisabled = true),
            csrfToken = createServerData(request).csrfToken,
        )
    }

    /**
     * Sets the new password. Requires a successful 2FA (not older than 10 minutes).
     */
    @PostMapping
    fun setPassword(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestBody postData: NextPasswordResetPostData,
    ): ResponseEntity<NextActionResult> {
        if (LoginService.getUserContext(request) != null) {
            return ResponseEntity.badRequest().body(
                NextActionResult(success = false, message = translate(I18nKeys.ERROR_NOT_AVAILABLE_FOR_LOGGED_IN_USERS))
            )
        }
        val data = getSessionData(request)
            ?: return ResponseEntity.badRequest()
                .body(NextActionResult(success = false, message = translate("password.reset.error")))
        if (!hasSuccessful2FA(request)) {
            SecurityLogging.logSecurityWarn(
                request,
                this::class.java,
                "User tried to reset the password without a successful 2FA (denied)"
            )
            return ResponseEntity.badRequest()
                .body(NextActionResult(success = false, message = translate("user.My2FA.required")))
        }
        if (!sessionCsrfService.checkToken(request, postData.csrfToken)) {
            log.warn { "Password reset declined: csrf token check failed." }
            return ResponseEntity.badRequest()
                .body(NextActionResult(success = false, message = translate("errorpage.csrfError")))
        }
        val newPassword = postData.newPassword
        if (newPassword == null || !Arrays.equals(newPassword, postData.newPasswordRepeat)) {
            return ResponseEntity.ok(
                NextActionResult(
                    success = false,
                    message = translate("user.error.passwordAndRepeatDoesNotMatch"),
                    field = "newPasswordRepeat",
                )
            )
        }
        log.info { "The user wants to change his password." }
        val errorMsgKeys = userService.internalChangePasswordAfterPasswordReset(data.user.id, newPassword)
        if (!errorMsgKeys.isNullOrEmpty()) {
            return ResponseEntity.ok(
                NextActionResult(
                    success = false,
                    message = errorMsgKeys.joinToString(" ") { translateMsg(it.key, *it.params) },
                    field = "newPassword",
                )
            )
        }
        cancel(request, response) // Invalidate the token and clear the session.
        return ResponseEntity.ok(
            NextActionResult(
                success = true,
                message = translate("user.changePassword.msg.passwordSuccessfullyChanged"),
            )
        )
    }

    @PostMapping("checkOTP")
    fun checkOtp(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestBody postData: NextTwoFactorPostData,
    ): ResponseEntity<NextTwoFactorResult> {
        return authenticated(request) { twoFactorSupport.checkOtp(request, response, postData) }
    }

    @GetMapping("sendSmsCode")
    fun sendSmsCode(request: HttpServletRequest): ResponseEntity<NextTwoFactorResult> {
        return authenticated(request) { twoFactorSupport.sendSmsCode(request) }
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
            twoFactorSupport.webAuthnFinish(request, response, webAuthnFinishRequest)
        }
    }

    /**
     * Cancels the password reset: the token is deleted (the link of the mail is invalid afterwards) and the session
     * is cleared.
     */
    @GetMapping("cancel")
    fun cancel(request: HttpServletRequest, response: HttpServletResponse): NextActionResult {
        getSessionData(request)?.let {
            passwordResetService.deleteToken(it.token)
            request.getSession(false)?.removeAttribute(SESSION_ATTRIBUTE_DATA)
        }
        loginServiceRest.logout(request, response)
        return NextActionResult(success = true)
    }

    /**
     * The user isn't logged-in here, but assigned by the token of the reset mail. This user has to be registered in
     * the thread, because the 2FA services work with the logged-in user.
     */
    private fun <T> authenticated(request: HttpServletRequest, doIt: () -> T): ResponseEntity<T> {
        if (LoginService.getUserContext(request) != null) {
            log.warn { "Logged-in user tried to do a 2FA of the password reset (denied)." }
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val data = getSessionData(request)
        if (data == null) {
            SecurityLogging.logSecurityWarn(
                request,
                this::class.java,
                "No password reset user tried to do a 2FA (denied)"
            )
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        try {
            RegisterUser4Thread.registerUser(data.user)
            return ResponseEntity.ok(doIt())
        } finally {
            RegisterUser4Thread.unregister()
        }
    }

    /**
     * @return true, if a successful 2FA of this session isn't older than 10 minutes (same rule as used by
     * [org.projectforge.rest.pub.PasswordResetPageRest]).
     */
    private fun hasSuccessful2FA(request: HttpServletRequest): Boolean {
        val lastSuccessful2FA = My2FAServicesRest.getLastSuccessful2FAFromSession(request) ?: return false
        return System.currentTimeMillis() - lastSuccessful2FA < 10 * TimeUnit.MINUTE.millis
    }

    private fun getSessionData(request: HttpServletRequest): SessionData? {
        return request.getSession(false)?.getAttribute(SESSION_ATTRIBUTE_DATA) as? SessionData
    }

    private class SessionData(var token: String, var user: PFUserDO) : java.io.Serializable

    companion object {
        const val REST_PATH = "nextPasswordReset"

        /**
         * Route of the password reset page of projectforge-next (below Constants.NEXT_APP_PATH).
         */
        const val NEXT_RESET_PAGE = "password-reset"

        private const val SESSION_ATTRIBUTE_DATA = "nextPasswordReset.data"
        private const val FIELD_ID_USERNAME_EMAIL = "usernameEmail"
    }
}
