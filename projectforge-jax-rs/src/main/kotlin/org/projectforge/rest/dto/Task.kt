package org.projectforge.rest.dto

import org.projectforge.business.gantt.GanttObjectType
import org.projectforge.business.gantt.GanttRelationType
import org.projectforge.business.task.TaskDO
import org.projectforge.common.i18n.Priority
import org.projectforge.common.task.TaskStatus
import org.projectforge.common.task.TimesheetBookingStatus
import java.math.BigDecimal

class Task(var parentTask: Task? = null,
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
           var kost2IsBlackList: Boolean = false,
           var protectionOfPrivacy: Boolean = false,
           var workpackageCode: String? = null,
           var ganttPredecessorOffset: Int? = null,
           var ganttRelationType: GanttRelationType? = null,
           var ganttObjectType: GanttObjectType? = null,
           var ganttPredecessor: Task? = null
) : BaseObject<TaskDO>() {

    override fun copyFromMinimal(src: TaskDO) {
        super.copyFromMinimal(src)
        if (src is TaskDO) {
            title = src.title
            if (src.parentTask != null) {
                parentTask = Task()
                parentTask?.copyFromMinimal(src.parentTask)
            }
        }
    }
}
