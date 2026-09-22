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

package org.projectforge.rest

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.rest.config.Rest
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.security.My2FARequestConfiguration
import org.projectforge.security.My2FARequestHandler
import org.projectforge.web.rest.RestUserFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class RestUserFilterTest : AbstractTestBase() {
    @Autowired
    private lateinit var userAuthenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var my2FARequestConfiguration: My2FARequestConfiguration

    @Autowired
    private lateinit var my2FARequestHandler: My2FARequestHandler

    val filter: RestUserFilter = RestUserFilter()

    var userId: Long = 0

    var userToken: String? = "token"

    @BeforeEach
    fun init() {
        // No 'run once' shortcut here: JUnit creates a new test instance per test method, so [filter] is a fresh,
        // not yet autowired RestUserFilter for every one of them.
        val user = userGroupCache.getUser(TEST_USER)
        this.userId = user!!.id!!
        ApplicationContextProvider.getApplicationContext().autowireCapableBeanFactory.autowireBean(this.filter)
        logon(TEST_ADMIN_USER)
        this.userToken =
            userAuthenticationsService.getToken(this.userId, UserTokenType.REST_CLIENT) // Admin access required.
        logoff()
    }

    @Test
    @Throws(IOException::class, ServletException::class, InterruptedException::class)
    fun testAuthentication() {
        val response = mockResponse(StringWriter())

        // Wrong password
        var request = mockRequest(TEST_USER, "failed".toCharArray(), null, null)
        var chain = Mockito.mock(FilterChain::class.java)
        suppressErrorLogs {
            filter.doFilter(request, response, chain)
        }
        Mockito.verify(chain, Mockito.never()).doFilter(
            Mockito.any(
                HttpServletRequest::class.java
            ), Mockito.any(
                HttpServletResponse::class.java
            )
        )
        Thread.sleep(1100) // Login penalty.
        // Correct user name and password
        request = mockRequest(TEST_USER, TEST_USER_PASSWORD, null, null)
        chain = Mockito.mock(FilterChain::class.java)
        suppressErrorLogs {
            filter.doFilter(request, response, chain)
        }

        // Wrong token
        request = mockRequest(null, null, userId, "wrongToken")
        chain = Mockito.mock(FilterChain::class.java)
        suppressErrorLogs {
            filter.doFilter(request, response, chain)
        }
        Mockito.verify(chain, Mockito.never()).doFilter(
            Mockito.any(
                HttpServletRequest::class.java
            ), Mockito.any(
                HttpServletResponse::class.java
            )
        )
        Thread.sleep(2100) // Login penalty.
        // Correct user name and password
        request = mockRequest(null, null, userId, userToken)
        chain = Mockito.mock(FilterChain::class.java)
        suppressErrorLogs {
            filter.doFilter(request, response, chain)
        }
        Mockito.verify(chain).doFilter(Mockito.eq(request), Mockito.eq(response))
    }

    /**
     * A call without any credentials (the typical /rs/userStatus of a not yet logged-in client) must answer 401 with a
     * json body, not sendError: the latter forwards to /error, and MyErrorController answers that with the view
     * '/index.html', which doesn't exist in the next setup. The client got a misleading
     * 404 ("No static resource index.html") instead of the 401.
     */
    @Test
    @Throws(IOException::class, ServletException::class)
    fun unauthorizedRequestSendsJsonError() {
        val writer = StringWriter()
        val response = mockResponse(writer)
        val request = mockRequest(null, null, null, null)
        val chain = Mockito.mock(FilterChain::class.java)
        suppressErrorLogs {
            filter.doFilter(request, response, chain)
        }
        Mockito.verify(chain, Mockito.never()).doFilter(
            Mockito.any(HttpServletRequest::class.java), Mockito.any(HttpServletResponse::class.java)
        )
        Mockito.verify(response, Mockito.never()).sendError(Mockito.anyInt())
        Mockito.verify(response).status = HttpServletResponse.SC_UNAUTHORIZED
        Mockito.verify(response).contentType = MediaType.APPLICATION_JSON_VALUE
        val body = writer.toString()
        Assertions.assertTrue(
            body.contains("\"status\":401"),
            "Json body with the real status expected, but was '$body'.",
        )
    }

    /**
     * A client authenticated by an authentication token can't do a 2FA (it has no session, so
     * UserContext.lastSuccessful2FA is always null). A protected url must therefore be answered with a 403 and a json
     * body, not with the ResponseAction of the UILayout clients: the latter is sent with status 200 and would be read
     * as a success by such a client.
     */
    @Test
    @Throws(IOException::class, ServletException::class)
    fun twoFactorProtectedUrlIsDeniedForTokenAuthentication() {
        val writer = StringWriter()
        val response = mockResponse(writer)
        val request = mockRequest(null, null, userId, userToken, "${Rest.URL}order/list")
        val chain = Mockito.mock(FilterChain::class.java)
        try {
            my2FARequestConfiguration.internalSet4TestCases(expiryPeriodMinutes10 = "${Rest.URL}order")
            my2FARequestHandler.reload()
            suppressErrorLogs {
                filter.doFilter(request, response, chain)
            }
        } finally {
            my2FARequestConfiguration.internalSet4TestCases() // Restore: the configuration is a shared bean.
            my2FARequestHandler.reload()
        }
        Mockito.verify(chain, Mockito.never()).doFilter(
            Mockito.any(HttpServletRequest::class.java), Mockito.any(HttpServletResponse::class.java)
        )
        Mockito.verify(response).status = HttpServletResponse.SC_FORBIDDEN
        Mockito.verify(response).contentType = MediaType.APPLICATION_JSON_VALUE
        val body = writer.toString()
        Assertions.assertTrue(
            body.contains("\"status\":403"),
            "Json body with the real status expected, but was '$body'.",
        )
    }

    /**
     * The writer has to be stubbed: a denied call writes its json error body into it (see
     * [org.projectforge.web.rest.RestAuthenticationUtils]), and a mock returns null for it by default.
     */
    private fun mockResponse(writer: StringWriter): HttpServletResponse {
        val response = Mockito.mock(HttpServletResponse::class.java)
        Mockito.`when`(response.writer).thenReturn(PrintWriter(writer))
        return response
    }

    private fun mockRequest(
        username: String?, password: CharArray?, userId: Long?,
        authenticationToken: String?,
        requestURI: String? = null,
    ): HttpServletRequest {
        val request = Mockito.mock(
            HttpServletRequest::class.java
        )
        if (username != null) {
            Mockito.`when`(request.getHeader(Mockito.eq(Authentication.AUTHENTICATION_USERNAME))).thenReturn(username)
        }
        if (password != null) {
            Mockito.`when`(request.getHeader(Mockito.eq(Authentication.AUTHENTICATION_PASSWORD))).thenReturn(
                String(password)
            )
        }
        if (userId != null) {
            Mockito.`when`(request.getHeader(Mockito.eq(Authentication.AUTHENTICATION_USER_ID)))
                .thenReturn(userId.toString())
        }
        if (authenticationToken != null) {
            Mockito.`when`(request.getHeader(Mockito.eq(Authentication.AUTHENTICATION_TOKEN)))
                .thenReturn(authenticationToken)
        }
        if (requestURI != null || authenticationToken != null) {
            Mockito.`when`(request.requestURI).thenReturn(requestURI ?: (Rest.URL + "...."))
        }
        return request
    }
}
