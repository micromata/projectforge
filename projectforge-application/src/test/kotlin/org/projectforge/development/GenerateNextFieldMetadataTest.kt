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
 * Guards the generated field metadata of projectforge-next. They are produced by
 * [GenerateNextFieldMetadataMain] from the entity classes and rewritten from scratch on every run, so
 * a hand-written change to them is lost silently.
 *
 * It fails on all three ways files and entities can drift apart:
 * 1. someone edited a `*.generated.ts` by hand,
 * 2. someone changed an entity (column length, `required`, a new enum constant) and committed without
 *    regenerating — the very silence this whole generator exists to end,
 * 3. an entity was renamed or removed and its file stayed behind as an orphan.
 *
 * Either way the fix is the same: run [DevelopmentMainForRelease].
 */
class GenerateNextFieldMetadataTest {
  @Test
  fun generatedFieldMetadataIsUpToDate() {
    val rootDir = GenerateNextFieldMetadataMain.resolveRootDir()
    assertTrue(
      rootDir.resolve("projectforge-next").isDirectory,
      "Cannot locate the repository root, resolved '${rootDir.absolutePath}' has no projectforge-next directory."
    )
    val expectedFiles = GenerateNextFieldMetadataMain.generate(rootDir)
    expectedFiles.forEach { (fileName, expected) ->
      val file = GenerateNextFieldMetadataMain.outFile(rootDir, fileName)
      assertTrue(file.isFile, "Generated field metadata is missing: ${file.absolutePath}")
      assertEquals(
        expected,
        file.readText(StandardCharsets.UTF_8),
        "lib/metadata/$fileName is out of date or was edited by hand. Please run DevelopmentMainForRelease;" +
            " the rules belong into the entity's @PropertyInfo / @Column, not into this file."
      )
    }
    // Orphans: a renamed or deleted entity would otherwise leave its file (and its stale rules) behind.
    val orphans = GenerateNextFieldMetadataMain.outDir(rootDir)
      .listFiles { file -> file.name.endsWith(".generated.ts") }
      .orEmpty()
      .map { it.name }
      .filterNot { expectedFiles.containsKey(it) }
      .sorted()
    assertTrue(
      orphans.isEmpty(),
      "lib/metadata holds generated files for entities that no longer exist: ${orphans.joinToString()}." +
          " Delete them and run DevelopmentMainForRelease."
    )
  }
}
