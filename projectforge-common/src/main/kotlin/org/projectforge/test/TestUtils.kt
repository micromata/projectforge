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

package org.projectforge.test

import mu.KotlinLogging
import java.io.File
import java.nio.file.Paths

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class TestUtils(modulName: String) {
  val baseDir: File = File(".").absoluteFile.parentFile
  val outputDir: File
  init {
    check(baseDir.name == modulName) {
     "Check of working test directory failed. Base dir isn't module directory: ${baseDir.absolutePath}"
    }
    val testDir = File(baseDir, "test")
    outputDir = File(testDir, "out")
    if (!outputDir.exists()) {
      outputDir.mkdirs()
    }
  }

  fun deleteAndCreateTestFile(name: String): File {
    val file = File(outputDir, name)
    file.deleteRecursively()
    return file
  }

  fun determineBaseDirFile(vararg path: String): File {
    return Paths.get(baseDir.absolutePath, *path).toFile()
  }

  fun determineOutputDirFile(vararg path: String): File {
    return Paths.get(baseDir.absolutePath, *path).toFile()
  }

  /*
  Automatic detection:
          var uriString = this::class.java.protectionDomain.codeSource.location.toString().removeSuffix("/src/main/kotlin/org/projectforge/jcr")
        Assertions.assertTrue(uriString.startsWith("file:"), "We're not running in a normal file system. Can't proceed with tests.")
        uriString = uriString.removePrefix("file:")
        Assertions.assertTrue(uriString.contains("/$MODULE_NAME"), "Where we're running? '/$MODULE_NAME' expected in path to find sources for testing, but not found '$uriString'.")
        // Determine location of module projectforge-jcr
        while (uriString.contains("/$MODULE_NAME/")) {
            uriString = uriString.substring(0, uriString.lastIndexOf('/'))
        }

   */
}
