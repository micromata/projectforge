/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.common.StringHelper
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.rest.dto.BankAccount
import org.projectforge.rest.importer.ImportFieldSettings
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportSettings
import org.projectforge.rest.importer.ImportStorage
import java.time.LocalDate

class BankingImportStorage(importSettings: String? = null, targetEntity: BankAccount? = null) :
  ImportStorage<BankAccountRecord>(
    ImportSettings()
      .addFieldSettings(ImportFieldSettings("account").withLabel(translate("plugins.banking.account")))
      .parseSettings(
        importSettings,
        BankAccountRecordDO::class.java,
        BankAccountRecordDO::bankAccount.name
      )
  ) {
  var fromDate: LocalDate? = null
  var untilDate: LocalDate? = null

  val bankAccountNormalizedIban = StringHelper.removeNonDigitsAndNonASCIILetters(targetEntity?.iban)

  var readTransactions = mutableListOf<BankAccountRecord>()
  var databaseTransactions: List<BankAccountRecordDO>? = null

  override val targetEntityTitle: String?
    get() = (targetEntity as? BankAccount)?.name

  init {
    this.targetEntity = targetEntity
  }


  override fun reconcileImportStorage(rereadDatabaseEntries: Boolean) {
    val from = fromDate
    val until = untilDate
    if (from == null || until == null || from > until) {
      return // Shouldn't occur
    }
    if (rereadDatabaseEntries) {
      (targetEntity as? BankAccount)?.id?.let { id ->
        databaseTransactions =
          ApplicationContextProvider.getApplicationContext().getBean(BankAccountRecordDao::class.java)
            .getByTimePeriod(id, from, until)
      }
    }
    clearEntries()
    var date = fromDate!!
    for (i in 0..999999) { // Paranoia counter, max 1 mio records.
      val readByDay = readTransactions.filter { it.date == date }
      val dbRecordsByDay = databaseTransactions?.filter { it.date == date } ?: emptyList()
      buildMatchingPairs(readByDay, dbRecordsByDay)
      date = date.plusDays(1)
      if (date > untilDate) {
        break
      }
    }
  }

  private fun buildMatchingPairs(
    readByDay: List<BankAccountRecord>,
    dbRecordsByDay: List<BankAccountRecordDO>,
  ) {
    if (readByDay.isEmpty()) {
      dbRecordsByDay.forEach { dbRecord ->
        addEntry(ImportPairEntry(null, createRecord(dbRecord)))
      }
      return // Nothing to import (only db records given).
    }
    val scoreMatrix = Array(readByDay.size) { IntArray(dbRecordsByDay.size) }
    for (k in readByDay.indices) {
      for (l in dbRecordsByDay.indices) {
        scoreMatrix[k][l] = readByDay[k].matchScore(dbRecordsByDay[l])
      }
    }
    val takenReadRecords = mutableSetOf<Int>()
    val takenDBRecords = mutableSetOf<Int>()
    for (i in 0..(readByDay.size + dbRecordsByDay.size)) { // Paranoia counter
      var maxScore = 0
      var maxK = -1
      var maxL = -1
      for (k in readByDay.indices) {
        if (takenReadRecords.contains(k)) {
          continue // Entry k is already taken.
        }
        for (l in dbRecordsByDay.indices) {
          if (takenDBRecords.contains(l)) {
            continue // Entry l is already taken.
          }
          if (scoreMatrix[k][l] > maxScore) {
            maxScore = scoreMatrix[k][l]
            maxK = k
            maxL = l
          }
        }
      }
      if (maxScore < 1) {
        break // No matching pair exists anymore.
      }
      takenReadRecords.add(maxK)
      takenDBRecords.add(maxL)
      addEntry(ImportPairEntry(readByDay[maxK], createRecord(dbRecordsByDay[maxL])))
    }
    // Now, add the unmatching records
    for (k in readByDay.indices) {
      if (takenReadRecords.contains(k)) {
        continue // Entry k is already taken.
      }
      addEntry(ImportPairEntry(readByDay[k], null))
    }
    for (l in dbRecordsByDay.indices) {
      if (takenDBRecords.contains(l)) {
        continue // Entry l is already taken.
      }
      addEntry(ImportPairEntry(null, createRecord(dbRecordsByDay[l])))
    }
    pairEntries.forEach { entry ->
      entry.read?.let { read ->
        if (read.bankAccount?.iban.isNullOrBlank()
          || !bankAccountNormalizedIban.contains(StringHelper.removeNonDigitsAndNonASCIILetters(read.bankAccount?.iban))
        ) {
          entry.error = translateMsg(
            "plugins.banking.import.error.recordWithWrongBankAccount",
            read.bankAccount?.iban,
            (targetEntity as BankAccount).iban
          )
        }
      }
    }
  }

  internal fun analyzeReadTransactions() {
    readTransactions.removeIf { it.date == null }
    readTransactions.sortBy { it.date }
    if (readTransactions.isEmpty()) {
      return // nothing to do.
    }
    fromDate = readTransactions.first().date!!
    untilDate = readTransactions.last().date!!
  }

  private fun createRecord(accountDO: BankAccountRecordDO): BankAccountRecord {
    val result = BankAccountRecord()
    result.copyFrom(accountDO)
    return result
  }

  override fun setProperty(obj: BankAccountRecord, fieldSettings: ImportFieldSettings, value: String): Boolean {
    if (fieldSettings.property != "account") {
      return false
    }
    var bankAccount = obj.bankAccount
    if (bankAccount == null) {
      bankAccount = BankAccount()
      obj.bankAccount = bankAccount
    }
    bankAccount.iban = value
    return true
  }

  override fun prepareEntity(): BankAccountRecord {
    return BankAccountRecord()
  }

  override fun commitEntity(obj: BankAccountRecord) {
    readTransactions.add(obj)
  }
}
