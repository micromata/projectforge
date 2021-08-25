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

package org.projectforge.security

import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Hex
import java.net.URLEncoder
import java.security.SecureRandom

/**
 * RFC6238 implementation of time-based one-time-passwords, used also by common authenticators such as
 * Microsoft or Google authenticator.
 * https://datatracker.ietf.org/doc/html/rfc6238
 * https://medium.com/@ihorsokolyk/two-factor-authentication-with-java-and-google-authenticator-9d7ea15ffee6
 */
class TimeBased2FA(
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
  val totp = TimeBasedOneTimePassword(hmacCrypto = hmacCrypto, numberOfDigits = numberOfDigits)

  /**
   * Generates an OTP compatible secret key. Should be used for initial creation of a user's secret key or for
   * setting a new secret key (for reset e. g. if the Smartphone was lost etc.).
   */
  fun generateSecretKey(): String {
    val random = SecureRandom()
    val bytes = ByteArray(20)
    random.nextBytes(bytes)
    val base32 = Base32()
    return base32.encodeToString(bytes)
  }

  /**
   * Gets the TOTP token for current time and one step before for the given secret key and compares it
   * with the given otp.
   * @param secretKey - secret credential key (HEX)
   * @param otp - OTP to validate
   * @return valid?
   */
  fun validate(secretKey: String, otp: String): Boolean {
    return totp.validate(secretKey, otp)
  }

  /**
   * Gets the current time-based one-time-password for the given secret key as number (digits). Only for
   * test cases.
   */
  internal fun getTOTPCode(secretKey: String): String {
    val base32 = Base32()
    val bytes = base32.decode(secretKey)
    val hexKey = Hex.encodeHexString(bytes)
    return totp.getOTP(hexKey)
  }

  /**
   * Gets the url for the 2d barcode, scanned by Microsoft or Google authenticator.
   */
  fun getAuthenticatorUrl(secretKey: String, account: String, issuer: String): String {
    return "otpauth://totp/${encode("$issuer:$account")}?secret=${encode(secretKey)}&issuer=${encode(issuer)}"
  }

  private fun encode(str: String): String {
    return URLEncoder.encode(str, "UTF-8").replace("+", "%20")
  }

  companion object {
    /**
     * The standard setup, used by Microsoft and Google authenticator.
     */
    @JvmStatic
    val standard = TimeBased2FA(hmacCrypto = "HmacSHA1", numberOfDigits = 6)
  }
}
