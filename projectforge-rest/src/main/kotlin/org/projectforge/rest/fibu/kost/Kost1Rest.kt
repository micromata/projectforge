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
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Kost1
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/kost1")
class Kost1Rest : AbstractDTORest<Kost1DO, Kost1, Kost1Dao>(Kost1Dao::class.java, "fibu.kost1.title") {
    override fun transformFromDB(obj: Kost1DO, editMode: Boolean): Kost1 {
        val kost1 = Kost1()
        kost1.copyFrom(obj)
        kost1.formattedNumber = KostFormatter.format(obj)
        return kost1
    }

    override fun transformForDB(dto: Kost1): Kost1DO {
        val kost1DO = Kost1DO()
        dto.copyTo(kost1DO)
        return kost1DO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "formattedNumber", "description", "kostentraegerStatus"))
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Kost1, userAccess: UILayout.UserAccess): UILayout {
        // TODO: EditPage needs customized component for the cost 1 id
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "nummer", "description", "kostentraegerStatus")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
