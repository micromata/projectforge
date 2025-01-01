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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.MenuItem
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.BankAccount
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import org.projectforge.rest.importer.AbstractImportPageRest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/bankAccount")
class BankAccountPagesRest : AbstractDTOPagesRest<BankAccountDO, BankAccount, BankAccountDao>(
    BankAccountDao::class.java,
    "plugins.banking.account.title",
    cloneSupport = AbstractPagesRest.CloneSupport.CLONE
) {
    override val autoCompleteSearchFields = arrayOf("name", "iban", "bic", "bank", "description")

    override fun newBaseDTO(request: HttpServletRequest?): BankAccount {
        val account = super.newBaseDTO(request)
        User.getUser(ThreadLocalUserContext.loggedInUserId)?.let {
            account.fullAccessUsers = listOf(it)
        }
        return account
    }

    override fun transformFromDB(obj: BankAccountDO, editMode: Boolean): BankAccount {
        val bankAccount = BankAccount()
        bankAccount.copyFrom(obj)
        // Group names needed by React client (for ReactSelect):
        Group.restoreDisplayNames(bankAccount.fullAccessGroups)
        Group.restoreDisplayNames(bankAccount.readonlyAccessGroups)
        Group.restoreDisplayNames(bankAccount.minimalAccessGroups)

        // Usernames needed by React client (for ReactSelect):
        User.restoreDisplayNames(bankAccount.fullAccessUsers)
        User.restoreDisplayNames(bankAccount.readonlyAccessUsers)
        User.restoreDisplayNames(bankAccount.minimalAccessUsers)

        return bankAccount
    }

    override fun transformForDB(dto: BankAccount): BankAccountDO {
        val bankAccountDO = BankAccountDO()
        dto.copyTo(bankAccountDO)
        return bankAccountDO
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
            .add(lc, BankAccountDO::name, BankAccountDO::bank, BankAccountDO::iban, BankAccountDO::description)

        layout.add(
            MenuItem(
                "banking.account.record.list",
                i18nKey = "plugins.banking.account.record.title.list",
                url = PagesResolver.getListPageUrl(BankAccountRecordPagesRest::class.java),
            )
        )
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: BankAccount, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
        dto.id?.let { id ->
            layout.add(
                UIDropArea(
                    "plugins.banking.account.importDropArea",
                    tooltip = "plugins.banking.account.importDropArea.tooltip",
                    uploadUrl = RestResolver.getRestUrl(BankingServicesRest::class.java, "import/$id"),
                )
            )
        }
        layout.add(
            lc,
            BankAccountDO::name,
            BankAccountDO::bank,
            BankAccountDO::iban,
            BankAccountDO::bic,
            BankAccountDO::description,
        )
            .add(
                UIFieldset(UILength(md = 12, lg = 12), title = "access.title.heading")
                    .add(
                        UIRow()
                            .add(
                                UIFieldset(6, title = "access.users")
                                    .add(
                                        UISelect.createUserSelect(
                                            lc,
                                            "fullAccessUsers",
                                            true,
                                            "plugins.banking.account.fullAccess"
                                        )
                                    )
                                    .add(
                                        UISelect.createUserSelect(
                                            lc,
                                            "readonlyAccessUsers",
                                            true,
                                            "plugins.banking.account.readonlyAccess"
                                        )
                                    )
                            )
                            .add(
                                UIFieldset(6, title = "access.groups")
                                    .add(
                                        UISelect.createGroupSelect(
                                            lc,
                                            "fullAccessGroups",
                                            true,
                                            "plugins.banking.account.fullAccess"
                                        )
                                    )
                                    .add(
                                        UISelect.createGroupSelect(
                                            lc,
                                            "readonlyAccessGroups",
                                            true,
                                            "plugins.banking.account.readonlyAccess"
                                        )
                                    )
                            )
                    )
            )
            .add(lc, BankAccountDO::importSettings)

        layout.add(AbstractImportPageRest.createSettingsHelp(BankingImportStorage(dto.importSettings).importSettings))
        layout.add(
            MenuItem(
                "banking.account.record.list",
                i18nKey = "plugins.banking.account.record.title.list",
                url = PagesResolver.getListPageUrl(
                    BankAccountRecordPagesRest::class.java,
                    params = mapOf("bankAccount" to dto.id)
                ),
            )
        )
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
