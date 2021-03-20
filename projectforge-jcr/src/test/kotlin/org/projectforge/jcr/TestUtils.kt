/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.jcr

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import java.io.File
import java.nio.file.Paths

private val log = KotlinLogging.logger {}

private const val MODULE_NAME = "projectforge-jcr"

object TestUtils {

    private val location: String
    private val testOutDir: File

    init {
        var uriString = this::class.java.protectionDomain.codeSource.location.toString().removeSuffix("/src/main/kotlin/org/projectforge/jcr")
        Assertions.assertTrue(uriString.startsWith("file:"), "We're not running in a normal file system. Can't proceed with tests.")
        uriString = uriString.removePrefix("file:")
        Assertions.assertTrue(uriString.contains("/$MODULE_NAME"), "Where we're running? '/$MODULE_NAME' expected in path to find sources for testing, but not found '$uriString'.")
        // Determine location of module projectforge-jcr
        while (uriString.contains("/$MODULE_NAME/")) {
            uriString = uriString.substring(0, uriString.lastIndexOf('/'))
        }
        location = uriString
        val testDir = File(location, "test")
        testOutDir = File(testDir, "out")
        if (!testOutDir.exists()) {
            testOutDir.mkdirs()
        }
    }

    internal fun deleteAndCreateTestFile(name: String): File {
        val file = File(testOutDir, name)
        file.deleteRecursively()
        return file
    }

    internal fun determineFile(vararg path: String): File {
        return Paths.get(location, *path).toFile()
    }
}
