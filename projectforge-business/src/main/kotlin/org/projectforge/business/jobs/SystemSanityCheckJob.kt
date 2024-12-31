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

package org.projectforge.business.jobs

import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskNode
import org.projectforge.jobs.AbstractJob
import org.projectforge.jobs.JobExecutionContext

class SystemSanityCheckJob(val taskDao: TaskDao) : AbstractJob("System integrity check") {
    override fun execute(jobContext: JobExecutionContext) {
        jobContext.addMessage("Task integrity (abandoned tasks)")
        val tasks: List<TaskDO> = taskDao.selectAll(false)
        jobContext.addMessage("Found ${tasks.size} tasks.")
        val taskMap: MutableMap<Long?, TaskDO> = HashMap()
        for (task in tasks) {
            taskMap[task.id] = task
        }
        var rootTask = false
        var abandonedTasks = false
        for (task in tasks) {
            if (task.parentTask == null) {
                if (rootTask) {
                    jobContext.addError("Found another root task: ${asString(task)}")
                } else {
                    jobContext.addMessage("Found root task: ${asString(task)}")
                    rootTask = true
                }
            } else {
                var ancestor = taskMap[task.parentTaskId]
                var rootTaskFound = false
                for (i in 0..49) { // Max. depth of 50, otherwise cyclic task!
                    if (ancestor == null) {
                        break
                    }
                    if (ancestor.parentTaskId == null) {
                        // Root task found, OK.
                        rootTaskFound = true
                        break
                    }
                    ancestor = taskMap[ancestor.parentTaskId]
                }
                if (!rootTaskFound) {
                    jobContext.addError("Found abandoned task (cyclic tasks without path to root): ${asString(task)}")
                    abandonedTasks = true
                }
            }
            taskMap[task.id] = task
        }
        if (!abandonedTasks) {
            jobContext.addMessage("Test OK, no abandoned tasks detected.")
        } else {
            jobContext.addError("Test FAILED, abandoned tasks detected.")
        }
    }

    private fun asString(task: TaskDO): String {
        return "TaskNode[id=[${task.id}], created=[${task.created}] title=[${task.title}]]"
    }
}
