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

package org.projectforge.i18n

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class I18nKeysUsageTest {
  /**
   * Creates the i18n key usage file on each test run (should be committed to Git after modifications).
   * This file is used for download Excel-file with i18 translations and usages from running ProjectForge-App.
   */
  @Test
  fun createI18nKeysUsageFile() {
    val usage = I18nKeysUsage(true) // Creates all i18n usages by analyzing sources and writes it to json file.
    val readUsage = I18nKeysUsage() // Loads all i18n usages from json file generated above.
    Assertions.assertEquals(usage.i18nKeyMap.size, readUsage.i18nKeyMap.size, "Size of analyze run should be the same after read from json.")
    usage.i18nKeyMap.values.forEach { entry ->
      val readEntry = readUsage.i18nKeyMap[entry.i18nKey]
      Assertions.assertNotNull(readEntry, "Entry should be load from json.")
      Assertions.assertEquals(entry.files, readEntry!!.files)
    }
  }
}
