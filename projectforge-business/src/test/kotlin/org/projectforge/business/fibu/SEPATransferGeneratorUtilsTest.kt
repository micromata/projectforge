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

class SEPATransferGeneratorUtilsTest {
  @Test
  fun replaceSubjectTest() {
    Assertions.assertEquals("", SEPATransferGeneratorUtils.eraseUnsuportedChars(null))
    Assertions.assertEquals("1234567890", SEPATransferGeneratorUtils.eraseUnsuportedChars("\t\n\r\u202D1234567890\u202C"))
    Assertions.assertEquals("Buerokosten a konto ss 4711,-,,", SEPATransferGeneratorUtils.eraseUnsuportedChars("Bürokosten à konto ß #4711;_;;"))
  }
}
