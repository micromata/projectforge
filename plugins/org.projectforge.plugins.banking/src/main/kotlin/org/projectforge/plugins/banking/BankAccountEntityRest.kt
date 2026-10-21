/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.dto.BankAccount
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import org.projectforge.rest.importer.ImportSettingsHelp
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the bank accounts, serving the hand-built projectforge-next page (see
 * components/features/bank-account). Replaces the retired `BankAccountPagesRest`, whose server-side `UILayout`
 * (React) moved onto the frontend; the path `/rs/bankAccount` is unchanged.
 *
 * Only the finance group has access, the rights per account are given to users and groups ([BankAccountDao]).
 * The transactions of an account are imported on a page of their own ([BankAccountRecordImportRest]).
 */
@RestController
@RequestMapping("${Rest.URL}/bankAccount")
class BankAccountEntityRest : AbstractDTOEntityRest<BankAccountDO, BankAccount, BankAccountDao>(
    BankAccountDao::class.java,
    "plugins.banking.account.title",
    cloneSupport = AbstractEntityRest.CloneSupport.CLONE
) {
    override val autoCompleteSearchFields = arrayOf("name", "iban", "bic", "bank", "description")

    /** Name hits first, see [autoCompletePrimarySearchFields]. */
    override val autoCompletePrimarySearchFields = arrayOf("name")

    override fun newBaseDTO(request: HttpServletRequest?): BankAccount {
        val account = super.newBaseDTO(request)
        User.getUser(ThreadLocalUserContext.loggedInUserId)?.let {
            account.fullAccessUsers = listOf(it)
        }
        account.importSettingsHelp = ImportSettingsHelp.asMarkdown(BankingImportStorage().importSettings)
        return account
    }

    override fun transformFromDB(obj: BankAccountDO, editMode: Boolean): BankAccount {
        val bankAccount = BankAccount()
        bankAccount.copyFrom(obj)
        // The display names of the users and groups, shown by the multi selects of the form and the list.
        Group.restoreDisplayNames(bankAccount.fullAccessGroups)
        Group.restoreDisplayNames(bankAccount.readonlyAccessGroups)
        Group.restoreDisplayNames(bankAccount.minimalAccessGroups)
        User.restoreDisplayNames(bankAccount.fullAccessUsers)
        User.restoreDisplayNames(bankAccount.readonlyAccessUsers)
        User.restoreDisplayNames(bankAccount.minimalAccessUsers)
        if (editMode) {
            // The current settings with every importable field, for copy & paste into the settings field.
            bankAccount.importSettingsHelp =
                ImportSettingsHelp.asMarkdown(BankingImportStorage(obj.importSettings).importSettings)
        }
        return bankAccount
    }

    override fun transformForDB(dto: BankAccount): BankAccountDO {
        val bankAccountDO = BankAccountDO()
        dto.copyTo(bankAccountDO)
        return bankAccountDO
    }
}
