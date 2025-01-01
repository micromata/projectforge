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
import java.io.*
import java.lang.IllegalArgumentException

class CryptStreamUtilsTest {
  @Test
  fun encryptionTest() {
    val result = checkFile("build.gradle.kts")
    val encrypted = result.first
    val decrypted = result.second
    Assertions.assertFalse(String(encrypted).contains("dependencies"))
    Assertions.assertTrue(String(decrypted).contains("dependencies"))

    checkFile("../doc/misc/ForecastExportProbabilities.xlsx")

    Assertions.assertFalse(CryptStreamUtils.wasWrongPassword(IllegalArgumentException("some other error")))
  }

  private fun checkFile(file: String): Pair<ByteArray, ByteArray> {
    var outStream = ByteArrayOutputStream()
    CryptStreamUtils.encrypt(FileInputStream(file), outStream, "myPassword")
    val encrypted = outStream.toByteArray()
    outStream = ByteArrayOutputStream()
    CryptStreamUtils.decrypt(ByteArrayInputStream(encrypted), outStream, "myPassword")
    val decrypted = outStream.toByteArray()
    Assertions.assertArrayEquals(File(file).readBytes(), decrypted)
    outStream = ByteArrayOutputStream()
    val ex = Assertions.assertThrows(IllegalArgumentException::class.java) {
      CryptStreamUtils.decrypt(ByteArrayInputStream(encrypted), outStream, "wrongPassword")
    }
    Assertions.assertTrue(CryptStreamUtils.wasWrongPassword(ex))
    return Pair(encrypted, decrypted)
  }
}
