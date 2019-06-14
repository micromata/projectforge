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

class MagicFilter<F : BaseSearchFilter>(
        /**
         * Optional searchfilter of ProjectForge's entities, such as [org.projectforge.business.address.AddressFilter],
         * [org.projectforge.business.timesheet.TimesheetFilter] etc.
         */
        val searchFilter: F? = null,
        /**
         * Optional entries for searching (keywords, field search, range search etc.)
         */
        val entries: MutableList<MagicFilterEntry>? = null
) {
    internal val log = org.slf4j.LoggerFactory.getLogger(MagicFilter::class.java)

    /**
     * Creates the search filter for the data-base query.
     * @param filterClass Needed for creating a new filter instance if not yet given.
     *
     * Please note: Range search and search for values must be implemented for every specific filter.
     */
    fun prepareQueryFilter(filterClass: Class<F>): F {
        val filter = searchFilter ?: filterClass.newInstance()
        if (entries.isNullOrEmpty()) {
            return filter // Nothing to configure.
        }
        val searchStrings = mutableListOf<String>()
        entries.forEach { entry ->
            when (entry.type()) {
                MagicFilterEntry.Type.STRING_SEARCH -> {
                    searchStrings.add(entry.getSearchStringStrategy())
                }
                MagicFilterEntry.Type.FIELD_STRING_SEARCH -> {
                    searchStrings.add("${entry.field}:${entry.getSearchStringStrategy()}")
                }
                MagicFilterEntry.Type.FIELD_VALUES_SEARCH -> {
                    log.warn("Unsupported field search: ${entry.type()}.")
                }
                MagicFilterEntry.Type.FIELD_RANGE_SEARCH -> {
                    log.warn("Unsupported field search: ${entry.type()}.")
                }
                MagicFilterEntry.Type.NONE -> { }
            }
        }
        return filter
    }
}
