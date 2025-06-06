/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.address.vcard

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.ImageType
import org.projectforge.business.test.TestSetup
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.Month

class VCardUtilsTest {
    @Test
    fun `test converting of vcards from and to AddressDO`() {
        TestSetup.init()
        AddressDO().also { address ->
            address.birthday = LocalDate.of(1992, Month.JULY, 11)
            address.firstName = "Joe"
            address.name = "Hill"
            val vcardString = VCardUtils.buildVCardString(address, VCardVersion.V_3_0, "https://www.projectforge.org/carddav/users/kai/photos/contact-1234.png", ImageType.JPEG)
            Assertions.assertTrue(vcardString.contains("BDAY:1992-07-11"))
        }
        AddressDO().also { address ->
            address.birthday = LocalDate.of(1992, Month.JULY, 11)
            address.firstName = "Joe"
            address.name = "Hill"
            val vcardString = VCardUtils.buildVCardString(address, VCardVersion.V_4_0)
            Assertions.assertTrue(vcardString.contains("BDAY:19920711"))
        }
        val vcard = VCardUtils.parseVCardsFromByteArray(EXAMPLE_VCF.toByteArray(StandardCharsets.UTF_8))
        Assertions.assertEquals(1, vcard.size)
        VCardUtils.buildAddressDO(vcard[0]).also { address ->
            Assertions.assertEquals("John", address.firstName)
            Assertions.assertEquals("Doe", address.name)
            Assertions.assertEquals(LocalDate.of(1992, Month.JULY, 11), address.birthday)
        }
    }

    private val EXAMPLE_VCF = """
        BEGIN:VCARD
        VERSION:3.0
        FN:John Doe
        N:Doe;John;;;
        ADR;TYPE=HOME:;;123 Main Street;Anytown;CA;12345;USA
        TEL;TYPE=CELL:+1-123-456-7890
        EMAIL:john.doe@example.com
        BDAY:1992-07-11
        END:VCARD""".trimIndent()
}
