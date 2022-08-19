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

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ImportContextTest {
  @Test
  fun matchingPairsTest() {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)

    val read = mutableListOf<BankAccountRecord>()
    read.add(createRecord(yesterday, BigDecimal("1.23"), "Ice", "DE1111"))
    read.add(createRecord(yesterday, BigDecimal("1.24"), "Ice", "DE1111"))
    read.add(createRecord(yesterday, BigDecimal("1.23"), "Cake", "DE8888"))
    read.add(createRecord(tomorrow, BigDecimal("27.12"), "Apple", "DE222"))
    read.add(createRecord(tomorrow, BigDecimal("1.23"), "Apple", "DE222"))

    val db = mutableListOf<BankAccountRecordDO>()
    db.add(createDBRecord(today, BigDecimal("1.23"), "Ice", "DE1111"))
    db.add(createDBRecord(today, BigDecimal("1.24"), "Ice", "DE1111"))
    db.add(createDBRecord(today, BigDecimal("1.23"), "Cake", "DE8888"))
    db.add(createDBRecord(tomorrow, BigDecimal("27.12"), "Cake", "DE888"))
    db.add(createDBRecord(tomorrow, BigDecimal("1.23"), "Apple", null))
    db.add(createDBRecord(tomorrow, BigDecimal("1.23"), "Apple", "DE222"))

    val context = ImportContext()
    context.readTransactions = read
    context.databaseTransactions = db
    context.analyzeReadTransactions()
    context.createPairs()
    context.pairsByDate.forEach { date, list ->
      println("*** Date: $date")
      list.forEach {
        println("read=[${it.read?.date}, ${it.read?.amount}, ${it.read?.subject}, ${it.read?.iban}]".padEnd(40)
        + "db=[${it.dbRecord?.date}, ${it.dbRecord?.amount}, ${it.dbRecord?.subject}, ${it.dbRecord?.iban}]")
      }
    }
  }

  private fun createRecord(
    date: LocalDate?,
    amount: BigDecimal?,
    subject: String?,
    iban: String? = null
  ): BankAccountRecord {
    return BankAccountRecord(date = date, amount = amount, subject = subject, iban = iban)
  }

  private fun createDBRecord(
    date: LocalDate?,
    amount: BigDecimal?,
    subject: String?,
    iban: String? = null
  ): BankAccountRecordDO {
    val result = BankAccountRecordDO()
    result.date = date
    result.amount = amount
    result.subject = subject
    result.iban = iban
    return result
  }
}
