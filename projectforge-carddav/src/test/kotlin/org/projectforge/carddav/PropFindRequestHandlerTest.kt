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

class PropFindRequestHandlerTest {
    @Test
    fun test() {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.requestURI).thenReturn("/carddav/users/kai/")
        val requestWrapper = RequestWrapper(request)
        listOf(Prop.RESOURCETYPE, Prop.DISPLAYNAME).let { props ->
            PropFindRequestHandler.generatePropFindResponse(
                requestWrapper,
                PFUserDO().also { it.username = "kai" },
                props,
            ).let {
                val expected = """
                    |<?xml version="1.0" encoding="UTF-8"?>
                    |<d:multistatus xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/">
                    |  <d:response>
                    |    <d:href>/carddav/users/kai/</d:href>
                    |    <d:propstat>
                    |      <d:prop>
                    |        <d:resourcetype>
                    |          <card:addressbook />
                    |          <d:collection />
                    |        </d:resourcetype>
                    |        <d:displayname>address.cardDAV.addressbook.displayName</d:displayname>
                    |      </d:prop>
                    |      <d:status>HTTP/1.1 200 OK</d:status>
                    |    </d:propstat>
                    |  </d:response>
                    |</d:multistatus>
                    |""".trimMargin()
                Assertions.assertEquals(expected, it)
            }
        }
        listOf(
            Prop.CURRENT_USER_PRINCIPAL,
            Prop.CURRENT_USER_PRIVILEGE_SET,
            Prop.PRINCIPAL_URL,
        ).let { props ->
            PropFindRequestHandler.generatePropFindResponse(
                requestWrapper,
                PFUserDO().also { it.username = "kai" },
                props
            ).let {
                val expected = """
                    |<?xml version="1.0" encoding="UTF-8"?>
                    |<d:multistatus xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/">
                    |  <d:response>
                    |    <d:href>/carddav/users/kai/</d:href>
                    |    <d:propstat>
                    |      <d:prop>
                    |        <d:current-user-principal>
                    |          <d:href>/carddav/principals/users/kai/</d:href>
                    |        </d:current-user-principal>
                    |        <d:principal-URL>
                    |          <d:href>/carddav/principals/users/kai/</d:href>
                    |        </d:principal-URL>
                    |        <d:current-user-privilege-set>
                    |          <d:privilege><d:read /></d:privilege>
                    |          <d:privilege><d:all /></d:privilege>
                    |          <d:privilege><d:write /></d:privilege>
                    |          <d:privilege><d:write-properties /></d:privilege>
                    |          <d:privilege><d:write-content /></d:privilege>
                    |        </d:current-user-privilege-set>
                    |      </d:prop>
                    |      <d:status>HTTP/1.1 200 OK</d:status>
                    |    </d:propstat>
                    |  </d:response>
                    |</d:multistatus>
                    |""".trimMargin()
                Assertions.assertEquals(expected, it)
            }
        }
    }
}
