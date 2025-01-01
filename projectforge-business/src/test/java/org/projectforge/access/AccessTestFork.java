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

package org.projectforge.access;

import org.junit.jupiter.api.Test;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.access.AccessEntryDO;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.business.test.AbstractTestBase;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.PFDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AccessTestFork extends AbstractTestBase {
    private static final Logger log = LoggerFactory.getLogger(AccessTestFork.class);

    @Autowired
    private AccessDao accessDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private TimesheetDao timesheetDao;

    @Autowired
    private UserGroupCache userGroupCache;

    @Test
    public void testAccessDO() {
        logon(AbstractTestBase.TEST_ADMIN_USER);
        final List<GroupTaskAccessDO> list = accessDao.selectAll(false);
        for (final GroupTaskAccessDO access : list) {
            log.info(access.toString());
        }
        final Serializable id = persistenceService.runInTransaction(context -> {
            initTestDB.addTask("accesstest", "root");
            GroupTaskAccessDO groupTaskAccess = new GroupTaskAccessDO();
            accessDao.setTask(groupTaskAccess, getTask("accesstest").getId());
            groupTaskAccess.setGroup(getGroup(AbstractTestBase.TEST_GROUP));
            final AccessEntryDO taskEntry = groupTaskAccess.ensureAndGetAccessEntry(AccessType.TASKS);
            taskEntry.setAccess(true, true, true, true);
            final AccessEntryDO timesheetEntry = groupTaskAccess.ensureAndGetAccessEntry(AccessType.TIMESHEETS);
            timesheetEntry.setAccess(false, false, false, false);
            return accessDao.insert(groupTaskAccess);
        });

        GroupTaskAccessDO groupTaskAccess = accessDao.find(id);
        checkAccessEntry(groupTaskAccess.getAccessEntry(AccessType.TASKS), true, true, true, true);
        checkAccessEntry(groupTaskAccess.getAccessEntry(AccessType.TIMESHEETS), false, false, false, false);
        groupTaskAccess.ensureAndGetAccessEntry(AccessType.TIMESHEETS).setAccessSelect(true);
        accessDao.update(groupTaskAccess);
        groupTaskAccess = accessDao.find(id);
        checkAccessEntry(groupTaskAccess.getAccessEntry(AccessType.TASKS), true, true, true, true);
        checkAccessEntry(groupTaskAccess.getAccessEntry(AccessType.TIMESHEETS), true, false, false, false);
    }

    /**
     * Moves task and checks after moving, if the group task access for the moved tasks are updated.
     */
    @Test
    public void checkTaskMoves() {
        logon(AbstractTestBase.TEST_ADMIN_USER);
        // First check initialization:
        final PFUserDO user1 = getUser("user1");
        assertTrue(userGroupCache.isUserMemberOfGroup(user1.getId(), getGroup("group1").getId()),
                "user1 should be member of group1");
        assertFalse(userGroupCache.isUserMemberOfGroup(user1.getId(), getGroup("group3").getId()),
                "user1 should not be member of group3");
        Serializable id = persistenceService.runInTransaction(context -> {
            initTestDB.addTask("checkTaskMoves", "root");
            initTestDB.addTask("cTm.1", "checkTaskMoves");
            initTestDB.addTask("cTm.child", "cTm.1");
            initTestDB.addTask("cTm.2", "checkTaskMoves");

            // Full access in task cTm.1
            GroupTaskAccessDO groupTaskAccess = new GroupTaskAccessDO();
            accessDao.setTask(groupTaskAccess, getTask("cTm.1").getId());
            groupTaskAccess.setGroup(getGroup("group1"));
            AccessEntryDO taskEntry = groupTaskAccess.ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS);
            taskEntry.setAccess(true, true, true, true);
            accessDao.insert(groupTaskAccess);
            // No access in task cTm.1
            groupTaskAccess = new GroupTaskAccessDO();
            accessDao.setTask(groupTaskAccess, getTask("cTm.2").getId());
            groupTaskAccess.setGroup(getGroup("group3"));

            taskEntry = groupTaskAccess.ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS);
            taskEntry.setAccess(false, false, false, false);
            accessDao.insert(groupTaskAccess);

            TimesheetDO timesheet = new TimesheetDO();
            timesheet.setTask(initTestDB.getTask("cTm.child"));
            timesheet.setUser(getUser("user1"));
            timesheet.setLocation("Office");
            timesheet.setDescription("A lot of stuff done and more.");
            final long current = System.currentTimeMillis();
            PFDateTime startTime = PFDateTime.now().withPrecision(DatePrecision.MINUTE_5);
            timesheet.setStartTime(startTime.getUtilDate());
            timesheet.setStopTime(startTime.plusHours(2).getUtilDate());
            final Serializable timesheetId = timesheetDao.insert(timesheet, false);
            logon(user1); // user1 is in group1, but not in group3
            timesheet = timesheetDao.find(timesheetId); // OK, because is selectable for group1
            // Move task ctm.child to cTm.2 with no access to user1:
            final TaskDO childTask = getTask("cTm.child");
            childTask.setParentTask(getTask("cTm.2"));
            taskDao.update(childTask, false);
            return timesheetId;
        });

        // try {
        TimesheetDO timesheet = timesheetDao.find(id); // AccessException, because is not selectable for group1
        // User has no access, but is owner of this timesheet, so the following properties are empty:
        assertEquals(TimesheetDao.HIDDEN_FIELD_MARKER, timesheet.getShortDescription(),"Field should be hidden");
        assertEquals(TimesheetDao.HIDDEN_FIELD_MARKER, timesheet.getDescription(),"Field should be hidden");
        assertEquals(TimesheetDao.HIDDEN_FIELD_MARKER, timesheet.getLocation(),"Field should be hidden");
        // fail("Timesheet should not be accessible for user1 (because he is not member of group3)");
        // } catch (AccessException ex) {
        // OK
        // }
    }

    private void checkAccessEntry(final AccessEntryDO entry, final boolean accessSelect, final boolean accessInsert,
                                  final boolean accessUpdate, final boolean accessDelete) {
        assertNotNull(entry);
        assertEquals(accessSelect, entry.getAccessSelect());
        assertEquals(accessInsert, entry.getAccessInsert());
        assertEquals(accessUpdate, entry.getAccessUpdate());
        assertEquals(accessDelete, entry.getAccessDelete());
    }
}
