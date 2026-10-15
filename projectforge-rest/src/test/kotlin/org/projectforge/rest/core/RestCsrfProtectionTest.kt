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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

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

    private fun mockRequest(secFetchSite: String?): HttpServletRequest {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getHeader(Mockito.eq(RestCsrfProtection.SEC_FETCH_SITE_HEADER)))
            .thenReturn(secFetchSite)
        return request
    }
}
