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
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.importer.AbstractImportPageRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@Service
class TransactionsImporter {
  @Autowired
  private lateinit var bankAccountRecordDao: BankAccountRecordDao

  fun import(
    request: HttpServletRequest,
    bankAccountDO: BankAccountDO,
    importStorage: BankingImportStorage,
  ) {
    importStorage.analyzeReadTransactions()
    val fromDate = importStorage.fromDate
    val untilDate = importStorage.untilDate
    log.info { "Trying to import account records from $fromDate-$untilDate for account '${bankAccountDO.name}': ${bankAccountDO.iban}" }
    ExpiringSessionAttributes.setAttribute(
      request,
      AbstractImportPageRest.getSessionAttributeName(BankAccountRecordImportPageRest::class.java),
      importStorage,
      20,
    )
    if (fromDate == null || untilDate == null) {
      return
    }
    // Ordered by date:
    importStorage.databaseTransactions = bankAccountRecordDao.getByTimePeriod(bankAccountDO.id, fromDate, untilDate)
    importStorage.reconcileImportStorage()
    importStorage.databaseTransactions = null // Save memory, not needed anymore.
    importStorage.readTransactions.clear() // Save memory, not needed anymore
  }
}
