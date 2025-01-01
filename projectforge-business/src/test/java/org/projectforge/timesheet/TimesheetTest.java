/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Test;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.test.AbstractTestBase;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.common.i18n.UserException;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.PFDateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TimesheetTest extends AbstractTestBase {
    // private static final Logger log = Logger.getLogger(TaskTest.class);
    @Autowired
    TimesheetDao timesheetDao;

    @Autowired
    TaskDao taskDao;

    @Test
    public void hasSelectAccess() {
        persistenceService.runInTransaction(context ->
        {
            final Serializable[] id = new Serializable[1];

            initTestDB.addTask("ts-hasSelectAccess-task", "root");
            initTestDB.addUser("ts-hasSelectAccess-user");
            TimesheetDO ts = new TimesheetDO();
            final long current = System.currentTimeMillis();

            ts.setTask(initTestDB.getTask("ts-hasSelectAccess-task"));
            ts.setUser(getUser("ts-hasSelectAccess-user"));
            ts.setLocation("Office");
            ts.setDescription("A lot of stuff done and more.");
            PFDateTime startTime = PFDateTime.now().withPrecision(DatePrecision.MINUTE_15);
            ts.setStartTime(startTime.getUtilDate());
            ts.setStopTime(startTime.plusHours(2).getUtilDate());
            id[0] = timesheetDao.insert(ts, false);

            logon(getUser("ts-hasSelectAccess-user"));
            ts = timesheetDao.find(id[0]); // Has no access, but is owner of this timesheet
            assertEquals(TimesheetDao.HIDDEN_FIELD_MARKER, ts.getShortDescription(), "Field should be hidden");
            assertEquals(TimesheetDao.HIDDEN_FIELD_MARKER, ts.getDescription(), "Field should be hidden");
            assertEquals(TimesheetDao.HIDDEN_FIELD_MARKER, ts.getLocation(), "Field should be hidden");
            ts = timesheetDao.find(id[0], false);
            assertEquals("A lot of stuff done and more.", ts.getShortDescription(), "Field should not be overwritten");
            assertEquals("A lot of stuff done and more.", ts.getDescription(), "Field should not be overwritten");
            assertEquals("Office", ts.getLocation(), "Field should not be overwritten");
            return null;
        });

    }

    @Test
    public void saveAndModify() {
        persistenceService.runInTransaction(context ->
        {
            initTestDB.addTask("saveAndModify-task", "root");
            initTestDB.addUser("saveAndModify-user");
            final TimesheetDO ts1 = new TimesheetDO();
            final long current = System.currentTimeMillis();
            ts1.setStartTime(new Date(current));
            ts1.setStopTime(new Date(current + 2 * 60 * 60 * 1000));
            try {
                timesheetDao.insert(ts1, false);
                fail("timesheet without task and/or user should not be possible.");
            } catch (final Exception ex) {
            }
            ts1.setTask(getTask("saveAndModify-task"));
            try {
                timesheetDao.insert(ts1, false);
                fail("timesheet without user should not be possible.");
            } catch (final Exception ex) {
            }
            ts1.setTask(null);
            ts1.setUser(getUser("saveAndModify-user"));
            try {
                timesheetDao.insert(ts1, false);
                fail("timesheet without task and/or user should not be possible.");
            } catch (final Exception ex) {
            }
            ts1.setTask(getTask("saveAndModify-task"));
            PFDateTime startTime = PFDateTime.now().withPrecision(DatePrecision.MINUTE_15);
            ts1.setStartTime(startTime.getUtilDate());
            ts1.setStopTime(startTime.plusHours(2).getUtilDate());
            timesheetDao.insert(ts1, false);
            // ToDo: Check onSaveOrUpdate: kost2Id vs. task!
            return null;
        });

    }

    @Test
    public void testOverlap() {
        persistenceService.runInTransaction(context ->
        {
            logon(AbstractTestBase.ADMIN);
            initTestDB.addTask("timesheet", "root");
            initTestDB.addUser("timesheet-user");
            TimesheetDO ts1 = new TimesheetDO();
            ts1.setTask(getTask("timesheet"));
            ts1.setUser(getUser("timesheet-user"));
            setTimeperiod(ts1, 21, 8, 0, 21, 16, 0); // 11/21 from 8:00 to 16:00
            Serializable id = timesheetDao.insert(ts1);
            ts1 = timesheetDao.find(id, false);

            final TimesheetDO ts2 = new TimesheetDO();
            ts2.setTask(getTask("timesheet"));
            ts2.setUser(getUser("timesheet-user"));
            setTimeperiod(ts2, 21, 15, 52, 21, 18, 0); // 11/21 from 15:45 to 18:00
            try {
                timesheetDao.insert(ts2); // Overlap with ts1!
                fail();
            } catch (final UserException ex) {
                assertEquals("timesheet.error.timeperiodOverlapDetection", ex.getI18nKey());
            }
            setTimeperiod(ts2, 21, 7, 0, 21, 8, 15); // 11/21 from 07:00 to 08:15
            try {
                timesheetDao.insert(ts2); // Overlap with ts1!
                fail();
            } catch (final UserException ex) {
                assertEquals("timesheet.error.timeperiodOverlapDetection", ex.getI18nKey());
            }
            setTimeperiod(ts2, 21, 16, 0, 21, 18, 0); // 11/21 from 16:00 to 18:00
            final Serializable id2 = timesheetDao.insert(ts2); // No overlap, OK.

            TimesheetDO ts3 = new TimesheetDO();
            ts3.setTask(getTask("timesheet"));
            ts3.setUser(getUser("timesheet-user"));
            setTimeperiod(ts3, 21, 16, 0, 21, 18, 0); // 11/21 from 16:00 to 18:00
            try {
                timesheetDao.insert(ts3); // Overlap with ts1!
                fail();
            } catch (final UserException ex) {
                assertEquals("timesheet.error.timeperiodOverlapDetection", ex.getI18nKey());
            }

            final TimesheetDO t = timesheetDao.find(id2, false);
            timesheetDao.markAsDeleted(t); // Delete conflicting time sheet

            id = timesheetDao.insert(ts3); // No overlap, OK.
            ts3 = timesheetDao.find(id);
            try {
                timesheetDao.undelete(ts2); // Overlap with ts1!
                fail();
            } catch (final UserException ex) {
                assertEquals("timesheet.error.timeperiodOverlapDetection", ex.getI18nKey());
            }
            return null;
        });
    }

    @Test
    public void testTimesheetProtection() {
        persistenceService.runInTransaction(context ->
        {
            logon(AbstractTestBase.ADMIN);
            // Create test tasks:
            initTestDB.addUser("tpt-user");
            TaskDO task;
            task = initTestDB.addTask("timesheetprotectiontest", "root");
            task = initTestDB.addTask("tpt", "timesheetprotectiontest");
            task = initTestDB.addTask("tpt.1", "tpt");
            task = initTestDB.addTask("tpt.1.1", "tpt.1");
            task = initTestDB.addTask("tpt.2", "tpt");
            PFDateTime date = PFDateTime.withDate(2008, Month.OCTOBER, 31, 0, 0, 0);
            task.setProtectTimesheetsUntil(date.getLocalDate());
            taskDao.update(task, false); // Without check access.
            task = initTestDB.addTask("tpt.2.1", "tpt.2");
            TimesheetDO sheet = new TimesheetDO();
            sheet.setUser(getUser("tpt-user"));
            System.out.println(sheet.getUserId());
            sheet.setTask(getTask("tpt.2.1"));
            setTimeperiod(sheet, 2008, Month.OCTOBER, 1, 7, 0, 21, 8, 15); // 10/01 from 07:00 to 08:15
            try {
                timesheetDao.insert(sheet);
                fail("AccessException caused by time sheet violation expected.");
            } catch (final AccessException ex) {
                // OK
            }
            setTimeperiod(sheet, 2008, Month.OCTOBER, 31, 23, 45, 31, 0, 15); // 10/30 from 23:45 to 00:15
            try {
                timesheetDao.insert(sheet);
                fail("AccessException caused by time sheet violation expected.");
            } catch (final AccessException ex) {
                // OK
            }
            setTimeperiod(sheet, 2008, Month.NOVEMBER, 1, 0, 0, 1, 2, 15); // 11/01 from 00:00 to 02:15
            final Serializable id = timesheetDao.insert(sheet);
            sheet = timesheetDao.find(id);
            date = PFDateTime.withDate(2008, Month.OCTOBER, 31, 23, 45, 0);
            sheet.setStartTime(date.getUtilDate());
            try {
                timesheetDao.update(sheet);
                fail("AccessException caused by time sheet violation expected.");
            } catch (final AccessException ex) {
                // OK
            }
            task = getTask("tpt.2");
            task.setProtectTimesheetsUntil(LocalDate.of(2008, Month.NOVEMBER, 30)); // Change protection date, so time sheet is now protected.
            taskDao.update(task, false); // Without check access.
            sheet = timesheetDao.find(id);
            sheet.setDescription("Hurzel"); // Should work, because start and stop time is not modified.
            timesheetDao.update(sheet);
            date = PFDateTime.withDate(2008, Month.NOVEMBER, 1, 2, 0, 0);
            sheet = timesheetDao.find(id);
            sheet.setStopTime(date.getUtilDate());
            try {
                timesheetDao.update(sheet);
                fail("AccessException caused by time sheet violation expected.");
            } catch (final AccessException ex) {
                // OK
            }
            sheet = timesheetDao.find(id);
            try {
                timesheetDao.markAsDeleted(sheet);
                fail("AccessException caused by time sheet violation expected.");
            } catch (final AccessException ex) {
                // OK
            }
            return null;
        });
    }

    @Test
    public void testTaskBookable() {
        persistenceService.runInTransaction(context ->
        {
            initTestDB.addTask("taskBookable", "root");
            final TaskDO task1 = initTestDB.addTask("dB.1", "taskBookable");
            final TaskDO task2 = initTestDB.addTask("dB.2", "taskBookable");
            initTestDB.addTask("dB.1.1", "dB.1");
            initTestDB.addUser("ttb-user");
            TimesheetDO sheet = new TimesheetDO();
            sheet.setUser(getUser("ttb-user"));
            sheet.setTask(getTask("dB.1.1"));
            setTimeperiod(sheet, 2009, Month.OCTOBER, 1, 7, 0, 1, 8, 15); // 10/01 from 07:00 to 08:15
            timesheetDao.insert(sheet);
            task1.setStatus(TaskStatus.C);
            taskDao.update(task1, false);
            task2.setStatus(TaskStatus.C);
            taskDao.update(task2, false);
            sheet = new TimesheetDO();
            sheet.setUser(getUser("ttb-user"));
            sheet.setTask(getTask("dB.1.1"));
            setTimeperiod(sheet, 2009, Month.OCTOBER, 2, 7, 0, 2, 8, 15); // 10/02 from 07:00 to 08:15
            try {
                timesheetDao.insert(sheet);
                fail("Exception expected: Task should not be bookable because parent task is closed.");
            } catch (final AccessException ex) {
                // OK
            }
            sheet.setTask(getTask("dB.1"));
            try {
                timesheetDao.insert(sheet);
                fail("Exception expected: Task should not be bookable because parent task is closed.");
            } catch (final AccessException ex) {
                // OK
            }
            return null;
        });
    }

    private void setTimeperiod(final TimesheetDO timesheet, final int fromDay, final int fromHour,
                               final int fromMinute,
                               final int toDay, final int toHour, final int toMinute) {
        setTimeperiod(timesheet, 1970, Month.NOVEMBER, fromDay, fromHour, fromMinute, toDay, toHour, toMinute);
    }

    private void setTimeperiod(final TimesheetDO timesheet, final int year, final Month month, final int fromDay,
                               final int fromHour, final int fromMinute, final int toDay, final int toHour,
                               final int toMinute) {
        PFDateTime date = PFDateTime.withDate(year, month, fromDay, fromHour, fromMinute, 0);
        timesheet.setStartTime(date.getUtilDate());
        date = PFDateTime.withDate(year, month, toDay, toHour, toMinute, 0);
        timesheet.setStopTime(date.getUtilDate());
    }
}
