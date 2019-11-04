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

package org.projectforge.timesheet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
public class TimesheetTestFork extends AbstractTestBase {
  // private static final Logger log = Logger.getLogger(TaskTest.class);
  @Autowired
  TimesheetDao timesheetDao;

  @Autowired
  TaskDao taskDao;

  DateHolder date;

  @BeforeEach
  public void setUp() {
    date = new DateHolder(new Date(), DatePrecision.MINUTE_15, Locale.GERMAN);
  }

  @Test
  public void hasSelectAccess() {
    final Serializable[] id = new Serializable[1];
    emf.runInTrans(emgr -> {
        initTestDB.addTask("ts-hasSelectAccess-task", "root");
        initTestDB.addUser("ts-hasSelectAccess-user");
        final TimesheetDO ts = new TimesheetDO();
        final long current = System.currentTimeMillis();

        ts.setTask(initTestDB.getTask("ts-hasSelectAccess-task"));
        ts.setUser(getUser("ts-hasSelectAccess-user"));
        ts.setLocation("Office");
        ts.setDescription("A lot of stuff done and more.");
        ts.setStartTime(new Timestamp(current));
        ts.setStopTime(new Timestamp(current + 2 * 60 * 60 * 1000));
        id[0] = timesheetDao.internalSave(ts);
        timesheetDao.internalSave(ts);
        return null;
    });
    emf.runInTrans(emgr -> {
        logon(getUser("ts-hasSelectAccess-user"));
        final TimesheetDO ts = timesheetDao.getById(id[0]); // Has no access, but is owner of this timesheet
        assertEquals("Field should be hidden", TimesheetDao.HIDDEN_FIELD_MARKER, ts.getShortDescription());
        assertEquals("Field should be hidden", TimesheetDao.HIDDEN_FIELD_MARKER, ts.getDescription());
        assertEquals("Field should be hidden", TimesheetDao.HIDDEN_FIELD_MARKER, ts.getLocation());
        return null;
    });
    emf.runInTrans(emgr -> {
        final TimesheetDO ts = timesheetDao.internalGetById(id[0]);
        assertEquals("Field should not be overwritten", "A lot of stuff done and more.", ts.getShortDescription());
        assertEquals("Field should not be overwritten", "A lot of stuff done and more.", ts.getDescription());
        assertEquals("Field should not be overwritten", "Office", ts.getLocation());
        return null;
    });
  }

  @Test
  public void saveAndModify() {
    initTestDB.addTask("saveAndModify-task", "root");
    initTestDB.addUser("saveAndModify-user");
    final TimesheetDO ts1 = new TimesheetDO();
    final long current = System.currentTimeMillis();
    ts1.setStartTime(new Timestamp(current));
    ts1.setStopTime(new Timestamp(current + 2 * 60 * 60 * 1000));
    try {
      timesheetDao.internalSave(ts1);
      fail("timesheet without task and/or user should not be possible.");
    } catch (final Exception ex) {
    }
    ts1.setTask(getTask("saveAndModify-task"));
    try {
      timesheetDao.internalSave(ts1);
      fail("timesheet without user should not be possible.");
    } catch (final Exception ex) {
    }
    ts1.setTask(null);
    ts1.setUser(getUser("saveAndModify-user"));
    try {
      timesheetDao.internalSave(ts1);
      fail("timesheet without task and/or user should not be possible.");
    } catch (final Exception ex) {
    }
    ts1.setTask(getTask("saveAndModify-task"));
    ts1.setStartTime(new Timestamp(current));
    ts1.setStopTime(new Timestamp(current + 2 * 60 * 60 * 1000));
    timesheetDao.internalSave(ts1);
    // ToDo: Check onSaveOrUpdate: kost2Id vs. task!
  }

  @Test
  public void testOverlap() {
    logon(AbstractTestBase.ADMIN);
    initTestDB.addTask("timesheet", "root");
    initTestDB.addUser("timesheet-user");
    TimesheetDO ts1 = new TimesheetDO();
    ts1.setTask(getTask("timesheet"));
    ts1.setUser(getUser("timesheet-user"));
    setTimeperiod(ts1, 21, 8, 0, 21, 16, 0); // 11/21 from 8:00 to 16:00
    Serializable id = timesheetDao.save(ts1);
    ts1 = timesheetDao.internalGetById(id);

    final TimesheetDO ts2 = new TimesheetDO();
    ts2.setTask(getTask("timesheet"));
    ts2.setUser(getUser("timesheet-user"));
    setTimeperiod(ts2, 21, 15, 52, 21, 18, 0); // 11/21 from 15:45 to 18:00
    try {
      timesheetDao.save(ts2); // Overlap with ts1!
      fail();
    } catch (final UserException ex) {
      assertEquals("timesheet.error.timeperiodOverlapDetection", ex.getI18nKey());
    }
    setTimeperiod(ts2, 21, 7, 0, 21, 8, 15); // 11/21 from 07:00 to 08:15
    try {
      timesheetDao.save(ts2); // Overlap with ts1!
      fail();
    } catch (final UserException ex) {
      assertEquals("timesheet.error.timeperiodOverlapDetection", ex.getI18nKey());
    }
    setTimeperiod(ts2, 21, 16, 0, 21, 18, 0); // 11/21 from 16:00 to 18:00
    final Serializable id2 = timesheetDao.save(ts2); // No overlap, OK.

    TimesheetDO ts3 = new TimesheetDO();
    ts3.setTask(getTask("timesheet"));
    ts3.setUser(getUser("timesheet-user"));
    setTimeperiod(ts3, 21, 16, 0, 21, 18, 0); // 11/21 from 16:00 to 18:00
    try {
      timesheetDao.save(ts3); // Overlap with ts1!
      fail();
    } catch (final UserException ex) {
      assertEquals("timesheet.error.timeperiodOverlapDetection", ex.getI18nKey());
    }
    emf.runInTrans(emgr -> {
        final TimesheetDO t = timesheetDao.internalGetById(id2);
        timesheetDao.markAsDeleted(t); // Delete conflicting time sheet
        return null;
    });
    id = timesheetDao.save(ts3); // No overlap, OK.
    ts3 = timesheetDao.getById(id);
    try {
      timesheetDao.undelete(ts2); // Overlap with ts1!
      fail();
    } catch (final UserException ex) {
      assertEquals("timesheet.error.timeperiodOverlapDetection", ex.getI18nKey());
    }
  }

  @Test
  public void testTimesheetProtection() {
    logon(AbstractTestBase.ADMIN);
    // Create test tasks:
    initTestDB.addUser("tpt-user");
    TaskDO task;
    task = initTestDB.addTask("timesheetprotectiontest", "root");
    task = initTestDB.addTask("tpt", "timesheetprotectiontest");
    task = initTestDB.addTask("tpt.1", "tpt");
    task = initTestDB.addTask("tpt.1.1", "tpt.1");
    task = initTestDB.addTask("tpt.2", "tpt");
    date.setDate(2008, Calendar.OCTOBER, 31, 0, 0, 0);
    task.setProtectTimesheetsUntil(date.getDate());
    taskDao.internalUpdate(task); // Without check access.
    task = initTestDB.addTask("tpt.2.1", "tpt.2");
    TimesheetDO sheet = new TimesheetDO();
    sheet.setUser(getUser("tpt-user"));
    System.out.println(sheet.getUserId());
    sheet.setTask(getTask("tpt.2.1"));
    setTimeperiod(sheet, 2008, Calendar.OCTOBER, 01, 7, 0, 21, 8, 15); // 10/01 from 07:00 to 08:15
    try {
      timesheetDao.save(sheet);
      fail("AccessException caused by time sheet violation expected.");
    } catch (final AccessException ex) {
      // OK
    }
    setTimeperiod(sheet, 2008, Calendar.OCTOBER, 31, 23, 45, 31, 0, 15); // 10/30 from 23:45 to 00:15
    try {
      timesheetDao.save(sheet);
      fail("AccessException caused by time sheet violation expected.");
    } catch (final AccessException ex) {
      // OK
    }
    setTimeperiod(sheet, 2008, Calendar.NOVEMBER, 1, 0, 0, 1, 2, 15); // 11/01 from 00:00 to 02:15
    final Serializable id = timesheetDao.save(sheet);
    sheet = timesheetDao.getById(id);
    date.setDate(2008, Calendar.OCTOBER, 31, 23, 45, 0);
    sheet.setStartTime(date.getTimestamp());
    try {
      timesheetDao.update(sheet);
      fail("AccessException caused by time sheet violation expected.");
    } catch (final AccessException ex) {
      // OK
    }
    task = getTask("tpt.2");
    date.setDate(2008, Calendar.NOVEMBER, 30, 0, 0, 0); // Change protection date, so time sheet is now protected.
    task.setProtectTimesheetsUntil(date.getDate());
    taskDao.internalUpdate(task); // Without check access.
    sheet = timesheetDao.getById(id);
    sheet.setDescription("Hurzel"); // Should work, because start and stop time is not modified.
    timesheetDao.update(sheet);
    date.setDate(2008, Calendar.NOVEMBER, 1, 2, 0, 0);
    sheet = timesheetDao.getById(id);
    sheet.setStopTime(date.getTimestamp());
    try {
      timesheetDao.update(sheet);
      fail("AccessException caused by time sheet violation expected.");
    } catch (final AccessException ex) {
      // OK
    }
    sheet = timesheetDao.getById(id);
    try {
      timesheetDao.markAsDeleted(sheet);
      fail("AccessException caused by time sheet violation expected.");
    } catch (final AccessException ex) {
      // OK
    }
  }

  @Test
  public void testTaskBookable() {
    initTestDB.addTask("taskBookable", "root");
    final TaskDO task1 = initTestDB.addTask("dB.1", "taskBookable");
    final TaskDO task2 = initTestDB.addTask("dB.2", "taskBookable");
    initTestDB.addTask("dB.1.1", "dB.1");
    initTestDB.addUser("ttb-user");
    TimesheetDO sheet = new TimesheetDO();
    sheet.setUser(getUser("ttb-user"));
    sheet.setTask(getTask("dB.1.1"));
    setTimeperiod(sheet, 2009, Calendar.OCTOBER, 01, 7, 0, 01, 8, 15); // 10/01 from 07:00 to 08:15
    timesheetDao.save(sheet);
    task1.setStatus(TaskStatus.C);
    taskDao.internalUpdate(task1);
    task2.setStatus(TaskStatus.C);
    taskDao.internalUpdate(task2);
    sheet = new TimesheetDO();
    sheet.setUser(getUser("ttb-user"));
    sheet.setTask(getTask("dB.1.1"));
    setTimeperiod(sheet, 2009, Calendar.OCTOBER, 02, 7, 0, 02, 8, 15); // 10/02 from 07:00 to 08:15
    try {
      timesheetDao.save(sheet);
      fail("Exception expected: Task should not be bookable because parent task is closed.");
    } catch (final AccessException ex) {
      // OK
    }
    sheet.setTask(getTask("dB.1"));
    try {
      timesheetDao.save(sheet);
      fail("Exception expected: Task should not be bookable because parent task is closed.");
    } catch (final AccessException ex) {
      // OK
    }
  }

  private void setTimeperiod(final TimesheetDO timesheet, final int fromDay, final int fromHour, final int fromMinute,
                             final int toDay, final int toHour, final int toMinute) {
    setTimeperiod(timesheet, 1970, Calendar.NOVEMBER, fromDay, fromHour, fromMinute, toDay, toHour, toMinute);
  }

  private void setTimeperiod(final TimesheetDO timesheet, final int year, final int month, final int fromDay,
                             final int fromHour, final int fromMinute, final int toDay, final int toHour,
                             final int toMinute) {
    date.setDate(year, month, fromDay, fromHour, fromMinute, 0);
    timesheet.setStartTime(date.getTimestamp());
    date.setDate(year, month, toDay, toHour, toMinute, 0);
    timesheet.setStopTime(date.getTimestamp());
  }
}
