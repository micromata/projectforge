/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Kost2Art
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/cost2Type")
class Kost2ArtPagesRest : AbstractDTOPagesRest<Kost2ArtDO, Kost2Art, Kost2ArtDao>(Kost2ArtDao::class.java, "fibu.kost2art.title") {
    override fun transformFromDB(obj: Kost2ArtDO, editMode: Boolean): Kost2Art {
        val kost2Art = Kost2Art()
        kost2Art.copyFrom(obj)
        return kost2Art
    }

    override fun transformForDB(dto: Kost2Art): Kost2ArtDO {
        val kost2ArtDO = Kost2ArtDO()
        dto.copyTo(kost2ArtDO)
        return kost2ArtDO
    }

    override val classicsLinkListUrl: String?
        get() = "wa/cost2TypeList"

    /**
     * LAYOUT List page
     */
    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
      layout.add(UITable.createUIResultSetTable()
                        .add(lc, "id", "name", "fakturiert", "workFraction",
                                "projektStandard", "description"))
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Kost2Art, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "id", "fakturiert", "projektStandard",                                     "name", "workFraction", "description")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
