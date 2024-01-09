/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.SecureRandom
import java.text.NumberFormat
import java.text.ParseException
import java.util.*

/**
 * Some helper methods ...
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object NumberHelper {
  const val ALLOWED_PHONE_NUMBER_CHARS = "+-/()."
  val TWENTY = BigDecimal(20)

  @JvmField
  val HUNDRED = BigDecimal(100)
  val THOUSAND = BigDecimal(1000)

  @JvmField
  val THREE_THOUSAND_SIX_HUNDRED = BigDecimal(3600)
  val MINUS_TWENTY = BigDecimal(-20)
  val MINUS_HUNDRED = BigDecimal(-100)

  @JvmField
  val BILLION = BigDecimal(1000000000)
  private val log = LoggerFactory.getLogger(NumberHelper::class.java)

  @JvmStatic
  fun getCurrencyFormat(locale: Locale?): NumberFormat {
    return getNumberFraction2Format(locale)
  }

  @JvmStatic
  fun getNumberFraction2Format(locale: Locale?): NumberFormat {
    val format = NumberFormat.getNumberInstance(locale)
    format.maximumFractionDigits = 2
    format.minimumFractionDigits = 2
    return format
  }

  @JvmStatic
  fun getNumberFractionFormat(locale: Locale?, fractionDigits: Int): NumberFormat {
    val format = NumberFormat.getNumberInstance(locale)
    format.maximumFractionDigits = fractionDigits
    format.minimumFractionDigits = fractionDigits
    return format
  }

  @JvmStatic
  fun formatBytes(bytes: Int?): String {
    return formatBytes(bytes?.toLong())
  }

  /**
   * Pretty output of bytes, "1023 bytes", "1.1 kb", "523 kb", "1.7 Mb", "143 Gb" etc.
   *
   * @param bytes
   * @return
   */
  @JvmStatic
  fun formatBytes(bytes: Long?): String {
    return FormatterUtils.formatBytes(bytes, ThreadLocalUserContext.locale!!)
  }

  /**
   * @param value
   * @return true, if value is not null and greater zero.
   */
  @JvmStatic
  fun greaterZero(value: Int?): Boolean {
    return value != null && value > 0
  }

  /**
   * @param value
   * @return true, if value is not null and greater zero.
   */
  @JvmStatic
  fun greaterZero(value: Long?): Boolean {
    return value != null && value.toInt() > 0
  }

  @JvmStatic
  fun isZeroOrNull(value: Int?): Boolean {
    return value == null || value == 0
  }

  @JvmStatic
  fun isGreaterZero(value: BigDecimal?): Boolean {
    return value != null && value > BigDecimal.ZERO
  }

  /**
   * @param value
   * @return true, if the given value is not null and not zero.
   */
  @JvmStatic
  fun isNotZero(value: Int?): Boolean {
    return !isZeroOrNull(value)
  }

  /**
   * Parses the given string as integer value.
   *
   * @param value The string representation of the integer value to parse.
   * @param logMessage If given (default) a warning will be logged if the given string isn't parsable.
   * @return Integer value or null if an empty string was given or a syntax error occurs.
   */
  @JvmStatic
  @JvmOverloads
  fun parseInteger(value: String?, logMessage: Boolean = true): Int? {
    var v = value ?: return null
    v = v.trim { it <= ' ' }
    if (v.isEmpty()) {
      return null
    }
    var result: Int? = null
    try {
      result = Integer.valueOf(v)
    } catch (ex: NumberFormatException) {
      if (logMessage == true) {
        log.warn("Can't parse integer: '$v'.")
      }
    }
    return result
  }

  /**
   * Parses the given string as short value.
   *
   * @param value The string representation of the short value to parse.
   * @return Short value or null if an empty string was given or a syntax error occurs.
   */
  @JvmStatic
  fun parseShort(value: String?): Short? {
    var v = value ?: return null
    v = v.trim { it <= ' ' }
    if (v.isEmpty()) {
      return null
    }
    var result: Short? = null
    try {
      result = v.toShort()
    } catch (ex: NumberFormatException) {
      log.debug(ex.message, ex)
    }
    return result
  }

  /**
   * Catches any NumberFormatException and returns 0, otherwise the long value represented by the given value is returned.
   */
  @JvmStatic
  fun parseLong(value: String?): Long? {
    var v = value ?: return null
    v = v.trim { it <= ' ' }
    if (v.isEmpty()) {
      return null
    }
    var result: Long? = null
    try {
      result = java.lang.Long.valueOf(v)
    } catch (ex: NumberFormatException) {
      log.debug(ex.message, ex)
    }
    return result
  }

  /**
   *
   */
  @JvmStatic
  fun parseBigDecimal(value: String?): BigDecimal? {
    var v = value ?: return null
    v = v.trim { it <= ' ' }
    if (v.isEmpty()) {
      return null
    }
    var result: BigDecimal? = null
    try {
      if (v.indexOf(',') > 0) { // Replace the german decimal character by '.':
        v = v.replace(',', '.')
      }
      result = BigDecimal(v)
    } catch (ex: NumberFormatException) {
      log.debug(ex.message, ex)
    }
    return result
  }

  /**
   *
   */
  @JvmStatic
  fun parseCurrency(value: String?, locale: Locale?): BigDecimal? {
    var v = value ?: return null
    v = v.trim { it <= ' ' }
    if (v.isEmpty()) {
      return null
    }
    val format = getCurrencyFormat(locale)
    var result: BigDecimal? = null
    try {
      val number = format.parse(v)
      if (number != null) {
        result = BigDecimal(number.toString())
        result = result.setScale(2, RoundingMode.HALF_UP)
      }
    } catch (ex: ParseException) {
      log.debug(ex.message, ex)
    }
    return result
  }

  /**
   * @param v1 null is supported.
   * @param v2 null is supported.
   * @return
   */
  @JvmStatic
  fun add(v1: BigDecimal?, v2: BigDecimal?): BigDecimal {
    return if (v1 == null) {
      v2 ?: BigDecimal.ZERO
    } else {
      if (v2 == null) {
        v1
      } else {
        v1.add(v2)
      }
    }
  }

  /**
   * Returns the given integer value as String representation.
   *
   * @param value The integer value to convert.
   * @return The String representation or empty String, if value is null.
   */
  @JvmStatic
  fun getAsString(value: Number?): String {
    return value?.toString() ?: ""
  }

  /**
   * Returns the given number value as String representation.
   *
   * @param value  The number value to convert.
   * @param format The format to use.
   * @return The String representation or empty String, if value is null.
   */
  @JvmStatic
  fun getAsString(value: Number?, format: NumberFormat): String {
    return if (value == null) {
      ""
    } else {
      format.format(value)
    }
  }

  /**
   * @see ThreadLocalUserContext.getLocale
   */
  @JvmStatic
  fun formatFraction2(value: Number?): String {
    val locale = ThreadLocalUserContext.locale!!
    val format = getNumberFraction2Format(locale)
    return format.format(value)
  }

  /**
   * Uses the default country phone prefix from the configuration.
   *
   * @see extractPhonenumber
   */
  @JvmStatic
  fun extractPhonenumber(str: String?): String? {
    return extractPhonenumber(str, defaultCountryPhonePrefix)
  }

  private val defaultCountryPhonePrefix: String
    get() {
      return TEST_COUNTRY_PREFIX_USAGE_IN_TESTCASES_ONLY
        ?: Configuration.instance.getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX) ?: "+1"
    }

  /**
   * "01234 5678" -> "+49 1234 5678", "0034 8888 88888" -> "+34 8888 88888"
   */
  @JvmStatic
  fun formatPhonenumber(str: String?): String? {
    str ?: return null
    if (str.startsWith("00")) {
      return if (str.length > 2) {
        "+${str.substring(2)}"
      } else {
        str
      }
    } else if (str.startsWith("0") && str.length > 1) {
      return "$defaultCountryPhonePrefix ${str.substring(1)}"
    }
    return str
  }

  /**
   * Extracts the phone number of the given string. All characters of the set "+-/()." and white spaces will be deleted and +## will be
   * replaced by 00##. Example: +49 561 / 316793-0 -> 00495613167930 <br></br>
   * Ignores any characters after the first occurence of ':' or any letter.
   *
   * @param str
   * @param countryPrefix If country prefix is given, for all numbers beginning with the country prefix the country prefix will be replaced
   * by 0. Example: ("+49 561 / 316793-0", "+49") -> 05613167930; ("+39 123456", "+49") -> 0039123456.
   * @return
   */
  @JvmStatic
  fun extractPhonenumber(str: String?, countryPrefix: String?): String? {
    var s = str ?: return null
    s = s.trim { it <= ' ' }
    s = s.replace(
      "\\p{C}".toRegex(),
      ""
    ) // Replace UTF controls chars, such as UTF-202C or UTF-202D (from Apple contacts app).
    val buf = StringBuilder()
    if (!countryPrefix.isNullOrEmpty() && s.startsWith(countryPrefix)) {
      buf.append('0')
      s = s.substring(countryPrefix.length)
    } else if (s.length > 3 && s[0] == '+' && Character.isDigit(s[1])) {
      buf.append("00")
      buf.append(s[1])
      if (Character.isDigit(s[2])) {
        buf.append(s[2])
        s = s.substring(3)
      } else {
        s = s.substring(2)
      }
    }
    s = s.replace("\\s+".toRegex(), "") // Remove whitespaces.
    s = s.replace("(0)", "") // Remove '(0)' in +49 (0) 123456789
    for (i in s.indices) {
      val ch = s[i]
      if (Character.isDigit(s[i])) {
        buf.append(ch)
      }
    }
    return buf.toString()
  }

  @JvmStatic
  fun matchesPhoneNumber(str: String?): Boolean {
    return str != null && str.matches("^\\+?[0-9/\\-()\\s]+$".toRegex()) && str.matches(".*\\d.*".toRegex())
  }

  /**
   * Compares two given BigDecimals. They are equal if the value is equal independent of the scale (5.70 is equals to 5.7 and null is equals
   * null, but null is not equals to 0).
   *
   * @param value1
   * @param value2
   * @return
   * @see BigDecimal.compareTo
   */
  @JvmStatic
  fun isEqual(value1: BigDecimal?, value2: BigDecimal?): Boolean {
    if (value1 == null) {
      return value2 == null
    }
    return if (value2 == null) {
      false
    } else value1.compareTo(value2) == 0
  }

  /**
   * @param value
   * @return true, if the given value is not null and not zero.
   */
  @JvmStatic
  fun isNotZero(value: BigDecimal?): Boolean {
    return !isZeroOrNull(value)
  }

  @JvmStatic
  fun isZeroOrNull(value: BigDecimal?): Boolean {
    return value == null || value.compareTo(BigDecimal.ZERO) == 0
  }

  /**
   * Compares two given Integers using compareTo method.
   *
   * @param value1
   * @param value
   * @return
   * @see Integer.compareTo
   */
  @JvmStatic
  fun isEqual(value1: Int?, value: Int?): Boolean {
    if (value1 == null) {
      return value == null
    }
    return if (value == null) {
      false
    } else value1.compareTo(value) == 0
  }

  /**
   * Splits string representation of the given number into digits. Examples:<br></br>
   * NumberHelper.splitToInts(11110511, 1, 3, 2, 2) = {1, 111, 5, 11}<br></br>
   * NumberHelper.splitToInts(10000511, 1, 3, 2, 2) = { 1, 0, 5, 11}<br></br>
   * NumberHelper.splitToInts(511, 1, 3, 2, 2) = { 0, 0, 5, 11}
   *
   * @param value
   * @param split
   * @return
   */
  @JvmStatic
  fun splitToInts(value: Number, vararg split: Int): IntArray {
    var numberOfDigits = 0
    for (n in split) {
      numberOfDigits += n
    }
    val str = StringUtils.leftPad(value.toInt().toString(), numberOfDigits, '0')
    val result = IntArray(split.size)
    var pos = 0
    var i = 0
    for (n in split) {
      result[i++] = parseInteger(str.substring(pos, pos + n))!!
      pos += n
    }
    return result
  }

  /**
   * If given string is an number (NumberUtils.isNumber(String)) then it will be converted to a plain string via BigDecimal.toPlainString().
   * Any exponent such as 1E7 will be avoided.
   *
   * @param str
   * @return Converted string if number, otherwise the origin string.
   * @see NumberUtils.isCreatable
   * @see NumberUtils.createBigDecimal
   * @see BigDecimal.toPlainString
   */
  @JvmStatic
  fun toPlainString(str: String): String {
    return if (NumberUtils.isCreatable(str)) {
      val bd = NumberUtils.createBigDecimal(str)
      bd.toPlainString()
    } else {
      str
    }
  }

  /**
   * Sets scale 0 for numbers greater 100, 1 for numbers greater 20 and 2 as default.
   *
   * @param number
   * @return
   */
  @JvmStatic
  fun setDefaultScale(number: BigDecimal?): BigDecimal? {
    if (number == null) {
      return null
    }
    if (number >= HUNDRED || number <= MINUS_HUNDRED) {
      return number.setScale(0, RoundingMode.HALF_UP)
    } else if (number >= TWENTY || number <= MINUS_TWENTY) {
      return number.setScale(1, RoundingMode.HALF_UP)
    }
    return number.setScale(2, RoundingMode.HALF_UP)
  }

  /**
   * Generates secure random String of the given length.
   *
   * @param length
   * @return Secure random string.
   */
  @JvmStatic
  fun getSecureRandomAlphanumeric(length: Int): String {
    return getSecureRandomString(ALPHA_NUMERICS_CHARSET, length)
  }

  private fun getSecureRandomString(usedChars: String, length: Int): String {
    val random = SecureRandom()
    val bytes = ByteArray(length)
    random.nextBytes(bytes)
    val sb = StringBuilder()
    val charsLength = usedChars.length
    for (i in 0 until length) {
      sb.append(usedChars[(bytes[i].toInt() and 0xFF) % charsLength])
    }
    return sb.toString()
    /*
            val charset = CharArray(length)
    for (i in 0 until length) {
        charset[i] = ALPHA_NUMERICS_CHARSET[(bytes[i].toInt() and 0xFF) % ALPHA_NUMERICS_CHARSET_LENGTH]
    }
    return String(charset)
     */
  }

  /**
   * Generates secure random String of the given length.
   *
   * @param length
   * @return Secure random string.
   */
  @JvmStatic
  fun getSecureRandomDigits(length: Int): String {
    val random = SecureRandom()
    val bytes = ByteArray(length)
    random.nextBytes(bytes)
    val sb = StringBuilder()
    for (i in 0 until length) {
      sb.append((bytes[i].toInt() and 0xFF) % 10)
    }
    return sb.toString()
  }

  @JvmStatic
  fun checkSecureRandomAlphanumeric(token: String?, minLength: Int): Boolean {
    if (token.isNullOrBlank() || token.length < minLength) {
      return false
    }
    token.forEach {
      if (!ALPHA_NUMERICS_CHARSET.contains(it)) {
        return false
      }
    }
    return true
  }

  /**
   * Generates secure random String of the given length. Doesn't user chars "lIO0".
   *
   * @param length
   * @return Secure random string.
   */
  @JvmStatic
  fun getSecureRandomReducedAlphanumeric(length: Int): String {
    return getSecureRandomString(REDUCED_ALPHA_NUMERICS_CHARSET, length)
  }

  @JvmStatic
  fun checkSecureRandomReducedAlphanumeric(token: String?, minLength: Int): Boolean {
    return checkUsedChars(usedChars = REDUCED_ALPHA_NUMERICS_CHARSET, token = token, minLength = minLength)
  }

  private fun checkUsedChars(usedChars: String, token: String?, minLength: Int): Boolean {
    if (token.isNullOrBlank() || token.length < minLength) {
      return false
    }
    token.forEach {
      if (!usedChars.contains(it)) {
        return false
      }
    }
    return true
  }

  /**
   * Generates secure random String of the given length. Doesn't user chars "lIO0".
   *
   * @param length
   * @return Secure random string.
   */
  @JvmStatic
  fun getSecureRandomReducedAlphanumericWithSpecialChars(length: Int): String {
    var result: String = getSecureRandomString(REDUCED_ALPHA_NUMERICS_CHARSET_WITH_SPECIAL_CHARS, length)
    for (i in 0..100) {
      if (result.any { SPECIAL_CHARS.contains(it) }) {
        return result
      }
      result = getSecureRandomString(REDUCED_ALPHA_NUMERICS_CHARSET_WITH_SPECIAL_CHARS, length)
    }
    // Giving up (should never occur):
    return if (length > 2)
      result.replaceRange(length - 2, length - 1, "!")
    else
      "!" + result.substring(1)
  }

  @JvmStatic
  fun checkSecureRandomReducedAlphanumericWithSpecialChars(token: String, minLength: Int): Boolean {
    return checkUsedChars(
      usedChars = REDUCED_ALPHA_NUMERICS_CHARSET_WITH_SPECIAL_CHARS,
      token = token,
      minLength = minLength
    ) &&
        token.any { SPECIAL_CHARS.contains(it) }
  }


  @JvmStatic
  fun isIn(value: Int, vararg numbers: Int): Boolean {
    numbers.forEach {
      if (value == it) {
        return true
      }
    }
    return false
  }

  /**
   * @param stripTrailingZeros For BigDecimal numbers, trailing zeros are stripped (default is false).
   */
  @JvmStatic
  @JvmOverloads
  fun toBigDecimal(number: Number?, stripTrailingZeros: Boolean = true): BigDecimal? {
    return FormatterUtils.toBigDecimal(number, stripTrailingZeros)
  }

  /**
   * @return value ensured inside the given range or null, if value is null.
   */
  @JvmStatic
  fun ensureRange(minVal: Int, maxVal: Int, value: Int?): Int? {
    value ?: return null
    return if (value < minVal) minVal
    else if (value > maxVal) maxVal
    else value
  }

  internal val ALPHA_NUMERICS_CHARSET = (('a'..'z') + ('A'..'Z') + ('0'..'9')).joinToString(separator = "")
  internal val REDUCED_ALPHA_NUMERICS_CHARSET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789"
  private val SPECIAL_CHARS = "*#+-.,:;?=()/&%$§!"
  internal val REDUCED_ALPHA_NUMERICS_CHARSET_WITH_SPECIAL_CHARS =
    "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789" + SPECIAL_CHARS

  var TEST_COUNTRY_PREFIX_USAGE_IN_TESTCASES_ONLY: String? = null
}
