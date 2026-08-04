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
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.login.LoginData
import org.projectforge.login.LoginService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.pub.LoginServiceRest
import org.projectforge.rest.pub.SystemStatusRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Login of projectforge-next. Public service (available without login).
 *
 * Same functionality as [org.projectforge.rest.pub.LoginPageRest], but with plain JSON instead of UILayout:
 * next builds the form by hand and needs the login state (and the reason of a failure) as data.
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
     * State of the login page. Has to be called before showing the login form: the user might already be logged-in
     * or might be pre-logged-in with a pending second factor (e. g. after a browser reload during the 2FA step).
     *
     * @param url The caller may specify the url to redirect to after the login (stored in the user's session).
     */
    @GetMapping("status")
    fun getStatus(request: HttpServletRequest, @RequestParam url: String? = null): NextLoginState {
        LoginServiceRest.storeOriginUrl(request, url)
        val userContext = LoginService.getUserContext(request)
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
        if (ThreadLocalUserContext.userContext == null) {
            // Password was OK, but the user isn't logged-in yet, so a second factor is required (same condition as
            // used by LoginPageRest.login):
            return NextLoginResult(
                status = NextLoginStatus.TWO_FACTOR_REQUIRED,
                methods = twoFactorMethodsService.getMethods(LoginService.getUserContext(request)),
            )
        }
        return NextLoginResult(
            status = NextLoginStatus.SUCCESS,
            redirectUrl = LoginServiceRest.getRedirectUrl(request, null),
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
