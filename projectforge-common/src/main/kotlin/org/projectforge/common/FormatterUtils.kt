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

package org.projectforge.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object FormatterUtils {
  @JvmStatic
  @JvmOverloads
  fun formatBytes(bytes: Int?, locale: Locale = Locale.getDefault()): String {
    return formatBytes(bytes?.toLong(), locale)
  }

  /**
   * null -> '--',
   * '5' -> '5bytes',
   * '1024' -> '1KB', ...
   */
  @JvmStatic
  @JvmOverloads
  fun formatBytes(bytes: Long?, locale: Locale = Locale.getDefault()): String {
    bytes ?: return "--"
    if (bytes == 0L) return "0"
    if (bytes < KILO_BYTES) return format(bytes, locale, BigDecimal.ONE, "bytes")
    if (bytes < MEGA_BYTES) return format(bytes, locale, KB_BD, "KB")
    if (bytes < GIGA_BYTES) return format(bytes, locale, MB_BD, "MB")
    if (bytes < TERRA_BYTES) return format(bytes, locale, GB_BD, "GB")
    var no = BigDecimal(bytes).divide(TB_BD, 1, RoundingMode.HALF_UP)
    if (no.toLong() >= 100) {
      no = no.setScale(0, RoundingMode.HALF_UP)
    }
    return NumberFormat.getInstance(locale).format(no) + "TB"
  }

  fun format(number: Number?, locale: Locale = Locale.getDefault()): String {
    return NumberFormat.getInstance(locale).format(number)
  }

  /**
   * @param stripTrailingZeros For BigDecimal numbers, trailing zeros are stripped (default is false).
   */
  @JvmStatic
  @JvmOverloads
  fun toBigDecimal(number: Number?, stripTrailingZeros: Boolean = true): BigDecimal? {
    if (number == null) return null
    val result = when (number) {
      is BigDecimal -> number.stripTrailingZeros()
      is Double -> BigDecimal(number)
      is Float -> BigDecimal(number.toDouble())
      is Int -> BigDecimal(number)
      is Short -> BigDecimal(number.toInt())
      is Long -> BigDecimal(number)
      else -> BigDecimal("$number")
    }
    return if (stripTrailingZeros) result.stripTrailingZeros() else result
  }

  private fun format(bytes: Long, locale: Locale, unit: BigDecimal, unitString: String): String {
    var no = BigDecimal(bytes).divide(unit, 1, RoundingMode.HALF_UP)
    if (no.toLong() >= 100) {
      no = no.setScale(0, RoundingMode.HALF_UP)
    }
    return "${NumberFormat.getInstance(locale).format(no)}$unitString"
  }

  private const val KILO_BYTES = 1024L
  private val KB_BD = BigDecimal(KILO_BYTES)
  private const val MEGA_BYTES = KILO_BYTES * 1024
  private val MB_BD = BigDecimal(MEGA_BYTES)
  private const val GIGA_BYTES = MEGA_BYTES * 1024
  private val GB_BD = BigDecimal(GIGA_BYTES)
  private const val TERRA_BYTES = GIGA_BYTES * 1024
  private val TB_BD = BigDecimal(TERRA_BYTES)
}
