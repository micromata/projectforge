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
     * Without `Sec-Fetch-Site` (older browsers), `Origin` and else `Referer` decide; only a request without any of
     * them is not detectable.
     */
    @Test
    fun isSameSiteRequestWithoutSecFetchSiteTest() {
        Assertions.assertTrue(
            RestCsrfProtection.isSameSiteRequest(mockRequest(null, origin = "https://pf.example.org")),
            "Origin of the own host.",
        )
        Assertions.assertTrue(
            RestCsrfProtection.isSameSiteRequest(mockRequest(null, origin = "https://PF.example.org:443")),
            "Default port and case don't matter.",
        )
        Assertions.assertFalse(
            RestCsrfProtection.isSameSiteRequest(mockRequest(null, origin = "https://evil.example.com")),
            "Origin of a foreign host.",
        )
        Assertions.assertFalse(
            RestCsrfProtection.isSameSiteRequest(mockRequest(null, origin = "https://pf.example.org.evil.com")),
            "A foreign host with the own one as prefix.",
        )
        Assertions.assertFalse(
            RestCsrfProtection.isSameSiteRequest(mockRequest(null, origin = "null")),
            "Opaque origin (sandboxed frame).",
        )
        Assertions.assertTrue(
            RestCsrfProtection.isSameSiteRequest(mockRequest(null, referer = "https://pf.example.org/next/task")),
            "Referer of a page of this app.",
        )
        Assertions.assertFalse(
            RestCsrfProtection.isSameSiteRequest(mockRequest(null, referer = "https://evil.example.com/a")),
            "Referer of a foreign page.",
        )
        Assertions.assertTrue(
            RestCsrfProtection.isSameSiteRequest(
                mockRequest(null, origin = "https://pf.example.org", host = "localhost:8080", forwardedHost = "pf.example.org"),
            ),
            "Behind a reverse proxy, X-Forwarded-Host is the own host.",
        )
        Assertions.assertFalse(
            RestCsrfProtection.isSameSiteRequest(mockRequest("cross-site", origin = "https://pf.example.org")),
            "Sec-Fetch-Site wins over Origin.",
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

    private fun mockRequest(
        secFetchSite: String?,
        origin: String? = null,
        referer: String? = null,
        host: String = "pf.example.org",
        forwardedHost: String? = null,
    ): HttpServletRequest {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getHeader(Mockito.eq(RestCsrfProtection.SEC_FETCH_SITE_HEADER)))
            .thenReturn(secFetchSite)
        Mockito.`when`(request.getHeader(Mockito.eq("Origin"))).thenReturn(origin)
        Mockito.`when`(request.getHeader(Mockito.eq("Referer"))).thenReturn(referer)
        Mockito.`when`(request.getHeader(Mockito.eq("Host"))).thenReturn(host)
        Mockito.`when`(request.getHeader(Mockito.eq("X-Forwarded-Host"))).thenReturn(forwardedHost)
        return request
    }

    companion object {
        private val TOKEN = "a".repeat(SessionCsrfService.TOKEN_LENGTH)
    }
}
