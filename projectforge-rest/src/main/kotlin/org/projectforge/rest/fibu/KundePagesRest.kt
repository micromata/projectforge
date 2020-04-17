/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.rest.AddressServicesRest
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Konto
import org.projectforge.rest.dto.Kunde
import org.projectforge.rest.orga.Contract
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct

@RestController
@RequestMapping("${Rest.URL}/customer")
class KundePagesRest
    : AbstractDTOPagesRest<KundeDO, Kunde, KundeDao>(
        KundeDao::class.java,
        "fibu.kunde.title") {

    @PostConstruct
    private fun postConstruct() {
        JacksonConfiguration.registerAllowedUnknownProperties(Kunde::class.java, "statusAsString")
    }


    override fun transformFromDB(obj: KundeDO, editMode: Boolean): Kunde {
        val kunde = Kunde()
        kunde.copyFrom(obj)
        return kunde
    }

    override fun transformForDB(dto: Kunde): KundeDO {
        val kundeDO = KundeDO()
        dto.copyTo(kundeDO)
        return kundeDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.createUIResultSetTable()
                        .add(UITableColumn("kost", title = "fibu.kunde.nummer"))
                        .add(lc, "identifier", "name", "division", "konto", "statusAsString", "description"))
        layout.getTableColumnById("konto").formatter = Formatter.KONTO
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Kunde, userAccess: UILayout.UserAccess): UILayout {
        val kto = UIInput("konto", lc, tooltip = "fibu.kunde.konto.tooltip")
        val konto = UISelect<Konto>("konto", lc,
                autoCompletion = AutoCompletion<Konto>(url = "account/acDebitors?search=:search"))


        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "nummer", "name")
                                .add(konto)
                                .add(lc, "identifier", "division", "description", "status")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override val autoCompleteSearchFields = arrayOf("name", "identifier")
}
