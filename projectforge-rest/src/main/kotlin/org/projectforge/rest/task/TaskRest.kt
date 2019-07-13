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

package org.projectforge.rest.task

import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.favorites.Favorites
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Task
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/task")
class TaskRest
    : AbstractDTORest<TaskDO, Task, TaskDao>(
        TaskDao::class.java,
        "task.title") {

    @Autowired
    private lateinit var taskDao: AddressbookDao

    override fun transformFromDB(obj: TaskDO, editMode: Boolean): Task {
        val task = Task()
        task.copyFrom(obj)
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
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "title"))
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Task): UILayout {
        val layout = super.createEditLayout(dto)
                .add(lc, "parentTask", "title", "status", "priority", "responsibleUser", "shortDescription", "reference", "description")
        layout.add(UIRow().add(UICol().add(UIInput("protectTimesheetsUntil", lc, dataType = UIDataType.DATE))))
        Favorites.addTranslations(layout.translations)
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
