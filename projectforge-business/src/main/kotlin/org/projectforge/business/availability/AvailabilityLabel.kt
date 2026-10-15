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


package org.projectforge.business.availability

import org.projectforge.framework.i18n.translate

/**
 * What an entry is called in the vacation export and the team calendar: its type, completed by the status if it's
 * more than "not available" (e.g. "Workation (Reachable if needed)"); without a type the status (or "Availability").
 */
val AvailabilityDO.label: String
    get() {
        val type = type
        val status = status
        return if (!type.isNullOrBlank()) {
            if (status != null && status != AvailabilityStatus.NOT_AVAILABLE) {
                "$type (${translate(status.i18nKey)})"
            } else {
                type
            }
        } else {
            status?.let { translate(it.i18nKey) } ?: translate("availability.title")
        }
    }
