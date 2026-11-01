/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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


package org.projectforge.development

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * Guards the generated catalogs of the mobile app, see [GenerateNextI18nMessagesTest] (same reasons, same fix:
 * run [DevelopmentMainForRelease]).
 */
class GenerateFlutterI18nMessagesTest {
  @Test
  fun generatedCatalogsAreUpToDate() {
    val rootDir = GenerateNextI18nMessagesMain.resolveRootDir()
    if (!rootDir.resolve("projectforge-flutter").isDirectory) {
      return
    }
    GenerateFlutterI18nMessagesMain.generate(rootDir).forEach { (locale, expected) ->
      val file = GenerateFlutterI18nMessagesMain.outFile(rootDir, locale)
      assertTrue(file.isFile, "Generated message catalog is missing: ${file.absolutePath}")
      assertEquals(
        expected,
        file.readText(StandardCharsets.UTF_8),
        "projectforge-flutter/assets/i18n/generated.$locale.json is out of date or was edited by hand. Please run" +
            " DevelopmentMainForRelease; hand-written texts belong in assets/i18n/app.$locale.json."
      )
    }
  }
}
