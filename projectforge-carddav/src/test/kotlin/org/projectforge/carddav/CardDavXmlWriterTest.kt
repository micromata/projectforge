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
import org.projectforge.framework.persistence.user.entities.PFUserDO

class CardDavXmlWriterTest {
    @Test
    fun test() {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.requestURI).thenReturn("/carddav")
        val requestWrapper = RequestWrapper(request)
        listOf(Prop.RESOURCETYPE, Prop.DISPLAYNAME).let { props ->
            PropFindRequestHandler.generatePropFindResponse(requestWrapper, PFUserDO().also { it.username = "kai" }, props).let {
                Assertions.assertTrue(it.contains("resourcetype"))
                Assertions.assertTrue(it.contains("displayname"))
                Assertions.assertFalse(it.contains("current-user-principal"))
                Assertions.assertFalse(it.contains("current-user-privilege-set"))
            }
        }
        listOf(
            Prop.CURRENT_USER_PRINCIPAL,
            Prop.CURRENT_USER_PRIVILEGE_SET,
        ).let { props ->
            PropFindRequestHandler.generatePropFindResponse(requestWrapper, PFUserDO().also { it.username = "kai" }, props).let {
                Assertions.assertFalse(it.contains("resourcetype"))
                Assertions.assertFalse(it.contains("displayname"))
                Assertions.assertTrue(it.contains("current-user-principal"))
                Assertions.assertTrue(it.contains("current-user-privilege-set"))
            }
        }
    }
}
