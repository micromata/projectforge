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
import org.projectforge.business.user.AppAuthService
import org.projectforge.business.user.AppDeviceDao
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.TimeUnit
import org.projectforge.login.LoginService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.pub.next.TwoFactorRequired
import org.projectforge.security.My2FAService
import org.projectforge.web.rest.RestAuthenticationUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * The logged-in half of the app login (see [AppAuthService]): the page `/next/appLogin` asks for the one-time code of
 * the app, the app opens a web session for its WebView, and the app logs itself out.
 */
@RestController
@RequestMapping("${Rest.URL}/appAuth")
class AppAuthRest {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var appAuthService: AppAuthService

    @Autowired
    private lateinit var appDeviceDao: AppDeviceDao

    @Autowired
    private lateinit var loginService: LoginService

    @Autowired
    private lateinit var my2FAService: My2FAService

    class AuthorizeRequest(
        val challenge: String? = null,
        val state: String? = null,
        val deviceName: String? = null,
        val platform: String? = null,
        /** The app is already registered and needs a new second factor: its device id. */
        val deviceId: Long? = null,
    )

    class AuthorizeResponse(
        /** The callback of the app with the one-time code; the page navigates there. */
        val redirectUrl: String,
    )

    class WebSessionResponse(
        /** The page the WebView of the app opens with the session cookie of this response. */
        val startUrl: String,
    )

    /**
     * Issues the one-time code for the app. Only for a browser session (a rest client or another app can't
     * register an app) and only after a second factor of at most [AppAuthService.MAX_2FA_AGE_MINUTES] minutes: the
     * token of the app is good for the 2FA protected urls as long as this second factor. Without a fresh one the
     * answer is the 403 of [TwoFactorRequired], so projectforge-next opens its 2FA dialog and repeats the call.
     */
    @AccessChecked("Logged-in user of a browser session (no token clients, no web session of an app), fresh 2FA required. CSRF token required")
    @PostMapping("authorize")
    fun authorize(request: HttpServletRequest, @RequestBody body: AuthorizeRequest): ResponseEntity<*> {
        val userContext = ThreadLocalUserContext.userContext
        val user = userContext?.user
        // The web session of an app (its WebView) is a browser session, but its credential is the app token.
        if (user == null || userContext.loggedInByAuthenticationToken || LoginService.isAppWebSession(request)) {
            log.warn { "App authorization refused: not a browser session." }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }
        // A demo account is shared: an app of one visitor would stay logged in for all others.
        accessChecker.checkRestrictedOrDemoUser()
        if (!my2FAService.checklastSuccessful2FA(AppAuthService.MAX_2FA_AGE_MINUTES, My2FAService.Unit.MINUTES)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(TwoFactorRequired(expiryMillis = AppAuthService.MAX_2FA_AGE_MINUTES * TimeUnit.MINUTE.millis))
        }
        val redirectUrl = appAuthService.authorize(
            user = user,
            lastSuccessful2FA = userContext.lastSuccessful2FA!!,
            challenge = body.challenge,
            state = body.state,
            deviceName = body.deviceName,
            platform = body.platform,
            renewDeviceId = body.deviceId,
        ) ?: return ResponseEntity.badRequest().build<Any>()
        return ResponseEntity.ok(AuthorizeResponse(redirectUrl))
    }

    /**
     * Opens a web session for the ProjectForge pages the app shows in its WebView: the session cookie comes with the
     * response (`Set-Cookie`), the app hands it to the WebView. Only for the app token, neither for a browser session
     * nor a REST client token. User and device are the ones of the token, nothing posted.
     *
     * The session has the second factor of the app's login and ends with the app (see [LoginService.appWebLogin]). Is
     * that second factor too old for a login, the answer is the 403 of [TwoFactorRequired]: the app logs in again in the
     * browser and repeats the call.
     */
    @AccessChecked("Own app only (the device of the app token of this request), session of its user")
    @PostMapping("webSession")
    fun webSession(request: HttpServletRequest): ResponseEntity<*> {
        val userContext = ThreadLocalUserContext.userContext
        val device = if (userContext?.loggedInByAuthenticationToken == true) {
            appDeviceDao.getValidDevice(RestAuthenticationUtils.getAppToken(request))
        } else null
        val user = userContext?.user
        val deviceId = device?.id
        if (user == null || deviceId == null || device.user?.id != user.id) {
            log.warn { "Web session for an app refused: not authenticated by an app token." }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }
        if (!loginService.appWebLogin(request, user, device.last2FA?.time, deviceId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(TwoFactorRequired())
        }
        return ResponseEntity.ok(WebSessionResponse(startUrl = WEB_START_URL))
    }

    /**
     * The app logs itself out: its token is deleted. The device is the one of the token, nothing posted.
     */
    @AccessChecked("Own app only (the device of the app token of this request)")
    @PostMapping("logout")
    fun logout(request: HttpServletRequest): ResponseEntity<*> {
        val userId = ThreadLocalUserContext.loggedInUserId
        val device = appDeviceDao.getValidDevice(RestAuthenticationUtils.getAppToken(request))
            ?: return ResponseEntity.badRequest().build<Any>()
        appDeviceDao.deleteOfUser(userId, device.id)
        return ResponseEntity.ok().build<Any>()
    }

    companion object {
        const val WEB_START_URL = "/next/"
    }
}
