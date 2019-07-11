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

import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Konto
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/konto")
class KontoRest
    : AbstractDTORest<KontoDO, Konto, KontoDao, BaseSearchFilter>(
        KontoDao::class.java,
        BaseSearchFilter::class.java,
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

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "nummer", "status", "bezeichnung", "description"))
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Konto): UILayout {
        val layout = super.createEditLayout(dto)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "nummer", "status", "bezeichnung", "description")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
