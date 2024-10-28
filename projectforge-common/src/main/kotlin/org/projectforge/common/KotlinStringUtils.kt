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

