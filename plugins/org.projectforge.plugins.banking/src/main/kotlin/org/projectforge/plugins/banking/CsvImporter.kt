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

package org.projectforge.plugins.banking

import mu.KotlinLogging
import org.projectforge.common.CSVParser
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.dto.BankAccount
import org.projectforge.rest.dto.BankAccountRecordMapping
import java.io.File
import java.io.Reader
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.jvm.javaField

private val log = KotlinLogging.logger {}

object CsvImporter {
  @JvmStatic
  fun main(vararg args: String) {
    val file = File(System.getProperty("user.home"), "tmp/test-transactions.csv")
    val mapping = BankAccountRecordMapping()
    val bankAccount = BankAccount()
    bankAccount.mappingTable = mapping
    parse(file.bufferedReader(), bankAccount, true)
  }

  fun parse(reader: Reader, bankAccount: BankAccount, testMode: Boolean = false): ImportContext {
    val context = ImportContext()
    val parser = CSVParser(reader)
    val headCols = parser.parseLine()
    val mapping = bankAccount.mappingTable ?: BankAccountRecordMapping()
    headCols.forEachIndexed { index, head ->
      val field = mapping.getField(head)
      if (field != null) {
        log.debug { "Field '$head' found: -> ${field.name}." }
        context.lineMapping[index] = field
        context.foundColumns[head] = field.name
      } else {
        log.debug { "Field '$head' not found." }
        context.ignoredColumns.add(head)
      }
    }
    for (i in 0..100000) { // Paranoi loop, read 100000 lines at max.
      val line = parser.parseLine()
      if (line == null) {
        // Finished
        break
      }
      val record = BankAccountRecord()
      line.forEachIndexed { index, value ->
        context.lineMapping[index]?.let { property ->
          when (property.javaField?.type) {
            LocalDate::class.java -> {
              if (testMode) {
                property.set(record, LocalDate.now())
              } else {
                PFDayUtils.parseDate(value)?.let {
                  property.set(record, it)
                }
              }
            }
            String::class.java -> {
              property.set(record, value)
            }
            BigDecimal::class.java -> {
              NumberHelper.parseBigDecimal(value)?.let {
                property.set(record, it)
              }
            }
          }
        }
      }
      if (record.date != null && record.amount != null) { // At least date and amount is required.
        context.readTransactions.add(record)
      }
    }
    return context
  }
}
