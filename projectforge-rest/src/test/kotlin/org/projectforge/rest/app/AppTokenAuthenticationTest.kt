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

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.AppDeviceDao
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.time.TimeUnit
import org.projectforge.rest.config.Rest
import org.projectforge.security.My2FARequestConfiguration
import org.projectforge.security.My2FARequestHandler
import org.projectforge.web.rest.RestUserFilter
import org.springframework.beans.factory.annotation.Autowired
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

/**
 * The token of the mobile app (`Authorization: PFApp <token>`) on `/rs`: accepted without session and CSRF token,
 * refused once revoked, and checked against the second factor of its login for the 2FA protected urls.
 */
class AppTokenAuthenticationTest : AbstractTestBase() {
    @Autowired
    private lateinit var appDeviceDao: AppDeviceDao

    @Autowired
    private lateinit var my2FARequestConfiguration: My2FARequestConfiguration

    @Autowired
    private lateinit var my2FARequestHandler: My2FARequestHandler

    private val filter = RestUserFilter()

    @BeforeEach
    fun init() {
        ApplicationContextProvider.getApplicationContext().autowireCapableBeanFactory.autowireBean(filter)
        LoginProtection.instance().clearAll()
    }

    @AfterEach
    fun cleanUp() {
        appDeviceDao.deleteAll(getUser(TEST_USER).id)
        LoginProtection.instance().clearAll()
    }

    @Test
    fun appTokenTest() {
        val (device, token) = appDeviceDao.createDevice(getUser(TEST_USER), "iPhone", "ios", Date(), null)
        // A state changing call without CSRF token: the app has no session (and no cookies), so there is no CSRF.
        assertPassed(doFilter(token, "${Rest.URL}/myBusinessCards/save", "POST"))

        appDeviceDao.deleteOfUser(getUser(TEST_USER).id, device.id) // Logged out in the web.
        val result = doFilter(token, "${Rest.URL}/myBusinessCards")
        assertRefused(result, HttpServletResponse.SC_UNAUTHORIZED)
    }

    @Test
    fun unknownAppTokenTest() {
        assertRefused(doFilter("a".repeat(40), "${Rest.URL}/myBusinessCards"), HttpServletResponse.SC_UNAUTHORIZED)
    }

    /**
     * Unlike a REST client token, the app can do a 2FA (in the browser), so it gets the json answer of
     * projectforge-next instead of a permanent refusal - and passes, while the 2FA of its login is fresh enough.
     */
    @Test
    fun twoFactorTest() {
        val user = getUser(TEST_USER)
        val (_, staleToken) = appDeviceDao.createDevice(user, "old", "ios", Date(System.currentTimeMillis() - TimeUnit.HOUR.millis), null)
        val (_, freshToken) = appDeviceDao.createDevice(user, "fresh", "ios", Date(), null)
        val (_, noneToken) = appDeviceDao.createDevice(user, "none", "ios", null, null)
        try {
            my2FARequestConfiguration.internalSet4TestCases(expiryPeriodMinutes10 = "${Rest.URL}/order")
            my2FARequestHandler.reload()
            val stale = doFilter(staleToken, "${Rest.URL}/order/list")
            assertRefused(stale, HttpServletResponse.SC_FORBIDDEN)
            Assertions.assertTrue(stale.body.contains("\"twoFactorRequired\":true"), "Body was '${stale.body}'.")
            assertRefused(doFilter(noneToken, "${Rest.URL}/order/list"), HttpServletResponse.SC_FORBIDDEN)
            assertPassed(doFilter(freshToken, "${Rest.URL}/order/list"))
        } finally {
            my2FARequestConfiguration.internalSet4TestCases()
            my2FARequestHandler.reload()
        }
    }

    private class Result(val chain: FilterChain, val response: HttpServletResponse, val writer: StringWriter) {
        val body: String
            get() = writer.toString()
    }

    private fun doFilter(token: String, uri: String, method: String = "GET"): Result {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getHeader(Mockito.eq("Authorization"))).thenReturn("PFApp $token")
        Mockito.`when`(request.requestURI).thenReturn(uri)
        Mockito.`when`(request.method).thenReturn(method)
        val writer = StringWriter()
        val response = Mockito.mock(HttpServletResponse::class.java)
        Mockito.`when`(response.writer).thenReturn(PrintWriter(writer))
        val chain = Mockito.mock(FilterChain::class.java)
        suppressErrorLogs {
            filter.doFilter(request, response, chain)
        }
        return Result(chain, response, writer)
    }

    private fun assertPassed(result: Result) {
        Mockito.verify(result.chain).doFilter(Mockito.any(), Mockito.any())
    }

    private fun assertRefused(result: Result, status: Int) {
        Mockito.verify(result.chain, Mockito.never()).doFilter(Mockito.any(), Mockito.any())
        Mockito.verify(result.response).status = status
    }
}
