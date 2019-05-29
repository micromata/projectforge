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
    override fun newBaseDO(request: HttpServletRequest): ToDoDO {
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
    override fun createEditLayout(dataObject: ToDoDO): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(lc, "subject")
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "type", "status", "dueDate"))
                        .add(UICol()
                                .add(lc, "priority", "assignee", "reporter")))
                .add(lc, "structureElement")

                .add(lc, "description", "comment", "options")
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
