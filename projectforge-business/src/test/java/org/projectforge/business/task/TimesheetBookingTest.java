/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.fibu.AuftragsPositionDO;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.common.task.TimesheetBookingStatus;
import org.projectforge.framework.access.*;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TimesheetBookingTest extends AbstractTestBase {
  @Autowired
  private TaskDao taskDao;

  @Autowired
  private AccessDao accessDao;

  @Autowired
  private TimesheetDao timesheetDao;

  @Autowired
  private AuftragDao auftragDao;

  private static DateHolder date;

  @Override
  protected void beforeAll() {
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
  public void testTimesheetBookingStatus() {
    logon(getUser(AbstractTestBase.TEST_USER));
    TimesheetDO sheet = createNewSheet();
    sheet.setTask(getTask("TBT-2"));
    save(sheet, "timesheet.error.taskNotBookable.treeClosedForBooking");
    sheet.setTask(getTask("TBT-2.1"));
    save(sheet, "timesheet.error.taskNotBookable.treeClosedForBooking");
    sheet.setTask(getTask("TBT-3"));
    save(sheet, "timesheet.error.taskNotBookable.onlyLeafsAllowedForBooking");
    sheet.setTask(getTask("TBT-3.1"));
    save(sheet, "timesheet.error.taskNotBookable.onlyLeafsAllowedForBooking");
    sheet.setTask(getTask("TBT-3.2"));
    timesheetDao.save(sheet); // Leaf task node.
    sheet = createNewSheet();
    sheet.setTask(getTask("TBT-3.1.1"));
    timesheetDao.save(sheet); // Leaf task node.
    sheet = createNewSheet();
    sheet.setTask(getTask("TBT-3.1.2"));
    timesheetDao.save(sheet); // Leaf task node.
    sheet = createNewSheet();
    sheet.setTask(getTask("TBT-4"));
    save(sheet, "timesheet.error.taskNotBookable.taskClosedForBooking");
    sheet.setTask(getTask("TBT-4.1"));
    save(sheet, "timesheet.error.taskNotBookable.taskClosedForBooking"); // Inherited (not opened)
    sheet.setTask(getTask("TBT-4.1.1"));
    timesheetDao.save(sheet); // Opened for booking.
  }

  @Test
  public void testOrderPositions() {
    logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
    AuftragsPositionDO pos1 = new AuftragsPositionDO();
    pos1.setTask(getTask("TBT-5.1"));
    pos1.setTitel("Pos 1");
    AuftragsPositionDO pos2 = new AuftragsPositionDO();
    pos1.setTask(getTask("TBT-5.2.1.1"));
    pos1.setTitel("Pos 2");
    final AuftragDO auftrag = new AuftragDO()
            .addPosition(pos1)
            .addPosition(pos2);
    auftrag.setNummer(auftragDao.getNextNumber(auftrag));
    auftragDao.save(auftrag);
    logon(getUser(AbstractTestBase.TEST_USER));
    TimesheetDO sheet = createNewSheet();
    sheet.setTask(getTask("TBT-5"));
    save(sheet, "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks");
    sheet.setTask(getTask("TBT-5.1"));
    timesheetDao.save(sheet);
    sheet = createNewSheet();
    sheet.setTask(getTask("TBT-5.1.1"));
    timesheetDao.save(sheet);
    sheet = createNewSheet();
    sheet.setTask(getTask("TBT-5.1.2"));
    timesheetDao.save(sheet);
    sheet = createNewSheet();
    sheet.setTask(getTask("TBT-5.2"));
    save(sheet, "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks");
    sheet.setTask(getTask("TBT-5.2.1"));
    save(sheet, "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks");
    sheet.setTask(getTask("TBT-5.2.1.1"));
    timesheetDao.save(sheet);
  }

  @Test
  public void testTaskStatus() {
    final PFUserDO user = getUser(AbstractTestBase.TEST_USER);
    logon(user);
    TimesheetDO sheet = createNewSheet();
    sheet.setTask(getTask("TBT-1"));
    timesheetDao.save(sheet);
    sheet = createNewSheet();
    sheet.setTask(getTask("TBT-1.1"));
    save(sheet, "timesheet.error.taskNotBookable.taskNotOpened");
    sheet.setTask(getTask("TBT-1.2"));
    save(sheet, "timesheet.error.taskNotBookable.taskDeleted");
    sheet.setTask(getTask("TBT-1.2.1"));
    save(sheet, "timesheet.error.taskNotBookable.taskDeleted");
  }

  private TimesheetDO createNewSheet() {
    TimesheetDO sheet = new TimesheetDO();
    sheet.setUser(getUser(AbstractTestBase.TEST_USER));
    sheet.setStartDate(date.getDate());
    sheet.setStopDate(date.add(Calendar.HOUR, 1).getDate());
    return sheet;
  }

  private void save(final TimesheetDO sheet, final String expectedErrorMsgKey) {
    try {
      timesheetDao.save(sheet);
      fail("AccessException expected: " + expectedErrorMsgKey);
    } catch (final AccessException ex) {
      assertEquals(expectedErrorMsgKey, ex.getI18nKey());
    }
  }
}
