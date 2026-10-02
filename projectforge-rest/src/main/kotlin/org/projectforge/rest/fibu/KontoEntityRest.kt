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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.EInvoiceSellerConfig
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.fibu.KontoStatus
import org.projectforge.business.fibu.kost.AccountingConfig
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.utils.IntRanges
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.Konto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the accounts ("Konten"), serving the hand-built projectforge-next page
 * (see components/features/account). Replaces the retired `KontoPagesRest`, whose server-side `UILayout`
 * moved onto the frontend, and the removed Wicket pages `KontoListPage`/`KontoEditPage`.
 *
 * The account pickers of other forms (invoice, creditor invoice, project, customer, accounting record) keep
 * using [getAccounts] & co. and the generic `autosearch`.
 */
@RestController
@RequestMapping("${Rest.URL}/account")
class KontoEntityRest
    : AbstractDTOEntityRest<KontoDO, Konto, KontoDao>(
        KontoDao::class.java,
        "fibu.konto.title") {

    @Autowired
    private lateinit var sellerConfig: EInvoiceSellerConfig

    /**
     * Feeds the generic `account/autosearch` (AbstractEntityRest.getAutoCompleteObjects) used by the
     * next frontend's account picker. Without it the endpoint throws and the picker stays empty — no
     * entry can be chosen and typing finds nothing. Mirrors the fields the custom `ac` endpoint searches.
     */
    override val autoCompleteSearchFields = arrayOf("nummer", "bezeichnung", "description")

    override fun transformFromDB(obj: KontoDO, editMode: Boolean): Konto {
        val konto = Konto()
        konto.copyFrom(obj)
        return konto
    }

    override fun transformForDB(dto: Konto): KontoDO {
        val kontoDO = KontoDO()
        dto.copyTo(kontoDO)
        return kontoDO
    }

    @GetMapping("ac")
    fun getAccounts(@RequestParam("search") search: String?): List<Konto> {
        return getAccounts(search)
    }

    @GetMapping("acDebitors")
    fun getDebitorAccounts(@RequestParam("search") search: String?): List<Konto> {
        return getAccounts(search, AccountingConfig.getInstance().debitorsAccountNumberRanges)
    }

    @GetMapping("acCreditors")
    fun getCreditorAccounts(@RequestParam("search") search: String?): List<Konto> {
        return getAccounts(search, AccountingConfig.getInstance().creditorsAccountNumberRanges)
    }

    /**
     * The seller's bank accounts (`EInvoiceSellerConfig.bankAccounts`), for the `sellerBankAccountName` select
     * of the e-invoice block. The value is the account's *name*, because that is what the column holds (as
     * Wicket's `KontoEditForm` stored it); the label adds the IBAN so equally named accounts stay apart.
     * Empty where the installation configured none.
     *
     * Read only, so the select access of the category is what has to be checked here.
     */
    @GetMapping("sellerBankAccounts")
    fun getSellerBankAccounts(): List<SellerBankAccount> {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        return sellerConfig.bankAccounts.map { SellerBankAccount(value = it.name, label = it.displayName) }
    }

    /** One entry of the `sellerBankAccountName` select, see [getSellerBankAccounts]. */
    class SellerBankAccount(val value: String, val label: String)

    private fun getAccounts(search: String?, accountRanges: IntRanges? = null): List<Konto> {
        val filter = BaseSearchFilter()
        filter.searchFields = arrayOf("nummer", "bezeichnung", "description")
        filter.searchString = search
        val list: List<KontoDO> = baseDao.select(filter)
        if (accountRanges == null) {
            return list.map { Konto(it) }
        }
        return list.filter { konto ->
            konto.status != KontoStatus.NONACTIVE && accountRanges.doesMatch(konto.nummer)
        }.map { Konto(it) }
    }
}
