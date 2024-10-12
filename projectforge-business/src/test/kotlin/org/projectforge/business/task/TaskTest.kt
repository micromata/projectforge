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

package org.projectforge.business.task

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.common.i18n.UserException
import org.projectforge.common.task.TimesheetBookingStatus
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

class TaskTest : AbstractTestBase() {
    // private static final Logger log = Logger.getLogger(TaskTest.class);
    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Test
    fun testTaskDO() {
        val list = taskDao.internalLoadAll()
        for (task in list) {
            if ("root" == task.title) {
                Assertions.assertNull(task.parentTaskId, "Only root node has no parent task.")
            } else {
                Assertions.assertNotNull(task.parentTaskId, "Only root node has no parent task.")
            }
        }
        val task = super.getTask("1.1")
        logon(ADMIN)
        val dbTask = taskDao.getById(task.id)
        Assertions.assertEquals(task.id, dbTask!!.id)
        Assertions.assertEquals(task.title, dbTask.title)
    }

    @Test
    fun testTaskTree() {
        val root = taskTree.rootTaskNode
        Assertions.assertNull(root.getParent())
        Assertions.assertEquals("root", root.getTask().title)
        Assertions.assertNotNull(root.getChildren(), "root node must have children")
        Assertions.assertTrue(root.getChildren().size > 0, "root node must have children")

        val node1_1 = taskTree.getTaskNodeById(getTask("1.1").id)
        Assertions.assertEquals(getTask("1.1").title, node1_1!!.getTask().title)
        Assertions.assertEquals(getTask("1.1").parentTaskId, node1_1.getParent().id)
        val node1 = taskTree.getTaskNodeById(getTask("1").id)
        val list = node1!!.getChildren()
        Assertions.assertEquals(2, list.size, "Children of 1 are 1.1 and 1.2")
        val task1_1_1 = taskTree.getTaskNodeById(getTask("1.1.1").id)
        val path = task1_1_1!!.pathToRoot
        Assertions.assertEquals(3, path.size, "Node has 2 ancestors plus itself.")
        Assertions.assertEquals(getTask("1").id, path[0].id, "Top task in path should be '1'")
        Assertions.assertEquals(getTask("1.1").id, path[1].id, "Second task in path sould be '1.1'")
        Assertions.assertEquals(getTask("1.1.1").id, path[2].id, "Third task in path is the node itself: '1.1'")
    }

    @Test
    fun testTraversingTaskTree() {
        val root = taskTree.rootTaskNode
        //logStart("Traversing TaskTree");
        traverseTaskTree(root)
        //logEnd();
    }

    @Test
    fun testCyclicTasks() {
        persistenceService.runInTransaction<Any?> { context: PfPersistenceContext? ->
            initTestDB.addTask("cyclictest", "root", context!!)
            initTestDB.addTask("c", "cyclictest", context)
            initTestDB.addTask("c.1", "c", context)
            initTestDB.addTask("c.1.1", "c.1", context)
            val c = taskTree.getTaskNodeById(getTask("c").id)
            val c_1_1 = taskTree.getTaskNodeById(getTask("c.1.1").id)
            try {
                c!!.setParent(c_1_1)
                Assertions.fail<Any>("Cyclic reference not detected.")
            } catch (ex: UserException) {
                Assertions.assertEquals(TaskDao.I18N_KEY_ERROR_CYCLIC_REFERENCE, ex.i18nKey)
            }
            try {
                c!!.setParent(c)
                Assertions.fail<Any>("Cyclic reference not detected.")
            } catch (ex: UserException) {
                Assertions.assertEquals(TaskDao.I18N_KEY_ERROR_CYCLIC_REFERENCE, ex.i18nKey)
            }
            null
        }
    }

    @Test
    fun testTaskDescendants() {
        persistenceService.runInTransaction<Any?> { context: PfPersistenceContext? ->
            initTestDB.addTask("descendanttest", "root", context!!)
            initTestDB.addTask("d", "descendanttest", context)
            initTestDB.addTask("d.1", "d", context)
            initTestDB.addTask("d.1.1", "d.1", context)
            initTestDB.addTask("d.1.2", "d.1", context)
            initTestDB.addTask("d.1.2.1", "d.1.2", context)
            initTestDB.addTask("d.2", "d", context)
            val d = taskTree.getTaskNodeById(getTask("d").id)
            val ids = d!!.descendantIds
            Assertions.assertEquals(5, ids.size)
            Assertions.assertTrue(ids.contains(getTask("d.1").id))
            Assertions.assertTrue(ids.contains(getTask("d.1.1").id))
            Assertions.assertTrue(ids.contains(getTask("d.1.2").id))
            Assertions.assertTrue(ids.contains(getTask("d.1.2.1").id))
            Assertions.assertTrue(ids.contains(getTask("d.2").id))
            Assertions.assertFalse(ids.contains(getTask("d").id))
            null
        }
    }

    @Test
    fun testTaskTreeUpdate() {
        persistenceService.runInTransaction<Any?> { context: PfPersistenceContext? ->
            initTestDB.addTask("taskTreeUpdateTest", "root", context!!)
            initTestDB.addTask("u", "taskTreeUpdateTest", context)
            val u = taskTree.getTaskNodeById(getTask("u").id)
            val parent = taskTree.getTaskNodeById(getTask("taskTreeUpdateTest").id)
            Assertions.assertEquals(false, u!!.hasChildren(), "Should have no children")
            Assertions.assertEquals(u.getParent().id, parent!!.id)
            initTestDB.addTask("u.1", "u", context)
            Assertions.assertEquals(true, u.hasChildren(), "Should have children")
            Assertions.assertEquals(1, u.getChildren().size, "Should have exact 1 child")
            initTestDB.addTask("u.2", "u", context)
            Assertions.assertEquals(2, u.getChildren().size, "Should have exact 2 children")
            initTestDB.addTask("u.2.1", "u.2", context)
            initTestDB.addTask("u.2.2", "u.2", context)
            initTestDB.addTask("u.2.3", "u.2", context)
            val u1 = taskTree.getTaskNodeById(getTask("u.1").id)
            val u2 = taskTree.getTaskNodeById(getTask("u.2").id)
            Assertions.assertEquals(3, u2!!.getChildren().size, "Should have exact 3 children")
            // Now we move u.2.3 to u.1.1:
            val tu_2_3 = taskDao.internalGetById(getTask("u.2.3").id, context)
            tu_2_3!!.title = "u.1.1"
            logon(ADMIN)
            taskDao.setParentTask(tu_2_3, getTask("u.1").id!!)
            taskDao.internalUpdateInTrans(tu_2_3)
            Assertions.assertEquals(2, u2.getChildren().size, "Should have exact 2 children")
            Assertions.assertEquals(1, u1!!.getChildren().size, "Should have exact 1 child")
            val tu_1_1 = taskDao.internalGetById(getTask("u.2.3").id)
            Assertions.assertEquals("u.1.1", tu_1_1!!.title)
            Assertions.assertEquals(getTask("u.1").id, tu_1_1.parentTaskId)
            val u_1_1 = taskTree.getTaskNodeById(tu_1_1.id)
            Assertions.assertEquals("u.1.1", u_1_1!!.getTask().title)
            Assertions.assertEquals(getTask("u.1").id, u_1_1.getParent().id)
            null
        }
    }

    /**
     * Checks task movements: Does the user has access to delete the task in the old hierarchy and the access to insert
     * the task in the new hierarchy?
     */
    @Test
    fun checkTaskAccess() {
        persistenceService.runInTransaction<Any?> { context: PfPersistenceContext? ->
            initTestDB.addTask("accesstest", "root", context!!)
            initTestDB.addTask("a", "accesstest", context)
            initTestDB.addTask("a.1", "a", context)
            initTestDB.addTask("a.1.1", "a.1", context)
            initTestDB.addTask("a.1.2", "a.1", context)
            initTestDB.addTask("a.2", "a", context)
            initTestDB.addTask("a.2.1", "a.2", context)
            initTestDB.addTask("a.2.2", "a.2", context)
            initTestDB.addUser("taskTest1", context)
            logon("taskTest1")
            try {
                taskDao.getById(getTask("a.1").id)
                Assertions.fail<Any>("User has no access to select task a.1")
            } catch (ex: AccessException) {
                assertAccessException(ex, getTask("a.1").id, AccessType.TASKS, OperationType.SELECT)
            }
            initTestDB.addGroup("taskTest1", context, "taskTest1")
            initTestDB.createGroupTaskAccess(
                getGroup("taskTest1"), getTask("a.1"), AccessType.TASKS, true, true, true, true,
                context
            )
            var task = taskDao.getById(getTask("a.1").id)!!
            Assertions.assertEquals("a.1", task.title, "Now readable.")
            task = taskDao.getById(getTask("a.1.1").id)!!
            Assertions.assertEquals("a.1.1", task.title, "Also child tasks are now readable.")
            taskDao.setParentTask(task, getTask("a.2").id!!)
            try {
                taskDao.updateInTrans(task)
                Assertions.fail<Any>("User has no access to insert task as child of a.2")
            } catch (ex: AccessException) {
                assertAccessException(ex, getTask("a.2").id, AccessType.TASKS, OperationType.INSERT)
            }
            initTestDB.createGroupTaskAccess(
                getGroup("taskTest1"), getTask("a.2"), AccessType.TASKS, true, false, false,
                false,
                context
            )
            task = taskDao.getById(getTask("a.2.1").id)!!
            task.title = "a.2.1test"
            try {
                taskDao.updateInTrans(task)
                Assertions.fail<Any>("User has no access to update child task of a.2")
            } catch (ex: AccessException) {
                assertAccessException(ex, getTask("a.2.1").id, AccessType.TASKS, OperationType.UPDATE)
            }

            initTestDB.addUser("taskTest2", context)
            logon("taskTest2")
            initTestDB.addGroup("taskTest2", context, "taskTest2")
            initTestDB.createGroupTaskAccess(
                getGroup("taskTest2"), getTask("a.1"), AccessType.TASKS, true, true, true, true,
                context
            )
            initTestDB.createGroupTaskAccess(
                getGroup("taskTest2"), getTask("a.2"), AccessType.TASKS, true, true, true, false,
                context
            )
            task = taskDao.getById(getTask("a.2.1").id)!!
            taskDao.setParentTask(task, getTask("a.1").id!!)
            try {
                taskDao.updateInTrans(task)
                Assertions.fail<Any>("User has no access to delete child task from a.2")
            } catch (ex: AccessException) {
                assertAccessException(ex, getTask("a.2").id, AccessType.TASKS, OperationType.DELETE)
            }
            null
        }
    }

    @Test
    fun checkAccess() {
        persistenceService.runInTransaction<Any?> { context: PfPersistenceContext ->
            logon(TEST_ADMIN_USER)
            val task = initTestDB.addTask("checkAccessTestTask", "root", context)
            initTestDB.addGroup("checkAccessTestGroup", context, TEST_USER)
            initTestDB.createGroupTaskAccess(
                getGroup("checkAccessTestGroup"), getTask("checkAccessTestTask"), AccessType.TASKS,
                true, true, true,
                true, context
            )
            logon(TEST_FINANCE_USER)
            val kost2Art = Kost2ArtDO()
            kost2Art.id = 42L
            kost2Art.name = "Test"
            kost2ArtDao.saveInTrans(kost2Art)
            val kost2 = Kost2DO()
            kost2.nummernkreis = 3
            kost2.bereich = 0
            kost2.teilbereich = 42
            kost2.kost2Art = kost2Art
            kost2Dao.saveInTrans(kost2)
            val projekt = ProjektDO()
            projekt.internKost2_4 = 123
            projekt.name = "Testprojekt"
            projektDao.saveInTrans(projekt)
            checkAccess(TEST_ADMIN_USER, task.id, projekt, kost2, context)
            checkAccess(TEST_USER, task.id, projekt, kost2, context)
            null
        }
    }

    @Test
    fun checkKost2AndTimesheetBookingStatusAccess() {
        persistenceService.runInTransaction<Any?> { context: PfPersistenceContext? ->
            logon(TEST_FINANCE_USER)
            val task = initTestDB.addTask("checkKost2AndTimesheetStatusAccessTask", "root", context!!)
            val groupName = "checkKost2AndTimesheetBookingStatusAccessGroup"
            // Please note: TEST_USER is no project manager or assistant!
            val projectManagers = initTestDB.addGroup(
                groupName, context,
                TEST_PROJECT_MANAGER_USER, TEST_PROJECT_ASSISTANT_USER,
                TEST_USER
            )
            initTestDB.createGroupTaskAccess(
                projectManagers, task, AccessType.TASKS, true, true, true, true,
                context
            ) // All rights.
            val projekt = ProjektDO()
            projekt.name = "checkKost2AndTimesheetBookingStatusAccess"
            projekt.internKost2_4 = 764
            projekt.nummer = 1
            projekt.projektManagerGroup = projectManagers
            projekt.task = task
            projektDao.saveInTrans(projekt)
            logon(TEST_USER)
            var task1: TaskDO? = TaskDO()
            task1!!.parentTask = task
            task1.title = "Task 1"
            task1.kost2BlackWhiteList = "Hurzel"
            try {
                taskDao.saveInTrans(task1)
                Assertions.fail<Any>("AccessException expected.")
            } catch (ex: AccessException) {
                Assertions.assertEquals("task.error.kost2Readonly", ex.i18nKey) // OK
            }
            try {
                task1.kost2BlackWhiteList = null
                task1.kost2IsBlackList = true
                taskDao.saveInTrans(task1)
                Assertions.fail<Any>("AccessException expected.")
            } catch (ex: AccessException) {
                Assertions.assertEquals("task.error.kost2Readonly", ex.i18nKey) // OK
            }
            try {
                task1.kost2IsBlackList = false
                task1.timesheetBookingStatus = TimesheetBookingStatus.ONLY_LEAFS
                taskDao.saveInTrans(task1)
                Assertions.fail<Any>("AccessException expected.")
            } catch (ex: AccessException) {
                Assertions.assertEquals("task.error.timesheetBookingStatus2Readonly", ex.i18nKey) // OK
            }
            logon(TEST_PROJECT_MANAGER_USER)
            task1.kost2IsBlackList = true
            task1.timesheetBookingStatus = TimesheetBookingStatus.ONLY_LEAFS
            task1 = taskDao.getById(taskDao.saveInTrans(task1))!!
            logon(TEST_USER)
            task1.kost2BlackWhiteList = "123456"
            try {
                taskDao.updateInTrans(task1)
                Assertions.fail<Any>("AccessException expected.")
            } catch (ex: AccessException) {
                Assertions.assertEquals("task.error.kost2Readonly", ex.i18nKey) // OK
            }
            try {
                task1.kost2BlackWhiteList = null
                task1.kost2IsBlackList = false
                taskDao.updateInTrans(task1)
                Assertions.fail<Any>("AccessException expected.")
            } catch (ex: AccessException) {
                Assertions.assertEquals("task.error.kost2Readonly", ex.i18nKey) // OK
            }
            try {
                task1.kost2IsBlackList = true
                task1.timesheetBookingStatus = TimesheetBookingStatus.INHERIT
                taskDao.updateInTrans(task1)
                Assertions.fail<Any>("AccessException expected.")
            } catch (ex: AccessException) {
                Assertions.assertEquals("task.error.timesheetBookingStatus2Readonly", ex.i18nKey) // OK
            }
            logon(TEST_PROJECT_MANAGER_USER)
            task1.kost2BlackWhiteList = "123456"
            task1.kost2IsBlackList = false
            task1.timesheetBookingStatus = TimesheetBookingStatus.INHERIT
            taskDao.updateInTrans(task1)
            null
        }
    }

    private fun checkAccess(
        user: String,
        id: Serializable?,
        projekt: ProjektDO,
        kost2: Kost2DO,
        context: PfPersistenceContext
    ) {
        logon(user)
        var task = taskDao.getById(id, context)!!
        task.protectTimesheetsUntil = LocalDate.now()
        try {
            taskDao.update(task, context)
            Assertions.fail<Any>("AccessException expected.")
        } catch (ex: AccessException) {
            // OK
            Assertions.assertEquals("task.error.protectTimesheetsUntilReadonly", ex.i18nKey)
        }
        task.protectTimesheetsUntil = null
        task.protectionOfPrivacy = true
        try {
            taskDao.update(task, context)
            Assertions.fail<Any>("AccessException expected.")
        } catch (ex: AccessException) {
            // OK
            Assertions.assertEquals("task.error.protectionOfPrivacyReadonly", ex.i18nKey)
        }
        task = taskDao.getById(id, context)!!
        task = TaskDO()
        task.parentTask = getTask("checkAccessTestTask")
        task.protectTimesheetsUntil = LocalDate.now()
        try {
            taskDao.save(task, context)
            Assertions.fail<Any>("AccessException expected.")
        } catch (ex: AccessException) {
            // OK
            Assertions.assertEquals("task.error.protectTimesheetsUntilReadonly", ex.i18nKey)
        }
        task.protectTimesheetsUntil = null
        task.protectionOfPrivacy = true
        try {
            taskDao.save(task, context)
            Assertions.fail<Any>("AccessException expected.")
        } catch (ex: AccessException) {
            // OK
            Assertions.assertEquals("task.error.protectionOfPrivacyReadonly", ex.i18nKey)
        }
        task = taskDao.getById(id, context)!!
    }

    /**
     * Sister tasks should have different names.
     */
    @Test
    fun testDuplicateTaskNames() {
        persistenceService.runInTransaction<Any?> { context: PfPersistenceContext? ->
            initTestDB.addTask("duplicateTaskNamesTest", "root", context!!)
            initTestDB.addTask("dT.1", "duplicateTaskNamesTest", context)
            initTestDB.addTask("dT.2", "duplicateTaskNamesTest", context)
            initTestDB.addTask("dT.1.1", "dT.1", context)
            try {
                // Try to insert sister task with same name:
                initTestDB.addTask("dT.1.1", "dT.1", context)
                Assertions.fail<Any>("Duplicate task was not detected.")
            } catch (ex: UserException) {
                Assertions.assertEquals(TaskDao.I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS, ex.i18nKey)
            }
            var task = initTestDB.addTask("dT.1.2", "dT.1", context)
            task.title = "dT.1.1"
            try {
                // Try to rename task to same name as a sister task:
                taskDao.internalUpdateInTrans(task)
                Assertions.fail<Any>("Duplicate task was not detected.")
            } catch (ex: UserException) {
                Assertions.assertEquals(TaskDao.I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS, ex.i18nKey)
            }
            task = initTestDB.addTask("dT.1.1", "dT.2", context)
            task.parentTask = initTestDB.getTask("dT.1")
            try {
                // Try to move task from dT.1.2 to dT.1.1 where already a task with the same name exists.
                taskDao.internalUpdateInTrans(task)
                Assertions.fail<Any>("Duplicate task was not detected.")
            } catch (ex: UserException) {
                Assertions.assertEquals(TaskDao.I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS, ex.i18nKey)
            }
            task.parentTask = initTestDB.getTask("dT.2")
            taskDao.internalUpdateInTrans(task)
            null
        }
    }

    @Test
    fun readTotalDuration() {
        lateinit var task: TaskDO
        lateinit var subTask1: TaskDO
        lateinit var subTask2: TaskDO
        persistenceService.runInTransaction<Any?> { context: PfPersistenceContext? ->
            logon(getUser(TEST_ADMIN_USER))
            task = initTestDB.addTask("totalDurationTask", "root", context!!)
            subTask1 = initTestDB.addTask("totalDurationTask.subtask1", "totalDurationTask", context)
            subTask2 = initTestDB.addTask("totalDurationTask.subtask2", "totalDurationTask", context)
            Assertions.assertEquals(0, taskDao.readTotalDuration(task.id))
            val dt = withDate(2010, Month.APRIL, 20, 8, 0)
            var ts = TimesheetDO()
            ts.user = getUser(TEST_USER)
            ts.setStartDate(dt.utilDate).stopTime = dt.plus(4, ChronoUnit.HOURS).sqlTimestamp
            ts.task = task
            timesheetDao.saveInTrans(ts)
            Assertions.assertEquals((4 * 3600).toLong(), taskDao.readTotalDuration(task.id))
            Assertions.assertEquals((4 * 3600).toLong(), getTotalDuration(taskTree, task.id))
            ts = TimesheetDO()
            ts.user = getUser(TEST_USER)
            ts.setStartDate(dt.plus(5, ChronoUnit.HOURS).utilDate)
                .stopTime = dt.plus(9, ChronoUnit.HOURS).sqlTimestamp
            ts.task = task
            timesheetDao.saveInTrans(ts)
            Assertions.assertEquals((8 * 3600).toLong(), taskDao.readTotalDuration(task.id))
            Assertions.assertEquals((8 * 3600).toLong(), getTotalDuration(taskTree, task.id))
            ts = TimesheetDO()
            ts.user = getUser(TEST_USER)
            ts.setStartDate(dt.plus(10, ChronoUnit.HOURS).utilDate)
                .stopTime = dt.plus(14, ChronoUnit.HOURS).sqlTimestamp
            ts.task = subTask1
            timesheetDao.saveInTrans(ts)
        }
        persistenceService.runReadOnly { context ->
            val list = taskDao.readTotalDurations(context)
            var taskFound = false
            var subtask1Found = false
            for (oa in list) {
                val taskId = oa[1] as Long
                if (taskId == task.id) {
                    Assertions.assertFalse(taskFound, "Entry should only exist once.")
                    Assertions.assertFalse(subtask1Found, "Entry not first.")
                    taskFound = true
                    Assertions.assertEquals((8 * 3600).toLong(), oa[0])
                } else if (taskId == subTask1.id) {
                    Assertions.assertFalse(subtask1Found, "Entry should only exist once.")
                    Assertions.assertTrue(taskFound, "Entry not second.")
                    subtask1Found = true
                    Assertions.assertEquals((4 * 3600).toLong(), oa[0])
                } else if (taskId == subTask2.id) {
                    Assertions.fail<Any>("Entry not not expected.")
                }
            }
            Assertions.assertEquals((12 * 3600).toLong(), getTotalDuration(taskTree, task.id))
            Assertions.assertEquals((8 * 3600).toLong(), getDuration(taskTree, task.id))
            Assertions.assertEquals((4 * 3600).toLong(), getTotalDuration(taskTree, subTask1.id))
            Assertions.assertEquals((4 * 3600).toLong(), getDuration(taskTree, subTask1.id))
            Assertions.assertEquals(0, getTotalDuration(taskTree, subTask2.id))
            Assertions.assertEquals(0, getDuration(taskTree, subTask2.id))
            taskTree.refresh() // Should be same after refresh (there was an error).
            Assertions.assertEquals((12 * 3600).toLong(), getTotalDuration(taskTree, task.id))
            Assertions.assertEquals((8 * 3600).toLong(), getDuration(taskTree, task.id))
            Assertions.assertEquals((4 * 3600).toLong(), getTotalDuration(taskTree, subTask1.id))
            Assertions.assertEquals((4 * 3600).toLong(), getDuration(taskTree, subTask1.id))
            Assertions.assertEquals(0, getTotalDuration(taskTree, subTask2.id))
            Assertions.assertEquals(0, getDuration(taskTree, subTask2.id))
        }
    }

    private fun getTotalDuration(taskTree: TaskTree, taskId: Long?): Long {
        return taskTree.getTaskNodeById(taskId)!!.getDuration(taskTree, true)
    }

    private fun getDuration(taskTree: TaskTree, taskId: Long?): Long {
        return taskTree.getTaskNodeById(taskId)!!.getDuration(taskTree, false)
    }

    private fun traverseTaskTree(node: TaskNode) {
        //logDot();
        val children = node.getChildren()
        if (children != null) {
            for (child in children) {
                Assertions.assertEquals(node.id, child.parentId, "Child should have parent id of current node.")
                traverseTaskTree(child)
            }
        }
    }
}
