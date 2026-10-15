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

package org.projectforge.plugins.todo.rest

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.NextMigration
import org.projectforge.common.i18n.Priority
import org.projectforge.plugins.todo.ToDoDO
import org.projectforge.plugins.todo.ToDoDao
import org.projectforge.plugins.todo.ToDoPlugin
import org.projectforge.plugins.todo.ToDoStatus
import org.projectforge.plugins.todo.ToDoType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateFieldDeclaration
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.rest.multiselect.TextFieldModification
import org.projectforge.ui.LayoutContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable

private val log = KotlinLogging.logger {}

/**
 * Mass update of to-dos after selection in the next list (`todo.page.tsx`): status, priority, type, assignee,
 * due and resubmission date, task, group, description and comment; and soft deletion/restoring.
 *
 * Status and assignee may only be set (a to-do always has both, see [ToDoEntityRest.validate]). A to-do whose
 * assignee, status or deletion state changes notifies by e-mail, the rule of a single save
 * ([ToDoEntityRest.sendNotification]).
 */
@RestController
@RequestMapping("${Rest.URL}/${ToDoPlugin.CATEGORY}${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class ToDoMultiSelectedPageRest : AbstractMultiSelectedPage<ToDoDO>() {

    @Autowired
    private lateinit var toDoDao: ToDoDao

    @Autowired
    private lateinit var toDoEntityRest: ToDoEntityRest

    override val layoutContext: LayoutContext = LayoutContext(ToDoDO::class.java)

    override val listPageUrl: String = "/${NextMigration.listUrl(ToDoPlugin.CATEGORY)}"

    @PostConstruct
    private fun postConstruct() {
        pagesRest = toDoEntityRest
    }

    override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
        return listOf(
            MassUpdateFieldDeclaration("status", showDeleteOption = false),
            MassUpdateFieldDeclaration("priority"),
            MassUpdateFieldDeclaration("type"),
            MassUpdateFieldDeclaration("assignee", showDeleteOption = false),
            MassUpdateFieldDeclaration("dueDate"),
            MassUpdateFieldDeclaration("resubmission"),
            MassUpdateFieldDeclaration("task"),
            MassUpdateFieldDeclaration("group"),
            MassUpdateFieldDeclaration("description"),
            MassUpdateFieldDeclaration("comment", showAppendOption = true),
        )
    }

    /** A reference posts its id only, which [MassUpdateParameter.hasAction] doesn't count as value. */
    override fun checkParamHasAction(
        params: Map<String, MassUpdateParameter>,
        param: MassUpdateParameter,
        field: String,
    ): Boolean {
        if (field == "assignee") {
            return param.id != null
        }
        if (field == "task" || field == "group") {
            return param.id != null || param.delete == true
        }
        return super.checkParamHasAction(params, param, field)
    }

    override fun proceedMassUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<ToDoDO>,
    ): ResponseEntity<*>? {
        val todos = toDoDao.select(selectedIds)
        if (todos.isNullOrEmpty()) {
            return null
        }
        val params = massUpdateContext.massUpdateParams
        todos.forEach { todo ->
            val oldAssigneeId = todo.assigneeId
            val oldStatus = todo.status
            massUpdateContext.startUpdate(todo)
            processEnum(params["status"]) { todo.status = it?.let { ToDoStatus.valueOf(it) } }
            processEnum(params["priority"]) { todo.priority = it?.let { Priority.valueOf(it) } }
            processEnum(params["type"]) { todo.type = it?.let { ToDoType.valueOf(it) } }
            params["assignee"]?.id?.let { toDoDao.setAssignee(todo, it) }
            params["dueDate"]?.let { param ->
                if (param.delete == true) todo.dueDate = null else param.localDateValue?.let { todo.dueDate = it }
            }
            params["resubmission"]?.let { param ->
                if (param.delete == true) todo.resubmission = null else param.localDateValue?.let { todo.resubmission = it }
            }
            params["task"]?.let { param ->
                if (param.delete == true) todo.task = null else param.id?.let { toDoDao.setTask(todo, it) }
            }
            params["group"]?.let { param ->
                if (param.delete == true) todo.group = null else param.id?.let { toDoDao.setGroup(todo, it) }
            }
            TextFieldModification.processTextParameter(todo, "description", params)
            TextFieldModification.processTextParameter(todo, "comment", params)
            massUpdateContext.commitUpdate(
                identifier4Message = identifierOf(todo),
                todo,
                update = {
                    toDoDao.update(todo)
                    if (todo.assigneeId != oldAssigneeId || todo.status != oldStatus) {
                        notify(todo)
                    }
                },
            )
        }
        return null
    }

    override fun supportsMassDeletion(): Boolean = true

    override fun proceedMassDelete(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<ToDoDO>,
    ) {
        toDoDao.select(selectedIds)?.forEach { todo ->
            massUpdateContext.startUpdate(todo)
            todo.deleted = true
            massUpdateContext.commitUpdate(
                identifier4Message = identifierOf(todo),
                todo,
                update = {
                    toDoDao.markAsDeleted(todo)
                    notify(todo)
                },
            )
        }
    }

    override fun proceedMassUndelete(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<ToDoDO>,
    ) {
        toDoDao.select(selectedIds)?.forEach { todo ->
            massUpdateContext.startUpdate(todo)
            todo.deleted = false
            massUpdateContext.commitUpdate(
                identifier4Message = identifierOf(todo),
                todo,
                update = {
                    toDoDao.undelete(todo)
                    notify(todo)
                },
            )
        }
    }

    /** An enum is posted as its name ([MassUpdateParameter.textValue]); delete clears it. */
    private fun processEnum(param: MassUpdateParameter?, set: (String?) -> Unit) {
        param ?: return
        if (param.delete == true) {
            set(null)
        } else {
            param.textValue?.takeIf { it.isNotBlank() }?.let { set(it) }
        }
    }

    /** A failed notification is no failure of the written to-do, so it is logged only. */
    private fun notify(todo: ToDoDO) {
        toDoEntityRest.sendNotification(todo)?.let { log.warn { it } }
    }

    private fun identifierOf(todo: ToDoDO): String {
        return todo.subject ?: "#${todo.id}"
    }
}
