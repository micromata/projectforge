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

import org.projectforge.business.multitenancy.TenantRegistry
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.business.user.UserGroupCache
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
        var searchHistory: String? = null,
        /**
         * If true, any searchstring (alphanumeric) without wildcard will be changed to '<searchString>*'.
         */
        var autoWildcardSearch: Boolean = false,
        /**
         * Extend the filter by additional variables and settings.
         */
        var extended: MutableMap<String, Any> = mutableMapOf(),
        name: String? = null,
        id: Int? = null
) : AbstractFavorite(name, id) {

    @Transient
    internal val log = org.slf4j.LoggerFactory.getLogger(MagicFilter::class.java)

    var sortProperties = mutableListOf<SortProperty>()

    /**
     * After deserialization from data base (prefs) this method should be called to rebuild some information needed by the
     * clients.
     */
    fun init() {
        entries.forEach {
            if (it.field == MagicFilterEntry.HistorySearch.MODIFIED_BY_USER.fieldName) {
                it.value.label = getUserGroupCache().getUser(it.value.value?.toInt())?.username
            }
        }
    }

    fun reset() {
        entries.clear()
        sortProperties.clear()
        sortAndLimitMaxRowsWhileSelect = true
        deleted = false
        searchHistory = null
        extended.clear()
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
        val mapper = UserPrefDao.getObjectMapper()
        val json = mapper.writeValueAsString(this)
        return mapper.readValue(json, MagicFilter::class.java)
    }

    companion object {
        private fun getTenantRegistry(): TenantRegistry {
            return TenantRegistryMap.getInstance().tenantRegistry
        }

        private fun getUserGroupCache(): UserGroupCache {
            return getTenantRegistry().userGroupCache
        }
    }
}
