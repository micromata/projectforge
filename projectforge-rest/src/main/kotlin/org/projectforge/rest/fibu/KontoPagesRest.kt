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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.fibu.KontoStatus
import org.projectforge.business.fibu.kost.AccountingConfig
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.utils.IntRanges
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Konto
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/account")
class KontoPagesRest
    : AbstractDTOPagesRest<KontoDO, Konto, KontoDao>(
        KontoDao::class.java,
        "fibu.konto.title") {

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

    override val classicsLinkListUrl: String?
        get() = "wa/accountList"

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

    private fun getAccounts(search: String?, accountRanges: IntRanges? = null): List<Konto> {
        val filter = BaseSearchFilter()
        filter.setSearchFields("nummer", "bezeichnung", "description")
        filter.searchString = search
        val list: List<KontoDO> = baseDao.getList(filter)
        if (accountRanges == null) {
            return list.map { Konto(it) }
        }
        return list.filter { konto ->
            konto.status != KontoStatus.NONACTIVE && accountRanges.doesMatch(konto.nummer)
        }.map { Konto(it) }
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
        layout.add(UITable.createUIResultSetTable()
                        .add(lc, "nummer", "status", "bezeichnung", "description"))
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Konto, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "nummer", "status", "bezeichnung", "description")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
