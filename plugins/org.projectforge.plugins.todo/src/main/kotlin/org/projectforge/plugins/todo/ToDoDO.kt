/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.i18n.Priority
import org.projectforge.Constants
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.UserPrefParameter
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.LocalDate
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_TODO", indexes = [jakarta.persistence.Index(name = "idx_fk_t_plugin_todo_assignee_fk", columnList = "assignee_fk"), jakarta.persistence.Index(name = "idx_fk_t_plugin_todo_group_id", columnList = "group_id"), jakarta.persistence.Index(name = "idx_fk_t_plugin_todo_reporter_fk", columnList = "reporter_fk"), jakarta.persistence.Index(name = "idx_fk_t_plugin_todo_task_id", columnList = "task_id")])
open class ToDoDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "plugins.todo.subject")
    @UserPrefParameter(i18nKey = "plugins.todo.subject")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TITLE)
    open var subject: String? = null

    @PropertyInfo(i18nKey = "plugins.todo.reporter")
    @UserPrefParameter(i18nKey = "plugins.todo.reporter")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "reporter_fk")
    open var reporter: PFUserDO? = null

    /**
     * @param assignee
     * @return this for chaining.
     */
    @PropertyInfo(i18nKey = "plugins.todo.assignee")
    @UserPrefParameter(i18nKey = "plugins.todo.assignee")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "assignee_fk")
    open var assignee: PFUserDO? = null

    @PropertyInfo(i18nKey = "task")
    @UserPrefParameter(i18nKey = "task")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "task_id", nullable = true)
    open var task: TaskDO? = null

    /**
     * Optional group.
     */
    @PropertyInfo(i18nKey = "group")
    @UserPrefParameter(i18nKey = "group")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "group_id", nullable = true)
    open var group: GroupDO? = null

    @PropertyInfo(i18nKey = "description")
    @UserPrefParameter(i18nKey = "description", multiline = true)
    @FullTextField
    @get:Column(length = Constants.LENGTH_TEXT)
    open var description: String? = null

    @PropertyInfo(i18nKey = "comment")
    @UserPrefParameter(i18nKey = "comment", multiline = true)
    @FullTextField
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    @PropertyInfo(i18nKey = "plugins.todo.type")
    @UserPrefParameter(i18nKey = "plugins.todo.type")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var type: ToDoType? = null

    @PropertyInfo(i18nKey = "plugins.todo.status")
    @UserPrefParameter(i18nKey = "plugins.todo.status")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var status: ToDoStatus? = null

    /**
     * After any modification of a to-do by other users than the assignee this flag is set to true. The assignee see in
     * his menu a red number showing the total number of recent to-do's. After displaying the to-do by the assignee the
     * recent flag will be set to false.
     *
     * @return true if any modification isn't seen by the assignee.
     */
    // @field:NoHistory
    @get:Column
    open var recent: Boolean = false

    @PropertyInfo(i18nKey = "priority")
    @UserPrefParameter(i18nKey = "priority")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var priority: Priority? = null

    @PropertyInfo(i18nKey = "dueDate")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column(name = "due_date")
    open var dueDate: LocalDate? = null

    @PropertyInfo(i18nKey = "resubmissionOnDate")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column
    open var resubmission: LocalDate? = null

    val reporterId: Int?
        @Transient
        get() = if (reporter != null) reporter!!.id else null

    val assigneeId: Int?
        @Transient
        get() = if (assignee != null) assignee!!.id else null

    val taskId: Int?
        @Transient
        get() = if (this.task != null) task!!.id else null

    val groupId: Int?
        @Transient
        get() = if (this.group != null) group!!.id else null
}
