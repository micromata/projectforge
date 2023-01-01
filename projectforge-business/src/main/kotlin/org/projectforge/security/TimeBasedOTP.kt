/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.security

import mu.KotlinLogging
import java.lang.reflect.UndeclaredThrowableException
import java.security.GeneralSecurityException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


private val log = KotlinLogging.logger {}

/**
 * RFC6238 implementation of time-based one-time-passwords, used also by common authenticators such as
 * Microsoft or Google authenticator.
 * https://datatracker.ietf.org/doc/html/rfc6238
 */
class TimeBasedOTP(
  /**
   * Default algo is "HmacSHA1", but also "HmacSHA256" and "HmacSHA512" is supported. Use "HmacSHA1" for usage
   * with Microsoft and Google authenticator.
   */
  private val hmacCrypto: String = "HmacSHA1",
  /**
   * Default number of digits of the OTP is 6 (used by Microsoft and Google authenticator).
   */
  private val numberOfDigits: Int = 6
) {
  /**
   * Gets the TOTP token for current time for the given secret key.
   * @param secretHexKey - secret credential key (HEX)
   * @return the OTP
   */
  fun getOTP(secretHexKey: String): String {
    return getOTP(getStep(), secretHexKey)
  }

  /**
   * Gets the TOTP token for current time and one step before for the given secret key and compares it
   * with the given otp.
   * @param secretHexKey - secret credential key (HEX)
   * @param otp - OTP to validate
   * @return valid?
   */
  fun validate(secretHexKey: String, otp: String): Boolean {
    return validate(getStep(), secretHexKey, otp)
  }

  internal fun validate(step: Long, secretHexKey: String, otp: String): Boolean {
    return getOTP(step, secretHexKey) == otp || getOTP(step - 1, secretHexKey) == otp
  }

  internal fun getOTP(step: Long, secretHexKey: String): String {
    // Get the HEX in a Byte[]
    val msg = hexStr2Bytes(asHex(step))
    val k = hexStr2Bytes(secretHexKey)
    val hash = hmacSHA(k, msg)

    // put selected bytes into result int
    val offset: Int = hash[hash.size - 1].toInt() and 0xf
    val binary: Int = hash[offset].toInt() and 0x7f shl 24 or
        (hash[offset + 1].toInt() and 0xff shl 16) or
        (hash[offset + 2].toInt() and 0xff shl 8) or
        (hash[offset + 3].toInt() and 0xff)
    val otp = binary % DIGITS_POWER[numberOfDigits]
    return otp.toString().padStart(numberOfDigits, '0')
  }


  /**
   * This method uses the JCE to provide the crypto algorithm. HMAC computes a Hashed Message Authentication Code with the crypto hash
   * algorithm as a parameter.
   *
   * @param keyBytes the bytes to use for the HMAC key
   * @param text the message or text to be authenticated.
   */
  private fun hmacSHA(keyBytes: ByteArray, text: ByteArray): ByteArray {
    return try {
      val hmac: Mac = Mac.getInstance(hmacCrypto)
      val macKey = SecretKeySpec(keyBytes, "RAW")
      hmac.init(macKey)
      hmac.doFinal(text)
    } catch (gse: GeneralSecurityException) {
      log.error("Can't create HmacSHA1")
      throw UndeclaredThrowableException(gse)
    }
  }

  companion object {
    private const val timeIntervalMillis = 30000 // 30 seconds

    private val DIGITS_POWER // 0 1  2   3    4     5      6       7        8
        = intArrayOf(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)

    /**
     * Get the 30 seconds step (epoch based).
     */
    internal fun getStep(timeInMillis: Long = System.currentTimeMillis()): Long {
      // 30 seconds step(ID of TOTP)
      return timeInMillis / timeIntervalMillis
    }

    /**
     * Get the 30 seconds step (epoch based) as hex string (16 chars length).
     */
    internal fun asHex(step: Long): String {
      // intervalNo as Hex string: "00000000033CB24E"
      return step.toString(16).uppercase().padStart(16, '0')
    }

    internal fun hexStr2Bytes(hex: String): ByteArray {
      return hex.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
    }
  }
}
