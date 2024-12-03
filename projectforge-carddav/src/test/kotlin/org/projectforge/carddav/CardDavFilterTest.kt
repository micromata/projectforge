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

package org.projectforge.carddav

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CardDavFilterTest {
    @Test
    fun `test handledByCardDavFilter`() {
        checkRequest(
            "/",
            "PROPFIND",
            "/path doesn't matter",
            true,
            "PROPFIND should be handled by CardDavFilter, independent of the request URI"
        )
        checkRequest("/carddav", "OPTIONS", "/carddav/users", true)
        checkRequest("/", "OPTIONS", "/users", true)
    }

    private fun checkRequest(
        basePath: String,
        method: String,
        requestUri: String?,
        expected: Boolean,
        msg: String? = null
    ): HttpServletRequest {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.requestURI).thenReturn(requestUri)
        Mockito.`when`(request.method).thenReturn(method)
        CardDavInit.cardDavBasePath = basePath
        Assertions.assertEquals(expected, CardDavFilter.handledByCardDavFilter(request), msg)
        return request
    }
}
