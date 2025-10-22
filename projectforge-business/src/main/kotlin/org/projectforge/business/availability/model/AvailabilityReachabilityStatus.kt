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

package org.projectforge.business.availability.model

import org.projectforge.common.i18n.I18nEnum

/**
 * Reachability status for availability entries.
 *
 * @author Kai Reinhard
 */
enum class AvailabilityReachabilityStatus(val key: String) : I18nEnum {
    UNAVAILABLE("unavailable"),
    AVAILABLE("available"),
    LIMITED("limited");

    /**
     * The key will be used e. g. for i18n.
     */
    override val i18nKey: String
        get() = "availability.reachabilityStatus.$key"

    companion object {
        fun fromString(value: String): AvailabilityReachabilityStatus {
            return entries.find { it.key.equals(value, ignoreCase = true) }
                ?: UNAVAILABLE
        }
    }
}
