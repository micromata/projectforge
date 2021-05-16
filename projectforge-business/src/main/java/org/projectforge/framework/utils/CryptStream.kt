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

package org.projectforge.framework.utils

import mu.KotlinLogging
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.spec.KeySpec
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


private val log = KotlinLogging.logger {}

/**
 * Encrypts and decrypts streams.
 * @author Kai Reinhard
 */
object CryptStream {
  @JvmStatic
  @JvmOverloads
  fun encrypt(inStream: InputStream, outStream: OutputStream, password: String, salt: String = STANDARD_SALT) {
    val secretKey = generateAESKey(password, salt)
    val cipher = createCipher()
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val iv = cipher.iv
    inStream.use { fileOut ->
      CipherOutputStream(outStream, cipher).use { cipherOut ->
        outStream.write(iv)
        inStream.copyTo(cipherOut)
      }
    }
  }

  @JvmStatic
  @JvmOverloads
  fun decrypt(inStream: InputStream, outStream: OutputStream, password: String, salt: String = STANDARD_SALT) {
    val fileIv = ByteArray(16)
    inStream.read(fileIv)
    val secretKey = generateAESKey(password, salt)
    val cipher = createCipher()
    cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(fileIv))
    CipherInputStream(inStream, cipher).use { cipherIn ->
      BufferedInputStream(cipherIn).use { reader ->
        reader.copyTo(outStream)
      }
    }
  }

  private fun createCipher(): Cipher {
    return Cipher.getInstance("AES/CBC/PKCS5Padding")
  }

  private fun generateAESKey(password: String, salt: String = STANDARD_SALT): SecretKey {
    // https://stackoverflow.com/questions/992019/java-256-bit-aes-password-based-encryption
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 65536, 256)
    val tmp = factory.generateSecret(spec)
    return SecretKeySpec(tmp.encoded, "AES")
  }

  /**
   * The salt avoids comparison of encrypted passwords with pre-calculated rainbow tables. You may use this
   * salt as standard.
   */
  const val STANDARD_SALT = "dMD15Gaij8FfwQ7n"
}
