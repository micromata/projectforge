/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserPrefDao
import org.projectforge.favorites.AbstractFavorite
import org.projectforge.framework.utils.NumberHelper

class MagicFilter(
  /**
   * Optional entries for searching (keywords, field search, range search etc.)
   */
  var entries: MutableList<MagicFilterEntry> = mutableListOf(),
  var sortAndLimitMaxRowsWhileSelect: Boolean = true,
  var maxRows: Int = QUERY_FILTER_MAX_ROWS,
  /**
   * If true, only deleted entries will be shown. If false, no deleted entries will be shown. If null, all entries will be shown.
   */
  var deleted: Boolean? = false,
  /**
   * Optional full text search on all indexed fields.
   */
  var searchString: String? = null,
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
  id: Int? = null,
  /**
   * If multi selection is chosen, the magic filter is not shown (only the entities of the last result list will be displayed for (de)selecting.
   */
  var multiSelection: Boolean? = null,
) : AbstractFavorite(name, id) {

  @Transient
  internal val log = org.slf4j.LoggerFactory.getLogger(MagicFilter::class.java)

  var sortProperties = mutableListOf<SortProperty>()

  /**
   * After deserialization from data base (prefs) this method should be called to rebuild some information needed by the
   * clients.
   */
  fun init() {
    entries.forEach { entry ->
      if (entry.field == MagicFilterEntry.HistorySearch.MODIFIED_BY_USER.fieldName) {
        // If user id is saved as prop id (by React-client), then copy it to value property:
        entry.value.id?.let { id ->
          entry.value.value = id.toString()
        }
        // client may use label or displayName:
        entry.value.displayName =
          UserGroupCache.getInstance().getUser(entry.value.id ?: entry.value.value?.toInt())?.getFullname()
        entry.value.label = entry.value.displayName
      }
    }
    entries.removeIf { it.field.isNullOrBlank() } // Former filter versions (7.0-SNAPSHOT in 2019 supported entries with no values. This is now replaced by searchString.
  }

  val paginationPageSize: Int?
    get() {
      var size: Int? = null
      entries.find { it.field == PAGINATION_PAGE_SIZE }?.let { entry ->
        entry.value.values?.let { values ->
          if (values.isNotEmpty()) {
            size = NumberHelper.parseInteger(values[0])
          }
        }
        if (size == null) {
          size = NumberHelper.parseInteger("${entry.value}")
        }
      }
      return size
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
    const val PAGINATION_PAGE_SIZE = "paginationPageSize"
  }
}
