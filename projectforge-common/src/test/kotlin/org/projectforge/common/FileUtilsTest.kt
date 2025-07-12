/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.mockito.Mockito
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

    @Test
    fun testFileExtension() {
        Assertions.assertFalse(FileUtils.checkExtension("", "txt"))
        Assertions.assertFalse(FileUtils.checkExtension("test.txt", "jpg"))
        Assertions.assertTrue(FileUtils.checkExtension("test.txt", "txt"))
        Assertions.assertTrue(FileUtils.checkExtension("test.TXT", "txt"))
        Assertions.assertTrue(FileUtils.checkExtension("test.TXT", "TXT"))
        Assertions.assertTrue(FileUtils.checkExtension("test.TXT", "txt", "jpg"))
        Assertions.assertFalse(FileUtils.checkExtension("test.TXT", "jpg"))
        Assertions.assertFalse(FileUtils.checkExtension("txt", "txt"))
        Assertions.assertTrue(FileUtils.checkExtension(".txt", "txt"))
        Assertions.assertFalse(FileUtils.checkExtension("test.txt", ""))
        Assertions.assertTrue(FileUtils.checkExtension("test", ""))
    }

    @Test
    fun testMaxBytes() {
        Assertions.assertTrue(FileUtils.checkMaxFileSize(mockFile(1000), 1000))
        Assertions.assertTrue(FileUtils.checkMaxFileSize(mockFile(1000), 0))
        Assertions.assertFalse(FileUtils.checkMaxFileSize(mockFile(1001), 1000))

        Assertions.assertTrue(FileUtils.checkMaxFileSize(mockFile(1000), 0, 1))
        Assertions.assertTrue(FileUtils.checkMaxFileSize(mockFile(1024), 0, 1))
        Assertions.assertTrue(FileUtils.checkMaxFileSize(mockFile(1025), 0, 0))
        Assertions.assertFalse(FileUtils.checkMaxFileSize(mockFile(1025), 0, 1))
        Assertions.assertTrue(FileUtils.checkMaxFileSize(mockFile(10 * 1024), 0, 10))
        Assertions.assertFalse(FileUtils.checkMaxFileSize(mockFile(10 * 1024 + 1), 0, 10))

        Assertions.assertTrue(FileUtils.checkMaxFileSize(mockFile(1000), 0, 0, 1))
        Assertions.assertTrue(FileUtils.checkMaxFileSize(mockFile(1024 * 1024), 0, 0, 1))
        Assertions.assertTrue(FileUtils.checkMaxFileSize(mockFile(1024 * 1024 + 1), 0, 0, 0))
        Assertions.assertFalse(FileUtils.checkMaxFileSize(mockFile(1024 * 1024 + 1), 0, 0, 1))
        Assertions.assertTrue(FileUtils.checkMaxFileSize(mockFile(10 * 1024 * 1024), 0, 0, 10))
        Assertions.assertFalse(FileUtils.checkMaxFileSize(mockFile(10 * 1024 * 1024 + 1), 0, 0, 10))
    }

    private fun mockFile(size: Long): File {
        val file = Mockito.mock(File::class.java)
        Mockito.`when`(file.length()).thenReturn(size)
        return file
    }
}
