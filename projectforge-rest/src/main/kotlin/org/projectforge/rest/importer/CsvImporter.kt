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
import org.projectforge.common.BeanHelper
import org.projectforge.common.CSVParser
import java.io.Reader
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

object CsvImporter {
  fun <O : ImportEntry.Modified<O>> parse(reader: Reader, settings: ImportSettings, importStorage: ImportStorage<O>) {
    val parser = CSVParser(reader)
    val headCols = parser.parseLine()
    headCols.forEachIndexed { index, head ->
      val fieldSettings = settings.getFieldSettings(head)
      if (fieldSettings != null) {
        log.debug { "Field '$head' found: -> ${fieldSettings.property}." }
        importStorage.columnMapping[index] = fieldSettings
        importStorage.foundColumns[head] = fieldSettings.property
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
        importStorage.columnMapping[index]?.let { fieldSettings ->
          if (!importStorage.setProperty(record, fieldSettings, value)) {
            val targetValue = when (BeanHelper.determinePropertyType(record::class.java, fieldSettings.property)) {
              LocalDate::class.java -> {
                fieldSettings.parseLocalDate(value)
              }
              Date::class.java -> {
                fieldSettings.parseDate(value)
              }
              BigDecimal::class.java -> {
                fieldSettings.parseBigDecimal(value)
              }
              Int::class.java -> {
                fieldSettings.parseInt(value)
              }
              Boolean::class.java -> {
                fieldSettings.parseBoolean(value)
              }
              else -> {
                value
              }
            }
            BeanHelper.setProperty(record, fieldSettings.property, targetValue)
          }
        }
      }
      importStorage.commitEntity(record)
    }
  }
}
