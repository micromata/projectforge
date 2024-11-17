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

package org.projectforge.framework.time

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.Temporal

object DateParser {
    /**
     * Parses a date string into a Temporal object.
     * The input string can be in one of the following formats:
     * - Epoch seconds (10 digits)
     * - Epoch milliseconds (13 digits)
     * - ISO 8601 date-time (e.g. 2024-12-31T12:34:56Z, 2024-12-31T12:34:56 +01:00, 2024-12-31T12:34:56.123Z)
     * - ISO 8601 date-time without seconds (e.g. 2024-12-31T12:34Z, 2024-12-31T12:34 +01:00)
     * - ISO 8601 date-time without seconds and with reduced precision (e.g. 20241231T123456Z, 20241231T123456 +01:00)
     * - ISO 8601 date-time without seconds and with reduced precision (e.g. 20241231T1234Z, 20241231T1234 +01:00)
     * - ISO 8601 date (e.g. 2024-12-31)
     * - ISO 8601 date with reduced precision (e.g. 20241231)
     * The input string is trimmed before parsing.
     * If the input string does not match any of the known formats, null is returned.
     * If a defaultZoneId is provided, the parsed Temporal object is converted to that zone.
     * If no defaultZoneId is provided, the zoneId from ThreadLocalUserContext is used.
     * @param input The date string to parse.
     * @param defaultZoneId The zoneId to convert the parsed Temporal object to.
     * @param parseLocalDateIfNoTimeOfDayGiven If true, local dates will be parsed, if no time of day is given.
     * @return The parsed Temporal object ([ZonedDateTime], [LocalDateTime], [LocalDate]) or null if the input string does not match any of the known formats.
     * @see ThreadLocalUserContext.zoneId
     */
    @JvmStatic
    @JvmOverloads
    fun parse(
        input: String,
        defaultZoneId: ZoneId? = null,
        parseLocalDateIfNoTimeOfDayGiven: Boolean = true,
    ): Temporal? {
        val str = input.trim()
        val instant = if (str.matches(epochSecondsRegex)) {
            Instant.ofEpochSecond(str.toLong())
        } else if (str.matches(epochMillisRegex)) {
            Instant.ofEpochMilli(str.toLong())
        } else {
            null
        }
        val zoneId = defaultZoneId ?: ThreadLocalUserContext.zoneId
        if (instant != null) {
            return instant.atZone(zoneId)
        }
        parseZonedDateTime(str)?.withZoneSameInstant(zoneId)?.let { return it }
        parseDateTime(str)?.let { return it }
        return if (parseLocalDateIfNoTimeOfDayGiven) {
            parseLocalDate(str)
        } else {
            null
        }
    }

    private fun parseZonedDateTime(input: String): ZonedDateTime? {
        val str = if (input.contains(' ')) {
            input.replace(' ', 'T')
        } else {
            input
        }
        for ((regex, formatter) in zonedDateTimeFormatters) {
            if (str.matches(regex)) {
                return try {
                    ZonedDateTime.parse(str, formatter)
                } catch (e: DateTimeParseException) {
                    null
                }
            }
        }
        return null
    }

    private fun parseDateTime(input: String): LocalDateTime? {
        val str = if (input.contains(' ')) {
            input.replace(' ', 'T')
        } else {
            input
        }
        for ((regex, formatter) in dateTimeFormatters) {
            if (str.matches(regex)) {
                return try {
                    LocalDateTime.parse(str, formatter)
                } catch (e: DateTimeParseException) {
                    null
                }
            }
        }
        return null
    }

    private fun parseLocalDate(str: String): LocalDate? {
        for ((regex, formatter) in localDateFormatters) {
            if (str.matches(regex)) {
                return try {
                    LocalDate.parse(str, formatter)
                } catch (e: DateTimeParseException) {
                    null
                }
            }
        }
        return null
    }

    private val epochSecondsRegex = Regex("\\d{10}")
    private val epochMillisRegex = Regex("\\d{13}")
    private val zonedDateTimeFormatters = mapOf(
        // 2024-12-31T12:34:56Z, 2024-12-31T12:34:56 +01:00 (ISO 8601), 2024-12-31T12:34:56.123Z
        Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,3})?(?:Z|[+-]\\d{2}:\\d{2})") to DateTimeFormatter.ISO_DATE_TIME,
        // 2024-12-31T12:34Z, 2024-12-31T12:34 +01:00
        Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})") to DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXXXXX"),
        // 20241231T123456Z, 20241231T123456 +01:00
        Regex("\\d{8}T\\d{6}(Z|[+-]\\d{2}:\\d{2})") to DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssXXXXX"),
        // 20241231T1234Z, 20241231T1234 +01:00
        Regex("\\d{8}T\\d{4}(Z|[+-]\\d{2}:\\d{2})") to DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmXXXXX"),
    )

    private val dateTimeFormatters = mapOf(
        // 2024-12-31T12:34:56, 2024-12-31T12:34:56.123
        Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,3})?") to DateTimeFormatter.ISO_DATE_TIME,
        // 2024-12-31T12:34Z, 2024-12-31T12:34 +01:00
        Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}") to DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
        // 20241231T123456Z, 20241231T123456 +01:00
        Regex("\\d{8}T\\d{6}") to DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"),
        // 20241231T1234Z, 20241231T1234 +01:00
        Regex("\\d{8}T\\d{4}") to DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"),
    )

    private val localDateFormatters = mapOf(
        // 2024-12-31
        Regex("\\d{4}-\\d{2}-\\d{2}") to DateTimeFormatter.ISO_LOCAL_DATE,
        // 20241231
        Regex("\\d{8}") to DateTimeFormatter.ofPattern("yyyyMMdd"),
    )
}
