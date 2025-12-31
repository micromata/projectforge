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

package org.projectforge.web

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.carddav.CardDavXmlUtils
import org.projectforge.carddav.model.Contact
import org.projectforge.carddav.model.User
import java.util.Date

class CardDavControllerTest {
    @Test
    fun `test writing xml response of PROPFIND`() {
        StringBuilder().let { sb ->
            CardDavXmlUtils.appendMultiStatusStart(sb)
            Assertions.assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<d:multistatus xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\" xmlns:cs=\"http://calendarserver.org/ns/\" xmlns:me=\"http://me.com/_namespace/\">\n",
                sb.toString()
            )
        }
        StringBuilder().let { sb ->
            CardDavXmlUtils.appendMultiStatusStart(sb, false)
            Assertions.assertEquals(
                "<d:multistatus xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\" xmlns:cs=\"http://calendarserver.org/ns/\" xmlns:me=\"http://me.com/_namespace/\">\n",
                sb.toString()
            )
        }
        StringBuilder().let { sb ->
            CardDavXmlUtils.appendMultiStatusEnd(sb)
            Assertions.assertEquals("</d:multistatus>\n", sb.toString())
        }
        StringBuilder().let { sb ->
            val user = User("kai")
            val contact = Contact(42L, "Kai", "Reinhard", Date(1234567890))
            //CardDavXmlWriter.appendPropfindContact(sb, user, contact)
            //Assertions.assertEquals(response, sb.toString())
        }
    }
}
