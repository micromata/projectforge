/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.hr

import org.projectforge.business.humanresources.HRPlanningEntryDO
import org.projectforge.business.humanresources.HRPlanningEntryDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.HRPlanningEntry
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/hrPlanningList")
class HRPlanningListPagesRest : AbstractDTOPagesRest<HRPlanningEntryDO, HRPlanningEntry, HRPlanningEntryDao>(HRPlanningEntryDao::class.java, "hr.planning.title") {
    override fun transformFromDB(obj: HRPlanningEntryDO, editMode: Boolean): HRPlanningEntry {
        val hrPlanningEntry = HRPlanningEntry()
        hrPlanningEntry.copyFrom(obj)
        return hrPlanningEntry
    }

    override fun transformForDB(dto: HRPlanningEntry): HRPlanningEntryDO {
        val hrPlanningEntryDO = HRPlanningEntryDO()
        dto.copyTo(hrPlanningEntryDO)
        return hrPlanningEntryDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
      layout.add(UITable.createUIResultSetTable()
                        .add(lc, "planning.user", "planning.week")
                        .add(UITableColumn("planning.formattedWeekOfYear", "calendar.weekOfYearShortLabel"))
                        .add(lc, "projekt.kunde.name")
                        .add(UITableColumn("projektNameOrStatus", "fibu.projekt"))
                        .add(lc, "priority", "probability")
                        .add(UITableColumn("planning.totalHours", "hr.planning.total"))
                        .add(UITableColumn("totalHours", "hr.planning.sum"))
                        .add(lc, "unassignedHours", "mondayHours", "tuesdayHours", "wednesdayHours", "thursdayHours", "fridayHours", "weekendHours", "description"))
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: HRPlanningEntry, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(UILabel("TODO"))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
