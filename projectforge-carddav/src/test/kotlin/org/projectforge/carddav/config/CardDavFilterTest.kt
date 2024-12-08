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

package org.projectforge.web

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.carddav.CardDavFilter

class CardDavFilterTest {
    @Test
    fun handledByMiltonFilterTest() {
        checkRequest("/.well-known/carddav", "PROPFIND", true)
        checkRequest("/carddav/users/", "PROPFIND", true)
        checkRequest("/wa/...", "PROPFIND", false)

        checkRequest("....", "GET", false)
        checkRequest("/users", "GET", false)

        arrayOf("OPTIONS", "PROPFIND", "REPORT").forEach {
            checkMethod(it)
        }
    }

    private fun checkMethod(method: String) {
        checkRequest("....", method, false)
        checkRequest("users", method, false)
        checkRequest("/carddav/users/", method, true)
    }

    private fun checkRequest(uri: String, method: String, expected: Boolean) {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.method).thenReturn(method)
        Mockito.`when`(request.requestURI).thenReturn(uri)
        Assertions.assertEquals(expected, CardDavFilter.handledByCardDavFilter(request))
    }
}
