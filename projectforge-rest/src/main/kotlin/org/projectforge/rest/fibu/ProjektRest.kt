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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Projekt
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/project")
class ProjektRest
    : AbstractDTORest<ProjektDO, Projekt, ProjektDao>(
        ProjektDao::class.java,
        "fibu.projekt.title") {

    override fun transformFromDB(obj: ProjektDO, editMode: Boolean): Projekt {
        val kunde = Projekt(null, false, null, null, null)
        kunde.copyFrom(obj)
        return kunde
    }

    override fun transformForDB(dto: Projekt): ProjektDO {
        val ProjektDO = ProjektDO()
        dto.copyTo(ProjektDO)
        return ProjektDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "nummer", "identifier", "kunde", "name", "kunde.division", "konto", "status",
                                "projektManagerGroup", "Kost2Art?", "description"))
        layout.getTableColumnById("konto").formatter = Formatter.KONTO
        layout.getTableColumnById("projektManagerGroup").formatter = Formatter.GROUP
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Projekt): UILayout {
        val konto = UIInput("konto", lc, tooltip = "fibu.kunde.konto.tooltip")

        val layout = super.createEditLayout(dto)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "nummer")
                                .add(UILabel("TODO: Customer selection"))
                                .add(UILabel("TODO: Koststellen"))
                                .add(konto)
                                .add(lc, "name", "identifier")
                                .add(UILabel("TODO: Structure Element"))
                                .add(lc, "projektManagerGroup", "projectManager", "headOfBusinessManager", "description")
                                .add(UILabel("TODO: Kost 2 Types"))))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
