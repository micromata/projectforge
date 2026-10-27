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

package org.projectforge.business.task

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import kotlin.concurrent.thread

/**
 * Tasks inserted or changed while the [TaskTree] is refreshed: the refresh reads the tasks without the change
 * and publishes its tree at the end, which formerly lost the change until the next refresh.
 */
class TaskTreeRefreshRaceTest : AbstractTestBase() {
    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Test
    fun `task inserted during a refresh is in the new tree`() {
        logon(ADMIN)
        val parent = insertTask("refreshRace.insert", taskTree.rootTaskNode.getTask())
        var inserted: TaskDO? = null
        refreshWith {
            inserted = insertTask("refreshRace.insert.child", parent)
        }
        val node = taskTree.getTaskNodeById(inserted!!.id)
        Assertions.assertNotNull(node, "Task inserted during the refresh must not get lost.")
        Assertions.assertEquals(parent.id, node!!.getParent().id)
        Assertions.assertTrue(taskTree.getTaskNodeById(parent.id)!!.getChildren().any { it.id == node.id })
    }

    /**
     * As on prod: the insert's own access found the tree expired and started the refresh, which read the tasks
     * before the insert was committed. A task in the tree, but not (yet) in the database, stands in for it.
     */
    @Test
    fun `task not committed when a refresh reads the tasks is in the new tree`() {
        logon(ADMIN)
        val parent = insertTask("refreshRace.uncommitted", taskTree.rootTaskNode.getTask())
        val uncommitted = TaskDO().also {
            it.id = 999_999_999L
            it.parentTask = parent
            it.title = "refreshRace.uncommitted.child"
        }
        taskTree.addOrUpdateTaskNode(uncommitted)
        taskTree.refresh()
        Assertions.assertEquals(parent.id, taskTree.getTaskNodeById(uncommitted.id)?.getParent()?.id)
        // Taken once: the next refresh shows the database only.
        taskTree.refresh()
        Assertions.assertNull(taskTree.getTaskNodeById(uncommitted.id))
    }

    /** A task changed, then moved below a task inserted meanwhile: applied after its new parent, not below the root. */
    @Test
    fun `task moved below a task inserted during a refresh is below it in the new tree`() {
        logon(ADMIN)
        val root = taskTree.rootTaskNode.getTask()
        val task = insertTask("refreshRace.order.task", root)
        var newParent: TaskDO? = null
        refreshWith {
            val dbTask = taskDao.find(task.id, checkAccess = false)!!
            dbTask.title = "refreshRace.order.task (changed)"
            taskDao.update(dbTask, checkAccess = false)
            newParent = insertTask("refreshRace.order.parent", root)
            taskDao.setParentTask(dbTask, newParent!!.id!!)
            taskDao.update(dbTask, checkAccess = false)
        }
        Assertions.assertEquals(newParent!!.id, taskTree.getTaskNodeById(task.id)!!.getParent().id)
    }

    @Test
    fun `task moved during a refresh is below its new parent in the new tree`() {
        logon(ADMIN)
        val root = taskTree.rootTaskNode.getTask()
        val oldParent = insertTask("refreshRace.move.old", root)
        val newParent = insertTask("refreshRace.move.new", root)
        val task = insertTask("refreshRace.move.task", oldParent)
        refreshWith {
            val dbTask = taskDao.find(task.id, checkAccess = false)!!
            taskDao.setParentTask(dbTask, newParent.id!!)
            taskDao.update(dbTask, checkAccess = false)
        }
        Assertions.assertEquals(newParent.id, taskTree.getTaskNodeById(task.id)!!.getParent().id)
        Assertions.assertTrue(taskTree.getTaskNodeById(oldParent.id)!!.getChildren().none { it.id == task.id })
        Assertions.assertTrue(taskTree.getTaskNodeById(newParent.id)!!.getChildren().any { it.id == task.id })
    }

    @Test
    fun `task of a rolled back transaction is dropped from the tree`() {
        logon(ADMIN)
        val parent = insertTask("refreshRace.rollback", taskTree.rootTaskNode.getTask())
        var inserted: TaskDO? = null
        Assertions.assertThrows(IllegalStateException::class.java) {
            persistenceService.runInTransaction { _ ->
                inserted = insertTask("refreshRace.rollback.child", parent)
                Assertions.assertNotNull(taskTree.getTaskNodeById(inserted!!.id, checkRefresh = false))
                throw IllegalStateException("Rollback")
            }
        }
        Assertions.assertTrue(taskTree.status().expired, "Task tree must be expired after the rollback.")
        Assertions.assertNull(taskTree.getTaskNodeById(inserted!!.id))
    }

    private fun insertTask(title: String, parent: TaskDO): TaskDO {
        val task = TaskDO().also {
            it.parentTask = parent
            it.title = title
        }
        taskDao.insert(task, checkAccess = false)
        return task
    }

    /**
     * Refreshes the task tree and runs [change] after the tasks were read, right before the new tree is published.
     * The change runs on its own thread, as a concurrent request: on the refresh's thread it would share the
     * refresh's persistence context and change the very instances the refresh has read.
     *
     * [TaskTree.refresh] is called directly, as by the refresh after the expiry time: the tree isn't expired
     * meanwhile, so the change doesn't wait for the refresh lock.
     */
    private fun refreshWith(change: () -> Unit) {
        val user = ThreadLocalUserContext.loggedInUser!!
        var error: Throwable? = null
        taskTree.beforePublishHook = {
            taskTree.beforePublishHook = null
            thread {
                try {
                    logon(user)
                    change()
                } catch (ex: Throwable) {
                    error = ex
                } finally {
                    ThreadLocalUserContext.clear()
                }
            }.join()
        }
        try {
            taskTree.refresh()
        } finally {
            taskTree.beforePublishHook = null
        }
        error?.let { throw it }
    }
}
