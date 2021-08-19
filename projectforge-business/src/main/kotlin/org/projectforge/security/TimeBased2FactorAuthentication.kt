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
class TimeBased2FactorAuthentication(private val hmacCrypto: String = "HmacSHA1", private val numberOfDigits: Int = 6) {
  val totp = TimeBasedOneTimePassword(hmacCrypto = hmacCrypto, numberOfDigits = numberOfDigits)

  fun generateSecretKey(): String {
    val random = SecureRandom()
    val bytes = ByteArray(20)
    random.nextBytes(bytes)
    val base32 = Base32()
    return base32.encodeToString(bytes)
  }

  fun getTOTPCode(secretKey: String): String {
    val base32 = Base32()
    val bytes = base32.decode(secretKey)
    val hexKey = Hex.encodeHexString(bytes)
    return totp.getOTP(hexKey)
  }

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
    val standard = TimeBased2FactorAuthentication(hmacCrypto = "HmacSHA1", numberOfDigits = 6)
  }
}
