/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Kost1
import org.projectforge.rest.dto.Kost2
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/kost2")
class Kost2Rest : AbstractDTORest<Kost2DO, Kost2, Kost2Dao>(Kost2Dao::class.java, "fibu.kost2.title") {
    override fun transformFromDB(obj: Kost2DO, editMode: Boolean): Kost2 {
        val kost2 = Kost2()
        kost2.copyFrom(obj)
        kost2.formattedNumber = KostFormatter.format(obj)
        return kost2
    }

    override fun transformForDB(dto: Kost2): Kost2DO {
        val kost2DO = Kost2DO()
        dto.copyTo(kost2DO)
        return kost2DO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "formattedNumber", "kost2Art", "kost2Art.fakturiert", "workFraction",
                                "projekt.kunde", "projekt", "kostentraegerStatus", "description", "comment"))
        layout.getTableColumnById("projekt").formatter = Formatter.PROJECT
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Kost2): UILayout {
        // TODO: EditPage needs customized component for the cost 2 id
        val layout = super.createEditLayout(dto)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "projekt", "workFraction", "description", "comment", "kostentraegerStatus")
                                .add(UILabel("TODO: Cost unit id"))))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
