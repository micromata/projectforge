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

package org.projectforge.framework.persistence.api

import org.projectforge.business.user.UserPrefDao
import org.projectforge.favorites.AbstractFavorite

class MagicFilter(
        /**
         * Optional entries for searching (keywords, field search, range search etc.)
         */
        var entries: MutableList<MagicFilterEntry> = mutableListOf(),
        var sortAndLimitMaxRowsWhileSelect: Boolean = true,
        var maxRows: Int = 50,
        /**
         * If true, only deleted entries will be shown. If false, no deleted entries will be shown. If null, all entries will be shown.
         */
        var deleted: Boolean? = false,
        var searchHistory: Boolean? = null,
        name: String? = null,
        id: Int? = null
) : AbstractFavorite(name, id) {

    @Transient
    internal val log = org.slf4j.LoggerFactory.getLogger(MagicFilter::class.java)

    val sortProperties = mutableListOf<SortProperty>()

    open fun reset() {
        entries.clear()
    }

    fun prepareQueryFilter() {
        val searchStrings = mutableListOf<String>()
        entries.forEach { entry ->
            when (entry.type()) {
                MagicFilterEntry.Type.STRING_SEARCH -> {
                    entry.search = entry.value as String // value not yet supported, but received from front-end.
                    entry.value = null
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
    }

    @Suppress("SENSELESS_COMPARISON")
    fun isModified(other: MagicFilter): Boolean {
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
        return false
    }

    fun clone(): MagicFilter {
        val mapper = UserPrefDao.createObjectMapper()
        val json = mapper.writeValueAsString(this)
        return mapper.readValue(json, MagicFilter::class.java)
    }
}
