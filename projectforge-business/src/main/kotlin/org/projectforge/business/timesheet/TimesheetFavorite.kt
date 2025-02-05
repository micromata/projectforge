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

package org.projectforge.business.timesheet

import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.favorites.AbstractFavorite
import org.projectforge.framework.persistence.user.entities.PFUserDO

class TimesheetFavorite(
    name: String? = null,
    id: Long = 0,
    var taskId: Long? = null,
    var userId: Long? = null,
    var location: String? = null,
    var tag: String? = null,
    var reference: String? = null,
    var description: String? = null,
    var cost2Id: Long? = null,
    // Don't copy these values to the timesheet. The user should enter them manually.
    // var timeSavedByAI: BigDecimal? = null,
    // var timeSavedByAIUnit: TimesheetDO.TimeSavedByAIUnit? = null,
    // var timeSavedByAIDescription: String? = null,
) : AbstractFavorite(name, id) {

    fun fillFromTimesheet(timesheet: TimesheetDO) {
        if (timesheet.taskId != null) {
            taskId = timesheet.taskId
        }
        if (timesheet.userId != null) {
            userId = timesheet.userId
        }
        if (!timesheet.location.isNullOrBlank()) {
            location = timesheet.location
        }
        tag = timesheet.tag
        if (!timesheet.reference.isNullOrBlank()) {
            reference = timesheet.reference
        }
        if (!timesheet.description.isNullOrBlank()) {
            description = timesheet.description
        }
        if (timesheet.kost2Id != null) {
            cost2Id = timesheet.kost2Id
        }
        // Don't copy these values to the timesheet. The user should enter them manually.
        /*if (timesheet.timeSavedByAI != null) {
            timeSavedByAI = timesheet.timeSavedByAI
        }
        if (timesheet.timeSavedByAIUnit != null) {
          timeSavedByAIUnit = timesheet.timeSavedByAIUnit
        }
        if (!timesheet.timeSavedByAIDescription.isNullOrBlank()) {
          timeSavedByAIDescription = timesheet.timeSavedByAIDescription
        }*/
    }

    fun copyToTimesheet(timesheet: TimesheetDO) {
        if (taskId != null) {
            val task = TaskDO()
            task.id = taskId
            timesheet.task = task
        }
        if (userId != null) {
            val user = PFUserDO()
            user.id = userId
            timesheet.user = user
        }
        timesheet.location = location ?: ""
        timesheet.tag = tag ?: "" // Overwrite client's value.
        timesheet.reference = reference ?: ""
        timesheet.description = description ?: ""
        // timesheet.timeSavedByAI = timeSavedByAI
        // timesheet.timeSavedByAIUnit = timeSavedByAIUnit
        // timesheet.timeSavedByAIDescription = timeSavedByAIDescription ?: ""
        if (cost2Id != null) {
            val cost2 = Kost2DO()
            cost2.id = cost2Id
            timesheet.kost2 = cost2
        }
    }
}
