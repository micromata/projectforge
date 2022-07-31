/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Some helper methods ...
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object StringHelper {

  /**
   * Usage: final StringBuffer buf = new StringBuffer();<br></br>
   * boolean first = true;<br></br>
   * if (...) {<br></br>
   * first = StringHelper.append(buf, first, "Hurzel", ", ");<br></br>
   * <br></br>
   * first = StringBuffer.append(buf, first, myString, ", ");<br></br>
   *
   * @param buf
   * @param first
   * @param str       String to append. If null, nothing will be done and first will be returned.
   * @param delimiter
   * @return true if str is not empty and appended to buffer, otherwise first will be returned.
   */
  @JvmStatic
  fun append(buf: StringBuffer, first: Boolean, str: String?, delimiter: String?): Boolean {
    if (StringUtils.isEmpty(str)) {
      return first
    }
    if (!first) {
      buf.append(delimiter)
    }
    buf.append(str)
    return false
  }

  /**
   * Usage: final StringBuffer buf = new StringBuffer();<br></br>
   * boolean first = true;<br></br>
   * if (...) {<br></br>
   * first = StringHelper.append(buf, first, "Hurzel", ", ");<br></br>
   * <br></br>
   * first = StringBuffer.append(buf, first, myString, ", ");<br></br>
   *
   * @param sb
   * @param first
   * @param str       String to append. If null, nothing will be done and first will be returned.
   * @param delimiter
   * @return true if str is not empty and appended to buffer, otherwise first will be returned.
   */
  @JvmStatic
  fun append(sb: StringBuilder, first: Boolean, str: String?, delimiter: String?): Boolean {
    if (StringUtils.isEmpty(str)) {
      return first
    }
    if (!first) {
      sb.append(delimiter)
    }
    sb.append(str)
    return false
  }

  @JvmStatic
  fun compareTo(s1: String?, s2: String?): Int {
    if (s1 == null) {
      return if (s2 == null) {
        0
      } else {
        -1
      }
    }
    return if (s2 == null) {
      +1
    } else s1.compareTo(s2)
  }

  @JvmStatic
  fun isIn(string: String?, vararg fields: String): Boolean {
    if (string.isNullOrEmpty()) {
      return false
    }
    for (field in fields) {
      if (string == field) {
        return true
      }
    }
    return false
  }

  /**
   * Nullpointer save version of String.endsWith.
   *
   * @return True, if the given string ends with one of the given suffixes, otherwise false.
   * @see String.endsWith
   */
  @JvmStatic
  fun endsWith(str: String?, vararg suffixes: String?): Boolean {
    if (str == null || suffixes.isEmpty()) {
      return false
    }
    for (suffix in suffixes) {
      if (str.endsWith(suffix!!)) {
        return true
      }
    }
    return false
  }

  /**
   * Nullpointer save version of String.startsWith.
   *
   * @return True, if the given string starts with one of the given prefixes, otherwise false.
   * @see String.startsWith
   */
  @JvmStatic
  fun startsWith(str: String?, vararg prefixes: String?): Boolean {
    if (str == null || prefixes.isEmpty()) {
      return false
    }
    for (prefix in prefixes) {
      if (str.startsWith(prefix!!)) {
        return true
      }
    }
    return false
  }

  /**
   * For example ["Micromata", "IT-Services", "Computer"] -> "Computer, IT-Services, Micromata".
   *
   * @param list      List of input strings.
   * @param delimiter The delimiter of the single string in output string.
   * @param sort      If true, the given list will be first sorted.
   * @return
   */
  @JvmStatic
  fun listToString(list: MutableList<String>, delimiter: String?, sort: Boolean): String {
    if (sort) {
      list.sort()
    }
    return colToString(list, delimiter)
  }

  /**
   * For example ["Micromata", "IT-Services", "Computer"] -> "Computer, IT-Services, Micromata".
   *
   * @param list      List of input strings.
   * @param delimiter The delimiter of the single string in output string.
   * @param sort      If true, the given list will be first sorted.
   * @return
   */
  fun colToString(col: Collection<String>, delimiter: String?): String {
    val buf = StringBuffer()
    var first = true
    for (item in col) {
      first = append(buf, first, item, delimiter)
    }
    return buf.toString()
  }

  /**
   * For example ["Micromata", "IT-Services", "Computer"] -> "Computer, IT-Services, Micromata".
   *
   * @param list      List of input strings.
   * @param delimiter The delimiter of the single string in output string.
   * @param sort      If true, the given list will be first sorted.
   * @return
   */
  @JvmStatic
  fun objectColToString(col: Collection<*>, delimiter: String?): String {
    val buf = StringBuffer()
    var first = true
    for (item in col) {
      val str = item?.toString() ?: ""
      first = append(buf, first, str, delimiter)
    }
    return buf.toString()
  }

  /**
   * @param delimiter
   * @param strings
   * @see .listToString
   */
  @JvmStatic
  fun listToString(delimiter: String?, vararg strings: String?): String? {
    val buf = StringBuffer()
    return listToString(buf, delimiter, *strings)
  }

  /**
   * @param delimiter
   * @param strings
   * @see .listToString
   */
  @JvmStatic
  fun listToString(buf: StringBuffer, delimiter: String?, vararg strings: String?): String? {
    if (strings.isEmpty()) {
      return ""
    }
    var first = true
    for (s in strings) {
      if (s == null || s.length == 0) {
        continue
      }
      first = append(buf, first, s, delimiter)
    }
    return buf.toString()
  }

  /**
   * @param delimiter
   * @param strings
   * @see .listToString
   */
  @JvmStatic
  fun listToString(delimiter: String?, vararg oa: Any?): String? {
    if (oa.isEmpty()) {
      return ""
    }
    val buf = StringBuilder()
    var first = true
    for (o in oa) {
      if (o == null) {
        continue
      }
      val s = o.toString()
      if (s.isEmpty()) {
        continue
      }
      first = append(buf, first, s, delimiter)
    }
    return buf.toString()
  }

  /**
   * @param delimiter
   * @param strings
   * @see .listToString
   */
  @JvmStatic
  fun doublesToString(delimiter: String?, vararg oa: Double?): String {
    if (oa.isEmpty()) {
      return ""
    }
    val buf = StringBuffer()
    var first = true
    for (o in oa) {
      if (o == null) {
        continue
      }
      val s = o.toString()
      if (s.isEmpty()) {
        continue
      }
      first = append(buf, first, s, delimiter)
    }
    return buf.toString()
  }

  /**
   * @param delimiter
   * @param prefix    will be prepended before every string.
   * @param suffix    will be appended to every string.
   * @param strings
   * @return
   * @see .listToString
   */
  @JvmStatic
  fun listToExpressions(
    delimiter: String?, prefix: String?, suffix: String?,
    vararg strings: String?
  ): String {
    val buf = StringBuffer()
    var first = true
    for (s in strings) {
      append(buf, first, prefix, delimiter)
      if (first) first = false
      buf.append(s).append(suffix)
    }
    return buf.toString()
  }

  @JvmStatic
  fun sortAndUnique(array: Array<String>?): Array<String>? {
    if (array == null || array.size <= 1) {
      return array
    }
    val set: MutableSet<String> = TreeSet()
    set.addAll(Arrays.asList(*array))
    return set.toTypedArray()
  }

  fun dateToSearchString(date: Date?): String {
    return date?.toString() ?: ""
  }

  /**
   * 0 -&gt; "00", 1 -&gt; "01", ..., 9 -&gt; "09", 10 -&gt; "10", 100 -&gt; "100" etc. Uses StringUtils.leftPad(str, 2,
   * '0');
   *
   * @param value
   * @return
   * @see StringUtils.leftPad
   */
  @JvmStatic
  fun format2DigitNumber(value: Int): String {
    return StringUtils.leftPad(value.toString(), 2, '0')
  }

  /**
   * 0 -&gt; "000", 1 -&gt; "001", ..., 9 -&gt; "009", 10 -&gt; "010", 100 -&gt; "0100", 1000 -&gt; "1000" etc. Uses
   * StringUtils.leftPad(str, 2, '0');
   *
   * @param value
   * @return
   * @see StringUtils.leftPad
   */
  @JvmStatic
  fun format3DigitNumber(value: Int): String {
    return StringUtils.leftPad(value.toString(), 3, '0')
  }

  /**
   * Remove all non digits from the given string and return the result. If null is given, "" is returned.
   *
   * @param str
   * @return
   */
  @JvmStatic
  fun removeNonDigits(str: String?): String {
    if (str == null) {
      return ""
    }
    val buf = StringBuilder()
    for (i in 0 until str.length) {
      val ch = str[i]
      if (ch >= '0' && ch <= '9') {
        buf.append(ch)
      }
    }
    return buf.toString()
  }

  @JvmStatic
  fun removeNonDigitsAndNonASCIILetters(str: String?): String {
    if (str == null) {
      return ""
    }
    val buf = StringBuilder()
    for (i in 0 until str.length) {
      val ch = str[i]
      if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9') {
        buf.append(ch)
      }
    }
    return buf.toString()
  }

  /**
   * Formats string array, each string with max with and separated by separator with a total width. See StringHelperTest
   * for documentation.
   *
   * @param strings
   * @param maxWidth
   * @param maxTotalLength
   * @param separator
   * @return
   */
  @JvmStatic
  fun abbreviate(
    strings: Array<String>, maxWidth: IntArray, maxTotalLength: Int,
    separator: String
  ): String {
    Validate.notNull(strings)
    Validate.notNull(maxWidth)
    Validate.isTrue(strings.size == maxWidth.size)
    var rest = maxTotalLength
    val buf = StringBuilder()
    val separatorLength = separator.length
    var output = false
    for (i in strings.indices) {
      val str = strings[i]
      if (StringUtils.isBlank(str)) {
        continue
      }
      if (output) {
        buf.append(separator)
        rest -= separatorLength
      } else {
        output = true
      }
      if (rest <= 0) {
        break
      }
      val max = Math.min(maxWidth[i], rest)
      buf.append(StringUtils.abbreviate(str, max))
      rest -= Math.min(str.length, max)
    }
    return buf.toString()
  }

  fun removeWhitespaces(value: String?): String? {
    if (value == null) {
      return null
    }
    val buf = StringBuilder()
    for (i in 0 until value.length) {
      val ch = value[i]
      if (!Character.isWhitespace(ch)) {
        buf.append(ch)
      }
    }
    return buf.toString()
  }

  /**
   * Examples
   *
   *  * null -&gt; ""
   *  * "Hello", "Hello ProjectForge", "Hello kitty" -&gt; "Hello"
   *  * "Hello", null, "Hello kitty" -&gt; ""
   *
   *
   * @param strs
   * @return The wild card string that matches all given strings. If no matching found (null or empty strings given)
   * then an empty string is returned.
   */
  @JvmStatic
  fun getWildcardString(vararg strs: String): String {
    if (strs == null) {
      return ""
    }
    var maxLength = Int.MAX_VALUE
    for (str in strs) {
      if (str == null) {
        return ""
      } else if (str.length < maxLength) {
        maxLength = str.length
      }
    }
    for (i in 0 until maxLength) {
      val ch = strs[0][i]
      for (str in strs) {
        if (ch != str[i]) {
          return if (i > 0) {
            strs[0].substring(0, i)
          } else {
            ""
          }
        }
      }
    }
    return strs[0].substring(0, maxLength)
  }
  /**
   * Valid characters are ''+'' as first char, ''-'', ''/'' and spaces.
   * @param countryCodeRequired If true, The leading country code is mandatory, e. g.: +49 561 316793-0
   */
  /**
   * Valid characters are ''+'' as first char, ''-'', ''/'' and spaces. The leading country code is mandatory, e. g.:
   * +49 561 316793-0
   */
  @JvmOverloads
  @JvmStatic
  fun checkPhoneNumberFormat(value: String, countryCodeRequired: Boolean = true): Boolean {
    if (StringUtils.isBlank(value)) {
      return true
    }
    if (!StringUtils.containsOnly(value, "+1234567890 -/") || value.length < 2 || value.indexOf(
        '+',
        1
      ) != -1
    ) {
      return false
    }
    if (countryCodeRequired &&
      (!value.startsWith("+") || !Character.isDigit(value[1]))
    ) {
      return false
    }
    val str = removeWhitespaces(value)
    return if (str!!.startsWith("+49") && str[3] == '0') {
      // +49 0561 123456 is not allowed
      false
    } else true
  }

  /**
   * Hides the last numberOfCharacters chars of the given string by replacing them with ch.<br></br>
   * Examples:
   *
   *  * hideStringEnding("0170 1234568", 'x', 3) -> "0170 12345xxx"
   *  * StringHelper.hideStringEnding("01", 'x', 3) -> "xx"
   *  * StringHelper.hideStringEnding(null, 'x', 3) -> "null
   *
   *
   * @param str                Original string.
   * @param ch                 Replace character.
   * @param numberOfCharacters
   * @return
   */
  @JvmStatic
  fun hideStringEnding(str: String?, ch: Char, numberOfCharacters: Int): String? {
    if (str == null) {
      return null
    }
    val buf = StringBuffer()
    val toPos = str.length - numberOfCharacters
    for (i in 0 until str.length) {
      if (i < toPos) {
        buf.append(str[i])
      } else {
        buf.append(ch)
      }
    }
    return buf.toString()
  }

  @JvmStatic
  fun splitToIntegers(str: String?, delim: String?): Array<Int?>? {
    if (str == null) {
      return null
    }
    val tokenizer = StringTokenizer(str, delim)
    val result = arrayOfNulls<Int>(tokenizer.countTokens())
    var i = 0
    while (tokenizer.hasMoreTokens()) {
      val token = tokenizer.nextToken()
      val value = IntegerHelper.parseInteger(token)
      result[i++] = value
    }
    return result
  }

  @JvmStatic
  fun splitToInts(str: String?, delim: String?): IntArray {
    if (str == null) {
      return IntArray(0)
    }
    val tokenizer = StringTokenizer(str, delim)
    val result = IntArray(tokenizer.countTokens())
    var i = 0
    while (tokenizer.hasMoreTokens()) {
      val token = tokenizer.nextToken()
      val value = IntegerHelper.parseInteger(token)
      result[i++] = value ?: 0
    }
    return result
  }

  /**
   * @param str
   * @param delim
   * @param ignoreEmptyItems If true then "1, ,2" returns [1,0,2], otherwise [1,2] is returned.
   * @return
   */
  @JvmStatic
  fun splitToInts(str: String?, delim: String?, ignoreEmptyItems: Boolean): IntArray {
    if (ignoreEmptyItems) {
      return splitToInts(str, delim)
    }
    if (str == null) {
      return IntArray(0)
    }
    val tokenizer = StringTokenizer(str, delim)
    val list: MutableList<Int> = ArrayList(tokenizer.countTokens())
    while (tokenizer.hasMoreTokens()) {
      val token = tokenizer.nextToken()
      val value = IntegerHelper.parseInteger(token)
      if (value != null) {
        list.add(value)
      }
    }
    val result = IntArray(list.size)
    var i = 0
    for (number in list) {
      result[i++] = number
    }
    return result
  }

  /**
   * Trims all string of the resulting array.
   *
   * @param str
   * @param separatorChars
   * @return
   * @see StringUtils.split
   */
  @JvmStatic
  fun splitAndTrim(str: String?, separatorChars: String?): Array<String?>? {
    if (str == null) {
      return null
    }
    val sa = StringUtils.split(str, separatorChars)
    val result = arrayOfNulls<String>(sa.size)
    for (i in sa.indices) {
      result[i] = sa[i].trim { it <= ' ' }
    }
    return result
  }

  /**
   * Calls ![.isBlank].
   *
   * @param strs
   * @return true if one of the given strings is not blank, otherwise false.
   * @see StringUtils.isNotBlank
   */
  @JvmStatic
  fun isNotBlank(vararg strs: String?): Boolean {
    return !isBlank(*strs)
  }

  /**
   * Calls [StringUtils.isBlank] for each of the given strings.
   *
   * @param strs
   * @return true if one of the given strings is not blank, otherwise false.
   * @see .isNotBlank
   */
  @JvmStatic
  fun isBlank(vararg strs: String?): Boolean {
    if (strs.isEmpty()) {
      return true
    }
    for (s in strs) {
      if (StringUtils.isNotBlank(s)) {
        return false
      }
    }
    return true
  }

  private val HEX_CHARS = "0123456789abcdef".toCharArray()

  @JvmStatic
  fun asHex(buf: ByteArray?): String {
    if (buf == null || buf.size == 0) {
      return ""
    }
    val chars = CharArray(2 * buf.size)
    for (i in buf.indices) {
      chars[2 * i] = HEX_CHARS[buf[i].toInt() and 0xF0 ushr 4]
      chars[2 * i + 1] = HEX_CHARS[buf[i].toInt() and 0x0F]
    }
    return String(chars)
  }

  /**
   * @param keyValues e. g. "name=Horst,street=Baker street"
   * @param delimiter
   * @return
   */
  @JvmStatic
  fun getKeyValues(keyValues: String?, delimiter: String?): Map<String, String>? {
    val map: MutableMap<String, String> = HashMap()
    if (keyValues == null) {
      return map
    }
    val tokenizer = StringTokenizer(keyValues, delimiter)
    while (tokenizer.hasMoreTokens()) {
      val token = tokenizer.nextToken()
      val pos = token.indexOf('=')
      if (pos <= 0) {
        log.error(
          "Bad request, decrypted parameter is not from type key=value: $token. String was: $keyValues"
        )
        return null
      }
      val key = token.substring(0, pos)
      var value: String
      if (pos == token.length - 1) {
        continue
      }
      value = token.substring(pos + 1)
      map[key] = value
    }
    return map
  }
  /**
   * @param str
   * @param toLowerCase
   * @return Normalized string or "" if str is null.
   * @see StringUtils.normalizeSpace
   * @see StringUtils.stripAccents
   */
  /**
   * @param str
   * @return Normalized string or "" if str is null.
   * @see StringUtils.normalizeSpace
   * @see StringUtils.stripAccents
   */
  @JvmStatic
  @JvmOverloads
  fun normalize(str: String?, toLowerCase: Boolean = false): String {
    var str = str ?: return ""
    if (toLowerCase) {
      str = str.lowercase(Locale.getDefault())
    }
    return StringUtils.normalizeSpace(StringUtils.stripAccents(str))
  }
}
