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

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.BankAccount
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.importer.AbstractImportPageRest
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportStorage
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UIAgGrid
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/importBankAccountRecords")
class BankAccountRecordImportPageRest : AbstractImportPageRest<BankAccountRecord>() {

  @Autowired
  private lateinit var bankAccountRecordDao: BankAccountRecordDao

  @Autowired
  private lateinit var bankAccountDao: BankAccountDao

  @Autowired
  private lateinit var jobHandler: JobHandler

  override val title: String = "plugins.banking.account.record.import.title"

  override fun callerPage(request: HttpServletRequest): String {
    val targetEntity = getImportStorage(request)?.targetEntity as? BankAccount
    val targetEntityId = targetEntity?.id
    return if (targetEntityId != null) {
      PagesResolver.getEditPageUrl(BankAccountPagesRest::class.java, targetEntityId, absolute = true)
    } else {
      PagesResolver.getListPageUrl(BankAccountPagesRest::class.java, absolute = true)
    }
  }

  override fun clearImportStorage(request: HttpServletRequest) {
    ExpiringSessionAttributes.removeAttribute(
      request,
      getSessionAttributeName(BankAccountRecordImportPageRest::class.java),
    )
  }

  override fun getImportStorage(request: HttpServletRequest): BankingImportStorage? {
    return ExpiringSessionAttributes.getAttribute(
      request,
      getSessionAttributeName(BankAccountRecordImportPageRest::class.java),
      BankingImportStorage::class.java,
    )
  }

  override fun import(importStorage: ImportStorage<*>, selectedEntries: List<ImportPairEntry<BankAccountRecord>>):
      Int? {
    val bankAccount = importStorage.targetEntity as? BankAccount
    if (bankAccount == null) {
      importStorage.addError(translate("plugins.banking.account.record.import.error.noBankAccountGiven"))
      return null
    }
    val bankAccountDO = bankAccountDao.getById(bankAccount.id)
    if (bankAccountDO == null) {
      importStorage.addError(translate("plugins.banking.account.record.import.error.noBankAccountGiven"))
      return null
    }
    return jobHandler.addJob(
      BankingImportJob(
        bankAccountDO,
        bankAccountDao,
        bankAccountRecordDao,
        selectedEntries,
        importStorage = importStorage as BankingImportStorage,
      )
    )
      .id
  }

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val importStorage = getImportStorage(request)
    return createFormLayoutData(request, importStorage)
  }

  override fun createListLayout(
    request: HttpServletRequest,
    layout: UILayout,
    agGrid: UIAgGrid,
  ) {
    val lc = LayoutContext(BankAccountRecordDO::class.java)
    addReadColumn(agGrid, lc, BankAccountRecordDO::date)
    addReadColumn(agGrid, lc, BankAccountRecordDO::valueDate)
    addReadColumn(agGrid, lc, BankAccountRecordDO::amount)
    addReadColumn(agGrid, lc, BankAccountRecordDO::receiverSender)
    addReadColumn(agGrid, lc, BankAccountRecordDO::iban)
    addReadColumn(agGrid, lc, BankAccountRecordDO::bic)
    addReadColumn(agGrid, lc, BankAccountRecordDO::subject)
    addReadColumn(agGrid, lc, BankAccountRecordDO::collectionReference)
    addReadColumn(agGrid, lc, BankAccountRecordDO::debteeId)
    addReadColumn(agGrid, lc, BankAccountRecordDO::mandateReference)
    addReadColumn(agGrid, lc, BankAccountRecordDO::customerReference)
    addReadColumn(agGrid, lc, BankAccountRecordDO::currency)
    addReadColumn(agGrid, lc, BankAccountRecordDO::type)
    addReadColumn(agGrid, lc, BankAccountRecordDO::info)
    // addStoredColumn(agGrid, lc, BankAccountRecordDO::date)
  }
}
