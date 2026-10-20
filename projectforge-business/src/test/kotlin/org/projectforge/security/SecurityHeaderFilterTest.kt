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


package org.projectforge.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockFilterConfig
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class SecurityHeaderFilterTest {
    @Test
    fun `headers are sent once, with the configured csp`() {
        val response = filter("default-src 'self'")
        assertEquals(listOf("SAMEORIGIN"), response.getHeaders("X-Frame-Options"))
        assertEquals(listOf("0"), response.getHeaders("X-XSS-Protection"))
        assertEquals(listOf("default-src 'self'"), response.getHeaders("Content-Security-Policy"))
        assertEquals("strict-origin-when-cross-origin", response.getHeader("Referrer-Policy"))
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"))
        assertNull(response.getHeader("X-Content-Security-Policy"))
        assertNull(response.getHeader("X-WebKit-CSP"))
    }

    @Test
    fun `no csp without a configured value`() {
        assertNull(filter("").getHeader("Content-Security-Policy"))
    }

    private fun filter(csp: String): MockHttpServletResponse {
        val filter = SecurityHeaderFilter()
        filter.init(MockFilterConfig().also { it.addInitParameter(SecurityHeaderFilter.PARAM_CSP_HEADER_VALUE, csp) })
        val response = MockHttpServletResponse()
        // A header already set (e.g. by a previous filter) is replaced, not doubled:
        response.addHeader("X-Frame-Options", "DENY")
        filter.doFilter(MockHttpServletRequest(), response, MockFilterChain())
        return response
    }
}
