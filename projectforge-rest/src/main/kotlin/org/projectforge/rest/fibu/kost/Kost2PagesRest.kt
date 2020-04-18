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

package org.projectforge.rest.fibu.kost

import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Kost2
import org.projectforge.rest.dto.Customer
import org.projectforge.rest.dto.Project
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/cost2")
class Kost2PagesRest : AbstractDTOPagesRest<Kost2DO, Kost2, Kost2Dao>(Kost2Dao::class.java, "fibu.kost2.title") {

    override fun transformFromDB(obj: Kost2DO, editMode: Boolean): Kost2 {
        val kost2 = Kost2()
        kost2.copyFrom(obj)
        kost2.formattedNumber = KostFormatter.format(obj)
        if(obj.projekt != null){
            kost2.project = Project()
            kost2.project!!.copyFrom(obj.projekt!!)
            if(obj.projekt!!.kunde != null){
                kost2.project!!.customer = Customer()
                kost2.project!!.customer!!.copyFrom(obj.projekt!!.kunde!!)
            }
        }
        if(obj.kost2Art != null){
            kost2.kost2Art!!.copyFrom(obj.kost2Art!!)
        }
        return kost2
    }

    override fun transformForDB(dto: Kost2): Kost2DO {
        val kost2DO = Kost2DO()
        dto.copyTo(kost2DO)
        return kost2DO
    }

    override val classicsLinkListUrl: String?
        get() = "wa/cost2List"

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.createUIResultSetTable()
                        .add(UITableColumn("formattedNumber", title = "fibu.kost2"))
                        .add(UITableColumn("kost2Art.name", title = "fibu.kost2.art"))
                        .add(lc, "kost2Art.fakturiert", "workFraction", "projekt.kunde.name", "projekt.name", "kostentraegerStatus", "description", "comment"))
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Kost2, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(UISelect.createProjectSelect(lc, "projekt", false, "fibu.projekt"))
                                .add(UICustomized("cost.number"))
                                .add(lc, "workFraction", "description", "comment", "kostentraegerStatus")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
