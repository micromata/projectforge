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
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.login.LoginData
import org.projectforge.login.LoginService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.RestCsrfProtection
import org.projectforge.rest.pub.LoginServiceRest
import org.projectforge.rest.pub.SystemStatusRest
import org.projectforge.rest.utils.RequestLog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * Login of projectforge-next - the only login of the application since the UILayout pages were removed, used
 * by Wicket and the legacy React app as well (they redirect to [Constants.NEXT_LOGIN_URL]).
 * Public service (available without login).
 *
 * Plain JSON instead of UILayout: next builds the form by hand and needs the login state (and the reason of a
 * failure) as data.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/${LoginNextRest.REST_PATH}")
open class LoginNextRest {
    @Autowired
    private lateinit var loginService: LoginService

    @Autowired
    private lateinit var loginServiceRest: LoginServiceRest

    @Autowired
    private lateinit var systemStatusRest: SystemStatusRest

    @Autowired
    private lateinit var twoFactorMethodsService: NextTwoFactorMethodsService

    /**
     * State of the login page. Has to be called before showing the login form: the user might already be logged-in,
     * might be pre-logged-in with a pending second factor (e. g. after a browser reload during the 2FA step), or
     * might carry a valid stay-logged-in cookie.
     */
    @GetMapping("status")
    fun getStatus(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): NextLoginState {
        // [LoginService.checkLogin] instead of getUserContext: the latter reads the http session only, so a user
        // arriving with nothing but a valid stay-logged-in cookie (typically after a server restart) was shown the
        // username/password form. The cookie is evaluated by RestUserFilter, and that one runs on /rs/* only - never
        // on this public endpoint.
        //
        // Only for same-site requests: unlike /rs/*, this endpoint has no filter and therefore no CSRF protection
        // around it, while checkLogin has side effects (it creates and rotates the session, writes the login stamp to
        // the database and refreshes the cookie's 30 days). Without this guard any foreign page could keep a
        // stay-logged-in cookie alive forever by pointing the browser here - the cookie has no SameSite=Strict and is
        // sent on a top-level navigation. Reading the state is all that is left for a cross-site caller.
        //
        // The elvis operator is required: checkLogin returns null as soon as a second factor is missing
        // (ensureSystemAccess), although the restored UserContext is already stored in the session. Re-reading it is
        // what turns that into twoFactorRequired instead of a plain 'not logged-in'.
        val userContext = if (RestCsrfProtection.isSameSiteRequest(request)) {
            loginService.checkLogin(request, response) ?: LoginService.getUserContext(request)
        } else {
            log.warn { "Cross-site request, so no stay-logged-in restore is done here: ${RequestLog.asString(request)}" }
            LoginService.getUserContext(request)
        }
        val systemData = systemStatusRest.getSystemStatus(request)
        // Username/password were OK, but the second factor is still missing:
        val twoFactorRequired = userContext?.new2FARequired == true
        return NextLoginState(
            loggedIn = userContext != null && !twoFactorRequired,
            twoFactorRequired = twoFactorRequired,
            messageOfTheDay = systemData.messageOfTheDay,
            setupRedirectUrl = systemData.setupRedirectUrl,
            methods = if (twoFactorRequired) twoFactorMethodsService.getMethods(userContext) else null,
        )
    }

    @PostMapping
    fun login(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestBody loginData: LoginData,
    ): NextLoginResult {
        val loginResultStatus = loginService.authenticate(request, response, loginData)
        if (loginResultStatus != LoginResultStatus.SUCCESS) {
            return NextLoginResult(
                status = NextLoginStatus.FAILED,
                messageKey = loginResultStatus.i18nKey,
                // Translated by the server: this is a public page, the user isn't set in ThreadLocalUserContext
                // (but the locale is, see LocaleFilter).
                message = loginResultStatus.localizedMessage,
            )
        }
        // Password was OK, but a second factor may still be missing. Read it from the session (as getStatus does):
        // ThreadLocalUserContext isn't an option here, because RestUserFilter runs on /rs/* only, not on /rsPublic/*
        // (see WebXMLInitializer), so it would always be null.
        val userContext = LoginService.getUserContext(request)
        if (userContext?.new2FARequired == true) {
            return NextLoginResult(
                status = NextLoginStatus.TWO_FACTOR_REQUIRED,
                methods = twoFactorMethodsService.getMethods(userContext),
            )
        }
        return NextLoginResult(
            status = NextLoginStatus.SUCCESS,
            redirectUrl = LoginServiceRest.getRedirectUrl(request),
        )
    }

    /**
     * Cancels a pending 2FA (or logs the user out) and clears the stay-logged-in cookie.
     */
    @GetMapping("cancel")
    fun cancel(request: HttpServletRequest, response: HttpServletResponse): NextActionResult {
        loginServiceRest.logout(request, response)
        return NextActionResult(success = true)
    }

    companion object {
        const val REST_PATH = "nextLogin"
    }
}
