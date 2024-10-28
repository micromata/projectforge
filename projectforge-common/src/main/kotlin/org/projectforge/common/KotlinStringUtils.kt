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

import java.util.*

/**
 * Abbreviates a string to a maximum length.
 * If the string is longer than the maximum length, it is abbreviated to the maximum length minus 3 characters and "..." is appended.<br>
 * If the string is shorter than the maximum length, it is returned unchanged.<br>
 * Example: "Hello, World!".abbreviate(10) returns "Hello, Wo...".<br>
 * @param maxLength The maximum length of the abbreviated string.
 * @return The abbreviated string.
 */
fun String?.abbreviate(maxLength: Int): String {
    return if (this == null) {
        ""
    } else if (this.length > maxLength) {
        this.take(maxLength - 3) + "..."
    } else {
        this
    }
}

/**
 * Capitalizes the first character of a string.
 * If the first character is a lower case letter, it is replaced by the corresponding upper case letter.
 * If the first character is not a lower case letter, the string is returned unchanged.<br>
 * Example: "hello, World!".capitalize() returns "Hello, World!".<br>
 * @return The capitalized string.
 */
fun String.capitalize(trimValues: Boolean = false): String {
    val str = if (trimValues) this.trim() else this
    return str.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

/**
 * Compares two strings for equality.
 * If both strings are null, they are considered equal.
 * If one string is null and the other is not, they are considered not equal.
 * If both strings are not null, they are compared for equality using the equals method.<br>
 * Example: "Hello, World!".isEqualsTo("Hello, World!") returns true.<br>
 * @param other The string to compare to.
 * @param trimValues If true, the strings are trimmed before comparison.
 * @return true if the strings are equal, false otherwise.
 */
fun String?.isEqualsTo(other: String?, trimValues: Boolean = false): Boolean {
    return if (trimValues) {
        (this?.trim() ?: "") == (other?.trim() ?: "")
    } else {
        (this ?: "") == (other ?: "")
    }
}
