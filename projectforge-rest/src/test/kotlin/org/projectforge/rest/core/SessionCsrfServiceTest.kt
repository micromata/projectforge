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
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class SessionCsrfServiceTest {
    /**
     * A token is replaced once it is older than [SessionCsrfService.MAX_TOKEN_AGE_MILLIS], so a leaked one is of
     * limited use even within a long session.
     */
    @Test
    fun tokenExpiresAfterMaxAge() {
        val service = SessionCsrfService()
        val attributes = mutableMapOf<String, Any?>()
        val request = mockRequest(attributes)

        val token = service.createServerData(request).csrfToken!!
        Assertions.assertTrue(service.checkToken(request, token), "A fresh token is valid.")
        Assertions.assertEquals(token, service.createServerData(request).csrfToken, "A fresh token is kept.")

        setTokenAge(attributes, SessionCsrfService.MAX_TOKEN_AGE_MILLIS - 60_000)
        Assertions.assertTrue(service.checkToken(request, token), "Valid until its max age.")

        setTokenAge(attributes, SessionCsrfService.MAX_TOKEN_AGE_MILLIS + 1)
        Assertions.assertFalse(service.checkToken(request, token), "An expired token must be refused.")
        val newToken = service.createServerData(request).csrfToken!!
        Assertions.assertNotEquals(token, newToken, "A new token replaces the expired one.")
        Assertions.assertTrue(service.checkToken(request, newToken), "The new token is valid.")
    }

    /**
     * A session older than the lifetime check has a token without a creation time: it stays valid and its age
     * counts from now on, instead of logging out every open client at the deployment.
     */
    @Test
    fun tokenWithoutCreationTimeIsKept() {
        val service = SessionCsrfService()
        val token = "b".repeat(SessionCsrfService.TOKEN_LENGTH)
        val attributes = mutableMapOf<String, Any?>("SessionCsrfService:csrfToken" to token)
        val request = mockRequest(attributes)
        Assertions.assertTrue(service.checkToken(request, token))
        Assertions.assertTrue(attributes.keys.any { it.endsWith("Created") }, "The creation time is set now.")
    }

    private fun setTokenAge(attributes: MutableMap<String, Any?>, ageMillis: Long) {
        val key = attributes.keys.single { it.endsWith("Created") }
        attributes[key] = System.currentTimeMillis() - ageMillis
    }

    private fun mockRequest(attributes: MutableMap<String, Any?>): HttpServletRequest {
        val session = Mockito.mock(HttpSession::class.java)
        Mockito.`when`(session.getAttribute(Mockito.anyString())).thenAnswer { attributes[it.getArgument(0)] }
        Mockito.doAnswer { attributes[it.getArgument(0)] = it.getArgument(1); null }
            .`when`(session).setAttribute(Mockito.anyString(), Mockito.any())
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getSession(false)).thenReturn(session)
        return request
    }
}
