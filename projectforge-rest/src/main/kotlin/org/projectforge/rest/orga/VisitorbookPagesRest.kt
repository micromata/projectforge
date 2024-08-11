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

package org.projectforge.rest.orga

import de.micromata.genome.db.jpa.tabattr.api.TimeableService
import org.projectforge.business.orga.VisitorbookDO
import org.projectforge.business.orga.VisitorbookDao
import org.projectforge.business.orga.VisitorbookTimedDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Visitorbook
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/visitorBook")
class VisitorbookPagesRest : AbstractDTOPagesRest<VisitorbookDO, Visitorbook, VisitorbookDao>(VisitorbookDao::class.java, "orga.visitorbook.title") {

    @Autowired
    private val timeableService: TimeableService? = null

    override fun transformForDB(dto: Visitorbook): VisitorbookDO {
        val visitorbookDO = VisitorbookDO()
        dto.copyTo(visitorbookDO)
        return visitorbookDO
    }

    override fun transformFromDB(obj: VisitorbookDO, editMode: Boolean): Visitorbook {
        val visitorbook = Visitorbook()
        visitorbook.copyFrom(obj)

        val timeableAttributes = timeableService!!.getTimeableAttrRowsForGroupName<Int, VisitorbookTimedDO>(obj, "timeofvisit")
        if (timeableAttributes != null && timeableAttributes.size > 0) {
            val sortedList = timeableService.sortTimeableAttrRowsByDateDescending(timeableAttributes)
            val newestEntry = sortedList[0]
            visitorbook.arrive = if (newestEntry.getAttribute("arrive") != null) newestEntry.getAttribute("arrive", String::class.java) else ""
            visitorbook.depart = if (newestEntry.getAttribute("depart") != null) newestEntry.getAttribute("depart", String::class.java) else ""
        }

        return visitorbook
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
      layout.add(UITable.createUIResultSetTable()
                        .add(lc, "id", "lastname", "firstname", "company", "visitortype")
                        .add(UITableColumn("arrive", title = "orga.visitorbook.arrive"))
                        .add(UITableColumn("depart", title = "orga.visitorbook.depart"))
                        .add(lc, "contactPersons"))
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Visitorbook, userAccess: UILayout.UserAccess): UILayout {
        val firstname = UIInput("firstname", lc) // Input-field instead of text-area (length > 255)
        val lastname = UIInput("lastname", lc)
        val company = UIInput("company", lc)
        val layout = super.createEditLayout(dto, userAccess)
                .add(firstname)
                .add(lastname)
                .add(company)
                .add(UISelect.createUserSelect(lc, "contactPersons", true, "orga.visitorbook.contactPerson"))
                .add(lc, "visitortype")
                .add(UILabel("TODO: Visitor Time Editor"))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
