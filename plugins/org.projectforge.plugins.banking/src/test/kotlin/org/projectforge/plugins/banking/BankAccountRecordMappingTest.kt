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

package org.projectforge.plugins.banking

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.rest.dto.BankAccountRecordMapping

class BankAccountRecordMappingTest {
  @Test
  fun regexTest() {
    Assertions.assertEquals("", BankAccountRecordMapping.createRegexString(""))
    Assertions.assertEquals("123", BankAccountRecordMapping.createRegexString("123"))
    Assertions.assertEquals("Test \\(or more\\)", BankAccountRecordMapping.createRegexString("Test (or more)"))
    Assertions.assertEquals("Test.*\\(or m.re\\)", BankAccountRecordMapping.createRegexString("Test*(or m?re)"))

    assertMatches(" Hurzel (Test)", "hurz*")
    assertMatches(" Hurzel (Test)", "hurz *", false)
    assertMatches(" Harzel (Test)", "h?rz*")
    assertMatches(" Haurzel (Test)", "h*rz*")
    assertMatches("This is a longer text", "*long*")
    assertMatches("This is a longer text", "*long", false)
  }

  private fun assertMatches(header: String, userString: String, matches: Boolean = true) {
    Assertions.assertEquals(matches, BankAccountRecordMapping.matches(header, BankAccountRecordMapping.createRegex(userString)))
  }
}
