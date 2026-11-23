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
import org.projectforge.business.fibu.KontoCustomerDataSheetPdfExport
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.fibu.KontoStatus
import org.projectforge.business.fibu.kost.AccountingConfig
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.utils.IntRanges
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.Konto
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

    @Autowired
    private lateinit var customerDataSheetPdfExport: KontoCustomerDataSheetPdfExport

    /**
     * Feeds the generic `account/autosearch` (AbstractEntityRest.getAutoCompleteObjects) used by the
     * next frontend's account picker. Without it the endpoint throws and the picker stays empty — no
     * entry can be chosen and typing finds nothing. Mirrors the fields the custom `ac` endpoint searches.
     */
    override val autoCompleteSearchFields = arrayOf("nummer", "bezeichnung", "description")

    /** Name hits first, see [autoCompletePrimarySearchFields]. */
    override val autoCompletePrimarySearchFields = arrayOf("nummer", "bezeichnung")

    /**
     * The kind of account (debitors, creditors, general ledger accounts) as a pill of the list, told apart by
     * the configured number ranges (see [KontoType]). Synthetic: no property of the account holds it.
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterListElement(
                TYPE_FILTER,
                label = translate("fibu.konto.filter.accountType"),
                defaultFilter = true,
                values = listOf(
                    UISelectValue(KontoType.DEBITOR.name, translate("fibu.konto.filter.debitors")),
                    UISelectValue(KontoType.CREDITOR.name, translate("fibu.konto.filter.creditors")),
                    UISelectValue(KontoType.GENERAL_LEDGER.name, translate("fibu.konto.filter.generalLedger")),
                ),
            )
        )
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<KontoDO>>? {
        val entry = source.entries.find { it.field == TYPE_FILTER } ?: return null
        entry.synthetic = true
        val types = entry.value.values?.mapNotNull { KontoType.entries.find { type -> type.name == it } }
        if (types.isNullOrEmpty()) {
            return null
        }
        val config = AccountingConfig.getInstance()
        return listOf(
            KontoTypeFilter(
                types.toSet(),
                KontoType.rangesOf(config.debitorsAccountNumberRangesString),
                KontoType.rangesOf(config.creditorsAccountNumberRangesString),
            )
        )
    }

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

    @AccessChecked("DAO: KontoDao.select")
    @GetMapping("ac")
    fun getAccounts(@RequestParam("search") search: String?): List<Konto> {
        return getAccounts(search)
    }

    @AccessChecked("DAO: KontoDao.select")
    @GetMapping("acDebitors")
    fun getDebitorAccounts(@RequestParam("search") search: String?): List<Konto> {
        return getAccounts(search, AccountingConfig.getInstance().debitorsAccountNumberRanges)
    }

    @AccessChecked("DAO: KontoDao.select")
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
    @AccessChecked("DAO: select access (hasLoggedInUserSelectAccess)")
    @GetMapping("sellerBankAccounts")
    fun getSellerBankAccounts(): List<SellerBankAccount> {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        return sellerConfig.bankAccounts.map { SellerBankAccount(value = it.name, label = it.displayName) }
    }

    /**
     * The customer data sheet of the account as PDF (see [KontoCustomerDataSheetPdfExport]): the invoice data of the
     * account to send to the customer for checking.
     *
     * Read only, so a GET; [baseDao] `find` checks the read access of the logged-in user and throws without it.
     */
    @AccessChecked("DAO: find access (KontoDao.find checks read access)")
    @GetMapping("customerDataSheet/{id}")
    fun getCustomerDataSheet(@PathVariable("id") id: Long): ResponseEntity<*> {
        val konto = baseDao.find(id) ?: return ResponseEntity.notFound().build<Any>()
        return RestUtils.downloadFile(customerDataSheetPdfExport.filename(konto), customerDataSheetPdfExport.export(konto))
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

    companion object {
        /** Name of the synthetic filter entry of the account list. */
        private const val TYPE_FILTER = "accountType"
    }
}
