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
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.ModificationStatus
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.importer.AbstractImportJob
import org.projectforge.rest.importer.ImportPairEntry

private val log = KotlinLogging.logger {}

class BankingImportJob(
  val bankAccountDO: BankAccountDO,
  val bankAccountDao: BankAccountDao,
  val bankAccountRecordDao: BankAccountRecordDao,
  val selectedEntries: List<ImportPairEntry<BankAccountRecord>>,
) : AbstractImportJob(
  translateMsg("plugins.banking.import.job.title", bankAccountDO.name),
  area = "BankingRecordsImport",
  queueName = "bankAccount#${bankAccountDO.id}",
  queueStrategy = QueueStrategy.PER_QUEUE_AND_USER,
  timeoutSeconds = 600,
) {
  init {
    totalNumber = selectedEntries.size
    processedNumber = 0
  }

  override suspend fun run() {
    log.info { "Starting import job for bank account records..." }
    selectedEntries.forEach { entry ->
      if (!isActive) {
        // Job is going to be cancelled.
        return
      }
      val storedEntryId = entry.stored?.id
      val readEntry = entry.read
      var dbEntry = if (storedEntryId != null) {
        bankAccountRecordDao.getById(storedEntryId)
      } else {
        null
      }
      if (dbEntry == null) {
        if (readEntry != null) {
          dbEntry = BankAccountRecordDO()
          readEntry.copyTo(dbEntry)
          dbEntry.bankAccount = bankAccountDO
          dbEntry.checksum = dbEntry.buildCheckSum()
          bankAccountRecordDao.internalSave(dbEntry)
          result.inserted += 1
        }
      } else {
        if (readEntry != null) {
          val id = dbEntry.id
          readEntry.copyTo(dbEntry)
          dbEntry.id = id
          dbEntry.checksum = dbEntry.buildCheckSum()
          dbEntry.isDeleted = false
          val modStatus = bankAccountRecordDao.update(dbEntry)
          if (modStatus != ModificationStatus.NONE) {
            result.updated += 1
          } else {
            result.unmodified += 1
          }
        } else {
          bankAccountRecordDao.markAsDeleted(dbEntry)
          result.deleted += 1
        }
      }
      processedNumber += 1
    }
  }

  override fun readAccess(user: PFUserDO?): Boolean {
    user ?: return false
    return bankAccountDao.hasUserSelectAccess(user, bankAccountDO, false)
  }

  override fun writeAccess(user: PFUserDO?): Boolean {
    user ?: return false
    return bankAccountDao.hasUpdateAccess(user, bankAccountDO, bankAccountDO, false)
  }
}
