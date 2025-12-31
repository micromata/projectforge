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

import mu.KotlinLogging
import org.apache.commons.codec.binary.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.security.Key
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.Security
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger {}

/**
 * @author Wolfgang Jung (W.Jung@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object Crypt {
  private const val CRYPTO_ALGORITHM = "AES/ECB/PKCS5Padding"
  private var initialized = false

  /**
   * Encrypts the given str with AES. The password is first converted using SHA-256.
   *
   * @param password
   * @param data
   * @return The base64 encoded result (url safe).
   */
  @JvmStatic
  fun encrypt(password: String, data: String): String? {
    return try {
      encrypt(password, data.toByteArray(charset("UTF-8")))
    } catch (ex: Exception) {
      log.error(
        "Exception encountered while trying to encrypt with Algorithm 'AES' and the given password: "
            + ex.message, ex
      )
      null
    }
  }

  /**
   * Encrypts the given str with AES. The password is first converted using SHA-256.
   *
   * @param password
   * @param data
   * @return The base64 encoded result (url safe).
   */
  @JvmStatic
  fun encrypt(password: String, data: ByteArray?): String? {
    initialize()
    return try {
      // AES is sometimes not part of Java, therefore use bouncy castle provider:
      val cipher =
        Cipher.getInstance(CRYPTO_ALGORITHM)
      val keyValue = getPassword(password)
      val key: Key = SecretKeySpec(keyValue, "AES")
      cipher.init(Cipher.ENCRYPT_MODE, key)
      val encVal = cipher.doFinal(data)
      Base64.encodeBase64URLSafeString(encVal)
    } catch (ex: Exception) {
      log.error(
        "Exception encountered while trying to encrypt with Algorithm 'AES' and the given password: "
            + ex.message, ex
      )
      null
    }
  }

  /**
   * @param password
   * @param encryptedString
   * @return
   */
  @JvmStatic
  fun decrypt(password: String, encryptedString: String?): String? {
    return try {
      val bytes = decryptBytes(password, encryptedString) ?: return null
      String(bytes, StandardCharsets.UTF_8)
    } catch (ex: Exception) {
      log.error(
        "Exception encountered while trying to encrypt with Algorithm 'AES' and the given password: "
            + ex.message, ex
      )
      null
    }
  }

  /**
   * @param password
   * @param encryptedString
   * @return
   */
  @JvmStatic
  fun decryptBytes(password: String, encryptedString: String?): ByteArray? {
    initialize()
    return try {
      val cipher = Cipher.getInstance(CRYPTO_ALGORITHM)
      val keyValue = getPassword(password)
      val key: Key = SecretKeySpec(keyValue, "AES")
      cipher.init(Cipher.DECRYPT_MODE, key)
      val decordedValue = Base64.decodeBase64(encryptedString)
      cipher.doFinal(decordedValue)
    } catch (bpe: BadPaddingException) {
      log.warn(bpe.message)
      null
    } catch (ex: Exception) {
      log.error(
        "Exception encountered while trying to encrypt with Algorithm 'AES' and the given password: "
            + ex.message, ex
      )
      null
    }
  }

  private fun getPassword(password: String): ByteArray? {
    return try {
      val digester =
        MessageDigest.getInstance("MD5") // 128 bit. 256 bit (SHA-256) doesn't work on Java versions without required security policy.
      digester.update(password.toByteArray(charset("UTF-8")))
      digester.digest()
    } catch (ex: NoSuchAlgorithmException) {
      log.error(
        "Exception encountered while trying to create a MD5 password: " + ex.message,
        ex
      )
      null
    } catch (ex: UnsupportedEncodingException) {
      log.error(
        "Exception encountered while trying to get bytes in UTF-8: " + ex.message,
        ex
      )
      null
    }
  }

  private fun initialize() {
    synchronized(log) {
      if (!initialized) {
        Security.addProvider(BouncyCastleProvider())
        initialized = true
      }
    }
  }

  /**
   * Encrypts the given String via SHA crypt algorithm.
   *
   * @param s
   * @return
   */
  @JvmStatic
  fun digest(s: String): String {
    return encode(s, "SHA")
  }

  /**
   * Encrypts the given String via SHA crypt algorithm.
   *
   * @param s
   * @return
   */
  @JvmStatic
  fun digest(s: CharArray): String {
    return encode(s, "SHA")
  }

  @JvmStatic
  fun digest(s: String, alg: String): String {
    return encode(s, alg)
  }

  @JvmStatic
  fun check(pass: String, encoded: String): Boolean {
    val alg = encoded.substring(0, encoded.indexOf('{'))
    return encoded == encode(pass, alg)
  }

  private fun encode(s: String, alg: String): String {
    return encode(s.toCharArray(), alg)
  }

  private fun convertToByteArray(src: CharArray): ByteArray {
    val dest = ByteArray(src.size)

    for (i in src.indices) {
      dest[i] = src[i].code.toByte()
    }
    return dest
  }

  private fun encode(ca: CharArray, alg: String): String {
    return try {
      val md = MessageDigest.getInstance(alg)
      md.reset()
      md.update(convertToByteArray(ca))
      val d = md.digest()
      var ret = ""
      for (byte in d) {
        val hex = charArrayOf(
          '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
          'F'
        )
        var unsigned = byte.toInt()
        if (byte < 0) {
          unsigned = 256 + byte
        }
        val hi = hex[unsigned / 16]
        val lo = hex[unsigned % 16]
        ret = hi.toString() + "" + lo + ret
      }
      md.algorithm + '{' + ret + '}'
    } catch (ex: NoSuchAlgorithmException) {
      log.error(ex.toString())
      "NONE{xxxxxxx}"
    }
  }
}
