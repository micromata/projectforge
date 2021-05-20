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

import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.InputStream
import java.io.OutputStream

object ZipUtils {
  @Suppress("unused")
  enum class EncryptionMode { ZIP_STANDARD, AES128, AES256 }

  @JvmStatic
  fun isEncrypted(inputStream: InputStream): Boolean {
    try {
      ZipInputStream(inputStream).nextEntry // nextEntry.isEncrypted doesn't work.
      return false
    } catch (ex: ZipException) {
      return ex.type == ZipException.Type.WRONG_PASSWORD || // Standard encryption
          // empty or null password provided for AES decryption:
          ex.type == ZipException.Type.UNKNOWN && ex.message?.contains("password") == true
    }
  }

  @JvmStatic
  @JvmOverloads
  fun encryptZipFile(
    fileName: String,
    password: String,
    inputStream: InputStream,
    outputStream: OutputStream,
    mode: EncryptionMode = EncryptionMode.ZIP_STANDARD,
  ) {
    val zipParameters = ZipParameters()
    zipParameters.isEncryptFiles = true
    if (mode == EncryptionMode.ZIP_STANDARD) {
      zipParameters.encryptionMethod = EncryptionMethod.ZIP_STANDARD
    } else {
      zipParameters.encryptionMethod = EncryptionMethod.AES
      zipParameters.aesKeyStrength = if (mode == EncryptionMode.AES128) {
        AesKeyStrength.KEY_STRENGTH_128
      } else {
        AesKeyStrength.KEY_STRENGTH_256
      }
    }
    val zipOutputStream = ZipOutputStream(outputStream, password.toCharArray())
    zipOutputStream.use { zipOut ->
      zipParameters.fileNameInZip = fileName
      zipOut.putNextEntry(zipParameters)
      inputStream.use {
        it.copyTo(zipOut)
      }
      zipOut.closeEntry()
    }
  }
}
