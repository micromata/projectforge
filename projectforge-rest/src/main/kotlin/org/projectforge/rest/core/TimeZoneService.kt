/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.core

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.stereotype.Component
import java.text.Collator
import java.util.*

/**
 * Language services.
 */
@Component
class TimeZoneService {
    class DisplayTimeZone(val value: String, val label: String) {
        constructor(timeZone: TimeZone) : this(timeZone.id, "${timeZone.id} (${timeZone.getDisplayName(false, TimeZone.SHORT, ThreadLocalUserContext.getLocale())})")
    }

    fun getAllTimeZones(): List<DisplayTimeZone> {
        return getTimeZones(TimeZone.getAvailableIDs())
    }

    fun getTimeZones(searchString: String?): List<DisplayTimeZone> {
        if (searchString.isNullOrBlank()) return getAllTimeZones()
        return getAllTimeZones().filter { it.label.contains(searchString, true) }
    }

    fun getTimeZones(timeZoneIds: Array<String>): List<DisplayTimeZone> {
        val usersLocale = ThreadLocalUserContext.getLocale()
        val comparator = Collator.getInstance(usersLocale)
        val timeZones = timeZoneIds.map { DisplayTimeZone(TimeZone.getTimeZone(it)) }
        return timeZones.sortedWith(compareBy(comparator) { it.label })
    }
}
