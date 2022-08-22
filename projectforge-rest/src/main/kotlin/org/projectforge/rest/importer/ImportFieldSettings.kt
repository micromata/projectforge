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

package org.projectforge.rest.importer

import org.projectforge.common.DateFormatType
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTimeUtils
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class ImportFieldSettings(
  val property: String,
) {
  var label: String = property

  var type: Class<*>? = null

  private val regexMap = mutableMapOf<String, Regex?>()

  /**
   * List of aliases of head of cols to map to bean prop. Wildcard characters '?' and '*' are supported.
   */
  val aliasList = mutableListOf<String>()

  /**
   * For dates, date-times, integers and BigDecimals the format(s) to use while parsing max be defined here.
   */
  val parseFormatList = mutableListOf<String>()

  val hasDefinitions: Boolean
    get() = aliasList.isNotEmpty() || parseFormatList.isNotEmpty()

  /**
   * @param name is the name of the property or the column head of the data table matching any alias.
   * @return true if the name matches the property or any alias.
   */
  fun matches(name: String): Boolean {
    val str = name.trim()
    if (str.startsWith(property, ignoreCase = true)
      || str.startsWith(label, ignoreCase = true)
    ) {
      return true
    }
    return aliasList.any { getRegex(it)?.matches(str) == true }
  }

  /**
   * aliases and formats to use while parsing may define by a single string. Different aliases and formats are
   * separated by '|'. Format strings begin with ':'. If an alias should start with ':' use wildchar '?' instead.
   * Examples: Birthday|born*|:MM/dd/yyyy|:MM/dd/yy (2 aliases and 2 parse formats will be defined.
   */
  fun parseSettings(settings: String) {
    settings.split("|").forEach { part ->
      val str = part.trim()
      if (str.startsWith(":")) {
        parseFormatList.add(str.substring(1))
      } else if (str.isNotBlank()) {
        aliasList.add(part.trim())
      }
    }
  }

  /**
   * @param createExampleEntry If true and no definition (alias/format patterns) are given, an example for copy & paste will be returned. If
   * false (default) an empty settingsString will be returned.
   */
  fun getSettingsAsString(createExampleEntry: Boolean = false): String {
    val sb = StringBuilder()
    if (hasDefinitions) {
      if (aliasList.isNotEmpty()) {
        sb.append(aliasList.joinToString("|"))
        if (parseFormatList.isNotEmpty()) {
          sb.append("|")
        }
      }
      sb.append(parseFormatList.joinToString("|") { ":$it" })
    } else if (createExampleEntry) {
      sb.append(label)
      if (type == LocalDate::class.java) {
        sb.append("|:${DateFormats.getFormatString(DateFormatType.DATE)}")
        sb.append("|:${DateFormats.getFormatString(DateFormatType.DATE_SHORT)}")
      } else if (type == BigDecimal::class.java) {
        sb.append("|:#,##0.0#|:#0.0#")
      } else if (type == Integer::class.java || type == Int::class.java || type == Short::class.java) {
        sb.append("|:#,##0|:#0")
      }

    }
    return sb.toString()
  }

  fun parseLocalDate(str: String?): LocalDate? {
    if (str.isNullOrBlank()) {
      return null
    }
    val value = str.trim()
    parseFormatList.forEach { format ->
      try {
        val formatter = DateTimeFormatter.ofPattern(format)
        val date = LocalDate.parse(value, formatter)
        if (date != null) {
          return date
        }
      } catch (ex: Exception) {
        // Might occur.
      }
    }
    try {
      return LocalDate.parse(value)
    } catch (ex: Exception) {
      // Might occur.
      return null
    }
  }

  fun parseDate(str: String?): Date? {
    if (str.isNullOrBlank()) {
      return null
    }
    val value = str.trim()
    parseFormatList.forEach { format ->
      try {
        val formatter = DateTimeFormatter.ofPattern(format)
        val localDateTime = LocalDateTime.parse(value, formatter)
        val dateTime = PFDateTime.fromOrNull(localDateTime)
        if (dateTime != null) {
          return dateTime.utilDate
        }
      } catch (ex: Exception) {
        // Might occur.
      }
    }
    try {
      return PFDateTimeUtils.parse(value)?.utilDate
    } catch (ex: Exception) {
      // Might occur.
      return null
    }
  }

  fun parseInt(str: String?): Int? {
    if (str.isNullOrBlank()) {
      return null
    }
    val value = str.trim()
    parseFormatList.forEach { pattern ->
      try {
        val decimalFormat = getIntFormat(pattern)
        synchronized(decimalFormat) {
          return decimalFormat.parse(value) as Int
        }
      } catch (ex: Exception) {
        println(ex)
        // Might occur.
      }
    }
    try {
      return value.toIntOrNull()
    } catch (ex: Exception) {
      // Might occur.
      return null
    }
  }

  fun parseBoolean(str: String?): Boolean? {
    str ?: return null
    parseFormatList.forEach { pattern ->
      if (str.startsWith(pattern)) {
        return true
      }
    }
    return str.startsWith('y', ignoreCase = true) || str.startsWith('j', ignoreCase = true) ||
        str.startsWith('1') || str.startsWith("true", ignoreCase = true)
  }

  fun parseBigDecimal(str: String?): BigDecimal? {
    if (str.isNullOrBlank()) {
      return null
    }
    val value = str.trim()
    parseFormatList.forEach { pattern ->
      try {
        val decimalFormat = getBigDecimalFormat(pattern)
        synchronized(decimalFormat) {
          return decimalFormat.parse(value) as BigDecimal
        }
      } catch (ex: Exception) {
        println(ex)
        // Might occur.
      }
    }
    try {
      return BigDecimal(value)
    } catch (ex: Exception) {
      // Might occur.
      return null
    }
  }

  internal fun getBigDecimalFormat(pattern: String): DecimalFormat {
    synchronized(bigDecimalFormatMap) {
      val result = bigDecimalFormatMap[pattern]
      if (result != null) {
        return result
      }
    }
    val decimalFormat = buildDecimalFormat(pattern)
    decimalFormat.isParseBigDecimal = true
    synchronized(bigDecimalFormatMap) {
      bigDecimalFormatMap[pattern] = decimalFormat
    }
    return decimalFormat
  }

  internal fun getIntFormat(pattern: String): DecimalFormat {
    synchronized(intFormatMap) {
      val result = intFormatMap[pattern]
      if (result != null) {
        return result
      }
    }
    val decimalFormat = buildDecimalFormat(pattern)
    decimalFormat.isParseIntegerOnly = true
    synchronized(intFormatMap) {
      intFormatMap[pattern] = decimalFormat
    }
    return decimalFormat
  }

  private fun buildDecimalFormat(str: String): DecimalFormat {
    val pattern = str.trim()
    val symbols = DecimalFormatSymbols()
    val comaPosition = pattern.indexOf(',')
    val decimalPosition = pattern.indexOf('.')
    var javaPattern = pattern
    if (comaPosition > decimalPosition) { // Last char defines decimal separator
      symbols.decimalSeparator = ','
      symbols.groupingSeparator = '.'
      val sb = StringBuilder()
      // swap , and .
      pattern.forEach { ch ->
        if (ch == '.') {
          sb.append(',')
        } else if (ch == ',') {
          sb.append('.')
        } else {
          sb.append(ch)
        }
      }
      javaPattern = sb.toString()
    } else {
      symbols.decimalSeparator = '.'
      symbols.groupingSeparator = ','
    }
    return DecimalFormat(javaPattern, symbols)
  }

  private fun getRegex(userString: String): Regex? {
    synchronized(regexMap) {
      if (regexMap.containsKey(userString)) {
        return regexMap[userString]
      }
      val regex = createRegex(userString)
      regexMap[userString] = regex
      return regex
    }
  }

  companion object {
    private val bigDecimalFormatMap = mutableMapOf<String, DecimalFormat>()
    private val intFormatMap = mutableMapOf<String, DecimalFormat>()

    internal fun matches(headerString: String, regex: Regex): Boolean {
      return regex.matches(headerString.trim())
    }

    internal fun createRegex(userString: String): Regex {
      return createRegexString(userString).toRegex(RegexOption.IGNORE_CASE)
    }

    internal fun createRegexString(userString: String): String {
      val sb = StringBuilder()
      userString.forEach { ch ->
        if ("<([{\\^-=\$!|]})+.>".contains(ch)) { // Don't escape * and ?
          sb.append('\\')
        }
        sb.append(ch)
      }
      return sb.toString().replace("*", ".*").replace("?", ".")
    }
  }
}
