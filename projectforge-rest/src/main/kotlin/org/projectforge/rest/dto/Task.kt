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

import org.projectforge.business.PfCaches
import org.projectforge.business.gantt.GanttObjectType
import org.projectforge.business.gantt.GanttRelationType
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskFormatter
import org.projectforge.business.task.TaskTree
import org.projectforge.common.i18n.Priority
import org.projectforge.common.task.TaskStatus
import org.projectforge.common.task.TimesheetBookingStatus
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.rest.task.Consumption
import org.projectforge.rest.task.TaskServicesRest
import java.math.BigDecimal
import java.time.LocalDate

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
           var startDate: LocalDate? = null,
           var endDate: LocalDate? = null,
           var duration: BigDecimal? = null,
           var protectTimesheetsUntil: LocalDate? = null,
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
) : BaseDTODisplayObject<TaskDO>(id, displayName = displayName), EntityAccessSupport {

    override var writeAccess: Boolean? = null
    override var deleteAccess: Boolean? = null

    /**
     * Whether `kost2BlackWhiteList`, `kost2IsBlackList` and `timesheetBookingStatus` may be changed:
     * `TaskDao.hasAccessForKost2AndTimesheetBookingStatus`, i.e. the finance group or the project
     * manager/assistant of the project of this task. `TaskDao.checkInsertAccess`/`checkUpdateAccess`
     * refuse a change with `task.error.kost2Readonly` /
     * `task.error.timesheetBookingStatus2Readonly` for anybody else.
     *
     * One flag per *rule*, not per field - the DAO knows exactly two, and Wicket disables exactly
     * these two groups (`TaskEditForm.onBeforeRender`).
     */
    var kost2AndBookingStatusWriteAccess: Boolean = false

    /**
     * Whether `protectTimesheetsUntil` and `protectionOfPrivacy` may be changed: membership of the
     * finance group, refused otherwise with `task.error.protectTimesheetsUntilReadonly` /
     * `task.error.protectionOfPrivacyReadonly`.
     */
    var protectTimesheetsUntilWriteAccess: Boolean = false

    /**
     * The consumption bar of a list row: booked hours against `maxHours`, as the tree's column shows it.
     * Only filled by [copyFrom4ListRow] — computed from the task tree, not a property of `TaskDO`.
     */
    var consumption: Consumption? = null

    /**
     * The cost units of a list row in wild card form, e. g. `5.123.45.*`. Only filled by
     * [copyFrom4ListRow], see [consumption].
     */
    var kost2WildCard: String? = null

    /**
     * The cost units of a list row, one formatted number per line — the tooltip of the kost2 column. Only
     * filled by [copyFrom4ListRow], see [consumption].
     */
    var kost2ListAsLines: String? = null

    /**
     * The path to the root as "Micromata -> Business Unit -> ProjectForge", shown as the tooltip of the
     * structure element column — the list counterpart of the tree's path (`TaskFormatter.getTaskPath`).
     * Only filled by [copyFrom4ListRow], see [consumption]; on a nested task of a time sheet row it is the
     * *parent* path (see [Timesheet.copyFrom4ListRow]).
     */
    var path: String? = null

    /**
     * The orders having a position booked against this task, for the list's `Aufträge` column. Only filled
     * by [copyFrom4ListRow], see [consumption].
     */
    var orderList: List<TaskServicesRest.Order>? = null

    /**
     * @see copyFromMinimal
     */
    constructor(src: TaskDO): this() {
        copyFromMinimal(src)
    }

    /**
     * The lean row of the hand built next list: the ten columns of `task.page.tsx` and nothing else, so
     * `JsonInclude.Include.NON_NULL` keeps the rest off the wire (see [BaseDTO.copyFrom4ListRow]).
     *
     * What [copyFrom] would add and no column reads: [description] and [parentTask]/[ganttPredecessor] as
     * whole nested tasks, the kost2 black/white list, the Gantt fields, [progress], [maxHours], the two
     * dates and the access flags — the last ones being the edit form's business (see
     * [kost2AndBookingStatusWriteAccess], filled only in edit mode by `TaskPagesRest.transformFromDB`,
     * which this path does not run through).
     *
     * Three of the ten columns are not properties of `TaskDO` at all and are computed here, from the task
     * tree the tree perspective computes them from — the same functions, so both perspectives can only
     * ever show the same value. It costs no query: the tree is held in memory and caches the order
     * positions by task id. [TaskServicesRest.addKost2List] is called without the cost unit objects, which
     * only the tree's picker needs.
     */
    override fun copyFrom4ListRow(src: TaskDO) {
        // Not [copyFromMinimal]: that one fills [parentTask] as a nested task, and no column of the list
        // reads it (the path to the root is the tree perspective's business).
        id = src.id
        deleted = src.deleted
        displayName = src.displayName
        // Two columns every next list offers, hidden until the user switches them on
        // (`lib/page-def/audit-columns.ts`).
        copyAuditFieldsFrom(src)
        title = src.title
        shortDescription = src.shortDescription
        protectTimesheetsUntil = src.protectTimesheetsUntil
        reference = src.reference
        priority = src.priority
        status = src.status
        // The display name only, from the cache — the column shows a name, not a user entity.
        responsibleUser = PfCaches.instance.getUser(src.responsibleUserId)?.let {
            User(id = it.id, displayName = it.displayName)
        }
        val id = src.id ?: return
        val node = TaskTree.instance.getTaskNodeById(id) ?: return
        consumption = Consumption.create(node)
        val serviceTask = TaskServicesRest.Task(id)
        // Non-recursive: the list column, like the tree's, shows a task's own cost units, not the ones it
        // inherits (see TaskServicesRest.addKost2List). Display only — no bookable object list here.
        TaskServicesRest.addKost2List(serviceTask, includeKost2ObjectList = false, recursive = false)
        TaskServicesRest.addOrderList(serviceTask)
        kost2WildCard = serviceTask.kost2WildCard
        kost2ListAsLines = serviceTask.kost2ListAsLines
        orderList = serviceTask.orderList
        // The whole path including this task, as the Wicket list shows it in the title cell's tooltip
        // (`WicketTaskFormatter.appendFormattedTask(..., showPathAsTooltip = true)`).
        path = TaskFormatter.getTaskPath(id)
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
