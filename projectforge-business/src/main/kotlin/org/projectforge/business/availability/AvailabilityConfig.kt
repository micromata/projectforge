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

import org.projectforge.framework.configuration.properties.ReloadableProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Configuration for availability types. The types are configurable via application.properties: codes, stored as such
 * and shown in the user's language by the key `availability.types.<code>` (see [translateAvailabilityType]), e.g.
 * of CustomerI18nResources for a code of an installation's own. A value without such a key is shown as given.
 * Takes effect without a restart ([ReloadableProperties]).
 */
@Component
open class AvailabilityConfig : ReloadableProperties {
    @Value("\${$TYPES_KEY:$DEFAULT_TYPES}")
    private lateinit var typesString: String

    override val reloadableKeys = listOf(TYPES_KEY)

    override fun reload(environment: Environment) {
        typesString = environment.getProperty(TYPES_KEY, DEFAULT_TYPES)
    }

    /**
     * Returns the configured availability types as a list.
     */
    open fun getTypes(): List<String> {
        return typesString.split(";").map { it.trim() }.filter { it.isNotBlank() }
    }

    companion object {
        private const val TYPES_KEY = "projectforge.availability.types"

        private const val DEFAULT_TYPES = "parentalLeave;sickness;absence;compensatoryTimeOff;mobileWork;workation"
    }
}
