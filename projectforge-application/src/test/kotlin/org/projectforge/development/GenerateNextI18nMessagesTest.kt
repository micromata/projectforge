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
 * Guards the generated next-intl catalogs of projectforge-next. They are produced by
 * [GenerateNextI18nMessagesMain] from the I18nResources bundle and rewritten from scratch on every
 * run, so a hand-written change to them is lost silently. The file name (`generated.<locale>.json`
 * next to the hand-written `<locale>.json`) is a convention only — this test is what enforces it.
 *
 * It fails on both ways the two can drift apart:
 * 1. someone edited `generated.<locale>.json` directly instead of `<locale>.json`,
 * 2. someone changed I18nResources and committed without regenerating.
 *
 * Either way the fix is the same: run [DevelopmentMainForRelease].
 */
class GenerateNextI18nMessagesTest {
  @Test
  fun generatedCatalogsAreUpToDate() {
    val rootDir = GenerateNextI18nMessagesMain.resolveRootDir()
    assertTrue(
      rootDir.resolve("projectforge-next").isDirectory,
      "Cannot locate the repository root, resolved '${rootDir.absolutePath}' has no projectforge-next directory."
    )
    GenerateNextI18nMessagesMain.generate(rootDir).forEach { (locale, expected) ->
      val file = GenerateNextI18nMessagesMain.outFile(rootDir, locale)
      assertTrue(file.isFile, "Generated message catalog is missing: ${file.absolutePath}")
      assertEquals(
        expected,
        file.readText(StandardCharsets.UTF_8),
        "messages/generated.$locale.json is out of date or was edited by hand. Please run" +
            " DevelopmentMainForRelease; hand-written texts belong in messages/$locale.json."
      )
    }
  }
}
