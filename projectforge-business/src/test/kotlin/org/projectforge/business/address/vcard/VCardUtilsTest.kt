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

package org.projectforge.business.address.vcard

import org.junit.Test

class VCardUtilsTest {
    @Test
    fun testConvert() {
        //VCardUtils.convert(EXAMPLE_VCF)
    }

    private val EXAMPLE_VCF = """BEGIN:VCARD
            VERSION:3.0
            FN:John Doe
            N:Doe;John;;;
            ADR;TYPE=HOME:;;123 Main Street;Anytown;CA;12345;USA
            TEL;TYPE=CELL:+1-123-456-7890
            EMAIL:john.doe@example.com
            END:VCARD""".trimIndent()
}
