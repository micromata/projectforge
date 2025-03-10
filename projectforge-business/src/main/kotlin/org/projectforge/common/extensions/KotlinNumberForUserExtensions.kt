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

package org.projectforge.common.extensions

import org.projectforge.Constants
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import java.math.MathContext
import java.math.RoundingMode

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
 * Formats a number of bytes for the user by using the locale of [ThreadLocalUserContext].
 * @see formatBytes
 */
fun Number?.formatBytesForUser(): String {
    this ?: return ""
    return this.formatBytes(ThreadLocalUserContext.locale)
}

/**
 * Formats a number for the user by using the locale of [ThreadLocalUserContext].
 */
fun Number?.formatCurrency(withCurrencySymbol: Boolean = false, scale: Int = 2): String {
    val amount = this.formatForUser(scale = scale)
    return if (!withCurrencySymbol || Constants.CURRENCY_SYMBOL.isBlank()) {
        amount
    } else {
        "$amount ${Constants.CURRENCY_SYMBOL}"
    }
}

/**
 * Formats a number (is already percent value) for the user by using the locale of [ThreadLocalUserContext].
 */
fun Number?.formatPercent(withSymbol: Boolean = false, scale: Int = 0): String {
    this ?: return ""
    val amount = this.formatForUser(scale)
    return if (withSymbol) {
        "$amount %"
    } else {
        amount
    }
}

/**
 * Formats a fraction for the user by using the locale of [ThreadLocalUserContext].
 */
fun Number?.formatFractionAsPercent(withSymbol: Boolean = false): String {
    this ?: return ""
    val amount = this.asBigDecimal()
        .multiply(NumberHelper.HUNDRED, MathContext(0, RoundingMode.HALF_UP))
        .formatForUser(0)
    return if (withSymbol) {
        "$amount %"
    } else {
        amount
    }
}
