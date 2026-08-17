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

package org.projectforge.rest.task

import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.favorites.Favorites
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Task
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/task")
class TaskPagesRest
    : AbstractDTOPagesRest<TaskDO, Task, TaskDao>(
        TaskDao::class.java,
        "task.title") {

    override fun transformFromDB(obj: TaskDO, editMode: Boolean): Task {
        val task = Task()
        task.copyFrom(obj)
        if (editMode) {
            // Only the edit page asks, and only it can afford it: this method runs per list row as well, and
            // hasAccessForKost2AndTimesheetBookingStatus resolves the project through the task tree and looks
            // up the groups of the user. writeAccess/deleteAccess are not filled here: they are the same for
            // every entity and come from AbstractEntityRest.getById, see EntityAccessSupport.
            val user = ThreadLocalUserContext.loggedInUser
            task.kost2AndBookingStatusWriteAccess = baseDao.hasAccessForKost2AndTimesheetBookingStatus(user, obj)
            task.protectTimesheetsUntilWriteAccess =
                accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP)
        }
        return task
    }

    /**
     * A new task is created below a parent, and that parent decides its rights: with no id of its own,
     * `TaskDao.hasAccessForKost2AndTimesheetBookingStatus` falls back to `parentTaskId` to resolve the
     * project (Wicket passes the parent explicitly, see `TaskEditForm.onBeforeRender`). Without the parent
     * a project assistant would see the kost2 fields disabled although the DAO would accept his save.
     *
     * The parameter is named as in Wicket (`TaskEditPage.PARAM_PARENT_TASK_ID`).
     */
    override fun newBaseDO(request: HttpServletRequest?): TaskDO {
        val task = super.newBaseDO(request)
        val parentTaskId = NumberHelper.parseLong(request?.getParameter("parentTaskId"))
        if (parentTaskId != null) {
            baseDao.setParentTask(task, parentTaskId)
        }
        return task
    }

    override fun transformForDB(dto: Task): TaskDO {
        val taskDO = TaskDO()
        dto.copyTo(taskDO)
        return taskDO
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: Task) {
        /* if (StringUtils.isAllBlank(obj.name, obj.firstName, obj.organization)) {
             validationErrors.add(ValidationError(translate("address.form.error.toFewFields"), fieldId = "name"))
         }*/
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
        layout.add(UITable.createUIResultSetTable()
                        .add(lc, "title"))
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Task, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(
                    lc, "parentTask", "title", "status", "priority", "responsibleUser", "shortDescription",
                    "reference", "description", "protectTimesheetsUntil"
                )
        Favorites.addTranslations(layout.translations)
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
