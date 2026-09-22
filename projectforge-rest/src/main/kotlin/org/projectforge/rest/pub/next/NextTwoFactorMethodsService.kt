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

import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.security.My2FAService
import org.projectforge.security.webauthn.WebAuthnSupport
import org.projectforge.web.My2FAHttpService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Determines which second factors are usable for a user.
 *
 * The old React app doesn't need this: the server builds the buttons for available methods only
 * (My2FAServicesRest.fillCodeCol). projectforge-next builds its form by hand and therefore has to ask.
 * The conditions here must stay in sync with fillCodeCol.
 */
@Service
class NextTwoFactorMethodsService {
    @Autowired
    private lateinit var my2FAService: My2FAService

    @Autowired
    private lateinit var my2FAHttpService: My2FAHttpService

    @Autowired
    private lateinit var webAuthnSupport: WebAuthnSupport

    /**
     * @param userContext The user to check. Might be a pre-logged-in user (login) or a user assigned by a password
     * reset token, so don't rely on ThreadLocalUserContext here.
     * @param mailOTPDisabled True for the password reset: sending the OTP to the mail account is no second factor,
     * if the reset link was sent to the same account.
     * @param lastSuccessful2FA Human readable time ago, only useful for logged-in users.
     */
    @JvmOverloads
    fun getMethods(
        userContext: UserContext?,
        mailOTPDisabled: Boolean = false,
        lastSuccessful2FA: String? = null,
    ): TwoFactorMethods {
        val user = userContext?.user
        val smsAvailable = my2FAHttpService.smsConfigured && NumberHelper.matchesPhoneNumber(user?.mobilePhone)
        val mailAvailable = !mailOTPDisabled
                && !user?.email.isNullOrBlank()
                && !my2FAService.isMail2FADisabledForUser(userContext)
        return TwoFactorMethods(
            otp = true,
            sms = smsAvailable,
            mail = mailAvailable,
            webAuthn = webAuthnSupport.isAvailableForUser(user?.id),
            lastSuccessful2FA = lastSuccessful2FA,
        )
    }
}
