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

package org.projectforge.gateway

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.gateway.push.MockIcsRequest

class GatewaySyncPushServiceTest {

    @Test
    fun mockIcsRequestProvidesCorrectParameters() {
        val request = MockIcsRequest(42L, "encryptedQueryParam")

        assertEquals("42", request.getParameter("user"))
        assertEquals("encryptedQueryParam", request.getParameter("q"))
        assertNull(request.getParameter("unknown"))
        assertEquals("user=42&q=encryptedQueryParam", request.queryString)
        assertEquals("127.0.0.1", request.remoteAddr)
    }

    @Test
    fun mockIcsRequestParameterMap() {
        val request = MockIcsRequest(7L, "abc123")

        val paramMap = request.parameterMap
        assertArrayEquals(arrayOf("7"), paramMap["user"])
        assertArrayEquals(arrayOf("abc123"), paramMap["q"])
    }

    @Test
    fun mockIcsRequestMetadata() {
        val request = MockIcsRequest(1L, "q")

        assertEquals("GET", request.method)
        assertEquals("/export/ProjectForge.ics", request.requestURI)
        assertEquals("HTTP/1.1", request.protocol)
        assertTrue(request.isSecure)
    }
}
