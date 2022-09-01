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

import java.math.BigDecimal

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
    fun isModified(other: O): Boolean
    fun isModified(value: Any?, dest: Any?): Boolean {
      value ?: return false
      if (dest == null) {
        return true
      }
      if (value::class.java != dest::class.java) {
        return true
      }
      return when (value) {
        is BigDecimal -> {
          value.compareTo(dest as BigDecimal) != 0
        }

        else -> dest != value
      }
    }
  }

  fun isModified(): Boolean? {
    val read = read
    val stored = stored
    if (read == null || stored == null) {
      return null
    }
    return read.isModified(stored)
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
        val modified = isModified()
        when (modified) {
          null -> Status.UNKNOWN_MODIFICATION
          true -> Status.MODIFIED
          else -> Status.UNMODIFIED
        }
      }
    }
}
