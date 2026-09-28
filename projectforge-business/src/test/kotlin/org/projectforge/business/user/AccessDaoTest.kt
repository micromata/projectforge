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

package org.projectforge.business.user

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.AccessEntryDO
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class AccessDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Test
    fun testAddUserWithHistory() {
        logon(ADMIN_USER)
        var groupTaskAccessId = 0L
        val hist = createHistoryTester()
        persistenceService.runInTransaction { _ ->
            val group = GroupDO()
            group.name = "$PREFIX-Group"
            groupDao.insert(group)
            val task = TaskDO()
            task.title = "$PREFIX-Task"
            task.parentTask = taskTree.rootTaskNode.task
            taskDao.insert(task)
            hist.reset()
            val access = GroupTaskAccessDO()
            access.group = group
            access.task = task
            access.addAccessEntry(createAccessEntry(AccessType.OWN_TIMESHEETS, true, true, true, true))
            access.addAccessEntry(createAccessEntry(AccessType.TIMESHEETS, true, false, false, false))
            groupTaskAccessId = accessDao.insert(access)
            hist.loadRecentHistoryEntries(1)
        }
        val access = accessDao.find(groupTaskAccessId)!!
        access.addAccessEntry(createAccessEntry(AccessType.TASKS, true, false, false, false))
        access.getAccessEntry(AccessType.TIMESHEETS)!!.let {
            it.accessSelect = false
            it.accessInsert = true
        }
        access.recursive = false
        accessDao.update(access)
        hist.loadRecentHistoryEntries(3, 2)
        val value = "TIMESHEETS={false,true,false,false}"
        val oldValue = "TIMESHEETS={true,false,false,false}"
        hist.recentEntries!!.find {
            it.entry.entityOpType == EntityOpType.Update && it.attributes?.any { it.propertyName == "accessEntries" } == true
        }!!.assertAttr("accessEntries", value = value, oldValue = oldValue, propertyTypeClass = AccessEntryDO::class)
        hist.recentEntries!!.find {
            it.entry.entityOpType == EntityOpType.Update && it.attributes?.any { it.propertyName == "recursive" } == true
        }!!.assertAttr("recursive", value = "false", oldValue = "true", propertyTypeClass = Boolean::class)

        hist.recentEntries!!.find { it.entry.entityOpType == EntityOpType.Insert }!!.let { holder ->
            Assertions.assertEquals(AccessEntryDO::class.qualifiedName, holder.entry.entityName)
        }
    }

    /**
     * The group-task access rights of a deleted group must no longer be accepted: as soon as the referenced group is
     * deleted, the access is treated as if the [GroupTaskAccessDO] itself was deleted. Undeleting the group makes the
     * access effective again.
     */
    @Test
    fun testDeletedGroupAccessIsIgnored() {
        logon(ADMIN_USER)
        val username = "$PREFIX-DeletedAccessUser"
        val user = initTestDB.addUser(username)
        lateinit var group: GroupDO
        lateinit var task: TaskDO
        persistenceService.runInTransaction { _ ->
            group = initTestDB.addGroup("$PREFIX-DeletedAccessGroup", username)
            task = initTestDB.addTask("$PREFIX-DeletedAccessTask", "root")
            initTestDB.createGroupTaskAccess(
                group, task, AccessType.TASKS,
                accessSelect = true, accessInsert = false, accessUpdate = false, accessDelete = false,
            )
        }
        userGroupCache.forceReload()
        Assertions.assertTrue(
            accessChecker.hasPermission(user, task.id, AccessType.TASKS, OperationType.SELECT, false),
            "User should have select access to the task via its group.",
        )

        // Delete the group: its access rights must no longer be accepted.
        persistenceService.runInTransaction { _ ->
            groupDao.markAsDeleted(group, checkAccess = false)
        }
        userGroupCache.forceReload()
        Assertions.assertFalse(
            accessChecker.hasPermission(user, task.id, AccessType.TASKS, OperationType.SELECT, false),
            "Access rights of a deleted group must be treated as deleted.",
        )

        // Undelete the group: its access rights become effective again.
        persistenceService.runInTransaction { _ ->
            groupDao.undelete(group, checkAccess = false)
        }
        userGroupCache.forceReload()
        Assertions.assertTrue(
            accessChecker.hasPermission(user, task.id, AccessType.TASKS, OperationType.SELECT, false),
            "Access rights must be effective again after the group has been undeleted.",
        )
    }

    private fun createAccessEntry(
        type: AccessType,
        select: Boolean,
        insert: Boolean,
        update: Boolean,
        delete: Boolean,
    ): AccessEntryDO {
        val entry = AccessEntryDO()
        entry.accessSelect = select
        entry.accessInsert = insert
        entry.accessUpdate = update
        entry.accessDelete = delete
        entry.accessType = type
        return entry
    }

    companion object {
        private val PREFIX = AccessDao::class.simpleName
    }
}
