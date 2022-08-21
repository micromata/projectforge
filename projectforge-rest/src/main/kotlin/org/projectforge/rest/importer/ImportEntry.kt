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

import org.projectforge.rest.core.AbstractDynamicPageRest

class ImportEntry<O: ImportEntry.Modified<O>>(
/**
 * If given, this entry was read from import file.
 */
var readEntry: O? = null,

/**
 * If given, this entry does already exist in the database.
 */
var storedEntry: O? = null,
) : AbstractDynamicPageRest() {
  enum class Status { NEW, DELETED, MODIFIED, UNMODIFIED, UNKNOWN_MODIFICATION, UNKNOWN }

  interface Modified<O> {
    fun isModified(other: O): Boolean
  }

  fun isModified(): Boolean? {
    val read = readEntry
    val stored = storedEntry
    if (read == null || stored == null) {
      return null
    }
    return read.isModified(stored)
  }

  /**
   * If both exists, read- and storedEntry, here is the list of modified / changed
   * properties.
   */
  var diff: List<ImportPropertyDiff>? = null
  set(value) {
    field = value
    dirtyFlag = false
  }

  private var dirtyFlag = true

  val status: Status
  get() {
    return if (readEntry == null) {
      if (storedEntry == null) {
        Status.UNKNOWN
      }
      Status.NEW
    } else if (storedEntry == null) {
      Status.DELETED
    } else if (dirtyFlag) {
      Status.UNKNOWN_MODIFICATION
    } else if (diff.isNullOrEmpty()) {
      Status.UNMODIFIED
    } else {
      Status.MODIFIED
    }
  }
}
