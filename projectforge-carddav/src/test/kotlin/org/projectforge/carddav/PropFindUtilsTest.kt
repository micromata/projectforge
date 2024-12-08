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

class PropFindUtilsTest {
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
            val propFinds = PropFindUtils.extractProps(xml)
            Assertions.assertEquals(4, propFinds.size)
            Assertions.assertEquals(PropFindUtils.Prop.RESOURCETYPE, propFinds[0])
            Assertions.assertEquals(PropFindUtils.Prop.DISPLAYNAME, propFinds[1])
            Assertions.assertEquals(PropFindUtils.Prop.CURRENT_USER_PRINCIPAL, propFinds[2])
            Assertions.assertEquals(PropFindUtils.Prop.CURRENT_USER_PRIVILEGE_SET, propFinds[3])
        }
        """
            <propfind xmlns="DAV:">
              <prop>
                <resourcetype/>
                <getetag/>
                <cs:getctag/>
                <sync-token/>
              </prop>
            </propfind>
        """.trimIndent().let { xml ->
            val propFinds = PropFindUtils.extractProps(xml)
            Assertions.assertEquals(4, propFinds.size)
            Assertions.assertEquals(PropFindUtils.Prop.RESOURCETYPE, propFinds[0])
            Assertions.assertEquals(PropFindUtils.Prop.GETETAG, propFinds[1])
            Assertions.assertEquals(PropFindUtils.Prop.GETCTAG, propFinds[2])
            Assertions.assertEquals(PropFindUtils.Prop.SYNCTOKEN, propFinds[3])
        }
    }
}
