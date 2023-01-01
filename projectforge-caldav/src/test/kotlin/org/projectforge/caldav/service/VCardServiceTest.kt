/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressImageDao
import org.projectforge.caldav.service.VCardService
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.Month

class VCardServiceTest {
    @Test
    fun vcardTest() {
        val address = AddressDO()
        address.birthday = LocalDate.of(1970, Month.NOVEMBER, 21)
        address.firstName = "Kai"
        address.name = "Reinhard"
        val byteArray = VCardService().buildVCardByteArray(address, AddressImageDao())
        Assertions.assertTrue(byteArray.toString(StandardCharsets.UTF_8).contains("BDAY:1970-11-21"))
    }
}
