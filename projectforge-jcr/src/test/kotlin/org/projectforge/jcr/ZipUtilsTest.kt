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

import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.model.LocalFileHeader
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.test.TestUtils
import java.io.*

class ZipUtilsTest {
  private var testUtils = TestUtils(MODULE_NAME)
  val testDir = testUtils.deleteAndCreateTestFile("zipTests")

  init {
    testDir.mkdirs()
  }

  @Test
  fun encryptionTest() {
    checkMethod(ZipMode.ENCRYPTED_STANDARD, "pom.zip")
    checkMethod(ZipMode.ENCRYPTED_AES128, "pom-AES128.zip")
    checkMethod(ZipMode.ENCRYPTED_AES256, "pom-AES256.zip")
  }

  private fun checkMethod(mode: ZipMode, zipFilename: String) {
    val istream = FileInputStream("pom.xml")
    val zipFile = File(testDir, zipFilename)
    val outputStream = FileOutputStream(zipFile)
    outputStream.use {
      ZipUtils.encryptZipFile("pom.xml", "test123", istream, it, mode)
    }
    val content = File("pom.xml").readBytes()
    Assertions.assertTrue(ZipUtils.isEncrypted(FileInputStream(zipFile)))
    FileInputStream(zipFile).use {
      decryptZipFile("test123", it, "pom.xml", content)
    }
    FileInputStream(zipFile).use {
      Assertions.assertTrue(ZipUtils.testDecryptZipFile("test123", it), "Test of decryption failed with correct password.")
      Assertions.assertFalse(ZipUtils.testDecryptZipFile("wrongPassword", it), "Test of decryption worked with incorrect password?!")
    }
  }

  private fun decryptZipFile(
    password: String,
    inputStream: InputStream,
    expectedFileName: String,
    expectedContent: ByteArray
  ) {
    var localFileHeader: LocalFileHeader?
    ZipInputStream(inputStream, password.toCharArray()).use { zipInputStream ->
      while (zipInputStream.nextEntry.also { localFileHeader = it } != null) {
        Assertions.assertEquals(expectedFileName, localFileHeader!!.fileName)
        val baos = ByteArrayOutputStream()
        baos.use { out ->
          zipInputStream.copyTo(out)
        }
        Assertions.assertArrayEquals(expectedContent, baos.toByteArray())
      }
    }
  }
}
