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
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired

class CookieServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var cookieService: CookieService

    /**
     * The stay-logged-in cookie is a long living credential, so it mustn't be sent on requests a foreign page
     * initiated. Lax and not Strict, because entering ProjectForge by an external link (mail, bookmark) is the normal
     * case - which is why the public endpoints check Sec-Fetch-Site in addition, see
     * [org.projectforge.rest.core.RestCsrfProtection.isSameSiteRequest].
     */
    @Test
    fun stayLoggedInCookieAttributesTest() {
        val user = PFUserDO()
        user.id = 42
        user.username = "testuser"
        val request = Mockito.mock(HttpServletRequest::class.java)
        val response = Mockito.mock(HttpServletResponse::class.java)
        cookieService.addStayLoggedInCookie(request, response, user, "aaaa-bbbb-cccc-dddd")
        val captor = ArgumentCaptor.forClass(Cookie::class.java)
        Mockito.verify(response).addCookie(captor.capture())
        val cookie = captor.value
        Assertions.assertEquals("Lax", cookie.getAttribute("SameSite"), "Not to be sent on cross-site requests.")
        Assertions.assertTrue(cookie.isHttpOnly, "Not to be readable by javascript.")
        Assertions.assertEquals("/", cookie.path)
        Assertions.assertEquals(30 * 24 * 3600, cookie.maxAge, "30 days.")
        Assertions.assertEquals(
            "aaaa-bbbb-cccc-dddd",
            StayLoggedInCookieValue.deserialize(cookie.value)?.stayLoggedInKey,
            "Round trip: the value has to be readable by the check on the next request.",
        )
    }
}
