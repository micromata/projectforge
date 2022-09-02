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

package org.projectforge.rest.importer

import org.projectforge.common.BeanHelper
import java.math.BigDecimal
import kotlin.reflect.KProperty

/**
 * An import pair entry contains both, the read entry and the db entry (if exist).
 * The status will be checked by this pair:
 *  - if only a read entry exists, it's a new entry,
 *  - if only a stored entry (db) exists, the entry is to be deleted.
 *  - if both exist, it's unmodified or modified (depending on the diff check).º
 */
class ImportPairEntry<O : ImportPairEntry.Modified<O>>(
  /**
   * If given, this entry was read from import file.
   */
  read: O? = null,

  /**
   * If given, this entry does already exist in the database.
   */
  val stored: O? = null,
) : ImportEntry<O>(read) {
  interface Modified<O> {
    fun buildOldDiffValue(map: MutableMap<String, Any>, property: String, value: Any?, old: Any?) {
      if (isModified(value, old)) {
        var useOldValue = old ?: ""
        if (useOldValue is String) {
          useOldValue = useOldValue.trim()
        }
        map["read.$property"] = useOldValue
      }
    }

    val properties: Array<KProperty<*>>?

    fun buildOldDiffValues(map: MutableMap<String, Any>, old: O) {}
  }

  private fun buildOldDiffValue(map: MutableMap<String, Any>) {
    read?.properties?.forEach { property ->
      val readValue = BeanHelper.getProperty(read, property.name)
      val oldValue = BeanHelper.getProperty(stored, property.name)
      if (isModified(readValue, oldValue)) {
        var useOldValue = oldValue ?: ""
        if (useOldValue is String) {
          useOldValue = useOldValue.trim()
        }
        map["read.${property.name}"] = useOldValue
      }
    }
  }

  fun isModified(): Boolean? {
    val read = read
    val stored = stored
    if (read == null || stored == null) {
      return null
    }
    if (oldDiffValues == null) {
      val map = mutableMapOf<String, Any>()
      read.buildOldDiffValues(map, stored)
      buildOldDiffValue(map)
      oldDiffValues = map
    }
    return !oldDiffValues.isNullOrEmpty()
  }

  override var status: Status
    get() {
      if (super.status == Status.UNKNOWN) {
        // Status unknown, try to find the status value:
        super.status = checkStatus
      }
      return super.status
    }
    set(value) {
      super.status = value
    }

  private val checkStatus: Status
    get() {
      return if (!error.isNullOrBlank()) {
        Status.FAULTY
      } else if (read == null) {
        if (stored == null) {
          Status.UNKNOWN
        }
        Status.DELETED
      } else if (stored == null) {
        Status.NEW
      } else {
        when (isModified()) {
          null -> Status.UNKNOWN_MODIFICATION
          true -> Status.MODIFIED
          else -> Status.UNMODIFIED
        }
      }
    }


  companion object {
    private fun isModified(value: Any?, dest: Any?): Boolean {
      if (value == null) {
        return isNotNullOrBlank(dest)
      }
      if (dest == null) {
        return isNotNullOrBlank(value)
      }
      if (value::class.java != dest::class.java) {
        return true
      }
      return when (value) {
        is BigDecimal -> {
          value.compareTo(dest as BigDecimal) != 0
        }

        is String -> {
          (dest as String).trim() != value.trim()
        }

        else -> dest != value
      }
    }

    private fun isNotNullOrBlank(value: Any?): Boolean {
      return when (value) {
        null -> false
        is String -> value.isNotBlank()
        else -> true
      }
    }
  }
}
