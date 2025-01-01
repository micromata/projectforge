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

package org.projectforge.business.task;

import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.*;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.common.task.TimesheetBookingStatus;
import org.projectforge.framework.access.*;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.temporal.ChronoUnit;

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

    private static PFDateTime dateTime;

    @Override
    protected void beforeAll() {
        persistenceService.runInTransaction(context -> {
            dateTime = PFDateTime.now().withPrecision(DatePrecision.MINUTE_15);
            logon(getUser(AbstractTestBase.TEST_ADMIN_USER));
            TaskDO task;
            task = initTestDB.addTask("TimesheetBookingTest", "root");
            final GroupTaskAccessDO access = new GroupTaskAccessDO();
            access.setGroup(initTestDB.addGroup("TBT", AbstractTestBase.TEST_USER));
            access.addAccessEntry(
                    new AccessEntryDO(AccessType.OWN_TIMESHEETS, true, true, true, true)).setTask(task);
            accessDao.insert(access);
            logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
            taskDao.update(initTestDB.addTask("TBT-1", "TimesheetBookingTest"));

            task = initTestDB.addTask("TBT-1.1", "TBT-1");
            task.setStatus(TaskStatus.C);

            taskDao.update(task);
            taskDao.markAsDeleted(initTestDB.addTask("TBT-1.2", "TBT-1"));
            taskDao.update(initTestDB.addTask("TBT-1.2.1", "TBT-1.2"));

            task = initTestDB.addTask("TBT-2", "TimesheetBookingTest");
            task.setTimesheetBookingStatus(TimesheetBookingStatus.TREE_CLOSED);

            taskDao.update(task);

            task = initTestDB.addTask("TBT-2.1", "TBT-2");
            task.setTimesheetBookingStatus(TimesheetBookingStatus.OPENED);

            taskDao.update(task);

            task = initTestDB.addTask("TBT-3", "TimesheetBookingTest");
            task.setTimesheetBookingStatus(TimesheetBookingStatus.ONLY_LEAFS);
            taskDao.update(task);
            initTestDB.addTask("TBT-3.1", "TBT-3");
            initTestDB.addTask("TBT-3.1.1", "TBT-3.1");
            initTestDB.addTask("TBT-3.1.2", "TBT-3.1");
            initTestDB.addTask("TBT-3.2", "TBT-3");

            task = initTestDB.addTask("TBT-4", "TimesheetBookingTest");
            task.setTimesheetBookingStatus(TimesheetBookingStatus.NO_BOOKING);
            taskDao.update(task);

            task = initTestDB.addTask("TBT-4.1", "TBT-4");
            task.setTimesheetBookingStatus(TimesheetBookingStatus.INHERIT);
            taskDao.update(task);

            task = initTestDB.addTask("TBT-4.1.1", "TBT-4.1");
            task.setTimesheetBookingStatus(TimesheetBookingStatus.OPENED);
            taskDao.update(task);

            initTestDB.addTask("TBT-5", "TimesheetBookingTest");
            initTestDB.addTask("TBT-5.1", "TBT-5");
            initTestDB.addTask("TBT-5.1.1", "TBT-5.1");
            initTestDB.addTask("TBT-5.1.2", "TBT-5.1");
            initTestDB.addTask("TBT-5.2", "TBT-5");
            initTestDB.addTask("TBT-5.2.1", "TBT-5.2");
            initTestDB.addTask("TBT-5.2.1.1", "TBT-5.2.1");
            return null;
        });
    }

    @Test
    public void testTimesheetBookingStatus() {
        persistenceService.runInTransaction(context -> {
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
            timesheetDao.insert(sheet); // Leaf task node.
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-3.1.1"));
            timesheetDao.insert(sheet); // Leaf task node.
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-3.1.2"));
            timesheetDao.insert(sheet); // Leaf task node.
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-4"));
            save(sheet, "timesheet.error.taskNotBookable.taskClosedForBooking");
            sheet.setTask(getTask("TBT-4.1"));
            save(sheet, "timesheet.error.taskNotBookable.taskClosedForBooking"); // Inherited (not opened)
            sheet.setTask(getTask("TBT-4.1.1"));
            timesheetDao.insert(sheet); // Opened for booking.
            return null;
        });
    }

    @Test
    public void testOrderPositions() {
        persistenceService.runInTransaction(context -> {
            logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
            AuftragsPositionDO pos1 = new AuftragsPositionDO();
            pos1.setStatus(AuftragsStatus.GELEGT);
            pos1.setTask(getTask("TBT-5.1"));
            pos1.setTitel("Pos 1");
            AuftragsPositionDO pos2 = new AuftragsPositionDO();
            pos2.setStatus(AuftragsStatus.GELEGT);
            pos2.setTask(getTask("TBT-5.2.1.1"));
            pos2.setTitel("Pos 2");
            final AuftragDO auftrag = new AuftragDO()
                    .addPosition(pos1)
                    .addPosition(pos2);
            auftrag.setStatus(AuftragsStatus.GELEGT);
            auftrag.setNummer(auftragDao.getNextNumber(auftrag));
            auftragDao.insert(auftrag);
            return null;
        });
        persistenceService.runInTransaction(context -> {
            logon(getUser(AbstractTestBase.TEST_USER));
            TimesheetDO sheet = createNewSheet();
            sheet.setTask(getTask("TBT-5"));
            save(sheet, "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks");
            sheet.setTask(getTask("TBT-5.1"));
            timesheetDao.insert(sheet);
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-5.1.1"));
            timesheetDao.insert(sheet);
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-5.1.2"));
            timesheetDao.insert(sheet);
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-5.2"));
            save(sheet, "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks");
            sheet.setTask(getTask("TBT-5.2.1"));
            save(sheet, "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks");
            sheet.setTask(getTask("TBT-5.2.1.1"));
            timesheetDao.insert(sheet);
            return null;
        });

    }

    @Test
    public void testTaskStatus() {
        persistenceService.runInTransaction(context ->
        {
            final PFUserDO user = getUser(AbstractTestBase.TEST_USER);
            logon(user);
            TimesheetDO sheet = createNewSheet();
            sheet.setTask(getTask("TBT-1"));
            timesheetDao.insert(sheet);
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-1.1"));
            save(sheet, "timesheet.error.taskNotBookable.taskNotOpened");
            sheet.setTask(getTask("TBT-1.2"));
            save(sheet, "timesheet.error.taskNotBookable.taskDeleted");
            sheet.setTask(getTask("TBT-1.2.1"));
            save(sheet, "timesheet.error.taskNotBookable.taskDeleted");
            return null;
        });
    }

    private TimesheetDO createNewSheet() {
        TimesheetDO sheet = new TimesheetDO();
        sheet.setUser(getUser(AbstractTestBase.TEST_USER));
        sheet.setStartDate(dateTime.getUtilDate());
        dateTime = dateTime.plus(1, ChronoUnit.HOURS);
        sheet.setStopDate(dateTime.getUtilDate());
        return sheet;
    }

    private void save(final TimesheetDO sheet, final String expectedErrorMsgKey) {
        try {
            timesheetDao.insert(sheet);
            fail("AccessException expected: " + expectedErrorMsgKey);
        } catch (final AccessException ex) {
            assertEquals(expectedErrorMsgKey, ex.getI18nKey());
        }
    }
}
