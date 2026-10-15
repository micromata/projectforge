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

package org.projectforge.rest.gantt

import org.projectforge.business.gantt.GanttObjectType
import org.projectforge.business.gantt.GanttRelationType
import java.math.BigDecimal
import java.time.LocalDate

/**
 * One node of the Gantt object tree as the projectforge-next editor holds it: the editable values of a
 * [org.projectforge.business.gantt.GanttTaskImpl] plus, read only, what the editor shows next to them.
 *
 * A positive [id] is the id of the ProjectForge task the node mirrors, a negative one marks an activity
 * that exists in this chart only (see `GanttTaskImpl.getNextId`). The predecessor travels by id, so a
 * predecessor outside the chart's task tree (an "external" one) survives the round trip.
 */
class GanttObject(
    var id: Long? = null,
    var title: String? = null,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    var duration: BigDecimal? = null,
    var progress: Int? = null,
    var predecessorId: Long? = null,
    var predecessorOffset: Int? = null,
    var relationType: GanttRelationType? = null,
    var type: GanttObjectType? = null,
    var workpackageCode: String? = null,
    var visible: Boolean = false,
    var children: MutableList<GanttObject>? = null,
) {
    /**
     * Display only: the title of the predecessor, which may live outside this tree.
     */
    var predecessorTitle: String? = null

    /**
     * The values of the mirrored ProjectForge task: the editor marks a differing value and offers to reject
     * it (restore the task's value) or to save it to the task. Null for a Gantt-only activity.
     */
    var task: TaskValues? = null

    /**
     * Display only, as calculated by the server (`GanttUtils`) from the predecessors and durations.
     */
    var calculatedStartDate: LocalDate? = null
    var calculatedEndDate: LocalDate? = null

    class TaskValues(
        var title: String? = null,
        var startDate: LocalDate? = null,
        var endDate: LocalDate? = null,
        var duration: BigDecimal? = null,
        var progress: Int? = null,
        var predecessorId: Long? = null,
        var predecessorTitle: String? = null,
        var predecessorOffset: Int? = null,
        var relationType: GanttRelationType? = null,
        var type: GanttObjectType? = null,
        /** Whether the logged-in user may write a value back to this task. */
        var updateAccess: Boolean = false,
    )
}
