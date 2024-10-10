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

package org.projectforge.business.user

import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.AccessEntryDO
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.test.AbstractTestBase
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

    @Test
    fun testAddUserWithHistory() {
        logon(ADMIN_USER)
        var groupTaskAccessId = 0L
        val hist = createHistoryTester()
        persistenceService.runInTransaction { context ->
            val group = GroupDO()
            group.name = "$PREFIX-Group"
            groupDao.insert(group, context)
            val task = TaskDO()
            task.title = "$PREFIX-Task"
            task.parentTask = taskTree.rootTaskNode.task
            taskDao.insert(task, context)
            hist.reset()
            val access = GroupTaskAccessDO()
            access.group = group
            access.task = task
            access.addAccessEntry(createAccessEntry(AccessType.OWN_TIMESHEETS, true, true, true, true))
            access.addAccessEntry(createAccessEntry(AccessType.TIMESHEETS, true, false, false, false))
            groupTaskAccessId = accessDao.insert(access, context)
            hist.loadRecentHistoryEntries(1)
        }
        val access = accessDao.getById(groupTaskAccessId)!!
        access.addAccessEntry(createAccessEntry(AccessType.TASKS, true, false, false, false))
        access.getAccessEntry(AccessType.TIMESHEETS)!!.let {
            it.accessSelect = false
            it.accessInsert = true
        }
        accessDao.saveOrUpdateInTrans(access)
        hist.loadRecentHistoryEntries(1, 1)
        val value = "TIMESHEETS={false,true,false,false}"
        val oldValue = "TIMESHEETS={true,false,false,false}"
        hist.getEntry(0).assertAttr("accessEntries", value = value, oldValue = oldValue, propertyTypeClass = AccessEntryDO::class)
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
