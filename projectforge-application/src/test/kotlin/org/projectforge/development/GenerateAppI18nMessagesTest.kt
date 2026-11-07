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
 * Guards the generated catalogs of the mobile apps, see [GenerateNextI18nMessagesTest] (same reasons, same fix:
 * run [DevelopmentMainForRelease]).
 */
class GenerateAppI18nMessagesTest {
  @Test
  fun generatedCatalogsAreUpToDate() {
    val rootDir = GenerateNextI18nMessagesMain.resolveRootDir()
    GenerateAppI18nMessagesMain.TARGETS.filter { rootDir.resolve(it.appDir).isDirectory }.forEach { target ->
      GenerateAppI18nMessagesMain.generate(rootDir, target).forEach { (locale, expected) ->
        val file = GenerateAppI18nMessagesMain.outFile(rootDir, target, locale)
        assertTrue(file.isFile, "Generated message catalog is missing: ${file.absolutePath}")
        assertEquals(
          expected,
          file.readText(StandardCharsets.UTF_8),
          "${target.outDir}/generated.$locale.json is out of date or was edited by hand. Please run" +
              " DevelopmentMainForRelease; hand-written texts belong in ${target.outDir}/app.$locale.json."
        )
      }
    }
  }
}
