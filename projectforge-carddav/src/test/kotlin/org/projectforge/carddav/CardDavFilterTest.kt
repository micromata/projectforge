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
        checkRequest(false, "OPTIONS", "/carddav/users/kai", true)
        checkRequest(true, "OPTIONS", "/users/kai", true)
        checkRequest(false, "OPTIONS", "/carddav/users", false)
        checkRequest(true, "OPTIONS", "/users", false)

        checkRequest(false, "PROPFIND", "/carddav/users/kai", true)
        checkRequest(true, "PROPFIND", "/users/kai", true)
        checkRequest(false, "PROPFIND", "/carddav/users", false)
        checkRequest(true, "PROPFIND", "/users", false)

        checkRequest(false, "OPTIONS", "/.well-known/carddav", true)
    }

    private fun checkRequest(
        useRootPath: Boolean,
        method: String,
        requestUri: String?,
        expected: Boolean,
        msg: String? = null
    ): HttpServletRequest {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.requestURI).thenReturn(requestUri)
        Mockito.`when`(request.method).thenReturn(method)
        CardDavInit.cardDavUseRootPath = useRootPath
        Assertions.assertEquals(expected, CardDavFilter.handledByCardDavFilter(request), msg)
        return request
    }
}

/*
Call from old CardDav-Server:




 */
