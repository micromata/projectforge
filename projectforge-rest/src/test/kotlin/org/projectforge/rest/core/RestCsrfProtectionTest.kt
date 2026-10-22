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

package org.projectforge.rest.core

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.web.rest.RestAuthenticationInfo
import java.io.PrintWriter
import java.io.StringWriter

class RestCsrfProtectionTest {
    /**
     * [org.projectforge.rest.pub.next.LoginNextRest.getStatus] relies on this to decide whether it may do a
     * stay-logged-in restore (which has side effects: session rotation, database write, cookie refresh). A foreign
     * page must not be able to trigger that, so anything but same-origin/none has to return false.
     */
    @Test
    fun isSameSiteRequestTest() {
        Assertions.assertTrue(
            RestCsrfProtection.isSameSiteRequest(mockRequest(null)),
            "No Sec-Fetch-Site header at all (curl, DAV clients, old browsers): not detectable as cross-site here.",
        )
        Assertions.assertTrue(
            RestCsrfProtection.isSameSiteRequest(mockRequest("same-origin")),
            "A request of a page of this app.",
        )
        Assertions.assertTrue(
            RestCsrfProtection.isSameSiteRequest(mockRequest("none")),
            "A user-initiated navigation (bookmark, typed url), not a request of a foreign page.",
        )
        Assertions.assertFalse(
            RestCsrfProtection.isSameSiteRequest(mockRequest("cross-site")),
            "Initiated by a foreign page.",
        )
        Assertions.assertFalse(
            RestCsrfProtection.isSameSiteRequest(mockRequest("same-site")),
            "Denied on purpose: a sibling subdomain shouldn't be able to trigger a login, see isSameSiteRequest.",
        )
    }

    /**
     * The token is checked for every state changing call of a session, whatever the client claims to be: a
     * request without the `X-PF-Frontend` header used to skip the check.
     */
    @Test
    fun tokenIsRequiredForEveryStateChangingCallOfASession() {
        val protection = createProtection()
        Assertions.assertFalse(
            protection.checkRequest(mockSessionRequest("POST", token = null), mockResponse(), authInfo()),
            "POST without token and without the X-PF-Frontend header must be denied.",
        )
        Assertions.assertFalse(
            protection.checkRequest(mockSessionRequest("DELETE", token = "x".repeat(TOKEN.length)), mockResponse(), authInfo()),
            "Wrong token must be denied.",
        )
        Assertions.assertTrue(
            protection.checkRequest(mockSessionRequest("POST", token = TOKEN), mockResponse(), authInfo()),
            "POST with the token of the session is allowed.",
        )
        Assertions.assertTrue(
            protection.checkRequest(mockSessionRequest("GET", token = null), mockResponse(), authInfo()),
            "A GET carries no token (and must change nothing).",
        )
        Assertions.assertTrue(
            protection.checkRequest(
                mockSessionRequest("POST", token = null), mockResponse(),
                authInfo().also { it.loggedInByAuthenticationToken = true },
            ),
            "A rest client authenticated by an access token doesn't rely on the session cookie.",
        )
    }

    private fun createProtection(): RestCsrfProtection {
        val protection = RestCsrfProtection()
        RestCsrfProtection::class.java.getDeclaredField("sessionCsrfService").let {
            it.isAccessible = true
            it.set(protection, SessionCsrfService())
        }
        return protection
    }

    private fun mockSessionRequest(method: String, token: String?): HttpServletRequest {
        val session = Mockito.mock(HttpSession::class.java)
        Mockito.`when`(session.getAttribute(Mockito.anyString())).thenReturn(TOKEN)
        val request = mockRequest("same-origin")
        Mockito.`when`(request.method).thenReturn(method)
        Mockito.`when`(request.requestURI).thenReturn("/rs/book/saveorupdate")
        Mockito.`when`(request.getSession(false)).thenReturn(session)
        Mockito.`when`(request.getHeader(Mockito.eq(RestCsrfProtection.CSRF_TOKEN_HEADER))).thenReturn(token)
        return request
    }

    private fun mockResponse(): HttpServletResponse {
        val response = Mockito.mock(HttpServletResponse::class.java)
        Mockito.`when`(response.writer).thenReturn(PrintWriter(StringWriter()))
        return response
    }

    private fun authInfo(): RestAuthenticationInfo {
        return RestAuthenticationInfo(Mockito.mock(HttpServletRequest::class.java), mockResponse())
    }

    private fun mockRequest(secFetchSite: String?): HttpServletRequest {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getHeader(Mockito.eq(RestCsrfProtection.SEC_FETCH_SITE_HEADER)))
            .thenReturn(secFetchSite)
        return request
    }

    companion object {
        private val TOKEN = "a".repeat(SessionCsrfService.TOKEN_LENGTH)
    }
}
