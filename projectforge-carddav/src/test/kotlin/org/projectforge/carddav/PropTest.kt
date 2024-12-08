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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PropTest {
    @Test
    fun `test of extracting propFinds`() {
        """
            <propfind xmlns="DAV:">
              <prop>
                <resourcetype/>
                <displayname/>
                <current-user-principal/>
                <current-user-privilege-set/>
              </prop>
            </propfind>
        """.trimIndent().let { xml ->
            val propFinds = Prop.extractProps(xml)
            Assertions.assertEquals(4, propFinds.size)
            Assertions.assertEquals(Prop.RESOURCETYPE, propFinds[0])
            Assertions.assertEquals(Prop.DISPLAYNAME, propFinds[1])
            Assertions.assertEquals(Prop.CURRENT_USER_PRINCIPAL, propFinds[2])
            Assertions.assertEquals(Prop.CURRENT_USER_PRIVILEGE_SET, propFinds[3])
        }
        """
            <sync-collection xmlns="DAV:">
              <sync-token>
                    https://www.projectforge.org/ns/sync/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
                  </sync-token>
              <sync-level>1</sync-level>
              <d:prop>
                <resourcetype/>
                <d:getetag/>
                <cs:getctag/>
                <sync-token/>
              </d:prop>
            </propfind>
        """.trimIndent().let { xml ->
            val propFinds = Prop.extractProps(xml)
            Assertions.assertEquals(4, propFinds.size)
            Assertions.assertEquals(Prop.RESOURCETYPE, propFinds[0])
            Assertions.assertEquals(Prop.GETETAG, propFinds[1])
            Assertions.assertEquals(Prop.GETCTAG, propFinds[2])
            Assertions.assertEquals(Prop.SYNCTOKEN, propFinds[3])
        }
    }
}
