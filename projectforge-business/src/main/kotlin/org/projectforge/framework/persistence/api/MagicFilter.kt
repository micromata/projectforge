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

package org.projectforge.framework.persistence.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserPrefDao
import org.projectforge.favorites.AbstractFavorite
import org.projectforge.framework.utils.NumberHelper
import java.security.MessageDigest

class MagicFilter(
  /**
   * Optional entries for searching (keywords, field search, range search etc.)
   */
  var entries: MutableList<MagicFilterEntry> = mutableListOf(),
  var sortAndLimitMaxRowsWhileSelect: Boolean = true,
  var maxRows: Int = QueryFilter.QUERY_FILTER_MAX_ROWS,
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
  id: Long? = null,
  /**
   * If multi selection is chosen, the magic filter is not shown (only the entities of the last result list will be displayed for (de)selecting.
   */
  var multiSelection: Boolean? = null,
) : AbstractFavorite(name, id) {
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
          UserGroupCache.getInstance().getUser(entry.value.id ?: entry.value.value?.toLong())?.getFullname()
        entry.value.label = entry.value.displayName
      }
    }
    entries.removeIf { it.field.isNullOrBlank() } // Former filter versions (7.0-SNAPSHOT in 2019 supported entries with no values. This is now replaced by searchString.
  }

  /**
   * Derived from the entry with field [PAGINATION_PAGE_SIZE] — it is serialized (the clients read it from
   * [org.projectforge.rest.core.ResultSet]), but never deserialized: a client echoing a filter it got from the
   * server back to `{entity}/list` would otherwise fail the whole request with an unrecognized field, because
   * there is no setter to bind it to. Read only, so Jackson knows the property and drops an incoming value; the
   * page size itself travels in [entries].
   */
  @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
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

  /**
   * A stable hash over everything that decides **which rows in which order** the filter selects. It is the
   * cache key of the server-side paged id list (see `MIGRATION-list-paging.md`): two filters with the same
   * fingerprint yield the same ordered result, so their materialized id list may be reused.
   *
   * Included: [entries] (sorted, [PAGINATION_PAGE_SIZE] excluded — page size does not change the row set),
   * [searchString], [searchHistory], [deleted], [sortProperties], [autoWildcardSearch], [extended] and
   * [maxRows]. Excluded: [name]/[id] (the favorite reference must not invalidate a cache) and
   * [multiSelection].
   *
   * Prefixed with [FINGERPRINT_VERSION] so a release changing filter semantics invalidates every cached
   * list without touching the stored favorites. Server-internal ([JsonIgnore]); it never travels the wire.
   */
  @get:JsonIgnore
  val resultFingerprint: String
    get() {
      val mapper = UserPrefDao.objectMapper
      // Serialize each entry whole (so any added field, e.g. operator, is covered) and sort, so entry
      // order does not change the fingerprint.
      val entriesCanonical = entries
        .filter { it.field != PAGINATION_PAGE_SIZE }
        .map { mapper.writeValueAsString(it) }
        .sorted()
      val canonical = buildString {
        append(FINGERPRINT_VERSION)
        append('|').append(entriesCanonical.joinToString(","))
        append('|').append(searchString ?: "")
        append('|').append(searchHistory ?: "")
        append('|').append(deleted)
        append('|').append(sortProperties.joinToString(",") { "${it.property}:${it.sortOrder}" })
        append('|').append(autoWildcardSearch)
        append('|').append(mapper.writeValueAsString(extended.toSortedMap()))
        append('|').append(maxRows)
      }
      val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
      return digest.joinToString("") { "%02x".format(it) }
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
    val mapper = UserPrefDao.objectMapper
    val json = mapper.writeValueAsString(this)
    return mapper.readValue(json, MagicFilter::class.java)
  }

  companion object {
    const val PAGINATION_PAGE_SIZE = "paginationPageSize"

    /**
     * Bumped whenever the meaning of a filter changes (a new operator semantics, a changed default), so
     * every cached paged id list ([resultFingerprint]) is invalidated on the next read.
     */
    const val FINGERPRINT_VERSION = "v1"
  }
}
