/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class FileUtilsTest {
    @Test
    fun testCreateFile() {
        var file = File("").absoluteFile
        Assertions.assertEquals(file.absolutePath, FileUtils.createFile("").absolutePath)
        file = File(file, "test")
        Assertions.assertEquals(file.absolutePath, FileUtils.createFile("test").absolutePath)
        file = File(file, "subdir")
        Assertions.assertEquals(file.absolutePath, FileUtils.createFile("test", "subdir").absolutePath)

        val parent = File(".")
        file = File("").absoluteFile
        Assertions.assertEquals(file.absolutePath, FileUtils.createFile(parent).canonicalPath)
        file = File(file, "test")
        Assertions.assertEquals(file.absolutePath, FileUtils.createFile(parent, "test").canonicalPath)
        file = File(file, "subdir")
        Assertions.assertEquals(file.absolutePath, FileUtils.createFile(parent, "test", "subdir").canonicalPath)
    }
}
