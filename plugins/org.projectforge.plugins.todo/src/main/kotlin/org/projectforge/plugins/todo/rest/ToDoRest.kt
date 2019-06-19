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

package org.projectforge.plugins.todo.rest

import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.plugins.todo.ToDoDO
import org.projectforge.plugins.todo.ToDoDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/todo")
class ToDoRest() : AbstractDORest<ToDoDO, ToDoDao, BaseSearchFilter>(ToDoDao::class.java, BaseSearchFilter::class.java, "plugins.todo.title") {
    /**
     * Initializes new toDos for adding.
     */
    override fun newBaseDO(request: HttpServletRequest?): ToDoDO {
        val toDo = super.newBaseDO(request)
        return toDo
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "created", "modified", "subject", "assignee", "reporter", "dueDate", "status",
                                "priority", "type", "structureElement", "group", "description"))
        layout.getTableColumnById("group").formatter = Formatter.GROUP
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: ToDoDO): UILayout {
        val layout = super.createEditLayout(dto)
                .add(lc, "subject")
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "type", "status", "dueDate"))
                        .add(UICol()
                                .add(lc, "priority", "assignee", "reporter")))
                .add(lc, "structureElement")

                .add(lc, "description", "comment", "options")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
