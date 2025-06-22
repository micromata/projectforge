/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Kost1
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired

@RestController
@RequestMapping("${Rest.URL}/cost1")
class Kost1PagesRest : AbstractDTOPagesRest<Kost1DO, Kost1, Kost1Dao>(Kost1Dao::class.java, "fibu.kost1.title") {
    @Autowired
    private lateinit var kostFormatter: KostFormatter

    override fun transformFromDB(obj: Kost1DO, editMode: Boolean): Kost1 {
        val kost1 = Kost1(obj)
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
    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
      layout.add(UITable.createUIResultSetTable()
                        .add(UITableColumn("formattedNumber", title = "fibu.kost1"))
                        .add(lc, "description", "kostentraegerStatus"))
    }

    override val classicsLinkListUrl: String?
        get() = "wa/cost1List"

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Kost1, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(UICustomized("cost.number"))
                                .add(lc, "description", "kostentraegerStatus")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun queryAutocompleteObjects(request: HttpServletRequest, filter: BaseSearchFilter): List<Kost1DO> {
        val list = super.queryAutocompleteObjects(request, filter)
        list.forEach { it.displayName = kostFormatter.formatKost1(it, KostFormatter.FormatType.TEXT) }
        return list
    }

    override val autoCompleteSearchFields = arrayOf("description", "nummer", "rawNumberString")
}
