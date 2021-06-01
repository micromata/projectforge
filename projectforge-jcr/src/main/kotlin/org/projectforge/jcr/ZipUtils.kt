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
import net.lingala.zip4j.model.LocalFileHeader
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


object ZipUtils {
  @JvmStatic
  fun isEncrypted(inputStream: InputStream): Boolean {
    try {
      ZipInputStream(inputStream).use { zipIn ->
        var zipEntry = zipIn.nextEntry
        while (zipEntry != null && zipEntry.isDirectory) {
          zipEntry = zipIn.nextEntry
        }
        return zipEntry?.isEncrypted == true
      }
    } catch (ex: ZipException) {
      return ex.type == ZipException.Type.WRONG_PASSWORD || // Standard encryption
          // empty or null password provided for AES decryption:
          ex.type == ZipException.Type.UNKNOWN && ex.message?.contains("password") == true
    }
  }

  @JvmStatic
  fun determineZipMode(inputStream: InputStream?): ZipMode? {
    inputStream ?: return null
    return if (isEncrypted(inputStream)) ZipMode.ENCRYPTED else ZipMode.STANDARD
  }

  @JvmStatic
  @JvmOverloads
  fun encryptZipFile(
    fileName: String,
    password: String,
    inputStream: InputStream,
    outputStream: OutputStream,
    mode: ZipMode = ZipMode.ENCRYPTED_STANDARD,
  ) {
    val zipParameters = ZipParameters()
    zipParameters.isEncryptFiles = true
    if (mode == ZipMode.ENCRYPTED_STANDARD || mode == ZipMode.ENCRYPTED) {
      zipParameters.encryptionMethod = EncryptionMethod.ZIP_STANDARD
    } else {
      zipParameters.encryptionMethod = EncryptionMethod.AES
      zipParameters.aesKeyStrength = if (mode == ZipMode.ENCRYPTED_AES128) {
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

  /*
  @JvmStatic
  @JvmOverloads
  fun deryptZipFile(
    password: String,
    inputStream: InputStream,
    outputStream: OutputStream,
  ) {
    var localFileHeader: LocalFileHeader?
    var readLen: Int
    val readBuffer = ByteArray(4096)

    ZipInputStream(inputStream, password.toCharArray()).use { zipInputStream ->
      while (zipInputStream.getNextEntry().also {
          localFileHeader = it
        } != null) {
        val extractedFile = File(localFileHeader.getFileName())
        FileOutputStream(extractedFile).use { outputStream ->
          while (zipInputStream.read(readBuffer).also {
              readLen = it
            } !== -1) {
            outputStream.write(readBuffer, 0, readLen)
          }
        }
      }
    }
  }*/
}
