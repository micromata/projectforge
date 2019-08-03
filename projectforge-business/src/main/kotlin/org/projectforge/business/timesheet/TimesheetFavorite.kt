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

package org.projectforge.business.timesheet

import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.favorites.AbstractFavorite
import org.projectforge.framework.persistence.user.entities.PFUserDO

class TimesheetFavorite(
        name: String? = null,
        id: Int = 0,
        var taskId: Int? = null,
        var userId: Int? = null,
        var location: String? = null,
        var description: String? = null,
        var cost2Id: Int? = null)
    : AbstractFavorite(name, id) {

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
        if (!timesheet.description.isNullOrBlank()) {
            description = timesheet.description
        }
        if (timesheet.kost2Id != null) {
            cost2Id = timesheet.kost2Id
        }
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
        if (!location.isNullOrBlank()) {
            timesheet.location = location
        }
        if (!description.isNullOrBlank()) {
            timesheet.description = description
        }
        if (cost2Id != null) {
            val cost2 = Kost2DO()
            cost2.id = cost2Id
            timesheet.kost2 = cost2
        }
    }
}
