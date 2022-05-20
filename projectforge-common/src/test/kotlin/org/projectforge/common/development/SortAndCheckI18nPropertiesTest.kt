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

package org.projectforge.common.development

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SortAndCheckI18nPropertiesTest {
  @Test
  fun fixApostrophCharsAndReplaceUTFCharTest() {
    check("", "")
    check("'", "'")
    check("'text", "'text")
    check("text'", "text'")
    check("Don't believe the hype.", "Don't believe the hype.")
    check("Don''t believe the hype.", "Don''t believe the hype.")
    check("Don''t escape '{0}'.", "Don't escape '{0}'.")
    check("Field ''\${label}'' is", "Field ''\${label}'' is")
    val germanText = "Dies ist ein gültiger Deutscher Text mit allen Umlauten: äöüÄÖÜß"
    check(
      "Dies ist ein gültiger Deutscher Text mit allen Umlauten: äöüÄÖÜß",
      germanText
    )
  }

  @Test
  fun multilineHandlingTest() {
    assertEquals("", SortAndCheckI18nPropertiesMain.reduceMultiLine(""))
    assertEquals("test", SortAndCheckI18nPropertiesMain.reduceMultiLine("test"))
    assertEquals("test... (multiline)", SortAndCheckI18nPropertiesMain.reduceMultiLine("test\\\nline 2"))
    println(multiline)
    assertEquals(reducedMultiline, SortAndCheckI18nPropertiesMain.reduceMultiLine(multiline))

    assertEquals("", SortAndCheckI18nPropertiesMain.commentMultiLine(""))
    assertEquals("test", SortAndCheckI18nPropertiesMain.commentMultiLine("test"))
    assertEquals("test\\\n#line 2", SortAndCheckI18nPropertiesMain.commentMultiLine("test\\\nline 2"))
  }

  private fun check(expected: String, src: String) {
    assertEquals(expected, SortAndCheckI18nPropertiesMain.fixApostrophCharsAndReplaceUTFChars(src))
  }

  private val multiline =
    """### Two factor authentication\n\n1. Simply scan this barcode with your smartphone.\n\
2. Your authenticator app should be opened.\n3. Done.\n\nEvery time ProjectForge requests a code, enter the displayed code in your Authenticator app.\n\nYou may setup others Authenticator apps on different devices as a backup, if you want.\n\n> 2FA are valid up to 30 days (depends on the security level of the used functionality), please use the stay-logge-in functionality on login to prevent annoying 2FA requests.\n"""
  private val reducedMultiline =
    """### Two factor authentication\n\n1. Simply scan this barcode with your smartphone.\n... (multiline)"""
}
