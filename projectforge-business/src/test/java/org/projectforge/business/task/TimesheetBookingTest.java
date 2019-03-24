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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.util.Calendar;

import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.fibu.AuftragsPositionDO;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.common.task.TimesheetBookingStatus;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.access.AccessEntryDO;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.test.AbstractTestBase;
import org.projectforge.test.AbstractTestNGBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class TimesheetBookingTest extends AbstractTestNGBase
{
  @Autowired
  private TaskDao taskDao;

  @Autowired
  private AccessDao accessDao;

  @Autowired
  private TimesheetDao timesheetDao;

  @Autowired
  private AuftragDao auftragDao;

  DateHolder date;

  boolean initialized = false;

  public void setAccessDao(final AccessDao accessDao)
  {
    this.accessDao = accessDao;
  }

  public void setTaskDao(final TaskDao taskDao)
  {
    this.taskDao = taskDao;
  }

  public void setTimesheetDao(final TimesheetDao timesheetDao)
  {
    this.timesheetDao = timesheetDao;
  }

  public void setAuftragDao(final AuftragDao auftragDao)
  {
    this.auftragDao = auftragDao;
  }

  private synchronized void initialize() // @BeforeClass not possible because DAOs are needed.
  {
    if (initialized == true) {
      return;
    }
    initialized = true;
    date = new DateHolder(DatePrecision.MINUTE_15);
    logon(getUser(AbstractTestBase.TEST_ADMIN_USER));
    TaskDO task;
    task = initTestDB.addTask("TimesheetBookingTest", "root");
    final GroupTaskAccessDO access = new GroupTaskAccessDO().setGroup(initTestDB.addGroup("TBT", AbstractTestBase.TEST_USER))
        .addAccessEntry(
            new AccessEntryDO(AccessType.OWN_TIMESHEETS, true, true, true, true))
        .setTask(task);
    accessDao.save(access);
    logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
    taskDao.update(initTestDB.addTask("TBT-1", "TimesheetBookingTest"));
    taskDao.update(initTestDB.addTask("TBT-1.1", "TBT-1").setStatus(TaskStatus.C));
    taskDao.markAsDeleted(initTestDB.addTask("TBT-1.2", "TBT-1"));
    taskDao.update(initTestDB.addTask("TBT-1.2.1", "TBT-1.2"));
    taskDao.update(initTestDB.addTask("TBT-2", "TimesheetBookingTest")
        .setTimesheetBookingStatus(TimesheetBookingStatus.TREE_CLOSED));
    taskDao.update(initTestDB.addTask("TBT-2.1", "TBT-2").setTimesheetBookingStatus(TimesheetBookingStatus.OPENED));
    taskDao.update(initTestDB.addTask("TBT-3", "TimesheetBookingTest")
        .setTimesheetBookingStatus(TimesheetBookingStatus.ONLY_LEAFS));
    initTestDB.addTask("TBT-3.1", "TBT-3");
    initTestDB.addTask("TBT-3.1.1", "TBT-3.1");
    initTestDB.addTask("TBT-3.1.2", "TBT-3.1");
    initTestDB.addTask("TBT-3.2", "TBT-3");
    taskDao.update(initTestDB.addTask("TBT-4", "TimesheetBookingTest")
        .setTimesheetBookingStatus(TimesheetBookingStatus.NO_BOOKING));
    taskDao.update(initTestDB.addTask("TBT-4.1", "TBT-4").setTimesheetBookingStatus(TimesheetBookingStatus.INHERIT));
    taskDao.update(initTestDB.addTask("TBT-4.1.1", "TBT-4.1").setTimesheetBookingStatus(TimesheetBookingStatus.OPENED));
    initTestDB.addTask("TBT-5", "TimesheetBookingTest");
    initTestDB.addTask("TBT-5.1", "TBT-5");
    initTestDB.addTask("TBT-5.1.1", "TBT-5.1");
    initTestDB.addTask("TBT-5.1.2", "TBT-5.1");
    initTestDB.addTask("TBT-5.2", "TBT-5");
    initTestDB.addTask("TBT-5.2.1", "TBT-5.2");
    initTestDB.addTask("TBT-5.2.1.1", "TBT-5.2.1");
  }

  @Test
  public void testTimesheetBookingStatus()
  {
    initialize();
    logon(getUser(AbstractTestBase.TEST_USER));
    TimesheetDO sheet = createNewSheet().setTask(getTask("TBT-2"));
    save(sheet, "timesheet.error.taskNotBookable.treeClosedForBooking");
    save(sheet.setTask(getTask("TBT-2.1")), "timesheet.error.taskNotBookable.treeClosedForBooking");
    save(sheet.setTask(getTask("TBT-3")), "timesheet.error.taskNotBookable.onlyLeafsAllowedForBooking");
    save(sheet.setTask(getTask("TBT-3.1")), "timesheet.error.taskNotBookable.onlyLeafsAllowedForBooking");
    timesheetDao.save(sheet.setTask(getTask("TBT-3.2"))); // Leaf task node.
    timesheetDao.save(createNewSheet().setTask(getTask("TBT-3.1.1"))); // Leaf task node.
    timesheetDao.save(createNewSheet().setTask(getTask("TBT-3.1.2"))); // Leaf task node.
    sheet = createNewSheet();
    save(sheet.setTask(getTask("TBT-4")), "timesheet.error.taskNotBookable.taskClosedForBooking");
    save(sheet.setTask(getTask("TBT-4.1")), "timesheet.error.taskNotBookable.taskClosedForBooking"); // Inherited (not opened)
    timesheetDao.save(sheet.setTask(getTask("TBT-4.1.1"))); // Opened for booking.
  }

  @Test
  public void testOrderPositions()
  {
    initialize();
    logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
    final AuftragDO auftrag = new AuftragDO()
        .addPosition(new AuftragsPositionDO().setTask(getTask("TBT-5.1")).setTitel("Pos 1"))
        .addPosition(new AuftragsPositionDO().setTask(getTask("TBT-5.2.1.1")).setTitel("Pos 2"));
    auftragDao.save(auftrag.setNummer(auftragDao.getNextNumber(auftrag)));
    logon(getUser(AbstractTestBase.TEST_USER));
    TimesheetDO sheet = createNewSheet();
    save(sheet.setTask(getTask("TBT-5")), "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks");
    timesheetDao.save(sheet.setTask(getTask("TBT-5.1")));
    timesheetDao.save(createNewSheet().setTask(getTask("TBT-5.1.1")));
    timesheetDao.save(createNewSheet().setTask(getTask("TBT-5.1.2")));
    sheet = createNewSheet();
    save(sheet.setTask(getTask("TBT-5.2")), "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks");
    save(sheet.setTask(getTask("TBT-5.2.1")), "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks");
    timesheetDao.save(sheet.setTask(getTask("TBT-5.2.1.1")));
  }

  @Test
  public void testTaskStatus()
  {
    initialize();
    final PFUserDO user = getUser(AbstractTestBase.TEST_USER);
    logon(user);
    TimesheetDO sheet = createNewSheet().setTask(getTask("TBT-1"));
    timesheetDao.save(sheet);
    sheet = createNewSheet().setTask(getTask("TBT-1.1"));
    save(sheet, "timesheet.error.taskNotBookable.taskNotOpened");
    save(sheet.setTask(getTask("TBT-1.2")), "timesheet.error.taskNotBookable.taskDeleted");
    save(sheet.setTask(getTask("TBT-1.2.1")), "timesheet.error.taskNotBookable.taskDeleted");
  }

  private TimesheetDO createNewSheet()
  {
    return new TimesheetDO().setUser(getUser(AbstractTestBase.TEST_USER)).setStartDate(date.getDate()).setStopTime(
        date.add(Calendar.MINUTE, 15).getTimestamp());
  }

  private void save(final TimesheetDO sheet, final String expectedErrorMsgKey)
  {
    try {
      timesheetDao.save(sheet);
      fail("AccessException expected: " + expectedErrorMsgKey);
    } catch (final AccessException ex) {
      assertEquals(expectedErrorMsgKey, ex.getI18nKey());
    }
  }
}
