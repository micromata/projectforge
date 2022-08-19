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

import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UILayout
import org.projectforge.ui.UIReadOnlyField
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/bankAccountRecord")
class BankAccountRecordPagesRest : AbstractDTOPagesRest<BankAccountRecordDO, BankAccountRecord, BankAccountRecordDao>(
  BankAccountRecordDao::class.java,
  "plugins.banking.account.record.title",
  cloneSupport = CloneSupport.CLONE
) {
  override fun transformFromDB(obj: BankAccountRecordDO, editMode: Boolean): BankAccountRecord {
    val bankAccountRecord = BankAccountRecord()
    bankAccountRecord.copyFrom(obj)
    return bankAccountRecord
  }

  override fun transformForDB(dto: BankAccountRecord): BankAccountRecordDO {
    val bankAccountRecordDO = BankAccountRecordDO()
    dto.copyTo(bankAccountRecordDO)
    return bankAccountRecordDO
  }


  /**
   * LAYOUT List page
   */
  override fun createListLayout(
    request: HttpServletRequest,
    layout: UILayout,
    magicFilter: MagicFilter,
    userAccess: UILayout.UserAccess
  ) {
    agGridSupport.prepareUIGrid4ListPage(
      request,
      layout,
      magicFilter,
      this,
      userAccess = userAccess,
    )
      .add("accountIban", headerName = "plugins.banking.account.record.accountIban")
      .add("accountName", headerName = "plugins.banking.account.record.accountName")
      .add(lc, BankAccountRecordDO::date, BankAccountRecordDO::subject, BankAccountRecordDO::amount)

    /*layout.add(
      MenuItem(
        "skillmatrix.export",
        i18nKey = "exportAsXls",
        url = "${SkillMatrixServicesRest.REST_EXCEL_EXPORT_PATH}",
        type = MenuItemTargetType.DOWNLOAD
      )
    )*/
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: BankAccountRecord, userAccess: UILayout.UserAccess): UILayout {
    val layout = super.createEditLayout(dto, userAccess)
      .add(UIReadOnlyField("accountName", label="plugins.banking.account.entry.accountName"))
      .add(UIReadOnlyField("accountIban", label="plugins.banking.account.entry.accountIban"))
      .add(
        lc,
        BankAccountRecordDO::date,
        BankAccountRecordDO::valueDate,
        BankAccountRecordDO::iban,
        BankAccountRecordDO::bic,
        BankAccountRecordDO::subject,
        BankAccountRecordDO::amount,
        BankAccountRecordDO::collectionReference,
        BankAccountRecordDO::comment,
        BankAccountRecordDO::currency,
        BankAccountRecordDO::customerReference,
        BankAccountRecordDO::debteeId,
        BankAccountRecordDO::mandateReference,
        BankAccountRecordDO::receiverSender,
        BankAccountRecordDO::info,
        BankAccountRecordDO::type,
      )
    return LayoutUtils.processEditPage(layout, dto, this)
  }
}
