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

package org.projectforge.business.user.filter

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.StayLoggedInTokenDao
import org.projectforge.framework.persistence.user.api.UserContext
import org.springframework.beans.factory.annotation.Autowired

class CookieServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var cookieService: CookieService

    @Autowired
    private lateinit var stayLoggedInTokenDao: StayLoggedInTokenDao

    /**
     * The stay-logged-in cookie is a long living credential, so it mustn't be sent on requests a foreign page
     * initiated. Lax and not Strict, because entering ProjectForge by an external link (mail, bookmark) is the normal
     * case - which is why the public endpoints check Sec-Fetch-Site in addition, see
     * [org.projectforge.rest.core.RestCsrfProtection.isSameSiteRequest].
     */
    @Test
    fun stayLoggedInCookieAttributesTest() {
        val user = getUser(TEST_USER)
        val request = Mockito.mock(HttpServletRequest::class.java)
        val response = Mockito.mock(HttpServletResponse::class.java)
        cookieService.addStayLoggedInCookie(request, response, user)
        val captor = ArgumentCaptor.forClass(Cookie::class.java)
        Mockito.verify(response).addCookie(captor.capture())
        val cookie = captor.value
        Assertions.assertEquals("Lax", cookie.getAttribute("SameSite"), "Not to be sent on cross-site requests.")
        Assertions.assertTrue(cookie.isHttpOnly, "Not to be readable by javascript.")
        Assertions.assertEquals("/", cookie.path)
        Assertions.assertEquals(30 * 24 * 3600, cookie.maxAge, "30 days.")
        // The cookie carries nothing but the token (no user id, no username): the token identifies the
        // device's row, and anything else in there would only be an attacker controlled input.
        Assertions.assertNotNull(
            stayLoggedInTokenDao.getValidToken(cookie.value),
            "The value has to be the token the check on the next request looks up.",
        )
        stayLoggedInTokenDao.deleteByToken(cookie.value)
    }

    /**
     * The cookie path is a login path, so it needs the same brute force brake as the password path. The key is
     * the client's ip: the cookie carries nothing but the token, so there is no user to name before the token
     * has been resolved.
     */
    @Test
    fun bruteForceProtectionTest() {
        val user = getUser(TEST_USER)
        val token = stayLoggedInTokenDao.createToken(user, null)
        val loginProtection = LoginProtection.instance()
        try {
            // A garbage token increments the offset of this ip:
            Assertions.assertNull(checkStayLoggedIn("garbage-token-of-an-attacker"))
            Assertions.assertTrue(
                loginProtection.getFailedLoginTimeOffsetIfExists(CLIENT_IP, null, AUTHENTICATION_TYPE) > 0,
                "Failed cookie attempt has to be counted.",
            )
            // ... but nothing else keyed by that ip, the authentication type namespaces it:
            Assertions.assertEquals(
                0,
                loginProtection.getFailedLoginTimeOffsetIfExists(CLIENT_IP, CLIENT_IP),
                "A failed cookie attempt mustn't brake the password login from the same ip.",
            )
            // While the ip is blocked even a valid token isn't looked up:
            Assertions.assertNull(checkStayLoggedIn(token), "Blocked ip, no restore even with a valid token.")

            loginProtection.clearLoginTimeOffset(CLIENT_IP, null, null, AUTHENTICATION_TYPE)
            Assertions.assertEquals(user.id, checkStayLoggedIn(token)?.user?.id, "Valid token, unblocked ip.")
            Assertions.assertEquals(
                0,
                loginProtection.getFailedLoginTimeOffsetIfExists(CLIENT_IP, null, AUTHENTICATION_TYPE),
                "A successful restore clears the offset of the ip.",
            )
        } finally {
            loginProtection.clearLoginTimeOffset(CLIENT_IP, null, null, AUTHENTICATION_TYPE)
            stayLoggedInTokenDao.deleteAll(user.id)
        }
    }

    private fun checkStayLoggedIn(token: String): UserContext? {
        val request = Mockito.mock(HttpServletRequest::class.java)
        val response = Mockito.mock(HttpServletResponse::class.java)
        Mockito.`when`(request.cookies).thenReturn(arrayOf(Cookie("stayLoggedIn", token)))
        Mockito.`when`(request.remoteAddr).thenReturn(CLIENT_IP)
        return cookieService.checkStayLoggedIn(request, response)
    }

    companion object {
        private const val CLIENT_IP = "192.168.42.42"

        /**
         * Same value as [CookieService]'s private constant: the namespace of [LoginProtection] on the cookie
         * path. The ip goes into the *user* map of [LoginProtection] (threshold 1 instead of 1000), see
         * [CookieService.checkStayLoggedIn].
         */
        private const val AUTHENTICATION_TYPE = "stayLoggedIn"
    }
}
