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

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Configuration for availability types. The types are configurable via application.properties: codes, stored as such
 * and shown in the user's language by the key `availability.types.<code>` (see [translateAvailabilityType]), e.g.
 * of CustomerI18nResources for a code of an installation's own. A value without such a key is shown as given.
 */
@Component
open class AvailabilityConfig {
    @Value("\${projectforge.availability.types:parentalLeave;sickness;absence;compensatoryTimeOff;mobileWork;workation}")
    private lateinit var typesString: String

    /**
     * Returns the configured availability types as a list.
     */
    open fun getTypes(): List<String> {
        return typesString.split(";").map { it.trim() }.filter { it.isNotBlank() }
    }
}
