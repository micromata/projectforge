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

package org.projectforge.business.user;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GroupTest extends AbstractTestBase {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GroupTest.class);

  @Autowired
  private GroupDao groupDao;

  @Override
  protected void afterAll() {
    recreateDataBase();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void test1SaveAndUpdate() {
    logon(AbstractTestBase.TEST_ADMIN_USER);
    GroupDO group = new GroupDO();
    group.setName("testgroup");
    final Set<PFUserDO> assignedUsers = new HashSet<>();
    group.setAssignedUsers(assignedUsers);
    assignedUsers.add(getUser(AbstractTestBase.TEST_USER));
    final Serializable id = groupDao.save(group);
    group = groupDao.getById(id);
    assertEquals("testgroup", group.getName());
    assertEquals(1, group.getAssignedUsers().size());
    assertTrue(group.getAssignedUsers().contains(getUser(AbstractTestBase.TEST_USER)));
    final PFUserDO user = getUser(AbstractTestBase.TEST_USER2);
    assertNotNull(user);
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    group = groupDao.getById(id);
    assertEquals(2, group.getAssignedUsers().size());
    assertTrue(group.getAssignedUsers().contains(getUser(AbstractTestBase.TEST_USER)));
    assertTrue(group.getAssignedUsers().contains(user));
  }

  // TODO HISTORY
  //  @SuppressWarnings({ "unchecked", "rawtypes" })
  //  @Test
  //  public void test2History()
  //  {
  //    txTemplate.execute(new TransactionCallback()
  //    {
  //      public Object doInTransaction(final TransactionStatus status)
  //      {
  //        final PFUserDO histUser = logon(TEST_ADMIN_USER);
  //
  //        GroupDO group = new GroupDO();
  //        group.setName("historyGroup");
  //        final Set<PFUserDO> assignedUsers = new HashSet<PFUserDO>();
  //        assignedUsers.add(getUser(TEST_USER));
  //        assignedUsers.add(getUser(TEST_USER2));
  //        group.setAssignedUsers(assignedUsers);
  //        final Serializable id = groupDao.save(group);
  //
  //        group = groupDao.getById(id);
  //        assertEquals(2, group.getAssignedUsers().size());
  //        group.getAssignedUsers().remove(getUser(TEST_USER2));
  //        groupDao.update(group);
  //
  //        group = groupDao.getById(id);
  //        assertEquals(1, group.getAssignedUsers().size());
  //        final PFUserDO user = initTestDB.addUser("historyGroupUser");
  //        group.getAssignedUsers().add(user);
  //        groupDao.update(group);
  //
  //        group = groupDao.getById(id);
  //        assertEquals(2, group.getAssignedUsers().size());
  //
  //        HistoryEntry[] historyEntries = groupDao.getHistoryEntries(group);
  //        assertEquals(3, historyEntries.length);
  //        HistoryEntry entry = historyEntries[2];
  //        assertHistoryEntry(entry, group.getId(), histUser, HistoryEntryType.INSERT);
  //        entry = historyEntries[1];
  //        assertHistoryEntry(entry, group.getId(), histUser, HistoryEntryType.UPDATE, "assignedUsers", PFUserDO.class,
  //            getUser(TEST_USER2)
  //                .getId().toString(),
  //            "");
  //        entry = historyEntries[0];
  //        assertHistoryEntry(entry, group.getId(), histUser, HistoryEntryType.UPDATE, "assignedUsers", PFUserDO.class, "",
  //            getUser("historyGroupUser").getId().toString());
  //        historyEntries = userDao.getHistoryEntries(getUser("historyGroupUser"));
  //        log.debug(entry);
  //        return null;
  //      }
  //    });
  //  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void test3CheckUnmodifiableGroupNames() {
    GroupDO adminGroup = getGroup(ProjectForgeGroup.ADMIN_GROUP.getName());
    final Integer id = adminGroup.getId();
    adminGroup.setName("Changed admin group");
    groupDao.internalUpdate(adminGroup);
    adminGroup = groupDao.internalGetById(id);
    assertEquals(ProjectForgeGroup.ADMIN_GROUP.getName(), adminGroup.getName(), "Group's name shouldn't be allowed to change.");
  }
}
