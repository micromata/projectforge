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

class ImportStorageInfo() {
  var totalNumber: Int = 0
  var numberOfNewEntries: Int = 0
  var numberOfDeletedEntries: Int = 0
  var numberOfModifiedEntries: Int = 0
  var numberOfUnmodifiedEntries: Int = 0

  var detectedColumns: Map<String, ImportFieldSettings>? = null
  var unknownColumns: List<String>? = null

  var displayOptions = ImportStorage.DisplayOptions()

  constructor(importStorage: ImportStorage<*>) : this() {
    totalNumber = importStorage.entries.size
    importStorage.entries.forEach {
      if (it.readEntry == null) {
        if (it.storedEntry != null) {
          numberOfDeletedEntries += 1
        } else {
          // ??? Shouldn't occur: empty entry.
        }
      } else if (it.storedEntry == null) {
        numberOfNewEntries += 1
      } else if (it.isModified() == true) {
        numberOfModifiedEntries += 1
      } else {
        numberOfUnmodifiedEntries += 1
      }
    }
    detectedColumns = importStorage.detectedColumns
    unknownColumns = importStorage.unknownColumns

  }
}
