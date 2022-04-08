/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.fibu.IBANFormatter.format

class IBANFormatterTest {
  @Test
  fun projektKundeAsStringText() {
    Assertions.assertNull(format(null))
    Assertions.assertEquals("", format(""))
    Assertions.assertEquals("DE12 3456", format("DE123456"))
    Assertions.assertEquals("DE123 456", format("DE123 456"), "Is already formattted.")
    Assertions.assertEquals(" DE123 456", format(" DE123 456"), "Is already formattted.")
    Assertions.assertEquals("DE12 3456 7890 12", format("  DE123456789012"))
  }
}
