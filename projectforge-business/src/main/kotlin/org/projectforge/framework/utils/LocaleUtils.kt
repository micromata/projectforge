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

import mu.KotlinLogging
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Utility class for parsing and handling Locale objects.
 */
object LocaleUtils {
    /**
     * Parses a Locale from a string representation.
     * Accepts language tags (e.g., "de", "en-US", "de-DE") or display names.
     *
     * @param value The string to parse
     * @return The parsed Locale, or null if parsing fails
     */
    @JvmStatic
    fun parse(value: String?): Locale? {
        if (value.isNullOrBlank()) return null

        return try {
            // Try parsing as language tag (e.g., "de", "en-US")
            val locale = Locale.forLanguageTag(value)

            // Locale.forLanguageTag returns a Locale even for invalid tags,
            // but with empty language. Check if we got a valid result.
            if (locale.language.isNotEmpty()) {
                locale
            } else {
                log.warn { "Failed to parse locale from language tag: $value" }
                null
            }
        } catch (e: Exception) {
            log.warn { "Failed to parse locale: $value - ${e.message}" }
            null
        }
    }
}
