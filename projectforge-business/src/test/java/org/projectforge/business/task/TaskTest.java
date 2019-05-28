/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.task;

import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.business.fibu.kost.Kost2ArtDao;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.common.task.TimesheetBookingStatus;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TaskTest extends AbstractTestBase {
  // private static final Logger log = Logger.getLogger(TaskTest.class);

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private ProjektDao projektDao;

  @Autowired
  private Kost2Dao kost2Dao;

  @Autowired
  private Kost2ArtDao kost2ArtDao;

  @Autowired
  private TimesheetDao timesheetDao;

  @Test
  public void testTaskDO() {
    final List<TaskDO> list = taskDao.internalLoadAll();
    for (final TaskDO task : list) {
      if ("root".equals(task.getTitle()) == true) {
        assertNull(task.getParentTaskId(), "Only root node has no parent task.");
      } else {
        assertNotNull(task.getParentTaskId(), "Only root node has no parent task.");
      }
    }
    final TaskDO task = super.getTask("1.1");
    logon(AbstractTestBase.ADMIN);
    final TaskDO dbTask = taskDao.getById(task.getId());
    assertEquals(task.getId(), dbTask.getId());
    assertEquals(task.getTitle(), dbTask.getTitle());
  }

  @Test
  public void testTaskTree() {
    final TaskTree taskTree = taskDao.getTaskTree();
    final TaskNode root = taskTree.getRootTaskNode();
    assertNull(root.getParent());
    assertEquals("root", root.getTask().getTitle());
    assertNotNull(root.getChilds(), "root node must have childs");
    assertTrue(root.getChilds().size() > 0, "root node must have childs");

    final TaskNode node1_1 = taskTree.getTaskNodeById(getTask("1.1").getId());
    assertEquals(getTask("1.1").getTitle(), node1_1.getTask().getTitle());
    assertEquals(getTask("1.1").getParentTaskId(), node1_1.getParent().getId());
    final TaskNode node1 = taskTree.getTaskNodeById(getTask("1").getId());
    final List<TaskNode> list = node1.getChilds();
    assertEquals(2, list.size(), "Childs of 1 are 1.1 and 1.2");
    final TaskNode task1_1_1 = taskTree.getTaskNodeById(getTask("1.1.1").getId());
    final List<TaskNode> path = task1_1_1.getPathToRoot();
    assertEquals(3, path.size(), "Node has 2 ancestors plus itself.");
    assertEquals(getTask("1").getId(), path.get(0).getId(), "Top task in path should be '1'");
    assertEquals(getTask("1.1").getId(), path.get(1).getId(), "Second task in path sould be '1.1'");
    assertEquals(getTask("1.1.1").getId(), path.get(2).getId(), "Third task in path is the node itself: '1.1'");
  }

  @Test
  public void testTraversingTaskTree() {
    final TaskTree taskTree = taskDao.getTaskTree();
    final TaskNode root = taskTree.getRootTaskNode();
    logStart("Traversing TaskTree");
    traverseTaskTree(root);
    logEnd();
  }

  @Test
  public void testCyclicTasks() {
    final TaskTree tree = taskDao.getTaskTree();
    initTestDB.addTask("cyclictest", "root");
    initTestDB.addTask("c", "cyclictest");
    initTestDB.addTask("c.1", "c");
    initTestDB.addTask("c.1.1", "c.1");
    final TaskNode c = tree.getTaskNodeById(getTask("c").getId());
    final TaskNode c_1_1 = tree.getTaskNodeById(getTask("c.1.1").getId());
    try {
      c.setParent(c_1_1);
      fail("Cyclic reference not detected.");
    } catch (final UserException ex) {
      assertEquals(TaskDao.I18N_KEY_ERROR_CYCLIC_REFERENCE, ex.getI18nKey());
    }
    try {
      c.setParent(c);
      fail("Cyclic reference not detected.");
    } catch (final UserException ex) {
      assertEquals(TaskDao.I18N_KEY_ERROR_CYCLIC_REFERENCE, ex.getI18nKey());
    }
  }

  @Test
  public void testTaskDescendants() {
    final TaskTree tree = taskDao.getTaskTree();
    initTestDB.addTask("descendanttest", "root");
    initTestDB.addTask("d", "descendanttest");
    initTestDB.addTask("d.1", "d");
    initTestDB.addTask("d.1.1", "d.1");
    initTestDB.addTask("d.1.2", "d.1");
    initTestDB.addTask("d.1.2.1", "d.1.2");
    initTestDB.addTask("d.2", "d");
    final TaskNode d = tree.getTaskNodeById(getTask("d").getId());
    final List<Integer> ids = d.getDescendantIds();
    assertEquals(5, ids.size());
    assertTrue(ids.contains(getTask("d.1").getId()));
    assertTrue(ids.contains(getTask("d.1.1").getId()));
    assertTrue(ids.contains(getTask("d.1.2").getId()));
    assertTrue(ids.contains(getTask("d.1.2.1").getId()));
    assertTrue(ids.contains(getTask("d.2").getId()));
    assertFalse(ids.contains(getTask("d").getId()));
  }

  @Test
  public void testTaskTreeUpdate() {
    final TaskTree tree = taskDao.getTaskTree();
    initTestDB.addTask("taskTreeUpdateTest", "root");
    initTestDB.addTask("u", "taskTreeUpdateTest");
    final TaskNode u = tree.getTaskNodeById(getTask("u").getId());
    final TaskNode parent = tree.getTaskNodeById(getTask("taskTreeUpdateTest").getId());
    assertEquals(false, u.hasChilds(), "Should have no childs");
    assertEquals(u.getParent().getId(), parent.getId());
    initTestDB.addTask("u.1", "u");
    assertEquals(true, u.hasChilds(), "Should have childs");
    assertEquals(1, u.getChilds().size(), "Should have exact 1 child");
    initTestDB.addTask("u.2", "u");
    assertEquals(2, u.getChilds().size(), "Should have exact 2 childs");
    initTestDB.addTask("u.2.1", "u.2");
    initTestDB.addTask("u.2.2", "u.2");
    initTestDB.addTask("u.2.3", "u.2");
    final TaskNode u1 = tree.getTaskNodeById(getTask("u.1").getId());
    final TaskNode u2 = tree.getTaskNodeById(getTask("u.2").getId());
    assertEquals(3, u2.getChilds().size(), "Should have exact 3 childs");
    // Now we move u.2.3 to u.1.1:
    final TaskDO tu_2_3 = taskDao.internalGetById(getTask("u.2.3").getId());
    tu_2_3.setTitle("u.1.1");
    logon(AbstractTestBase.ADMIN);
    taskDao.setParentTask(tu_2_3, getTask("u.1").getId());
    taskDao.internalUpdate(tu_2_3);
    assertEquals(2, u2.getChilds().size(), "Should have exact 2 childs");
    assertEquals(1, u1.getChilds().size(), "Should have exact 1 child");
    final TaskDO tu_1_1 = taskDao.internalGetById(getTask("u.2.3").getId());
    assertEquals("u.1.1", tu_1_1.getTitle());
    assertEquals(getTask("u.1").getId(), tu_1_1.getParentTaskId());
    final TaskNode u_1_1 = tree.getTaskNodeById(tu_1_1.getId());
    assertEquals("u.1.1", u_1_1.getTask().getTitle());
    assertEquals(getTask("u.1").getId(), u_1_1.getParent().getId());
  }

  /**
   * Checks task movements: Does the user has access to delete the task in the old hierarchy and the access to insert
   * the task in the new hierarchy?
   */
  @Test
  public void checkTaskAccess() {
    initTestDB.addTask("accesstest", "root");
    initTestDB.addTask("a", "accesstest");
    initTestDB.addTask("a.1", "a");
    initTestDB.addTask("a.1.1", "a.1");
    initTestDB.addTask("a.1.2", "a.1");
    initTestDB.addTask("a.2", "a");
    initTestDB.addTask("a.2.1", "a.2");
    initTestDB.addTask("a.2.2", "a.2");
    initTestDB.addUser("taskTest1");
    logon("taskTest1");
    try {
      taskDao.getById(getTask("a.1").getId());
      fail("User has no access to select task a.1");
    } catch (final AccessException ex) {
      assertAccessException(ex, getTask("a.1").getId(), AccessType.TASKS, OperationType.SELECT);
    }
    initTestDB.addGroup("taskTest1", new String[]{"taskTest1"});
    initTestDB.createGroupTaskAccess(getGroup("taskTest1"), getTask("a.1"), AccessType.TASKS, true, true, true, true);
    TaskDO task = taskDao.getById(getTask("a.1").getId());
    assertEquals("a.1", task.getTitle(), "Now readable.");
    task = taskDao.getById(getTask("a.1.1").getId());
    assertEquals( "a.1.1", task.getTitle(), "Also child tasks are now readable.");
    taskDao.setParentTask(task, getTask("a.2").getId());
    try {
      taskDao.update(task);
      fail("User has no access to insert task as child of a.2");
    } catch (final AccessException ex) {
      assertAccessException(ex, getTask("a.2").getId(), AccessType.TASKS, OperationType.INSERT);
    }
    initTestDB.createGroupTaskAccess(getGroup("taskTest1"), getTask("a.2"), AccessType.TASKS, true, false, false,
            false);
    task = taskDao.getById(getTask("a.2.1").getId());
    task.setTitle("a.2.1test");
    try {
      taskDao.update(task);
      fail("User has no access to update child task of a.2");
    } catch (final AccessException ex) {
      assertAccessException(ex, getTask("a.2.1").getId(), AccessType.TASKS, OperationType.UPDATE);
    }

    initTestDB.addUser("taskTest2");
    logon("taskTest2");
    initTestDB.addGroup("taskTest2", new String[]{"taskTest2"});
    initTestDB.createGroupTaskAccess(getGroup("taskTest2"), getTask("a.1"), AccessType.TASKS, true, true, true, true);
    initTestDB.createGroupTaskAccess(getGroup("taskTest2"), getTask("a.2"), AccessType.TASKS, true, true, true, false);
    task = taskDao.getById(getTask("a.2.1").getId());
    taskDao.setParentTask(task, getTask("a.1").getId());
    try {
      taskDao.update(task);
      fail("User has no access to delete child task from a.2");
    } catch (final AccessException ex) {
      assertAccessException(ex, getTask("a.2").getId(), AccessType.TASKS, OperationType.DELETE);
    }
  }

  @Test
  public void checkAccess() {
    logon(AbstractTestBase.TEST_ADMIN_USER);
    final TaskDO task = initTestDB.addTask("checkAccessTestTask", "root");
    initTestDB.addGroup("checkAccessTestGroup", new String[]{AbstractTestBase.TEST_USER});
    initTestDB.createGroupTaskAccess(getGroup("checkAccessTestGroup"), getTask("checkAccessTestTask"), AccessType.TASKS,
            true, true, true,
            true);
    logon(AbstractTestBase.TEST_FINANCE_USER);
    final Kost2ArtDO kost2Art = new Kost2ArtDO();
    kost2Art.setId(42);
    kost2Art.setName("Test");
    kost2ArtDao.save(kost2Art);
    final Kost2DO kost2 = new Kost2DO();
    kost2.setNummernkreis(3);
    kost2.setBereich(0);
    kost2.setTeilbereich(42);
    kost2.setKost2Art(kost2Art);
    kost2Dao.save(kost2);
    final ProjektDO projekt = new ProjektDO();
    projekt.setInternKost2_4(123);
    projekt.setName("Testprojekt");
    projektDao.save(projekt);
    checkAccess(AbstractTestBase.TEST_ADMIN_USER, task.getId(), projekt, kost2);
    checkAccess(AbstractTestBase.TEST_USER, task.getId(), projekt, kost2);
  }

  @Test
  public void checkKost2AndTimesheetBookingStatusAccess() {
    logon(AbstractTestBase.TEST_FINANCE_USER);
    final TaskDO task = initTestDB.addTask("checkKost2AndTimesheetStatusAccessTask", "root");
    final String groupName = "checkKost2AndTimesheetBookingStatusAccessGroup";
    // Please note: TEST_USER is no project manager or assistant!
    final GroupDO projectManagers = initTestDB.addGroup(groupName,
            new String[]{AbstractTestBase.TEST_PROJECT_MANAGER_USER, AbstractTestBase.TEST_PROJECT_ASSISTANT_USER,
                    AbstractTestBase.TEST_USER});
    initTestDB.createGroupTaskAccess(projectManagers, task, AccessType.TASKS, true, true, true, true); // All rights.
    final ProjektDO projekt = new ProjektDO().setName("checkKost2AndTimesheetBookingStatusAccess").setInternKost2_4(764)
            .setNummer(1)
            .setProjektManagerGroup(projectManagers).setTask(task);
    projektDao.save(projekt);
    logon(AbstractTestBase.TEST_USER);
    TaskDO task1 = new TaskDO().setParentTask(task).setTitle("Task 1").setKost2BlackWhiteList("Hurzel");
    try {
      taskDao.save(task1);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      assertEquals("task.error.kost2Readonly", ex.getI18nKey()); // OK
    }
    try {
      task1.setKost2BlackWhiteList(null);
      task1.setKost2IsBlackList(true);
      taskDao.save(task1);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      assertEquals("task.error.kost2Readonly", ex.getI18nKey()); // OK
    }
    try {
      task1.setKost2IsBlackList(false);
      task1.setTimesheetBookingStatus(TimesheetBookingStatus.ONLY_LEAFS);
      taskDao.save(task1);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      assertEquals("task.error.timesheetBookingStatus2Readonly", ex.getI18nKey()); // OK
    }
    logon(AbstractTestBase.TEST_PROJECT_MANAGER_USER);
    task1.setKost2IsBlackList(true);
    task1.setTimesheetBookingStatus(TimesheetBookingStatus.ONLY_LEAFS);
    task1 = taskDao.getById(taskDao.save(task1));
    logon(AbstractTestBase.TEST_USER);
    task1.setKost2BlackWhiteList("123456");
    try {
      taskDao.update(task1);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      assertEquals("task.error.kost2Readonly", ex.getI18nKey()); // OK
    }
    try {
      task1.setKost2BlackWhiteList(null);
      task1.setKost2IsBlackList(false);
      taskDao.update(task1);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      assertEquals("task.error.kost2Readonly", ex.getI18nKey()); // OK
    }
    try {
      task1.setKost2IsBlackList(true);
      task1.setTimesheetBookingStatus(TimesheetBookingStatus.INHERIT);
      taskDao.update(task1);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      assertEquals("task.error.timesheetBookingStatus2Readonly", ex.getI18nKey()); // OK
    }
    logon(AbstractTestBase.TEST_PROJECT_MANAGER_USER);
    task1.setKost2BlackWhiteList("123456");
    task1.setKost2IsBlackList(false);
    task1.setTimesheetBookingStatus(TimesheetBookingStatus.INHERIT);
    taskDao.update(task1);
  }

  private void checkAccess(final String user, final Serializable id, final ProjektDO projekt, final Kost2DO kost2) {
    logon(user);
    TaskDO task = taskDao.getById(id);
    task.setProtectTimesheetsUntil(new Date());
    try {
      taskDao.update(task);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      // OK
      assertEquals("task.error.protectTimesheetsUntilReadonly", ex.getI18nKey());
    }
    task.setProtectTimesheetsUntil(null);
    task.setProtectionOfPrivacy(true);
    try {
      taskDao.update(task);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      // OK
      assertEquals("task.error.protectionOfPrivacyReadonly", ex.getI18nKey());
    }
    task = taskDao.getById(id);
    task = new TaskDO();
    task.setParentTask(getTask("checkAccessTestTask"));
    task.setProtectTimesheetsUntil(new Date());
    try {
      taskDao.save(task);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      // OK
      assertEquals("task.error.protectTimesheetsUntilReadonly", ex.getI18nKey());
    }
    task.setProtectTimesheetsUntil(null);
    task.setProtectionOfPrivacy(true);
    try {
      taskDao.save(task);
      fail("AccessException expected.");
    } catch (final AccessException ex) {
      // OK
      assertEquals("task.error.protectionOfPrivacyReadonly", ex.getI18nKey());
    }
    task = taskDao.getById(id);
  }

  /**
   * Sister tasks should have different names.
   */
  @Test
  public void testDuplicateTaskNames() {
    initTestDB.addTask("duplicateTaskNamesTest", "root");
    initTestDB.addTask("dT.1", "duplicateTaskNamesTest");
    initTestDB.addTask("dT.2", "duplicateTaskNamesTest");
    initTestDB.addTask("dT.1.1", "dT.1");
    try {
      // Try to insert sister task with same name:
      initTestDB.addTask("dT.1.1", "dT.1");
      fail("Duplicate task was not detected.");
    } catch (final UserException ex) {
      assertEquals(TaskDao.I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS, ex.getI18nKey());
    }
    TaskDO task = initTestDB.addTask("dT.1.2", "dT.1");
    task.setTitle("dT.1.1");
    try {
      // Try to rename task to same name as a sister task:
      taskDao.internalUpdate(task);
      fail("Duplicate task was not detected.");
    } catch (final UserException ex) {
      assertEquals(TaskDao.I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS, ex.getI18nKey());
    }
    task = initTestDB.addTask("dT.1.1", "dT.2");
    task.setParentTask(initTestDB.getTask("dT.1"));
    try {
      // Try to move task from dT.1.2 to dT.1.1 where already a task with the same name exists.
      taskDao.internalUpdate(task);
      fail("Duplicate task was not detected.");
    } catch (final UserException ex) {
      assertEquals(TaskDao.I18N_KEY_ERROR_DUPLICATE_CHILD_TASKS, ex.getI18nKey());
    }
    task.setParentTask(initTestDB.getTask("dT.2"));
    taskDao.internalUpdate(task);
  }

  @Test
  public void readTotalDuration() {
    logon(getUser(AbstractTestBase.TEST_ADMIN_USER));
    final TaskDO task = initTestDB.addTask("totalDurationTask", "root");
    final TaskDO subTask1 = initTestDB.addTask("totalDurationTask.subtask1", "totalDurationTask");
    final TaskDO subTask2 = initTestDB.addTask("totalDurationTask.subtask2", "totalDurationTask");
    final TaskTree taskTree = TaskTreeHelper.getTaskTree();
    assertEquals(0, taskDao.readTotalDuration(task.getId()));
    final DateHolder dh = new DateHolder();
    dh.setDate(2010, Calendar.APRIL, 20, 8, 0);
    TimesheetDO ts = new TimesheetDO();
    ts.setUser(getUser(AbstractTestBase.TEST_USER));
    ts.setStartDate(dh.getDate())
            .setStopTime(dh.add(Calendar.HOUR_OF_DAY, 4).getTimestamp());
    ts.setTask(task);
    timesheetDao.save(ts);
    assertEquals(4 * 3600, taskDao.readTotalDuration(task.getId()));
    assertEquals(4 * 3600, getTotalDuration(taskTree, task.getId()));
    ts = new TimesheetDO();
    ts.setUser(getUser(AbstractTestBase.TEST_USER));
    ts.setStartDate(dh.add(Calendar.HOUR_OF_DAY, 1).getDate())
            .setStopTime(dh.add(Calendar.HOUR_OF_DAY, 4).getTimestamp());
    ts.setTask(task);
    timesheetDao.save(ts);
    assertEquals(8 * 3600, taskDao.readTotalDuration(task.getId()));
    assertEquals(8 * 3600, getTotalDuration(taskTree, task.getId()));
    ts = new TimesheetDO();
    ts.setUser(getUser(AbstractTestBase.TEST_USER));
    ts.setStartDate(dh.add(Calendar.HOUR_OF_DAY, 1).getDate())
            .setStopTime(dh.add(Calendar.HOUR_OF_DAY, 4).getTimestamp());
    ts.setTask(subTask1);
    timesheetDao.save(ts);
    final List<Object[]> list = taskDao.readTotalDurations();
    boolean taskFound = false;
    boolean subtask1Found = false;
    for (final Object[] oa : list) {
      final Integer taskId = (Integer) oa[1];
      if (taskId == task.getId()) {
        assertFalse(taskFound, "Entry should only exist once.");
        assertFalse(subtask1Found, "Entry not first.");
        taskFound = true;
        assertEquals(new Long(8 * 3600), oa[0]);
      } else if (taskId == subTask1.getId()) {
        assertFalse(subtask1Found, "Entry should only exist once.");
        assertTrue(taskFound, "Entry not second.");
        subtask1Found = true;
        assertEquals(new Long(4 * 3600), oa[0]);
      } else if (taskId == subTask2.getId()) {
        fail("Entry not not expected.");
      }
    }
    assertEquals(12 * 3600, getTotalDuration(taskTree, task.getId()));
    assertEquals(8 * 3600, getDuration(taskTree, task.getId()));
    assertEquals(4 * 3600, getTotalDuration(taskTree, subTask1.getId()));
    assertEquals(4 * 3600, getDuration(taskTree, subTask1.getId()));
    assertEquals(0, getTotalDuration(taskTree, subTask2.getId()));
    assertEquals(0, getDuration(taskTree, subTask2.getId()));
    TaskTreeHelper.getTaskTree().refresh(); // Should be same after refresh (there was an error).
    assertEquals(12 * 3600, getTotalDuration(taskTree, task.getId()));
    assertEquals(8 * 3600, getDuration(taskTree, task.getId()));
    assertEquals(4 * 3600, getTotalDuration(taskTree, subTask1.getId()));
    assertEquals(4 * 3600, getDuration(taskTree, subTask1.getId()));
    assertEquals(0, getTotalDuration(taskTree, subTask2.getId()));
    assertEquals(0, getDuration(taskTree, subTask2.getId()));
  }

  private long getTotalDuration(final TaskTree taskTree, final Integer taskId) {
    return taskTree.getTaskNodeById(taskId).getDuration(taskTree, true);
  }

  private long getDuration(final TaskTree taskTree, final Integer taskId) {
    return taskTree.getTaskNodeById(taskId).getDuration(taskTree, false);
  }

  private void traverseTaskTree(final TaskNode node) {
    logDot();
    final List<TaskNode> childs = node.getChilds();
    if (childs != null) {
      for (final TaskNode child : childs) {
        assertEquals(node.getId(), child.getParentId(), "Child should have parent id of current node.");
        traverseTaskTree(child);
      }
    }
  }
}
