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

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal

/**
 * Gets the given [Temporal] as an ISO string (with time zone, if given).
 */
fun Temporal?.isoString(): String {
    this ?: return ""
    return when (this) {
        is java.time.ZonedDateTime -> this.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
        is java.time.LocalDateTime -> this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        is java.time.LocalDate -> this.format(DateTimeFormatter.ISO_LOCAL_DATE)
        is java.time.OffsetDateTime -> this.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        else -> throw IllegalArgumentException("Unsupported Temporal type: ${this::class}")
    }
}

/**
 * Gets the given [Temporal] as an ISO string in UTC time zone, if time zone is given.
 */
fun Temporal?.isoUTCString(): String {
    this ?: return ""
    return when (this) {
        is java.time.ZonedDateTime -> this.withZoneSameInstant(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_ZONED_DATE_TIME)

        is java.time.LocalDateTime -> this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        is java.time.LocalDate -> this.format(DateTimeFormatter.ISO_LOCAL_DATE)
        is java.time.OffsetDateTime -> this.atZoneSameInstant(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        else -> throw IllegalArgumentException("Unsupported Temporal type: ${this::class}")
    }
}
