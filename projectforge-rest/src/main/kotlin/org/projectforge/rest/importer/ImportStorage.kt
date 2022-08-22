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

import org.projectforge.framework.i18n.translate

abstract class ImportStorage<O : ImportEntry.Modified<O>>(
  val importSettings: ImportSettings
) {
  private var idCounter = 0

  class DisplayOptions(
    var new: Boolean? = true,
    var modified: Boolean? = true,
    var unmodified: Boolean? = null,
    var deleted: Boolean? = true,
  )

  /**
   * If the user is able to import data for different target entities, please set the targetEntity here.
   */
  var targetEntity: Any? = null

  /**
   * If the user is able to import data for different target entities, this title is displayed
   * in the import tool for clarification.
   */
  open val targetEntityTitle: String? = null

  val title: String
    get() {
      targetEntityTitle?.let {
        if (it.isNotBlank()) {
          return "${translate("import.title")}: $it"
        }
      }
      return translate("import.title")
    }

  val detectedColumns = mutableMapOf<String, ImportFieldSettings>()
  val unknownColumns = mutableListOf<String>()

  /**
   * Mapping of columns to properties.
   */
  val columnMapping = mutableMapOf<Int, ImportFieldSettings>()

  var entries = mutableListOf<ImportEntry<O>>()

  val ensureEntriesIds: MutableList<ImportEntry<O>>
    get() {
      entries.forEach { entry ->
        if (entry.id < 0) {
          entry.id = idCounter++
        }
      }
      return entries
    }

  fun addEntry(entry: ImportEntry<O>) {
    entry.id = idCounter++
    entries.add(entry)
  }

  var displayOptions = DisplayOptions()

  val info: ImportStorageInfo
    get() = ImportStorageInfo(this)

  /**
   * Prepares an entity (normally only by return new object).
   */
  abstract fun prepareEntity(): O

  /**
   * Set the property of the prepared entity on your own.
   * @return true if the property is set by the implementation. If false (default), then the property will be set automatically
   * by the importer tool (if possible).
   */
  open fun setProperty(obj: O, fieldSettings: ImportFieldSettings, value: String): Boolean {
    return false
  }

  /**
   * Store or skip this entity after the setting of all properties.
   */
  abstract fun commitEntity(obj: O)
}
