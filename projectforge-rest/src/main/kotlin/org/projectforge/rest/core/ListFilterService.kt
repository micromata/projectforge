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

import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.servlet.http.HttpSession

/**
 * For persisting list filters.
 */
@Component
class ListFilterService {
    private val log = org.slf4j.LoggerFactory.getLogger(ListFilterService::class.java)

    @Autowired
    private lateinit var userPrefRestService: UserPrefRestService

    fun getSearchFilter(session: HttpSession, filterClazz: Class<out BaseSearchFilter>): BaseSearchFilter {
        val filter = userPrefRestService.getEntry(session, PREF_AREA, filterClazz.name)
        if (filter != null) {
            if (filter.javaClass == filterClazz) {
                try {
                    return filter as BaseSearchFilter
                } catch (ex: ClassCastException) {
                    // No output needed, info message follows:
                }
                // Probably a new software release results in an incompability of old and new filter format.
                log.info(
                        "Could not restore filter from user prefs: (old) filter type "
                                + filter.javaClass.getName()
                                + " is not assignable to (new) filter type "
                                + filterClazz.javaClass.getName()
                                + " (OK, probably new software release).")
            }
        }
        val result = filterClazz.newInstance()
        result.reset()
        userPrefRestService.putEntry(session, PREF_AREA, filterClazz.name, result, true)
        return result
    }

    companion object {
        private const val PREF_AREA = "listFilter"
    }
}
