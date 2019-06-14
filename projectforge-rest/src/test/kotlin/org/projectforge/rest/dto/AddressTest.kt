/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressbookDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class AddressTest {

    @Test
    fun dtoTest() {
        val src = AddressDO()
        src.firstName = "Kai"
        src.uid = "42"
        src.id = 1
        val addressbook = AddressbookDO()
        val user = PFUserDO()
        user.id = 3
        user.username = "kai"
        user.email = "email"
        addressbook.id = 8
        addressbook.description = "Description"
        addressbook.title = "Title"
        addressbook.owner = user
        src.addressbookList!!.add(addressbook)
        val dest = Address()
        dest.copyFrom(src)

        assertEquals("Kai", dest.firstName)
        assertEquals(1, dest.id)
        assertEquals(1, dest.addressbookList?.size)

        val addressDO = AddressDO()
        dest.copyTo(addressDO)
        assertEquals("Kai", addressDO.firstName)
        assertEquals(1, addressDO.id)
        assertEquals(1, addressDO.addressbookList?.size)
    }
}
