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
import org.projectforge.business.user.GroupDao
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.springframework.beans.factory.annotation.Autowired

/**
 * The rules of the structure wizard, which Wicket's `TaskWizardPage` used to carry: the picked element
 * gets the template of the group's role recursively, every ancestor below the root gets read access on
 * the tasks alone, and the root gets nothing at all.
 */
class TaskWizardServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var taskWizardService: TaskWizardService

    @Test
    fun `the manager group leads the element and only reads its ancestors`() {
        logon(ADMIN_USER)
        val group = createGroup("manager")
        val (parent, child) = createSubtree("manager")

        val result = taskWizardService.grantAccess(taskId = child.id!!, managerGroupId = group.id)

        Assertions.assertEquals(child.title, result.taskTitle)
        Assertions.assertEquals(1, result.granted.size)
        Assertions.assertEquals(TaskWizardService.GroupType.MANAGER, result.granted[0].groupType)
        // The element itself plus its one ancestor below the root — the root is not counted because it
        // gets no entry.
        Assertions.assertEquals(2, result.granted[0].accessEntries)
        // The picked element first, then upwards: what the report of the wizard is built on.
        result.granted[0].entries.let { entries ->
            Assertions.assertEquals(listOf(child.id, parent.id), entries.map { it.taskId })
            Assertions.assertEquals(listOf(child.title, parent.title), entries.map { it.taskTitle })
            Assertions.assertEquals(listOf(true, false), entries.map { it.pickedElement })
            Assertions.assertTrue(entries.all { it.groupName == group.name })
        }
        Assertions.assertEquals(2, result.count(TaskWizardService.AccessStatus.CREATED))
        Assertions.assertEquals(0, result.count(TaskWizardService.AccessStatus.UPDATED))
        Assertions.assertEquals(0, result.count(TaskWizardService.AccessStatus.UNCHANGED))

        entry(child, group).let { access ->
            Assertions.assertTrue(access.recursive, "The role holds for the whole subtree.")
            assertAccess(access, AccessType.TASKS, select = true, insert = true, update = true, delete = true)
            // What separates the leader from the employee: the time sheets of the others.
            assertAccess(access, AccessType.TIMESHEETS, select = true, insert = true, update = true, delete = true)
        }
        entry(parent, group).let { access ->
            Assertions.assertFalse(access.recursive, "An ancestor's entry must not reach its other children.")
            assertAccess(access, AccessType.TASKS, select = true, insert = false, update = false, delete = false)
            assertAccess(access, AccessType.TIMESHEETS, select = false, insert = false, update = false, delete = false)
        }
    }

    @Test
    fun `the team works and the external staff only books its own time`() {
        logon(ADMIN_USER)
        val team = createGroup("team")
        val external = createGroup("external")
        val (_, child) = createSubtree("roles")

        val result = taskWizardService.grantAccess(
            taskId = child.id!!,
            teamGroupId = team.id,
            externalGroupId = external.id,
        )

        Assertions.assertEquals(
            listOf(TaskWizardService.GroupType.TEAM, TaskWizardService.GroupType.EXTERNAL),
            result.granted.map { it.groupType },
        )
        entry(child, team).let { access ->
            assertAccess(access, AccessType.TASKS, select = true, insert = true, update = true, delete = true)
            assertAccess(access, AccessType.TIMESHEETS, select = true, insert = false, update = false, delete = false)
        }
        entry(child, external).let { access ->
            assertAccess(access, AccessType.TASKS, select = true, insert = false, update = false, delete = false)
            assertAccess(
                access, AccessType.OWN_TIMESHEETS,
                select = true, insert = true, update = true, delete = true,
            )
            assertAccess(access, AccessType.TIMESHEETS, select = false, insert = false, update = false, delete = false)
        }
    }

    @Test
    fun `the root element never gets an entry`() {
        logon(ADMIN_USER)
        val group = createGroup("root")
        val root = taskTree.rootTaskNode.task

        val result = taskWizardService.grantAccess(taskId = root.id!!, managerGroupId = group.id)

        Assertions.assertEquals(0, result.granted[0].accessEntries)
        Assertions.assertTrue(result.granted[0].entries.isEmpty())
        Assertions.assertNull(accessDao.getEntry(root, group))
    }

    @Test
    fun `a second run updates the entries instead of adding more`() {
        logon(ADMIN_USER)
        val group = createGroup("twice")
        val (parent, child) = createSubtree("twice")

        taskWizardService.grantAccess(taskId = child.id!!, teamGroupId = group.id)
        // The team first, then the managing role on the same element: the wizard is a shortcut into the
        // access management, not an append-only log, so the entry has to be raised rather than doubled.
        val result = taskWizardService.grantAccess(taskId = child.id!!, managerGroupId = group.id)

        Assertions.assertEquals(2, result.granted[0].accessEntries)
        Assertions.assertEquals(
            1,
            entriesOf(group).count { it.task?.id == child.id },
            "One entry per element and group, whatever the number of runs.",
        )
        assertAccess(
            entry(child, group), AccessType.TIMESHEETS,
            select = true, insert = true, update = true, delete = true,
        )
        Assertions.assertEquals(TaskWizardService.AccessStatus.UPDATED, statusOf(result, child))
        Assertions.assertEquals(
            TaskWizardService.AccessStatus.UNCHANGED, statusOf(result, parent),
            "An ancestor gets read access whatever the role, so the second run leaves it as it is.",
        )
    }

    @Test
    fun `a second identical run reports everything as unchanged`() {
        logon(ADMIN_USER)
        val group = createGroup("again")
        val (parent, child) = createSubtree("again")
        taskWizardService.grantAccess(taskId = child.id!!, teamGroupId = group.id)
        val lastUpdate = entry(child, group).lastUpdate

        val result = taskWizardService.grantAccess(taskId = child.id!!, teamGroupId = group.id)

        Assertions.assertEquals(2, result.count(TaskWizardService.AccessStatus.UNCHANGED))
        Assertions.assertEquals(0, result.count(TaskWizardService.AccessStatus.CREATED))
        Assertions.assertEquals(0, result.count(TaskWizardService.AccessStatus.UPDATED))
        Assertions.assertEquals(
            TaskWizardService.AccessStatus.UNCHANGED, statusOf(result, parent),
        )
        Assertions.assertEquals(
            lastUpdate, entry(child, group).lastUpdate,
            "Nothing was written, so nothing was touched either — that is what unchanged means.",
        )
    }

    @Test
    fun `a right that was changed by hand is reported as changed`() {
        logon(ADMIN_USER)
        val group = createGroup("byhand")
        val (parent, child) = createSubtree("byhand")
        taskWizardService.grantAccess(taskId = child.id!!, teamGroupId = group.id)
        accessDao.update(entry(child, group).also { access ->
            access.ensureAndGetTimesheetsEntry().setAccess(false, false, false, false)
        })

        val result = taskWizardService.grantAccess(taskId = child.id!!, teamGroupId = group.id)

        Assertions.assertEquals(TaskWizardService.AccessStatus.UPDATED, statusOf(result, child))
        Assertions.assertEquals(TaskWizardService.AccessStatus.UNCHANGED, statusOf(result, parent))
    }

    @Test
    fun `an entry that lost its recursion is reported as changed`() {
        logon(ADMIN_USER)
        val group = createGroup("recursive")
        val (_, child) = createSubtree("recursive")
        taskWizardService.grantAccess(taskId = child.id!!, teamGroupId = group.id)
        // Not a permission but a plain field of the entry: the one and the other have to be seen.
        accessDao.update(entry(child, group).also { it.recursive = false })

        val result = taskWizardService.grantAccess(taskId = child.id!!, teamGroupId = group.id)

        Assertions.assertEquals(TaskWizardService.AccessStatus.UPDATED, statusOf(result, child))
        Assertions.assertTrue(entry(child, group).recursive)
    }

    @Test
    fun `a deleted entry is undeleted rather than left behind`() {
        logon(ADMIN_USER)
        val group = createGroup("undelete")
        val (_, child) = createSubtree("undelete")
        taskWizardService.grantAccess(taskId = child.id!!, teamGroupId = group.id)
        accessDao.markAsDeleted(entry(child, group))
        Assertions.assertTrue(entry(child, group).deleted)

        val result = taskWizardService.grantAccess(taskId = child.id!!, teamGroupId = group.id)

        Assertions.assertFalse(entry(child, group).deleted, "The wizard has to revive its own entry.")
        Assertions.assertEquals(
            TaskWizardService.AccessStatus.UPDATED, statusOf(result, child),
            "The rights were already right, but the entry was gone — that is a change, not a no-op.",
        )
    }

    @Test
    fun `without a group nothing is written`() {
        logon(ADMIN_USER)
        val (parent, child) = createSubtree("nogroup")

        val result = taskWizardService.grantAccess(taskId = child.id!!)

        Assertions.assertTrue(result.granted.isEmpty())
        Assertions.assertTrue(result.entries.isEmpty())
        Assertions.assertEquals(0, result.count(TaskWizardService.AccessStatus.CREATED))
        Assertions.assertEquals(child.title, result.taskTitle)
        Assertions.assertTrue(
            accessDao.selectAll(checkAccess = false).none { it.task?.id == child.id || it.task?.id == parent.id },
        )
    }

    @Test
    fun `an unknown element is refused`() {
        logon(ADMIN_USER)
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            taskWizardService.grantAccess(taskId = -42L)
        }
    }

    /** A `parent` below the root with one `child`, so the walk up has one ancestor to write and one to skip. */
    private fun createSubtree(name: String): Pair<TaskDO, TaskDO> {
        val parent = TaskDO()
        parent.title = "$PREFIX-$name-parent"
        parent.parentTask = taskTree.rootTaskNode.task
        taskDao.insert(parent)
        val child = TaskDO()
        child.title = "$PREFIX-$name-child"
        child.parentTask = parent
        taskDao.insert(child)
        return parent to child
    }

    private fun createGroup(name: String): GroupDO {
        val group = GroupDO()
        group.name = "$PREFIX-$name"
        groupDao.insert(group)
        return group
    }

    /** What the wizard reported for one element of the path it walked. */
    private fun statusOf(
        result: TaskWizardService.Result,
        task: TaskDO,
    ): TaskWizardService.AccessStatus {
        return result.entries.firstOrNull { it.taskId == task.id }?.status
            ?: Assertions.fail("No entry reported for task '${task.title}'.")
    }

    private fun entry(task: TaskDO, group: GroupDO): GroupTaskAccessDO {
        return accessDao.getEntry(task, group)
            ?: Assertions.fail("No access entry for task '${task.title}' and group '${group.name}'.")
    }

    private fun entriesOf(group: GroupDO): List<GroupTaskAccessDO> {
        return accessDao.selectAll(checkAccess = false).filter { it.group?.id == group.id }
    }

    private fun assertAccess(
        access: GroupTaskAccessDO,
        type: AccessType,
        select: Boolean,
        insert: Boolean,
        update: Boolean,
        delete: Boolean,
    ) {
        val entry = access.getAccessEntry(type) ?: Assertions.fail("No $type entry.")
        Assertions.assertEquals(select, entry.accessSelect, "$type select")
        Assertions.assertEquals(insert, entry.accessInsert, "$type insert")
        Assertions.assertEquals(update, entry.accessUpdate, "$type update")
        Assertions.assertEquals(delete, entry.accessDelete, "$type delete")
    }

    companion object {
        private val PREFIX = TaskWizardService::class.simpleName
    }
}
