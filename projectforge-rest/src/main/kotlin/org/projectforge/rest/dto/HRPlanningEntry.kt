/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import org.projectforge.business.humanresources.HRPlanningEntryDO
import org.projectforge.common.i18n.Priority
import java.math.BigDecimal

class HRPlanningEntry(
        var projektNameOrStatus: String? = null,
        var priority: Priority? = null,
        var probability: Int? = null,
        var totalHours: BigDecimal? = null,
        var unassignedHours: BigDecimal? = null,
        var mondayHours: BigDecimal? = null,
        var tuesdayHours: BigDecimal? = null,
        var wednesdayHours: BigDecimal? = null,
        var thursdayHours: BigDecimal? = null,
        var fridayHours: BigDecimal? = null,
        var weekendHours: BigDecimal? = null,
        var description: String? = null
) : BaseDTO<HRPlanningEntryDO>() {
    var planning: HRPlanning? = null
    var project: Project? = null

    override fun copyFrom(src: HRPlanningEntryDO) {
        super.copyFrom(src)
        this.planning = src.planning?.let {
            HRPlanning(it)
        }
        this.project = src.projekt?.let {
            val projekt = Project()
            projekt.copyFrom(it)
            projekt
        }
        projektNameOrStatus = src.projektNameOrStatus
        totalHours = src.totalHours
        unassignedHours = src.unassignedHours
    }
}
