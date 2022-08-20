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

import mu.KotlinLogging
import org.projectforge.common.CSVParser
import java.io.Reader

private val log = KotlinLogging.logger {}

object CsvImporter {
  /*@JvmStatic
  fun main(vararg args: String) {
    val file = File(System.getProperty("user.home"), "tmp/test-transactions.csv")
    val mapping = BankAccountRecordMapping()
    val bankAccount = BankAccount()
    bankAccount.mappingTable = mapping
    parse(file.bufferedReader(), bankAccount, true)
  } */

  fun <O: Any> parse(reader: Reader, mappingInfo: MappingInfo, importStorage: ImportStorage<O>) {
    val parser = CSVParser(reader)
    val headCols = parser.parseLine()
    headCols.forEachIndexed { index, head ->
      val mapping = mappingInfo.getMapping(head)
      if (mapping != null) {
        log.debug { "Field '$head' found: -> ${mapping.property}." }
        importStorage.columnMapping[index] = mapping
        importStorage.foundColumns[head] = mapping.property
      } else {
        log.debug { "Field '$head' not found." }
        importStorage.ignoredColumns.add(head)
      }
    }
    for (i in 0..100000) { // Paranoi loop, read 100000 lines at max.
      val line = parser.parseLine()
      if (line == null) {
        // Finished
        break
      }
      val record = importStorage.prepareEntity()
      line.forEachIndexed { index, value ->
        importStorage.columnMapping[index]?.let { mappingInfoEntry ->
          importStorage.setProperty(record, mappingInfoEntry, value)
        }
      }
      importStorage.commitEntity(record)
    }
  }
}
