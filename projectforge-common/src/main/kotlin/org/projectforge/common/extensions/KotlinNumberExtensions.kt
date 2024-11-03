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

package org.projectforge.common.extensions

import org.projectforge.common.FormatterUtils
import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue

/**
 * Formats a number for the user by using the given locale.
 * @param locale The locale to use. If not given [Locale.getDefault] is used.
 * @param scale The number of digits after the decimal point.
 * @return The formatted number or an empty string if the number is null.
 */
fun Number?.format(locale: Locale? = null, scale: Int? = null): String {
    this ?: return ""
    val format = NumberFormat.getNumberInstance(locale ?: Locale.getDefault())
    if (scale != null) {
        format.maximumFractionDigits = scale
        format.minimumFractionDigits = scale
    }
    return when (this) {
        is BigDecimal -> format.format(this)
        is BigInteger -> format.format(this)
        is Double -> format.format(this)
        is Float -> format.format(this)
        is Int -> format.format(this)
        is Long -> format.format(this)
        else -> format.format(this.toDouble())
    }
}

/**
 * If not given, then ?? will be returned. If given, then the number will be formatted with leading zeros.
 * @param number If null, then "???" will be returned.
 * @return 2 digits.
 */
fun Number?.format2Digits(): String {
    return formatDigits(2)
}

/**
 * If not given, then ??? will be returned. If given, then the number will be formatted with leading zeros.
 * @param number If null, then "???" will be returned.
 * @return 3 digits.
 */
fun Number?.format3Digits(): String {
    return formatDigits(3)
}

fun Number?.formatDigits(digits: Int): String {
    this ?: return "?".repeat(digits)
    if (this.toDouble() < 0) {
        val absolute = when (this) {
            is Int -> this.absoluteValue
            is Long -> this.absoluteValue
            is Float -> this.absoluteValue
            is Double -> this.absoluteValue
            is BigDecimal -> this.abs()
            is BigInteger -> this.abs()
            is Byte -> this.toInt().absoluteValue
            is Short -> this.toInt().absoluteValue
            else -> throw IllegalArgumentException("Unsupported number type")
        }
        return "-${absolute.toString().padStart(digits - 1, '0')}"
    }
    return this.toString().padStart(digits, '0')
}

/**
 * Formats a number as bytes: 1,024 bytes are 1 KB, 1,048,576 bytes are 1 MB, 1,073,741,824 bytes are 1 GB, 1,099,511,627,776 bytes are 1 TB.
 * @param locale The locale to use. If not given [Locale.getDefault] is used.
 * @return The formatted number or an empty string if the number is null.
 * @see FormatterUtils.formatBytes
 */
fun Number?.formatBytes(locale: Locale? = null): String {
    this ?: return "--"
    return FormatterUtils.formatBytes(this.toLong(), locale ?: Locale.getDefault())
}

fun Number?.formatMillis(): String {
    this ?: return ""
    val millis = this.toLong()
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis / (1000 * 60)) % 60
    val seconds = (millis / 1000) % 60
    val milliseconds = millis % 1000

    return when {
        hours > 0 -> String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds)
        // minutes > 0 -> String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
        // else -> String.format("%02d.%03d", seconds, milliseconds)
        else -> String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
    }
}