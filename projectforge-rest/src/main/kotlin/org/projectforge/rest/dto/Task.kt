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

import org.projectforge.business.gantt.GanttObjectType
import org.projectforge.business.gantt.GanttRelationType
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskTree
import org.projectforge.common.i18n.Priority
import org.projectforge.common.task.TaskStatus
import org.projectforge.common.task.TimesheetBookingStatus
import org.projectforge.framework.persistence.api.BaseDO
import java.math.BigDecimal

class Task(id: Long? = null,
           displayName: String? = null,
           var parentTask: Task? = null,
           var title: String? = null,
           var status: TaskStatus? = null,
           var priority: Priority? = null,
           var shortDescription: String? = null,
           var description: String? = null,
           var progress: Int? = null,
           var maxHours: Int? = null,
           var startDate: java.util.Date? = null,
           var endDate: java.util.Date? = null,
           var duration: BigDecimal? = null,
           var protectTimesheetsUntil: java.util.Date? = null,
           var responsibleUser: User? = null,
           var reference: String? = null,
           var timesheetBookingStatus: TimesheetBookingStatus? = null,
           var kost2BlackWhiteList: String? = null,
           var kost2IsBlackList: Boolean? = null,
           var protectionOfPrivacy: Boolean? = null,
           var workpackageCode: String? = null,
           var ganttPredecessorOffset: Int? = null,
           var ganttRelationType: GanttRelationType? = null,
           var ganttObjectType: GanttObjectType? = null,
           var ganttPredecessor: Task? = null
) : BaseDTODisplayObject<TaskDO>(id, displayName = displayName) {

    /**
     * @see copyFromMinimal
     */
    constructor(src: TaskDO): this() {
        copyFromMinimal(src)
    }

    override fun copyFromMinimal(src: TaskDO) {
        super.copyFromMinimal(src)
        title = src.title
        if (src.parentTask != null) {
            parentTask = Task()
            parentTask?.copyFromMinimal(src.parentTask!!)
        }
    }

    companion object {
        fun getTask(taskId: Long?, minimal: Boolean = true): Task? {
            taskId ?: return null
            val taskDO = TaskTree.instance.getTaskById(taskId) ?: return null
            val task = Task()
            if (minimal) {
                task.copyFromMinimal(taskDO)
            } else {
                task.copyFrom(taskDO)
            }
            return task
        }
    }
}
