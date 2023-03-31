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

package org.projectforge.framework.utils

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Some helper methods for parsing numbers
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object ValueParser {
  /**
   * Doesn't throw an exception, returns null instead.
   * @param patterns Optional number formats to test before "".toInt() is tried.
   */
  fun parseInt(number: String?, patterns: Iterable<String>): Int? {
    if (number.isNullOrBlank()) {
      return null
    }
    val value = number.trim()
    patterns.forEach { pattern ->
      try {
        val decimalFormat = getIntFormat(pattern)
        synchronized(decimalFormat) {
          return decimalFormat.parse(value) as Int
        }
      } catch (ex: Exception) {
        // Might occur.
      }
    }
    return try {
      value.toIntOrNull()
    } catch (ex: Exception) {
      // Might occur.
      null
    }
  }

  fun parseBigDecimal(str: String?, parseFormatList: Iterable<String>): BigDecimal? {
    if (str.isNullOrBlank()) {
      return null
    }
    val value = str.trim()
    // val germanStyle = str.indexOf('.') in 0 until str.indexOf(',') // #.###.###,##
    parseFormatList.forEach { pattern ->
      try {
        /*
        var usePattern = pattern
        val patternGermanStyle = pattern.indexOf('.') in 0 until pattern.indexOf(',') // #.###.###,##
        if (germanStyle != patternGermanStyle) {
          val sb = StringBuilder()
          pattern.forEach { ch ->
            if (ch == '.') {
              sb.append(',')
            } else if (ch == ',') {
              sb.append('.')
            } else {
              sb.append(ch)
            }
          }
          usePattern = sb.toString()
        }*/
        val decimalFormat = getBigDecimalFormat(pattern)
        synchronized(decimalFormat) {
          return decimalFormat.parse(value) as BigDecimal
        }
      } catch (ex: Exception) {
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

  fun isGermanStyle(str: String): Boolean {
    return matchStyle(str, '.', ',')
  }

  fun isEnglishStyle(str: String): Boolean {
    return matchStyle(str, ',', '.')
  }

  private fun matchStyle(str: String, thousandsSeparator: Char, decimalChar: Char): Boolean {
    val thousandsPos = str.indexOf(thousandsSeparator)
    val decimalPos = str.indexOf(decimalChar)
    if (thousandsPos >= 0 && decimalPos >= 0) {
      return decimalPos > thousandsPos
    }
    if (thousandsPos >= 0) {
      if (str.count { it == thousandsSeparator } > 1) { // 1.234.000
        return true
      }
      return thousandsPos == str.length - 4 // 1.000?
    } else {
      if (str.count { it == decimalChar } > 1) { // 1,234,000
        return false
      }
      return decimalPos != str.length - 4 // 1,000?
    }
  }

  private fun getBigDecimalFormat(pattern: String): DecimalFormat {
    return getFormat(bigDecimalFormatMap, pattern, false)
  }

  private fun getIntFormat(pattern: String): DecimalFormat {
    return getFormat(intFormatMap, pattern, true)
  }

  private fun getFormat(map: MutableMap<String, DecimalFormat>, pattern: String, integer: Boolean): DecimalFormat {
    synchronized(map) {
      val result = map[pattern]
      if (result != null) {
        return result
      }
    }
    val decimalFormat = buildDecimalFormat(pattern)
    if (integer) {
      decimalFormat.isParseIntegerOnly = true
    } else {
      decimalFormat.isParseBigDecimal = true
    }
    synchronized(map) {
      map[pattern] = decimalFormat
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

  private val bigDecimalFormatMap = mutableMapOf<String, DecimalFormat>()
  private val intFormatMap = mutableMapOf<String, DecimalFormat>()
}
