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
import org.projectforge.business.user.filter.CookieService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.login.LoginService
import org.projectforge.security.OTPCheckResult
import org.projectforge.web.My2FAHttpService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Check of a code (OTP of an Authenticator app, or sent by sms or e-mail) of the logged-in user and the bookkeeping
 * of a successful 2FA (cookie and session), shared by the 2FA dialog of projectforge-next ([My2FANextRest]) and the
 * WebAuthn authentication. The rest endpoints of the former React dialog (`/rs/2FA/...`) are gone.
 */
@Service
class My2FAServicesRest {
    @Autowired
    private lateinit var cookieService: CookieService

    @Autowired
    private lateinit var loginService: LoginService

    @Autowired
    private lateinit var my2FAHttpService: My2FAHttpService

    /**
     * Checks the given otp and stores it as cookie and in the user's current session on success.
     */
    fun internalCheckOTP(
        request: HttpServletRequest,
        response: HttpServletResponse,
        otp: String?,
        password: CharArray? = null
    ): OTPCheckResult {
        val otpCheck = my2FAHttpService.checkOTP(request, code = otp, password = password)
        if (otpCheck == OTPCheckResult.SUCCESS) {
            updateCookieAndSession(request, response)
        }
        return otpCheck
    }

    /**
     * Should be called after [ThreadLocalUserContext.getUserContext] -> [UserContext.updateLastSuccessful2FA].
     * Updates the time stamp of the last successful 2FA as cookie as well as in the user's session.
     */
    fun updateCookieAndSession(request: HttpServletRequest, response: HttpServletResponse) {
        ThreadLocalUserContext.userContext!!.lastSuccessful2FA?.let { lastSuccessful2FA ->
            cookieService.addLast2FACookie(request, response, lastSuccessful2FA)
            // Store it also in the user's session, e. g. used by public password reset service.
            request.getSession(false)?.setAttribute(SESSION_KEY_LAST_SUCCESSFUL_2FA, lastSuccessful2FA)
        }
        // A stay-logged-in cookie the user asked for at login time is only issued now, after the second
        // factor. Every 2FA path (OTP as well as WebAuthn) passes through here.
        loginService.onSecondFactorSucceeded(request, response)
    }

    companion object {
        private val SESSION_KEY_LAST_SUCCESSFUL_2FA = "${My2FAServicesRest::class.java.name}.lastSuccessFul2FA"

        /**
         * Gets the last successful 2FA from the user's session.
         */
        fun getLastSuccessful2FAFromSession(request: HttpServletRequest): Long? {
            return request.getSession(false)?.getAttribute(SESSION_KEY_LAST_SUCCESSFUL_2FA) as? Long
        }
    }
}
