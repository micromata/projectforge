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

package org.projectforge.common

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.util.*

/**
 * Formats a number for the user by using the locale of [ThreadLocalUserContext].
 * @param scale The number of digits after the decimal point.
 * @return The formatted number or an empty string if the number is null.
 */
fun Number?.formatForUser(scale: Int? = null): String {
    this ?: return ""
    return this.format(ThreadLocalUserContext.locale, scale)
}

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
 * Formats a number of bytes for the user by using the locale of [ThreadLocalUserContext].
 * @see formatBytes
 */
fun Number?.formatBytesForUser(): String {
    this ?: return ""
    return this.formatBytes(ThreadLocalUserContext.locale)
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
