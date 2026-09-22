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

package org.projectforge.login

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.StayLoggedInTokenDao
import org.projectforge.business.user.UserAuthenticationsService
import org.springframework.beans.factory.annotation.Autowired

class LoginServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var loginService: LoginService

    @Autowired
    private lateinit var stayLoggedInTokenDao: StayLoggedInTokenDao

    @Autowired
    private lateinit var userAuthenticationsService: UserAuthenticationsService

    /**
     * The password alone doesn't earn a 30 day cookie: whoever aborts the second factor mustn't walk away with
     * one. The wish is parked in the session and redeemed by [LoginService.onSecondFactorSucceeded].
     */
    @Test
    fun stayLoggedInCookieOnlyAfter2FATest() {
        val user = getUser(TEST_USER)
        val userId = user.id!!
        logon(TEST_USER)
        // Makes 2FA mandatory right after the login, see LoginService.check2FARequiredAfterLogin:
        userAuthenticationsService.createNewAuthenticatorToken()
        logoff()
        stayLoggedInTokenDao.deleteAll(userId)
        try {
            val request = mockRequest()
            val response = Mockito.mock(HttpServletResponse::class.java)
            val loginData = LoginData(TEST_USER, TEST_USER_PASSWORD.copyOf(), stayLoggedIn = true)
            Assertions.assertEquals(LoginResultStatus.SUCCESS, loginService.authenticate(request, response, loginData))
            Assertions.assertNull(stayLoggedInCookie(response), "2FA pending: no cookie yet.")
            Assertions.assertTrue(
                stayLoggedInTokenDao.getEntries(userId).isEmpty(),
                "2FA pending: no token in the database either (an aborted 2FA leaves nothing behind).",
            )

            // The second factor was checked successfully (My2FAServicesRest.updateCookieAndSession):
            loginService.onSecondFactorSucceeded(request, response)
            val cookie = stayLoggedInCookie(response)
            Assertions.assertNotNull(cookie, "Cookie is issued after the second factor.")
            Assertions.assertEquals(userId, stayLoggedInTokenDao.getValidToken(cookie!!.value)?.user?.id)

            // Consumed: a later in-session 2FA check must not hand out a second cookie.
            val response2 = Mockito.mock(HttpServletResponse::class.java)
            loginService.onSecondFactorSucceeded(request, response2)
            Assertions.assertNull(stayLoggedInCookie(response2), "The wish is redeemed only once.")
            Assertions.assertEquals(1, stayLoggedInTokenDao.getEntries(userId).size)
        } finally {
            stayLoggedInTokenDao.deleteAll(userId)
            logon(TEST_USER)
            userAuthenticationsService.clearAuthenticatorToken()
            logoff()
        }
    }

    /**
     * Without a pending second factor the cookie comes right away - and only if the user asked for it.
     */
    @Test
    fun stayLoggedInCookieWithout2FATest() {
        val userId = getUser(TEST_USER).id!!
        stayLoggedInTokenDao.deleteAll(userId)
        try {
            var response = Mockito.mock(HttpServletResponse::class.java)
            loginService.authenticate(
                mockRequest(),
                response,
                LoginData(TEST_USER, TEST_USER_PASSWORD.copyOf(), stayLoggedIn = true),
            )
            Assertions.assertNotNull(stayLoggedInCookie(response), "No 2FA pending: cookie right away.")
            Assertions.assertEquals(1, stayLoggedInTokenDao.getEntries(userId).size)

            response = Mockito.mock(HttpServletResponse::class.java)
            loginService.authenticate(
                mockRequest(),
                response,
                LoginData(TEST_USER, TEST_USER_PASSWORD.copyOf(), stayLoggedIn = false),
            )
            Assertions.assertNull(stayLoggedInCookie(response), "Not asked for, so no cookie.")
            Assertions.assertEquals(1, stayLoggedInTokenDao.getEntries(userId).size, "... and no second device.")
        } finally {
            stayLoggedInTokenDao.deleteAll(userId)
        }
    }

    private fun stayLoggedInCookie(response: HttpServletResponse): Cookie? {
        val captor = ArgumentCaptor.forClass(Cookie::class.java)
        Mockito.verify(response, Mockito.atLeast(0)).addCookie(captor.capture())
        return captor.allValues.lastOrNull { it.name == "stayLoggedIn" && !it.value.isNullOrBlank() }
    }

    /**
     * A request with a session that really keeps its attributes: [LoginService] parks the stay-logged-in wish
     * there and reads the user context back out of it.
     */
    private fun mockRequest(): HttpServletRequest {
        val attributes = mutableMapOf<String, Any?>()
        val session = Mockito.mock(HttpSession::class.java)
        Mockito.`when`(session.isNew).thenReturn(true) // Otherwise internalLogin invalidates the mock.
        Mockito.`when`(session.getAttribute(Mockito.anyString())).thenAnswer { attributes[it.arguments[0] as String] }
        Mockito.`when`(session.setAttribute(Mockito.anyString(), Mockito.any()))
            .thenAnswer { attributes[it.arguments[0] as String] = it.arguments[1]; null }
        Mockito.`when`(session.removeAttribute(Mockito.anyString()))
            .thenAnswer { attributes.remove(it.arguments[0] as String); null }
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getSession(Mockito.anyBoolean())).thenReturn(session)
        Mockito.`when`(request.getSession()).thenReturn(session)
        return request
    }
}
