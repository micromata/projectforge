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

import org.projectforge.security.dto.WebAuthnFinishRequest

/**
 * Plain JSON DTOs for the authentication of projectforge-next.
 *
 * The classical login pages ([org.projectforge.rest.pub.LoginPageRest],
 * [org.projectforge.rest.pub.PasswordResetPageRest], [org.projectforge.rest.my2fa.My2FAPageRest]) describe themselves
 * as UILayout for the backend driven renderer of the old React app. projectforge-next builds its forms by hand, so it
 * needs the plain state instead of a layout: which 2FA methods are usable at all, why a login failed etc.
 */

/**
 * Which second factors the user may actually use. The frontend must not offer a method the server would reject
 * (same conditions as used by [org.projectforge.rest.my2fa.My2FAServicesRest] for building its buttons).
 */
class TwoFactorMethods(
    /**
     * OTP of an authenticator app (or a code sent before). Always available.
     */
    val otp: Boolean = true,
    /**
     * Only if sms is configured and the user has a valid mobile phone number.
     */
    val sms: Boolean = false,
    /**
     * Not available for the password reset (the mail account might be the compromised one).
     */
    val mail: Boolean = false,
    /**
     * Only if the user has registered at least one token.
     */
    val webAuthn: Boolean = false,
    /**
     * Human readable time ago of the last successful 2FA, if any: "3 minutes ago".
     */
    val lastSuccessful2FA: String? = null,
)

/**
 * Result of a login attempt or of a second factor check.
 */
enum class NextLoginStatus {
    SUCCESS,

    /**
     * Username and password were OK, but the user has to enter a second factor before being logged-in.
     */
    TWO_FACTOR_REQUIRED,
    FAILED,
}

class NextLoginResult(
    val status: NextLoginStatus,
    /**
     * I18n key of [org.projectforge.business.login.LoginResultStatus], e. g. 'login.error.loginFailed'. For logging
     * and tests, the frontend should display [message].
     */
    val messageKey: String? = null,
    /**
     * Localized message. The public pages can't translate on the client side, because the user isn't known yet
     * (see comment in LoginPageRest.getForm).
     */
    val message: String? = null,
    /**
     * Where to go after a successful login (origin url of the caller), if given.
     */
    val redirectUrl: String? = null,
    /**
     * Only given for [NextLoginStatus.TWO_FACTOR_REQUIRED].
     */
    val methods: TwoFactorMethods? = null,
)

/**
 * State of the login page: is a login needed at all, is a second factor pending, and what to display.
 */
class NextLoginState(
    val loggedIn: Boolean,
    /**
     * True, if the user is pre-logged-in by username/password, but a second factor is still missing. The login page has
     * to show the 2FA form then (also after a browser reload).
     */
    val twoFactorRequired: Boolean,
    val messageOfTheDay: String? = null,
    /**
     * If given, the client has to redirect to this url (setup wizard).
     */
    val setupRedirectUrl: String? = null,
    val methods: TwoFactorMethods? = null,
)

/**
 * Result of a second factor check outside of the login (in-session 2FA and password reset).
 */
class NextTwoFactorResult(
    val success: Boolean,
    /**
     * Localized message, given on failure.
     */
    val message: String? = null,
    /**
     * Where to continue after a successful 2FA, if the server knows a target.
     */
    val redirectUrl: String? = null,
)

class NextTwoFactorState(
    val methods: TwoFactorMethods,
    /**
     * Localized hint, why a 2FA is required now ("Your last check is older than ...").
     */
    val expiryMessage: String? = null,
)

/**
 * Post data of a 2FA check. Own class instead of [org.projectforge.security.My2FAData], because next doesn't use
 * the UILayout protocol (no serverData/target/modal).
 */
class NextTwoFactorPostData(
    var code: String? = null,
    /**
     * Only needed as additional factor if the OTP was sent via e-mail.
     */
    var password: CharArray? = null,
)

class NextWebAuthnPostData(
    var webAuthnFinishRequest: WebAuthnFinishRequest? = null,
)

/**
 * Request of a password reset mail.
 */
class NextPasswordForgottenPostData(
    var usernameEmail: String? = null,
)

/**
 * State of the password reset page (after clicking the link of the mail).
 */
class NextPasswordResetState(
    /**
     * False, if the token is unknown or expired. The client shows an error then.
     */
    val tokenValid: Boolean,
    val username: String? = null,
    /**
     * True, if the user already did a successful 2FA (not older than 10 minutes). Only then the password fields
     * may be shown.
     */
    val twoFactorDone: Boolean = false,
    val methods: TwoFactorMethods? = null,
    /**
     * Needed for the final post of the new password.
     */
    val csrfToken: String? = null,
)

class NextPasswordResetPostData(
    var newPassword: CharArray? = null,
    var newPasswordRepeat: CharArray? = null,
    var csrfToken: String? = null,
)

/**
 * Body of the 403 response, if [org.projectforge.security.My2FARequestHandler] requires a (new) second factor for a
 * request of projectforge-next. The client shows its 2FA dialog and repeats the request afterwards.
 *
 * The old React app gets a [org.projectforge.ui.ResponseAction] with the url of the UILayout based 2FA page instead
 * (see RestAuthenticationUtils.doFilter).
 */
class TwoFactorRequired(
    val twoFactorRequired: Boolean = true,
    /**
     * The expiry period of the protected action in millis (only used for the message shown to the user).
     */
    val expiryMillis: Long? = null,
)

/**
 * Generic result with optional field bound error message, used by the password reset.
 */
class NextActionResult(
    val success: Boolean,
    val message: String? = null,
    /**
     * Field id of the error, if the message belongs to a single input.
     */
    val field: String? = null,
)
