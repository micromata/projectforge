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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.NextMigration
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.security.My2FAShortCut
import org.projectforge.web.WicketSupport

private val log = KotlinLogging.logger {}

/**
 *
 * @author Kai Reinhard
 */
class BankingPlugin : AbstractPlugin(
    PluginAdminService.PLUGIN_BANKING_ID,
    "Banking",
    "You may import and browse your bank accounts here."
) {

    override fun initialize() {
        val bankAccountDao = WicketSupport.get(BankAccountDao::class.java)
        val menuCreator = WicketSupport.get(MenuCreator::class.java)
        // Register it:
        register(id, BankAccountDao::class.java, bankAccountDao, "plugins.banking")
        // Bank accounts and their records are for the finance group only (BankAccountDao). The import of the records
        // (/rs/bankAccountRecordImport) doesn't pass the save of /rs/bankAccountRecord, so WRITE:bankAccountRecord
        // doesn't catch it.
        registerShortCutValues(My2FAShortCut.FINANCE_WRITE, "WRITE:bankAccount;WRITE:bankAccountRecord")
        registerShortCutClasses(
            My2FAShortCut.FINANCE_WRITE,
            BankAccountRecordImportRest::class.java,
        )
        registerShortCutClasses(
            My2FAShortCut.FINANCE,
            BankAccountEntityRest::class.java,
            BankAccountRecordEntityRest::class.java,
            BankAccountRecordImportRest::class.java,
        )

        menuCreator.register(
            MenuItemDefId.FIBU,
            MenuItemDef(info.id,
                "plugins.banking.menu",
                NextMigration.listUrl(BANK_ACCOUNT_CATEGORY),
                checkAccess =
                { bankAccountDao.hasLoggedInUserSelectAccess(false) }),
        );

        // All the i18n stuff:
        addResourceBundle(RESOURCE_BUNDLE_NAME)
    }

    companion object {
        const val RESOURCE_BUNDLE_NAME = "BankingI18nResources"

        /** The REST category and next route of the bank accounts (see [NextMigration]). */
        const val BANK_ACCOUNT_CATEGORY = "bankAccount"
    }
}
