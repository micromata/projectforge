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

package org.projectforge.framework.utils

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.util.*

object NumberFormatter {
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

    /**
     * Returns the given integer value as String representation (scale = 2).
     *
     * @param value The integer value to convert.
     */
    @JvmStatic
    @JvmOverloads
    fun formatCurrency(value: Number?): String {
        return internalFormat(value, 2) ?: ""
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

    private fun internalFormat(value: Number?, scale: Int? = null, locale: Locale = ThreadLocalUserContext.getLocale()): String? {
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
