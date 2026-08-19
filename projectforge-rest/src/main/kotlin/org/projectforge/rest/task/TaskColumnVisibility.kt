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
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.AccessChecker

/**
 * Which of the optional task columns to show: one is shown if any task in the tree has a value for it,
 * as the Wicket pages do (`TaskTreeBuilder.createColumns`), so a tree without a single reference doesn't
 * carry an empty Reference column across the screen.
 *
 * Over the whole tree rather than over one answer, because an answer is a moving target — it depends on
 * the filter, on which nodes are open and on the highlighted node, and the columns would come and go with
 * every click. The tree is held in memory, so this costs no query.
 *
 * Shared by both perspectives of the task: [TaskServicesRest] sends the flags as column defs of the tree,
 * [TaskPagesRest] sends them as variables of the list's meta data (`addVariablesForListPage`), where the
 * declared columns of the next page gate on them. The rules — which group may see the orders, which the
 * timesheet protection — are the backend's to know, so there is exactly one place that knows them.
 */
class TaskColumnVisibility(
    val orders: Boolean = false,
    val protectTimesheetsUntil: Boolean = false,
    val reference: Boolean = false,
    val priority: Boolean = false,
) {
    companion object {
        /** The groups that may see which orders are booked against a task, as `TaskTreePage` has it. */
        val ORDER_GROUPS = arrayOf(
            ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.CONTROLLING_GROUP,
            ProjectForgeGroup.PROJECT_ASSISTANT,
            ProjectForgeGroup.PROJECT_MANAGER,
        )

        /**
         * Which optional columns the tree has data for, and which of them the logged-in user may see at all.
         *
         * The access rules are the Wicket page's (`TaskTreePage.onInitialize`): orders for project staff and
         * above, the timesheet protection for financial staff only.
         */
        fun of(accessChecker: AccessChecker, taskTree: TaskTree): TaskColumnVisibility {
            val maySeeOrders = accessChecker.isLoggedInUserMemberOfGroup(*ORDER_GROUPS)
            val maySeeProtection = accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP)
            // One walk over the tasks in memory, so a column that nothing fills doesn't show up empty.
            val tasks = mutableListOf<TaskDO>()
            collectTasks(taskTree.rootTaskNode, tasks)
            return TaskColumnVisibility(
                orders = maySeeOrders && taskTree.hasOrderPositionsEntries(),
                protectTimesheetsUntil = maySeeProtection && tasks.any { it.protectTimesheetsUntil != null },
                reference = tasks.any { !it.reference.isNullOrBlank() },
                priority = tasks.any { it.priority != null },
            )
        }

        /**
         * The node's task and those of all its descendants, depth first.
         *
         * Own walk rather than [TaskTree.getDescendants]: that one only collects the direct children.
         */
        private fun collectTasks(node: TaskNode, result: MutableList<TaskDO>) {
            result.add(node.task)
            node.children.forEach { collectTasks(it, result) }
        }
    }
}
