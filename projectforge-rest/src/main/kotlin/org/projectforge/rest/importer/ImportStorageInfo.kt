/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
  var numberOfUnknownEntries: Int = 0
  var numberOfFaultyEntries: Int = 0

  var detectedColumns: List<String>? = null
  var unknownColumns: List<String>? = null

  var displayOptions = ImportStorage.DisplayOptions()

  constructor(importStorage: ImportStorage<*>) : this() {
    totalNumber = importStorage.pairEntries.size
    importStorage.pairEntries.forEach {
      when (it.status) {
        ImportEntry.Status.NEW -> numberOfNewEntries += 1
        ImportEntry.Status.UNKNOWN, ImportEntry.Status.UNKNOWN_MODIFICATION -> numberOfUnknownEntries += 1
        ImportEntry.Status.MODIFIED -> numberOfModifiedEntries += 1
        ImportEntry.Status.UNMODIFIED -> numberOfUnmodifiedEntries += 1
        ImportEntry.Status.DELETED -> numberOfDeletedEntries += 1
        ImportEntry.Status.FAULTY -> numberOfFaultyEntries += 1
      }
    }
    detectedColumns = importStorage.detectedColumns.keys.sorted()
    unknownColumns = importStorage.unknownColumns
  }
}
