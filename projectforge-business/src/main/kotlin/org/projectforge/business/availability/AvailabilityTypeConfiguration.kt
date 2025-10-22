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

package org.projectforge.business.availability

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration for employee availability types (e.g., absent, remote, partial absence).
 *
 * Example configuration in application.properties:
 * ```
 * projectforge.availability.types[0].key=ABSENT
 * projectforge.availability.types[0].i18nKey=availability.type.absent
 * projectforge.availability.types[0].color=#DC3545
 * projectforge.availability.types[0].reachabilityStatus=UNAVAILABLE
 * projectforge.availability.types[0].percentage=100
 * projectforge.availability.types[0].hrOnly=false
 *
 * projectforge.availability.types[1].key=REMOTE
 * projectforge.availability.types[1].i18nKey=availability.type.remote
 * projectforge.availability.types[1].color=#17A2B8
 * projectforge.availability.types[1].reachabilityStatus=AVAILABLE
 * projectforge.availability.types[1].hrOnly=false
 *
 * projectforge.availability.types[2].key=PARTIAL_ABSENCE
 * projectforge.availability.types[2].i18nKey=availability.type.partialAbsence
 * projectforge.availability.types[2].color=#FFC107
 * projectforge.availability.types[2].reachabilityStatus=LIMITED
 * projectforge.availability.types[2].percentage=50
 * projectforge.availability.types[2].hrOnly=false
 *
 * projectforge.availability.types[3].key=PARENTAL_LEAVE
 * projectforge.availability.types[3].i18nKey=availability.type.parentalLeave
 * projectforge.availability.types[3].color=#6C757D
 * projectforge.availability.types[3].reachabilityStatus=UNAVAILABLE
 * projectforge.availability.types[3].percentage=100
 * projectforge.availability.types[3].hrOnly=true
 * ```
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Configuration
@ConfigurationProperties(prefix = "projectforge.availability")
open class AvailabilityTypeConfiguration {
    /**
     * List of availability types configured for this ProjectForge instance.
     */
    var types: MutableList<AvailabilityTypeConfig> = mutableListOf()

    /**
     * Configuration for a single availability type.
     */
    data class AvailabilityTypeConfig(
        /**
         * Unique key for this type (e.g., "ABSENT", "REMOTE", "PARTIAL_ABSENCE").
         */
        var key: String = "",

        /**
         * I18n key for the type name (e.g., "availability.type.absent").
         */
        var i18nKey: String = "",

        /**
         * Hex color for calendar display (e.g., "#DC3545").
         */
        var color: String = "#000000",

        /**
         * Reachability status: UNAVAILABLE, AVAILABLE, LIMITED.
         */
        var reachabilityStatus: String = "UNAVAILABLE",

        /**
         * Percentage of absence (e.g., 50 for 50%, 100 for full absence).
         * Null if not applicable.
         */
        var percentage: Int? = null,

        /**
         * If true, only HR can create/modify entries of this type.
         */
        var hrOnly: Boolean = false
    )

    init {
        // Default configuration if no properties are set
        if (types.isEmpty()) {
            types.add(
                AvailabilityTypeConfig(
                    key = "ABSENT",
                    i18nKey = "availability.type.absent",
                    color = "#DC3545",
                    reachabilityStatus = "UNAVAILABLE",
                    percentage = 100,
                    hrOnly = false
                )
            )
            types.add(
                AvailabilityTypeConfig(
                    key = "REMOTE",
                    i18nKey = "availability.type.remote",
                    color = "#17A2B8",
                    reachabilityStatus = "AVAILABLE",
                    percentage = null,
                    hrOnly = false
                )
            )
            types.add(
                AvailabilityTypeConfig(
                    key = "PARTIAL_ABSENCE",
                    i18nKey = "availability.type.partialAbsence",
                    color = "#FFC107",
                    reachabilityStatus = "LIMITED",
                    percentage = 50,
                    hrOnly = false
                )
            )
        }
    }

    /**
     * Get availability type config by key.
     */
    fun getTypeByKey(key: String): AvailabilityTypeConfig? {
        return types.find { it.key == key }
    }

    /**
     * Get all type keys.
     */
    fun getAllKeys(): List<String> {
        return types.map { it.key }
    }
}
