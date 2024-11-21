/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.rest.config.Rest
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.web.rest.RestUserFilter
import org.springframework.beans.factory.annotation.Autowired
import java.io.IOException

class RestUserFilterTest : AbstractTestBase() {
    @Autowired
    private lateinit var userAuthenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    val filter: RestUserFilter = RestUserFilter()

    var userId: Long = 0

    var userToken: String? = "token"

    @BeforeEach
    fun init() {
        if (initialized) return
        initialized = true
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
        val response = Mockito.mock(
            HttpServletResponse::class.java
        )

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

    private fun mockRequest(
        username: String?, password: CharArray?, userId: Long?,
        authenticationToken: String?
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
            Mockito.`when`(request.requestURI).thenReturn(Rest.URL + "....")
        }
        return request
    }

    companion object {
        private var initialized = false
    }
}
