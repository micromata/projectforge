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

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.framework.i18n.translate

open class ImportEntry<O : Any>(
  val read: O? = null,
) {
  enum class Status { NEW, DELETED, MODIFIED, UNMODIFIED, UNKNOWN_MODIFICATION, UNKNOWN, FAULTY }

  var id: Int = -1

  open var status: Status = Status.UNKNOWN

  var error: String? = null

  @get:JsonProperty
  val statusAsString: String
    get() = translate("import.entry.status.${status.name.lowercase()}")

  /**
   * If both exists, read- and storedEntry, here is the list of modified / changed
   * properties.
   */
  var diff: List<ImportPropertyDiff>? = null

  fun match(displayOptions: ImportStorage.DisplayOptions): Boolean {
    return when (status) {
      Status.NEW -> displayOptions.new == true
      Status.DELETED -> displayOptions.deleted == true
      Status.MODIFIED -> displayOptions.modified == true
      Status.UNMODIFIED -> displayOptions.unmodified == true
      Status.UNKNOWN, Status.UNKNOWN_MODIFICATION -> displayOptions.unknown == true
      Status.FAULTY -> displayOptions.faulty == true
      else -> false
    }
  }
}
