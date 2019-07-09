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

package org.projectforge.business.common

import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.projectforge.business.user.UserPrefDao
import org.projectforge.favorites.AbstractFavorite
import org.projectforge.framework.persistence.api.BaseSearchFilter

class MagicFilter<F : BaseSearchFilter>(
        /**
         * Optional searchfilter of ProjectForge's entities, such as [org.projectforge.business.address.AddressFilter],
         * [org.projectforge.business.timesheet.TimesheetFilter] etc.
         */
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
        var searchFilter: F? = null,
        /**
         * Optional entries for searching (keywords, field search, range search etc.)
         */
        var entries: MutableList<MagicFilterEntry> = mutableListOf(),
        name: String? = null,
        id: Int? = null
) : AbstractFavorite(name, id) {

    @Transient
    internal val log = org.slf4j.LoggerFactory.getLogger(MagicFilter::class.java)

    /**
     * Creates the search filter for the data-base query.
     * @param filterClass Needed for creating a new filter instance if not yet given.
     *
     * Please note: Range search and search for values must be implemented for every specific filter.
     */
    fun prepareQueryFilter(filterClass: Class<F>): F {
        val filter = searchFilter ?: filterClass.newInstance()
        if (searchFilter == null)
            searchFilter = filter
        if (filter.maxRows <= 0)
            filter.maxRows = 50
        filter.isSortAndLimitMaxRowsWhileSelect = true
        if (entries.isNullOrEmpty()) {
            filter.searchString = null // Must be reset from any previous run
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
                MagicFilterEntry.Type.NONE -> {
                    searchStrings.add(entry.getSearchStringStrategy())
                }
            }
        }
        filter.searchString = searchStrings.joinToString(" AND ")
        return filter
    }

    @Suppress("SENSELESS_COMPARISON")
    fun isModified(other: MagicFilter<F>): Boolean {
        if (this.name != other.name) return true
        if (this.id != other.id) return true

        val entries1 = this.entries
        val entries2 = other.entries
        if (entries1 == null) { // Might be null after deserialization
            return entries2 != null
        }
        if (entries2 == null) { // Might be null after deserialization
            return true
        }
        if (entries1.size != entries2.size) {
            return true
        }
        entries1.forEachIndexed { i, value ->
            if (entries2[i].isModified(value)) {
                return true
            }
        }
        return "${this.searchFilter}" != "${other.searchFilter}" // Compares json representation (toString)
    }

    fun clone(): MagicFilter<F> {
        val mapper = UserPrefDao.createObjectMapper()
        val json = mapper.writeValueAsString(this)
        @Suppress("UNCHECKED_CAST")
        return mapper.readValue(json, MagicFilter::class.java) as MagicFilter<F>
    }
}
