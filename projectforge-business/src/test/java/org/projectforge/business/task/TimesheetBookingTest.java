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
import org.projectforge.framework.persistence.jpa.PfPersistenceContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.test.AbstractTestBase;
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
            task = initTestDB.addTask("TimesheetBookingTest", "root", context);
            final GroupTaskAccessDO access = new GroupTaskAccessDO();
            access.setGroup(initTestDB.addGroup("TBT", context, AbstractTestBase.TEST_USER));
            access.addAccessEntry(
                    new AccessEntryDO(AccessType.OWN_TIMESHEETS, true, true, true, true)).setTask(task);
            accessDao.save(access, context);
            logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
            taskDao.update(initTestDB.addTask("TBT-1", "TimesheetBookingTest", context), context);

            task = initTestDB.addTask("TBT-1.1", "TBT-1", context);
            task.setStatus(TaskStatus.C);

            taskDao.update(task, context);
            taskDao.markAsDeleted(initTestDB.addTask("TBT-1.2", "TBT-1", context), context);
            taskDao.update(initTestDB.addTask("TBT-1.2.1", "TBT-1.2", context), context);

            task = initTestDB.addTask("TBT-2", "TimesheetBookingTest", context);
            task.setTimesheetBookingStatus(TimesheetBookingStatus.TREE_CLOSED);

            taskDao.update(task, context);

            task = initTestDB.addTask("TBT-2.1", "TBT-2", context);
            task.setTimesheetBookingStatus(TimesheetBookingStatus.OPENED);

            taskDao.update(task, context);

            task = initTestDB.addTask("TBT-3", "TimesheetBookingTest", context);
            task.setTimesheetBookingStatus(TimesheetBookingStatus.ONLY_LEAFS);
            taskDao.update(task, context);
            initTestDB.addTask("TBT-3.1", "TBT-3", context);
            initTestDB.addTask("TBT-3.1.1", "TBT-3.1", context);
            initTestDB.addTask("TBT-3.1.2", "TBT-3.1", context);
            initTestDB.addTask("TBT-3.2", "TBT-3", context);

            task = initTestDB.addTask("TBT-4", "TimesheetBookingTest", context);
            task.setTimesheetBookingStatus(TimesheetBookingStatus.NO_BOOKING);
            taskDao.update(task, context);

            task = initTestDB.addTask("TBT-4.1", "TBT-4", context);
            task.setTimesheetBookingStatus(TimesheetBookingStatus.INHERIT);
            taskDao.update(task, context);

            task = initTestDB.addTask("TBT-4.1.1", "TBT-4.1", context);
            task.setTimesheetBookingStatus(TimesheetBookingStatus.OPENED);
            taskDao.update(task, context);

            initTestDB.addTask("TBT-5", "TimesheetBookingTest", context);
            initTestDB.addTask("TBT-5.1", "TBT-5", context);
            initTestDB.addTask("TBT-5.1.1", "TBT-5.1", context);
            initTestDB.addTask("TBT-5.1.2", "TBT-5.1", context);
            initTestDB.addTask("TBT-5.2", "TBT-5", context);
            initTestDB.addTask("TBT-5.2.1", "TBT-5.2", context);
            initTestDB.addTask("TBT-5.2.1.1", "TBT-5.2.1", context);
            return null;
        });
    }

    @Test
    public void testTimesheetBookingStatus() {
        persistenceService.runInTransaction(context -> {
            logon(getUser(AbstractTestBase.TEST_USER));
            TimesheetDO sheet = createNewSheet();
            sheet.setTask(getTask("TBT-2"));
            save(sheet, "timesheet.error.taskNotBookable.treeClosedForBooking", context);
            sheet.setTask(getTask("TBT-2.1"));
            save(sheet, "timesheet.error.taskNotBookable.treeClosedForBooking", context);
            sheet.setTask(getTask("TBT-3"));
            save(sheet, "timesheet.error.taskNotBookable.onlyLeafsAllowedForBooking", context);
            sheet.setTask(getTask("TBT-3.1"));
            save(sheet, "timesheet.error.taskNotBookable.onlyLeafsAllowedForBooking", context);
            sheet.setTask(getTask("TBT-3.2"));
            timesheetDao.save(sheet, context); // Leaf task node.
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-3.1.1"));
            timesheetDao.save(sheet, context); // Leaf task node.
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-3.1.2"));
            timesheetDao.save(sheet, context); // Leaf task node.
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-4"));
            save(sheet, "timesheet.error.taskNotBookable.taskClosedForBooking", context);
            sheet.setTask(getTask("TBT-4.1"));
            save(sheet, "timesheet.error.taskNotBookable.taskClosedForBooking", context); // Inherited (not opened)
            sheet.setTask(getTask("TBT-4.1.1"));
            timesheetDao.save(sheet, context); // Opened for booking.
            return null;
        });
    }

    @Test
    public void testOrderPositions() {
        persistenceService.runInTransaction(context -> {
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
            auftragDao.save(auftrag, context);
            return null;
        });
        persistenceService.runInTransaction(context -> {
            logon(getUser(AbstractTestBase.TEST_USER));
            TimesheetDO sheet = createNewSheet();
            sheet.setTask(getTask("TBT-5"));
            save(sheet, "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks", context);
            sheet.setTask(getTask("TBT-5.1"));
            timesheetDao.save(sheet, context);
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-5.1.1"));
            timesheetDao.save(sheet, context);
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-5.1.2"));
            timesheetDao.save(sheet, context);
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-5.2"));
            save(sheet, "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks", context);
            sheet.setTask(getTask("TBT-5.2.1"));
            save(sheet, "timesheet.error.taskNotBookable.orderPositionsFoundInSubTasks", context);
            sheet.setTask(getTask("TBT-5.2.1.1"));
            timesheetDao.save(sheet, context);
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
            timesheetDao.save(sheet, context);
            sheet = createNewSheet();
            sheet.setTask(getTask("TBT-1.1"));
            save(sheet, "timesheet.error.taskNotBookable.taskNotOpened", context);
            sheet.setTask(getTask("TBT-1.2"));
            save(sheet, "timesheet.error.taskNotBookable.taskDeleted", context);
            sheet.setTask(getTask("TBT-1.2.1"));
            save(sheet, "timesheet.error.taskNotBookable.taskDeleted", context);
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

    private void save(final TimesheetDO sheet, final String expectedErrorMsgKey, final PfPersistenceContext context) {
        try {
            timesheetDao.save(sheet, context);
            fail("AccessException expected: " + expectedErrorMsgKey);
        } catch (final AccessException ex) {
            assertEquals(expectedErrorMsgKey, ex.getI18nKey());
        }
    }
}
