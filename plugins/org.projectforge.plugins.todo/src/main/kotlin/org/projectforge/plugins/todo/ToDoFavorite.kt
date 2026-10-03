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


package org.projectforge.plugins.todo

import org.projectforge.business.task.TaskDO
import org.projectforge.common.i18n.Priority
import org.projectforge.favorites.AbstractFavorite
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * A template of the to-do edit form, stored as the user's favorite (see [ToDoFavoritesService]). It carries the
 * values the templates of the removed Wicket form carried (the `@UserPrefParameter` fields of [ToDoDO]).
 */
class ToDoFavorite(
    name: String? = null,
    id: Long? = null,
    var subject: String? = null,
    var reporterId: Long? = null,
    var assigneeId: Long? = null,
    var taskId: Long? = null,
    var groupId: Long? = null,
    var description: String? = null,
    var comment: String? = null,
    var type: ToDoType? = null,
    var status: ToDoStatus? = null,
    var priority: Priority? = null,
) : AbstractFavorite(name, id) {

    fun fillFrom(todo: ToDoDO) {
        subject = todo.subject
        reporterId = todo.reporterId
        assigneeId = todo.assigneeId
        taskId = todo.taskId
        groupId = todo.groupId
        description = todo.description
        comment = todo.comment
        type = todo.type
        status = todo.status
        priority = todo.priority
    }

    /** The references as id-only stubs, as a posted form carries them. */
    fun copyTo(todo: ToDoDO) {
        todo.subject = subject
        todo.reporter = reporterId?.let { PFUserDO().also { user -> user.id = it } }
        todo.assignee = assigneeId?.let { PFUserDO().also { user -> user.id = it } }
        todo.task = taskId?.let { TaskDO().also { task -> task.id = it } }
        todo.group = groupId?.let { GroupDO().also { group -> group.id = it } }
        todo.description = description
        todo.comment = comment
        todo.type = type
        todo.status = status
        todo.priority = priority
    }
}
