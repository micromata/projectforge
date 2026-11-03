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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.AppDeviceDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.TimeUnit
import org.projectforge.login.LoginService
import org.projectforge.rest.pub.next.TwoFactorRequired
import org.projectforge.security.My2FARequestConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import java.util.Date

/**
 * The web session of the app's WebView (`POST /rs/appAuth/webSession`): only for the app token, only of the token's
 * user, with a new session id, and ended with the app.
 */
class AppWebSessionTest : AbstractTestBase() {
    @Autowired
    private lateinit var appAuthRest: AppAuthRest

    @Autowired
    private lateinit var appDeviceDao: AppDeviceDao

    @Autowired
    private lateinit var loginService: LoginService

    @Autowired
    private lateinit var my2FARequestConfiguration: My2FARequestConfiguration

    @AfterEach
    fun cleanUp() {
        appDeviceDao.deleteAll(getUser(TEST_USER).id)
        appDeviceDao.deleteAll(getUser(TEST_ADMIN_USER).id)
    }

    @Test
    fun `app token opens a web session of its user`() {
        val last2FA = Date(System.currentTimeMillis() - TimeUnit.MINUTE.millis)
        val (device, token) = appDeviceDao.createDevice(getUser(TEST_USER), "iPhone", "ios", last2FA, null)
        logonByAppToken(TEST_USER)
        val oldSession = MockHttpSession().also { it.setNew(false) }
        val request = appRequest(token).also { it.setSession(oldSession) }

        val response = appAuthRest.webSession(request)

        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        Assertions.assertEquals("/next/", (response.body as AppAuthRest.WebSessionResponse).startUrl)
        val session = request.getSession(false) as MockHttpSession
        Assertions.assertNotEquals(oldSession.id, session.id, "Session fixation: the session must get a new id.")
        Assertions.assertTrue(oldSession.isInvalid)
        val userContext = LoginService.getUserContext(request)!!
        Assertions.assertEquals(getUser(TEST_USER).id, userContext.user?.id)
        Assertions.assertEquals(last2FA.time, userContext.lastSuccessful2FA, "Only the 2FA of the app's login.")
        Assertions.assertFalse(userContext.loggedInByAuthenticationToken, "A web session needs the CSRF token.")

        // The session works as a browser session, until the app is logged out:
        Assertions.assertNotNull(loginService.checkLogin(sessionRequest(session), MockHttpServletResponse()))
        appDeviceDao.deleteOfUser(getUser(TEST_USER).id, device.id)
        Assertions.assertNotNull(
            loginService.checkLogin(sessionRequest(session), MockHttpServletResponse()),
            "Checked at most once a minute.",
        )
        session.setAttribute(LoginService.SESSION_KEY_LAST_APP_DEVICE_CHECK, 0L)
        Assertions.assertNull(loginService.checkLogin(sessionRequest(session), MockHttpServletResponse()))
    }

    @Test
    fun `no web session without the app token of the user`() {
        val (_, token) = appDeviceDao.createDevice(getUser(TEST_USER), "iPhone", "ios", Date(), null)
        val (_, adminToken) = appDeviceDao.createDevice(getUser(TEST_ADMIN_USER), "iPhone", "ios", Date(), null)

        // A browser session, even with the app token in the header:
        logon(TEST_USER)
        assertRefused(appRequest(token))

        // A REST client token (no app token):
        logonByAppToken(TEST_USER)
        assertRefused(MockHttpServletRequest())

        // The app token of another user:
        assertRefused(appRequest(adminToken))

        // The own app token:
        Assertions.assertEquals(HttpStatus.OK, appAuthRest.webSession(appRequest(token)).statusCode)
    }

    /**
     * The WebView session is a browser session, but its credential is the app token: it must not register further
     * apps, or a stolen token could mint new ones that outlive its revocation.
     */
    @Test
    fun `web session of an app registers no further apps`() {
        val (_, token) = appDeviceDao.createDevice(getUser(TEST_USER), "iPhone", "ios", Date(), null)
        logonByAppToken(TEST_USER)
        val request = appRequest(token)
        appAuthRest.webSession(request)
        val session = request.getSession(false) as MockHttpSession
        val authorize = AppAuthRest.AuthorizeRequest(challenge = "a".repeat(43), state = "s".repeat(16), deviceName = "x")

        logon(TEST_USER) // As RestUserFilter does for a session.
        ThreadLocalUserContext.userContext!!.updateLastSuccessful2FA()
        Assertions.assertEquals(HttpStatus.FORBIDDEN, appAuthRest.authorize(sessionRequest(session), authorize).statusCode)
        // A browser session of the user with a fresh second factor:
        Assertions.assertEquals(HttpStatus.OK, appAuthRest.authorize(MockHttpServletRequest(), authorize).statusCode)
    }

    /** The account of the app review has no second factor to show; any other user without one is refused. */
    @Test
    fun `exempt user authorizes without a second factor`() {
        val authorize = AppAuthRest.AuthorizeRequest(challenge = "a".repeat(43), state = "s".repeat(16), deviceName = "x")
        try {
            my2FARequestConfiguration.internalSetExemptUsers4TestCases(TEST_ADMIN_USER)
            logon(TEST_USER)
            Assertions.assertNull(ThreadLocalUserContext.userContext!!.lastSuccessful2FA)
            val refused = appAuthRest.authorize(MockHttpServletRequest(), authorize)
            Assertions.assertEquals(HttpStatus.FORBIDDEN, refused.statusCode)
            Assertions.assertTrue((refused.body as TwoFactorRequired).twoFactorRequired)
            logon(TEST_ADMIN_USER)
            Assertions.assertNull(ThreadLocalUserContext.userContext!!.lastSuccessful2FA)
            Assertions.assertEquals(HttpStatus.OK, appAuthRest.authorize(MockHttpServletRequest(), authorize).statusCode)
        } finally {
            my2FARequestConfiguration.internalSetExemptUsers4TestCases("")
        }
    }

    @Test
    fun `second factor of the app too old for a login`() {
        val old = Date(System.currentTimeMillis() - 2 * TimeUnit.DAY.millis)
        val (_, token) = appDeviceDao.createDevice(getUser(TEST_USER), "iPhone", "ios", old, null)
        logonByAppToken(TEST_USER)
        val loginExpiryDays = my2FARequestConfiguration.loginExpiryDays
        try {
            my2FARequestConfiguration.internalSetLoginExpiryDays4TestCases(1)
            val request = appRequest(token)
            val response = appAuthRest.webSession(request)
            Assertions.assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            Assertions.assertTrue((response.body as TwoFactorRequired).twoFactorRequired)
            Assertions.assertNull(request.getSession(false), "No session without a second factor.")
        } finally {
            my2FARequestConfiguration.internalSetLoginExpiryDays4TestCases(loginExpiryDays)
        }
    }

    /** What RestUserFilter leaves for a request authenticated by a token. */
    private fun logonByAppToken(username: String) {
        logon(username)
        ThreadLocalUserContext.userContext!!.loggedInByAuthenticationToken = true
    }

    private fun appRequest(token: String) = MockHttpServletRequest("POST", "/rs/appAuth/webSession").also {
        it.addHeader("Authorization", "PFApp $token")
    }

    private fun sessionRequest(session: MockHttpSession) = MockHttpServletRequest("GET", "/rs/userStatus").also {
        it.setSession(session.also { s -> s.setNew(false) })
    }

    private fun assertRefused(request: MockHttpServletRequest) {
        Assertions.assertEquals(HttpStatus.FORBIDDEN, appAuthRest.webSession(request).statusCode)
        Assertions.assertNull(request.getSession(false))
    }
}
