/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.rest.dto.BankAccount
import org.projectforge.rest.importer.ImportEntry
import org.projectforge.rest.importer.ImportPairEntry
import java.math.BigDecimal
import java.time.LocalDate

class BankingImportStorageTest {
  @Test
  fun matchingPairsTest() {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)
    val dayAfterTomorrow = today.plusDays(2)

    val read = mutableListOf<BankAccountRecord>()
    read.add(createRecord(yesterday, "1.23", "Ice", "DE1111"))
    read.add(createRecord(yesterday, "1.24", "Ice", "DE1111"))
    read.add(createRecord(yesterday, "1.23", "Cake", "DE8888"))
    read.add(createRecord(dayAfterTomorrow, "27.12", "Apple", "DE222"))
    read.add(createRecord(dayAfterTomorrow, "1.23", "Apple", "DE222"))

    val db = mutableListOf<BankAccountRecordDO>()
    db.add(createDBRecord(today, "1.23", "Ice", "DE1111"))
    db.add(createDBRecord(today, "1.24", "Ice", "DE1111"))
    db.add(createDBRecord(today, "1.23", "Cake", "DE8888"))
    db.add(createDBRecord(tomorrow, "2.00", "To be removed", "DE888"))
    db.add(createDBRecord(dayAfterTomorrow, "27.12", "Cake", "DE888"))
    db.add(createDBRecord(dayAfterTomorrow, "1.23", "Apple", null))
    db.add(createDBRecord(dayAfterTomorrow, "1.23", "Apple", "DE222"))

    val storage = BankingImportStorage(targetEntity = BankAccount(id = 42))
    storage.readTransactions = read
    storage.databaseTransactions = db
    storage.analyzeReadTransactions()
    storage.reconcileImportStorage(false)
    /*//  PRINT storage for debugging:
    storage.entries.forEach {
      val read = it.readEntry
      val stored = it.storedEntry
      println(
        "${read?.date ?: stored?.date}: read=[${read?.date}, ${read?.amount}, ${read?.subject}, ${read?.iban}]".padEnd(
          55
        )
            + "db=[${stored?.date}, ${stored?.amount}, ${stored?.subject}, ${stored?.iban}]"
      )
    }*/
    Assertions.assertEquals(10, storage.pairEntries.size)
    storage.pairEntries.filter { it.read?.date == yesterday }.let { list ->
      Assertions.assertEquals(3, list.size)
      Assertions.assertTrue(list.all { it.stored == null })
      Assertions.assertTrue(list.none { it.read == null })
    }
    storage.pairEntries.filter { (it.read?.date ?: it.stored?.date) == today }.let { list ->
      Assertions.assertEquals(3, list.size)
      Assertions.assertTrue(list.all { it.read == null })
      Assertions.assertTrue(list.none { it.stored == null })
    }
    storage.pairEntries.filter { (it.read?.date ?: it.stored?.date) == tomorrow }.let { list ->
      Assertions.assertEquals(1, list.size)
      Assertions.assertTrue(list.all { it.read == null })
      Assertions.assertTrue(list.none { it.stored == null })
      Assertions.assertEquals(ImportEntry.Status.DELETED, list[0].status)
    }
    storage.pairEntries.filter { (it.read?.date ?: it.stored?.date) == dayAfterTomorrow }.let { list ->
      Assertions.assertEquals(3, list.size)
      Assertions.assertEquals(1, list.filter { it.read == null }.size)
      Assertions.assertTrue(list.none { it.stored == null })
      assertPair(list[0], "1.23", "Apple", "DE222", "1.23", "Apple", "DE222")
      assertPair(list[1], "27.12", "Apple", "DE222", "27.12", "Cake", "DE888")
      Assertions.assertNull(list[2].read)
      assertRecord(list[2].stored, "1.23", "Apple", null)
    }
  }

  private fun createRecord(
    date: LocalDate?,
    amount: String?,
    subject: String?,
    iban: String? = null
  ): BankAccountRecord {
    return BankAccountRecord(
      date = date,
      amount = if (amount != null) BigDecimal(amount) else null,
      subject = subject,
      iban = iban
    )
  }

  private fun createDBRecord(
    date: LocalDate?,
    amount: String?,
    subject: String?,
    iban: String? = null
  ): BankAccountRecordDO {
    val result = BankAccountRecordDO()
    result.date = date
    result.amount = if (amount != null) BigDecimal(amount) else null
    result.subject = subject
    result.iban = iban
    return result
  }

  private fun assertRecord(record: BankAccountRecord?, amount: String, subject: String?, iban: String?) {
    Assertions.assertNotNull(record)
    Assertions.assertEquals(BigDecimal(amount), record!!.amount)
    Assertions.assertEquals(subject, record.subject)
    Assertions.assertEquals(iban, record.iban)

  }

  private fun assertPair(
    pair: ImportPairEntry<BankAccountRecord>,
    readAmount: String,
    readSubject: String?,
    readIban: String?,
    storedAmount: String,
    storedSubject: String?,
    storedIban: String?,
  ) {
    assertRecord(pair.read, readAmount, readSubject, readIban)
    assertRecord(pair.stored, storedAmount, storedSubject, storedIban)
  }

/*
  @Test
  fun localTest() {
    val file = File(System.getProperty("user.home"), "tmp/test-transactions.csv")
    if (!file.exists()) {
      return
    }
    val importSettings = """
encoding=iso-8859-15
date=buchungstag|:dd.MM.yyyy|:dd.MM.yy
valueDate=valuta*|:dd.MM.yyyy|:dd.MM.yy
amount=betrag*|:#.##0,0#|:#0,0#
type=buchungstext*
debteeId=gläub*|glaeu*
subject=verwendung*
mandateReference=Mandat*
customerReference=Kundenref*
collectionReference=sammler*
receiverSender=*beguen*|*zahlungspflicht*
iban=*iban*
bic=*bic*|*swift*
currency=waehrung|währung
info=info
    """.trimMargin()
    val settings = ImportSettings.parseSettings(importSettings)
    val importStorage = BankingImportStorage()
    CsvImporter.parse(file.reader(), settings, importStorage)
    Assertions.assertTrue(importStorage.readTransactions.isNotEmpty())
    println(JsonUtils.toJson(importStorage))
  }*/
}
