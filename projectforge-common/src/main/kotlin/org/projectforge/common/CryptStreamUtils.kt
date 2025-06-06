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

import java.io.*
import java.security.spec.KeySpec
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// private val log = KotlinLogging.logger {}

/**
 * Encrypts and decrypts streams by using AES.
 * @author Kai Reinhard
 */
object CryptStreamUtils {
  @JvmStatic
  @JvmOverloads
  fun encrypt(sourceStream: InputStream, outStream: OutputStream, password: String, salt: String = STANDARD_SALT) {
    // https://www.baeldung.com/java-cipher-input-output-stream
    val cipher = createEncryptionCipher(password, salt)
    val iv = cipher.iv
    sourceStream.use { istream ->
      CipherOutputStream(outStream, cipher).use { cipherOut ->
        outStream.write(iv)
        istream.copyTo(cipherOut)
      }
    }
  }

  @JvmStatic
  @JvmOverloads
  fun pipeToEncryptedInputStream(
    sourceStream: InputStream,
    password: String,
    salt: String = STANDARD_SALT
  ): InputStream {
    val pipedInputStream = PipedInputStream()
    val pipedOutputStream = PipedOutputStream(pipedInputStream)
    val cipher = createEncryptionCipher(password, salt)
    val iv = cipher.iv
    Thread {
      sourceStream.use { istream ->
        CipherOutputStream(pipedOutputStream, cipher).use { cipherOut ->
          cipherOut.write(iv)
          istream.copyTo(cipherOut)
        }
      }
    }.start()
    return pipedInputStream
  }

  @JvmOverloads
  @JvmStatic
  fun createEncryptionCipher(password: String, salt: String = STANDARD_SALT): Cipher {
    val secretKey = generateAESKey(password, salt)
    val cipher = createCipher()
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    return cipher
  }

  @JvmOverloads
  @JvmStatic
  fun createDecryptionCipher(password: String, salt: String = STANDARD_SALT): Cipher {
    val secretKey = generateAESKey(password, salt)
    val cipher = createCipher()
    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    return cipher
  }

  /**
   * For wrong passwords, an Exception is thrown.
   */
  @JvmStatic
  @JvmOverloads
  fun decrypt(sourceStream: InputStream, outStream: OutputStream, password: String, salt: String = STANDARD_SALT) {
    // https://www.baeldung.com/java-cipher-input-output-stream
    val fileIv = ByteArray(16)
    try {
      sourceStream.use { inputStream ->
        inputStream.read(fileIv)
        val secretKey = generateAESKey(password, salt)
        val cipher = createCipher()
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(fileIv))
        CipherInputStream(inputStream, cipher).use { cipherIn ->
          BufferedInputStream(cipherIn).use { reader ->
            reader.copyTo(outStream)
          }
        }
      }
    } catch (ex: IOException) {
      if (ex.cause is BadPaddingException) {
        throw IllegalArgumentException(WRONG_PASSWORD_MESSAGE)
      } else {
        throw ex
      }
    }
  }

  @JvmStatic
  @JvmOverloads
  fun pipeToDecryptedInputStream(
    sourceStream: InputStream,
    password: String,
    salt: String = STANDARD_SALT
  ): InputStream {
    // https://www.baeldung.com/java-cipher-input-output-stream
    val baos = ByteArrayOutputStream()
    decrypt(sourceStream, baos, password, salt)
    return ByteArrayInputStream(baos.toByteArray())
/*    val fileIv = ByteArray(16)
    val pipedInputStream = PipedInputStream()
    val pipedOutputStream = PipedOutputStream(pipedInputStream)
    sourceStream.use { inputStream ->
      inputStream.read(fileIv)
      val secretKey = generateAESKey(password, salt)
      val cipher = createCipher()
      cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(fileIv))
      Thread {
        CipherInputStream(inputStream, cipher).use { cipherIn ->
          BufferedInputStream(cipherIn).use { reader ->
            reader.copyTo(pipedOutputStream)
          }
        }
      }
    }.start()
    return pipedInputStream*/
  }

  fun wasWrongPassword(ex: Exception): Boolean {
    return ex is IllegalArgumentException && ex.message == WRONG_PASSWORD_MESSAGE
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
  private const val STANDARD_SALT = "dMD15Gaij8FfwQ7n"

  private const val WRONG_PASSWORD_MESSAGE = "Wrong password."
}
