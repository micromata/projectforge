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

package org.projectforge.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.utils.Crypt.encrypt
import org.projectforge.framework.utils.Crypt.decrypt
import org.projectforge.framework.utils.CryptStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

class CryptStreamTest {
  @Test
  fun encryptionTest() {
    val result = checkFile("pom.xml")
    val encrypted = result.first
    val decrypted = result.second
    Assertions.assertFalse(String(encrypted).contains("<artifactId>projectforge-common</artifactId>"))
    Assertions.assertTrue(String(decrypted).contains("<artifactId>projectforge-common</artifactId>"))
    
    checkFile("../doc/misc/ForecastExportProbabilities.xlsx")
  }

  private fun checkFile(file: String): Pair<ByteArray, ByteArray> {
    var outStream = ByteArrayOutputStream()
    CryptStream.encrypt(FileInputStream(file), outStream, "myPassword")
    val encrypted = outStream.toByteArray()
    outStream = ByteArrayOutputStream()
    CryptStream.decrypt(ByteArrayInputStream(encrypted), outStream, "myPassword")
    val decrypted = outStream.toByteArray()
    Assertions.assertArrayEquals(File(file).readBytes(), decrypted)
    return Pair(encrypted, decrypted)
  }
}
