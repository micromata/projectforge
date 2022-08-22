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

abstract class ImportStorage<O : ImportEntry.Modified<O>>(
  val importSettings: ImportSettings
) {
  class DisplayOptions(
    var new: Boolean? = true,
    var modified: Boolean? = true,
    var unmodified: Boolean? = null,
    var deleted: Boolean? = true,
  )

  val detectedColumns = mutableMapOf<String, ImportFieldSettings>()
  val unknownColumns = mutableListOf<String>()

  /**
   * Mapping of columns to properties.
   */
  val columnMapping = mutableMapOf<Int, ImportFieldSettings>()

  var entries = mutableListOf<ImportEntry<O>>()

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
