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

package org.projectforge.framework.utils

import org.projectforge.Constants
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.*

object NumberFormatter {
  /**
   * @param value Format this value.
   * @param pattern The format string for [DecimalFormat].
   * @param roundingMode [RoundingMode.HALF_UP] is default.
   * @return The formatted number or empty string if value is null.
   * @see ThreadLocalUserContext.getLocale
   */
  @JvmStatic
  @JvmOverloads
  fun format(value: Number?, pattern: String, roundingMode: RoundingMode = RoundingMode.HALF_UP): String {
    value ?: return ""
    return format(value, pattern, ThreadLocalUserContext.locale!!, roundingMode)
  }

  /**
   * @param value Format this value.
   * @param pattern The format string for [DecimalFormat].
   * @param locale The locale to use.
   * @param roundingMode [RoundingMode.HALF_UP] is default.
   * @return The formatted number or empty string if value is null.
   */
  @JvmStatic
  @JvmOverloads
  fun format(
    value: Number?,
    pattern: String,
    locale: Locale,
    roundingMode: RoundingMode = RoundingMode.HALF_UP
  ): String {
    value ?: return ""
    val df = DecimalFormat.getInstance(locale) as DecimalFormat
    df.applyPattern(pattern);
    df.roundingMode = roundingMode
    return df.format(value)
  }

  /**
   * Returns the given integer value as String representation.
   *
   * @param value The integer value to convert.
   * @param scale Scaling the output, null is default, meaning scale of object is used.
   * @return The String representation or empty/null String, if value is null.
   */
  @JvmStatic
  fun format(value: Number?, locale: Locale): String {
    return internalFormat(value, locale = locale) ?: ""
  }

  /**
   * Returns the given integer value as String representation.
   *
   * @param value The integer value to convert.
   * @param scale Scaling the output, null is default, meaning scale of object is used.
   * @return The String representation or empty/null String, if value is null.
   */
  @JvmStatic
  @JvmOverloads
  fun format(value: Number?, scale: Int? = null): String {
    return internalFormat(value, scale) ?: ""
  }

  @JvmStatic
  @JvmOverloads
  fun format(month: Month, locale: Locale? = ThreadLocalUserContext.locale): String {
    val useLocale = locale ?: Locale.ENGLISH
    return month.getDisplayName(TextStyle.FULL_STANDALONE, useLocale)
  }

  /**
   * Returns the given integer value as String representation (scale = 2).
   *
   * @param value The integer value to convert.
   */
  @JvmStatic
  @JvmOverloads
  fun formatCurrency(value: Number?, withCurrencySymbol: Boolean = false): String {
    val amount = internalFormat(value, 2) ?: return ""
    return if (!withCurrencySymbol || Constants.CURRENCY_SYMBOL.isNullOrBlank()) {
      amount
    } else {
      "$amount ${Constants.CURRENCY_SYMBOL}"
    }
  }

  /**
   * Returns the given integer value as String representation.
   *
   * @param value The integer value to convert.
   * @param defaultValue For null values this value will be returned (default is "").
   * @param scale Scaling the output, null is default, meaning scale of object is used.
   * @return The String representation or empty/null String, if value is null.
   */
  @JvmStatic
  @JvmOverloads
  fun formatNull(value: Number?, scale: Int? = null): String? {
    return internalFormat(value, scale)
  }

  private fun internalFormat(
    value: Number?,
    scale: Int? = null,
    locale: Locale = ThreadLocalUserContext.locale!!
  ): String? {
    if (value == null)
      return null
    val format = NumberFormat.getNumberInstance(locale)
    if (scale != null) {
      format.maximumFractionDigits = scale
      format.minimumFractionDigits = scale
    }
    return when (value) {
      is BigDecimal -> format.format(value)
      is BigInteger -> format.format(value)
      is Double -> format.format(value)
      is Float -> format.format(value)
      is Int -> format.format(value)
      is Long -> format.format(value)
      else -> format.format(value.toDouble())
    }
  }

  /**
   * Strips trailing zeros. Int values will not be multiplied with 100!
   * @param value e. g. 0.19
   * @return e. g. 19% or empty string if value is null.
   * @see BigDecimal.stripTrailingZeros
   */
  @JvmStatic
  fun formatPercent(value: Number?): String {
    return if (value == null) {
      ""
    } else {
      val bigDecimal = NumberHelper.toBigDecimal(value, true)!!
      "${format(bigDecimal.multiply(NumberHelper.HUNDRED))}%"
    }
  }
}
