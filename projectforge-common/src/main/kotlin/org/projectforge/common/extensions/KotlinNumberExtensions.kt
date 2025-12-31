/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
import java.math.RoundingMode
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
        format.roundingMode = RoundingMode.HALF_UP
    }
    return when (this) {
        is BigDecimal, is BigInteger, is Double, is Float, is Int, is Long -> {
            format.format(this)
        }

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
            else -> throw IllegalArgumentException("Unsupported number type: $this")
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

/**
 * Formats a number given in millis to a string in the format HH:mm:ss.SSS.
 */
fun Number?.formatMillis(showMillis: Boolean = true, showSeconds: Boolean = true): String {
    this ?: return ""
    val millis = this.toLong()
    val hours = millis / (1000 * 60 * 60)
    var minutes = (millis / (1000 * 60)) % 60
    var seconds = (millis / 1000) % 60
    val milliseconds = millis % 1000
    if (!showMillis && milliseconds >= 500) {
        ++seconds // Round up.
    }
    if (!showSeconds && seconds >= 30) {
        ++minutes // Round up.
    }
    return when {
        hours > 0 || !showSeconds -> if (showMillis && showSeconds) {
            String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds)
        } else if (showSeconds) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", hours, minutes)
        }
        // minutes > 0 -> String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
        // else -> String.format("%02d.%03d", seconds, milliseconds)
        else -> if (showMillis && showSeconds) {
            String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
        } else if (showSeconds) {
            String.format("%02d:%02d", minutes, seconds)
        } else {
            String.format("%02d", minutes)
        }
    }
}

fun Number.asBigDecimal(): BigDecimal {
    return when (this) {
        is BigDecimal -> this
        is Long -> this.toBigDecimal()
        is Int -> this.toBigDecimal()
        is Short -> BigDecimal(this.toInt())
        is Float -> this.toBigDecimal()
        is Double -> this.toBigDecimal()
        else -> BigDecimal(this.toDouble())
    }
}

fun Number?.isZeroOrNull(): Boolean {
    this ?: return true
    return when (this) {
        is Int -> this == 0
        is Long -> this == 0L
        is Float -> this.absoluteValue < 0.000001
        is Double -> this.absoluteValue < 0.000001
        is BigDecimal -> this.compareTo(BigDecimal.ZERO) == 0
        is BigInteger -> this == BigInteger.ZERO
        is Byte -> this.toInt() == 0
        is Short -> this.toInt() == 0
        else -> throw IllegalArgumentException("Unsupported number type: $this")
    }
}

private val MILLIS_PER_HOUR = BigDecimal(1000 * 60 * 60)

private val MILLIS_PER_24H = BigDecimal(1000 * 60 * 60 * 24)

fun Number.millisAsHours(): BigDecimal {
    return this.asBigDecimal().divide(MILLIS_PER_HOUR, 2, RoundingMode.HALF_UP)
}

/**
 * Returns the given number as a fraction of 24 hours. This format is useful for Excel.
 * @return The fraction of 24 hours.
 */
fun Number.millisAsFractionOf24h(): BigDecimal {
    return this.asBigDecimal().divide(MILLIS_PER_24H, 8, RoundingMode.HALF_UP)
}
