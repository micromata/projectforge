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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KundeFormatterTest {
    @Test
    fun testFormatting() {
        Assertions.assertEquals("", KundeFormatter.internalFormatKundeAsString(null, null))
        Assertions.assertEquals("", KundeFormatter.internalFormatKundeAsString(null, ""))
        Assertions.assertEquals("text", KundeFormatter.internalFormatKundeAsString(null, "text"))

        val kunde = KundeDO()
        Assertions.assertEquals("", KundeFormatter.internalFormatKundeAsString(kunde, null))
        Assertions.assertEquals("", KundeFormatter.internalFormatKundeAsString(kunde, ""))
        Assertions.assertEquals("text", KundeFormatter.internalFormatKundeAsString(kunde, "text"))

        kunde.name = ""
        Assertions.assertEquals("", KundeFormatter.internalFormatKundeAsString(kunde, null))
        Assertions.assertEquals("", KundeFormatter.internalFormatKundeAsString(kunde, ""))
        Assertions.assertEquals("text", KundeFormatter.internalFormatKundeAsString(kunde, "text"))

        kunde.name = "ACME"
        Assertions.assertEquals("ACME", KundeFormatter.internalFormatKundeAsString(kunde, null))
        Assertions.assertEquals("ACME", KundeFormatter.internalFormatKundeAsString(kunde, ""))
        Assertions.assertEquals("text; ACME", KundeFormatter.internalFormatKundeAsString(kunde, "text"))
    }
}
