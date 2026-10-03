/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.humanresources.HRPlanningEntryStatus
import org.projectforge.common.i18n.Priority
import java.math.BigDecimal
import java.time.LocalDate

/**
 * One entry of a planned week: a project (or a status such as illness) and the hours planned for it.
 *
 * Two uses: a row of the entries of the edit page ([HRPlanning.entries]), and a row of the HR planning list,
 * which lists entries rather than weeks. Only for the latter the week's fields ([planningId], [user],
 * [week], ...) are filled ([copyFrom4ListRow]): within the edit page they are the planning's own.
 */
class HRPlanningEntry(
    var projektNameOrStatus: String? = null,
    var status: HRPlanningEntryStatus? = null,
    var projekt: Project? = null,
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
    var description: String? = null,
) : BaseDTO<HRPlanningEntryDO>() {
    /** The planning this entry belongs to: what a click on a list row opens. List rows only. */
    var planningId: Long? = null

    /** The employee of the planned week. List rows only. */
    var user: User? = null

    /** The Monday of the planned week. List rows only. */
    var week: LocalDate? = null

    /** The calendar week of [week], formatted. List rows only. */
    var formattedWeekOfYear: String? = null

    /** The name of the project's customer. List rows only. */
    var kunde: String? = null

    /** The planned hours of the whole week, i.e. of all entries of the planning. List rows only. */
    var planningTotalHours: BigDecimal? = null

    override fun copyFrom(src: HRPlanningEntryDO) {
        super.copyFrom(src)
        projektNameOrStatus = src.projektNameOrStatus
        totalHours = src.totalHours
    }

    /**
     * The entry plus the fields of its planned week, as a row of the HR planning list shows them.
     *
     * A grouped row (`HRPlanningEntryDao.groupEntries`) is no stored entry but the sums of a whole week, so
     * it carries no id: it gets its planning's, which is unique among the grouped rows, and the projects of
     * the week are its description already.
     */
    override fun copyFrom4ListRow(src: HRPlanningEntryDO) {
        copyFrom(src)
        val planning = src.planning ?: return
        if (id == null) {
            id = planning.id
        }
        planningId = planning.id
        user = planning.user?.let { User(it) }
        week = planning.week
        formattedWeekOfYear = planning.formattedWeekOfYear
        kunde = src.projekt?.kunde?.name
        planningTotalHours = planning.totalHours
    }
}
