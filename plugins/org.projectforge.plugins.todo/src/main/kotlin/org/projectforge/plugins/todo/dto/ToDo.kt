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


package org.projectforge.plugins.todo.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.business.PfCaches
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskFormatter
import org.projectforge.common.i18n.Priority
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.todo.ToDoDO
import org.projectforge.plugins.todo.ToDoStatus
import org.projectforge.plugins.todo.ToDoType
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.EntityAccessSupport
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.Task
import org.projectforge.rest.dto.User
import java.time.LocalDate

/**
 * A to-do as the projectforge-next page reads and writes it (see `ToDoEntityRest`).
 *
 * The references (reporter, assignee, task, group) carry their id and display name only, taken from the
 * caches: the list shows a name per column and the form a selected value, neither needs a nested entity.
 */
class ToDo(
    var subject: String? = null,
    var reporter: User? = null,
    var assignee: User? = null,
    var task: Task? = null,
    var group: Group? = null,
    var description: String? = null,
    var comment: String? = null,
    var type: ToDoType? = null,
    var status: ToDoStatus? = null,
    var priority: Priority? = null,
    var dueDate: LocalDate? = null,
    var resubmission: LocalDate? = null,
) : BaseDTO<ToDoDO>(), EntityAccessSupport {
    /**
     * Whether a change of another user isn't seen by the assignee yet ([ToDoDO.recent]). Read only: it is
     * set by the dao on a save of somebody else and cleared when the assignee opens the to-do.
     */
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var recent: Boolean = false

    /**
     * Whether [recent] concerns the logged-in user, being the assignee: the rows the list highlights, as the
     * Wicket list did. Read only.
     */
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var recentForMe: Boolean = false

    /**
     * Whether the save should notify assignee and reporter by e-mail, the checkbox of the edit form. A save
     * notifies anyhow on a new to-do or a changed assignee, status or deletion state (see `ToDoEntityRest`).
     * Not persisted, [ToDoDO] has no such property.
     */
    var sendNotification: Boolean = false

    /**
     * Whether e-mails can be sent at all, so the form offers the [sendNotification] checkbox only then.
     * Read only, set by the server.
     */
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var mailConfigured: Boolean = false

    override var writeAccess: Boolean? = null
    override var deleteAccess: Boolean? = null

    /**
     * Copies the values one by one instead of by reflection ([BaseDTO.copyFrom]), which would fill the
     * references through their lazy proxies and the task with its whole parent chain.
     */
    override fun copyFrom(src: ToDoDO) {
        copyFrom4ListRow(src)
        deactivated = src.deactivated
        comment = src.comment
    }

    /** The list row is the whole to-do but its comment, nothing of it is large. */
    override fun copyFrom4ListRow(src: ToDoDO) {
        id = src.id
        deleted = src.deleted
        copyAuditFieldsFrom(src)
        subject = src.subject
        description = src.description
        type = src.type
        status = src.status
        priority = src.priority
        dueDate = src.dueDate
        resubmission = src.resubmission
        recent = src.recent
        recentForMe = src.recent && src.assigneeId == ThreadLocalUserContext.loggedInUserId
        copyReferencesFrom(src)
    }

    /**
     * The references from the caches by their ids, so neither a lazy load per row nor the parent chain of
     * the task ([Task.copyFromMinimal]) is needed, and id-only stubs (of a template) get their names, too.
     * The task carries its path (without the task itself) for the tooltip of the list column, as the time
     * sheet list does.
     */
    private fun copyReferencesFrom(src: ToDoDO) {
        val caches = PfCaches.instance
        reporter = toUser(caches.getUser(src.reporterId))
        assignee = toUser(caches.getUser(src.assigneeId))
        task = caches.getTask(src.taskId)?.let { toTask(it) }
        group = caches.getGroup(src.groupId)?.let { toGroup(it) }
    }

    companion object {
        fun toUser(user: PFUserDO?): User? {
            user ?: return null
            return User(id = user.id, displayName = user.displayName).also {
                it.deleted = user.deleted
                it.deactivated = user.deactivated
            }
        }

        fun toTask(task: TaskDO): Task {
            return Task(id = task.id, displayName = task.displayName, title = task.title).also {
                it.path = TaskFormatter.getTaskPath(task.id, showCurrentTask = false)
                it.deleted = task.deleted
            }
        }

        fun toGroup(group: GroupDO): Group {
            return Group(id = group.id, displayName = group.displayName).also {
                it.name = group.name
                it.deleted = group.deleted
            }
        }
    }
}
