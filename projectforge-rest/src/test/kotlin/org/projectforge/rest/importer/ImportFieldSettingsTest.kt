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

package org.projectforge.rest.importer

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ImportFieldSettingsTest {
  @Test
  fun regexTest() {
    Assertions.assertEquals("", ImportFieldSettings.createRegexString(""))
    Assertions.assertEquals("123", ImportFieldSettings.createRegexString("123"))
    Assertions.assertEquals("Test \\(or more\\)", ImportFieldSettings.createRegexString("Test (or more)"))
    Assertions.assertEquals("Test.*\\(or m.re\\)", ImportFieldSettings.createRegexString("Test*(or m?re)"))

    assertMatches(" Hurzel (Test)", "hurz*")
    assertMatches(" Hurzel (Test)", "hurz *", false)
    assertMatches(" Harzel (Test)", "h?rz*")
    assertMatches(" Haurzel (Test)", "h*rz*")
    assertMatches("This is a longer text", "*long*")
    assertMatches("This is a longer text", "*long", false)
  }

  private fun assertMatches(header: String, userString: String, matches: Boolean = true) {
    Assertions.assertEquals(matches, ImportFieldSettings.matches(header, ImportFieldSettings.createRegex(userString)))
  }

  @Test
  fun parseFieldSettingsTest() {
    checkValues("", emptyArray(), emptyArray())
    checkValues("alias ", arrayOf("alias"), emptyArray())
    checkValues(":dd.MM.yyyy | | ", emptyArray(), arrayOf("dd.MM.yyyy"))
    checkValues(":dd.MM.yyyy | alias| :dd.MM.yy|alias2 ", arrayOf("alias", "alias2"), arrayOf("dd.MM.yyyy", "dd.MM.yy"))
  }

  companion object {

    internal fun checkValues(str: String, expectedAliases: Array<String>, expectedParseFormats: Array<String>) {
      val entry = ImportFieldSettings("someProp")
      entry.parseSettings(str)
      checkFieldSettings(entry, expectedAliases, expectedParseFormats)
    }

    internal fun checkFieldSettings(
      entry: ImportFieldSettings,
      expectedAliases: Array<String>,
      expectedParseFormats: Array<String>,
    ) {
      Assertions.assertEquals(expectedAliases.size, entry.aliasList.size)
      for (i in 0 until expectedAliases.size) {
        Assertions.assertEquals(expectedAliases[i], entry.aliasList[i])
      }
      Assertions.assertEquals(expectedParseFormats.size, entry.parseFormatList.size)
      for (i in 0 until expectedParseFormats.size) {
        Assertions.assertEquals(expectedParseFormats[i], entry.parseFormatList[i])
      }
    }
  }
}
