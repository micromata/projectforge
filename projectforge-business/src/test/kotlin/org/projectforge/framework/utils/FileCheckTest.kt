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

package org.projectforge.framework.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.test.TestSetup.init
import java.io.File

class FileCheckTest {
    @Test
    fun testCheckFile() {
        Assertions.assertNull(FileCheck.checkFile(mockFile(1024, "test.txt"), "txt", kiloBytes = 1))
        Assertions.assertEquals(
            "File upload rejected: maximum size of 1KB exceeded.",
            FileCheck.checkFile(mockFile(1025, "test.txt"), "txt", kiloBytes = 1)
        )
        Assertions.assertEquals(
            "The file format is not supported. Supported file formats: txt",
            FileCheck.checkFile(mockFile(1024, "test.xls"), "txt", kiloBytes = 1)
        )
    }

    private fun mockFile(size: Long, name: String = "test.txt"): File {
        val file = Mockito.mock(File::class.java)
        Mockito.`when`(file.length()).thenReturn(size)
        Mockito.`when`(file.absolutePath).thenReturn("/mock/path/$name")
        return file
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setUp() {
            // Needed if this test runs before the ConfigurationTest.
            val user = init()
            // Override the default German locale set by init() to English for consistent test expectations
            user.locale = java.util.Locale.ENGLISH
        }
    }
}
