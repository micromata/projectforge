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

package org.projectforge.carddav

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.user.entities.PFUserDO

class CardDavUtilsTest {
    @Test
    fun `test getPrincipalsUsersUrl`() {
        Assertions.assertEquals("/carddav/principals/users/joe/", CardDavUtils.getPrincipalsUsersUrl("/carddav/users/...", PFUserDO().also { it.username = "joe" }))
        Assertions.assertEquals("/principals/users/joe/", CardDavUtils.getPrincipalsUsersUrl("/users/...", PFUserDO().also { it.username = "joe" }))
    }

    @Test
    fun `test normalizedUri`() {
        Assertions.assertEquals("users", CardDavUtils.normalizedUri("/carddav/users/"))
        Assertions.assertEquals("users", CardDavUtils.normalizedUri("/users/"))
        Assertions.assertEquals("principals", CardDavUtils.normalizedUri("/principals/"))
    }

    @Test
    fun `test extraction of contact id`() {
        Assertions.assertEquals(123, CardDavUtils.extractContactId("/carddav/users/joe/ProjectForge-123.vcf"))
        Assertions.assertEquals(123, CardDavUtils.extractContactId("/carddav/photos/contact-123.jpg"))
        Assertions.assertEquals(123, CardDavUtils.extractContactId("/carddav/photos/contact-123.png"))
    }
}
