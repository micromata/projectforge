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

package org.projectforge.business.user;

import static org.testng.AssertJUnit.*;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.framework.i18n.I18nKeyAndParams;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.testng.annotations.Test;

public class UserTest extends AbstractTestBase
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserTest.class);
  private static final String STRONGOLDPW = "ja6gieyai8quie0Eey!ooS8eMonah:";

  @Autowired
  private TransactionTemplate txTemplate;

  @Autowired
  private GroupService groupService;

  @Test
  public void testUserDO()
  {
    logon(TEST_ADMIN_USER);
    final PFUserDO user = userService.getByUsername(TEST_ADMIN_USER);
    assertEquals(user.getUsername(), TEST_ADMIN_USER);
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final PFUserDO user1 = getUser("user1");
    final String groupnames = groupService.getGroupnames(user1.getId());
    assertEquals("Groupnames", "group1; group2", groupnames);
    assertEquals(true, userGroupCache.isUserMemberOfGroup(user1.getId(), getGroupId("group1")));
    assertEquals(false, userGroupCache.isUserMemberOfGroup(user1.getId(), getGroupId("group3")));
    final GroupDO group = groupService.getGroup(getGroupId("group1"));
    assertEquals("group1", group.getName());
    final PFUserDO admin = getUser(ADMIN);
    assertEquals("Administrator", true, userGroupCache.isUserMemberOfAdminGroup(admin.getId()));
    assertEquals("Not administrator", false, userGroupCache.isUserMemberOfAdminGroup(user1.getId()));
  }

  @Test
  public void testGetUserDisplayname()
  {
    final PFUserDO user = new PFUserDO();
    user.setUsername("hurzel");
    assertEquals("getUserDisplayname", "hurzel", user.getUserDisplayname());
    user.setLastname("Reinhard");
    assertEquals("getFullname", "Reinhard", user.getFullname());
    assertEquals("getUserDisplayname", "Reinhard (hurzel)", user.getUserDisplayname());
    user.setFirstname("Kai");
    assertEquals("getFullname", "Kai Reinhard", user.getFullname());
    assertEquals("getUserDisplayname", "Kai Reinhard (hurzel)", user.getUserDisplayname());
  }

  @Test
  public void testSaveAndUpdate()
  {
    logon(TEST_ADMIN_USER);
    PFUserDO user = new PFUserDO();
    user.setUsername("UserTest");
    user.setPassword("Hurzel");
    user.setDescription("Description");
    final Serializable id = userService.save(user);
    user = userService.getById(id);
    assertEquals("UserTest", user.getUsername());
    assertNull(user.getPassword()); // Not SHA, should be ignored.
    assertEquals("Description", user.getDescription());
    assertEquals(Integer.valueOf(1), user.getTenant() != null ? user.getTenant().getPk() : Integer.valueOf(-1));
    user.setDescription("Description\ntest");
    user.setPassword("secret");
    userService.update(user);
    user = userService.getById(id);
    assertEquals("Description\ntest", user.getDescription());
    assertEquals(Integer.valueOf(1), user.getTenant() != null ? user.getTenant().getPk() : Integer.valueOf(-1));
    assertNull(user.getPassword()); // Not SHA, should be ignored.
    user.setPassword("SHA{...}");
    userService.update(user);
    user = userService.getById(id);
    assertEquals("SHA{...}", user.getPassword());
    assertEquals(Integer.valueOf(1), user.getTenant() != null ? user.getTenant().getPk() : Integer.valueOf(-1));
  }

  @Test
  public void testCopyValues()
  {
    final PFUserDO src = new PFUserDO();
    src.setPassword("test");
    src.setUsername("usertest");
    final PFUserDO dest = new PFUserDO();
    dest.copyValuesFrom(src);
    assertNull(dest.getPassword());
    assertEquals("usertest", dest.getUsername());
    log.error("Last error message about not encrypted password is OK for this test!");
    src.setPassword("SHA{9B4DDF20612345C5FC7A9355022E07368CDDF23A}");
    dest.copyValuesFrom(src);
    assertEquals("SHA{9B4DDF20612345C5FC7A9355022E07368CDDF23A}", dest.getPassword());
  }

  @Test
  public void testPasswordQuality()
  {
    final Set<I18nKeyAndParams> passwordQualityMessage = userService.getPasswordQualityI18nKeyAndParams();
    assertEquals("Empty password not allowed.", passwordQualityMessage, userService.checkPasswordQualityOnChange(STRONGOLDPW, null));
    assertEquals("Password with less than 10 characters not allowed.", passwordQualityMessage, userService.checkPasswordQualityOnChange(STRONGOLDPW, ""));
    assertEquals("Password with less than 10 characters not allowed.", passwordQualityMessage,
        userService.checkPasswordQualityOnChange(STRONGOLDPW, "abcd12345"));
    assertEquals("Password must have one non letter at minimum.", passwordQualityMessage,
        userService.checkPasswordQualityOnChange(STRONGOLDPW, "ProjectForge"));
    assertEquals("Password must have one letter at minimum.", passwordQualityMessage, userService.checkPasswordQualityOnChange(STRONGOLDPW, "1234567890"));
    assertEquals("Password must have one letter at minimum.", passwordQualityMessage, userService.checkPasswordQualityOnChange(STRONGOLDPW, "12345678901"));
    assertNull("Password OK.", userService.checkPasswordQualityOnChange(STRONGOLDPW, "kabcdjh!id"));
    assertNull("Password OK.", userService.checkPasswordQualityOnChange(STRONGOLDPW, "kjh8iabcddsf"));
    assertNull("Password OK.", userService.checkPasswordQualityOnChange(STRONGOLDPW, "  5     g "));
  }

  // TODO HISTORY
  //  @SuppressWarnings({ "unchecked", "rawtypes" })
  //  @Test
  //  public void history()
  //  {
  //    logon(TEST_ADMIN_USER);
  //    txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  //    txTemplate.execute(new TransactionCallback()
  //    {
  //      public Object doInTransaction(final TransactionStatus status)
  //      {
  //        initTestDB.addUser("UserTest.historyUser1a");
  //        initTestDB.addUser("UserTest.historyUser1b");
  //        initTestDB.addUser("UserTest.historyUser2a");
  //        initTestDB.addUser("UserTest.historyUser2b");
  //        initTestDB.addGroup("UserTest.historyGroup1",
  //            new String[] { "UserTest.historyUser1a", "UserTest.historyUser1b" });
  //        initTestDB.addGroup("UserTest.historyGroup2", (String[]) null);
  //        initTestDB.addGroup("UserTest.historyGroup3", (String[]) null);
  //        return null;
  //      }
  //    });
  //
  //    txTemplate.execute(new TransactionCallback()
  //    {
  //      public Object doInTransaction(final TransactionStatus status)
  //      {
  //        // Checking history entries of user for new group:
  //        HistoryEntry[] historyEntries = userDao.getHistoryEntries(getUser("UserTest.historyUser1a"));
  //        assertEquals(2, historyEntries.length); // insert and update assignedGroups
  //        final HistoryEntry entry = historyEntries[0]; // Update assignedGroups entry
  //        assertEquals(1, entry.getDelta().size());
  //        assertEquals("", entry.getDelta().get(0).getOldValue());
  //        assertGroupIds(new String[] { "UserTest.historyGroup1" }, entry.getDelta().get(0).getNewValue());
  //
  //        // Checking history entries of new group:
  //        historyEntries = groupDao.getHistoryEntries(getGroup("UserTest.historyGroup1"));
  //        assertEquals(1, historyEntries.length); // insert
  //        return null;
  //      }
  //    });
  //
  //    txTemplate.execute(new TransactionCallback()
  //    {
  //      public Object doInTransaction(final TransactionStatus status)
  //      {
  //        // (Un)assigning groups:
  //        final PFUserDO user = userDao.internalGetById(getUserId("UserTest.historyUser1a"));
  //        final Set<GroupDO> groupsToAssign = new HashSet<GroupDO>();
  //        groupsToAssign.add(getGroup("UserTest.historyGroup2"));
  //        groupsToAssign.add(getGroup("UserTest.historyGroup3"));
  //        final Set<GroupDO> groupsToUnassign = new HashSet<GroupDO>();
  //        groupsToUnassign.add(getGroup("UserTest.historyGroup1"));
  //        groupDao.assignGroups(user, groupsToAssign, groupsToUnassign);
  //        return null;
  //      }
  //    });
  //
  //    txTemplate.execute(new TransactionCallback()
  //    {
  //      public Object doInTransaction(final TransactionStatus status)
  //      {
  //        // Checking history of updated user:
  //        HistoryEntry[] historyEntries = userDao.getHistoryEntries(getUser("UserTest.historyUser1a"));
  //        assertEquals(3, historyEntries.length);
  //        assertUserHistoryEntry(historyEntries[0], new String[] { "UserTest.historyGroup2", "UserTest.historyGroup3" },
  //            new String[] { "UserTest.historyGroup1" });
  //
  //        // Checking history entries of updated groups:
  //        historyEntries = groupDao.getHistoryEntries(getGroup("UserTest.historyGroup1"));
  //        final GroupDO group = groupDao.internalGetById(getGroupId("UserTest.historyGroup1"));
  //        final Set<PFUserDO> users = group.getAssignedUsers();
  //        assertEquals(1, users.size()); // Assigned users are: "UserTest.historyUser1b"
  //        assertEquals("2 history entries (1 insert and 1 assigned users", 2, historyEntries.length); // insert and update assignedUsers
  //        assertGroupHistoryEntry(historyEntries[0], null, new String[] { "UserTest.historyUser1a" });
  //        return null;
  //      }
  //    });
  //  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testUniqueUsernameDO()
  {
    final Serializable[] ids = new Integer[2];
    txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(new TransactionCallback()
    {
      @Override
      public Object doInTransaction(final TransactionStatus status)
      {
        PFUserDO user = createTestUser("42");
        ids[0] = userService.save(user);
        user = createTestUser("100");
        ids[1] = userService.save(user);
        return null;
      }
    });
    txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(new TransactionCallback()
    {
      @Override
      public Object doInTransaction(final TransactionStatus status)
      {
        final PFUserDO user = createTestUser("42");
        assertTrue("Username should already exist.", userService.doesUsernameAlreadyExist(user));
        user.setUsername("5");
        assertFalse("Signature should not exist.", userService.doesUsernameAlreadyExist(user));
        userService.save(user);
        return null;
      }
    });
    txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(new TransactionCallback()
    {
      @Override
      public Object doInTransaction(final TransactionStatus status)
      {
        final PFUserDO dbBook = userService.getById(ids[1]);
        final PFUserDO user = new PFUserDO();
        user.copyValuesFrom(dbBook);
        assertFalse("Signature does not exist.", userService.doesUsernameAlreadyExist(user));
        user.setUsername("42");
        assertTrue("Signature does already exist.", userService.doesUsernameAlreadyExist(user));
        user.setUsername("4711");
        assertFalse("Signature does not exist.", userService.doesUsernameAlreadyExist(user));
        userService.update(user);
        return null;
      }
    });
    txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(new TransactionCallback()
    {
      @Override
      public Object doInTransaction(final TransactionStatus status)
      {
        final PFUserDO user = userService.getById(ids[1]);
        assertFalse("Signature does not exist.", userService.doesUsernameAlreadyExist(user));
        return null;
      }
    });
  }

  private PFUserDO createTestUser(final String username)
  {
    final PFUserDO user = new PFUserDO();
    user.setUsername(username);
    return user;
  }

  // TODO HISTORY
  //  /**
  //   * Checks if the history entry contains all and only all expected entries in old and new value.
  //   * 
  //   * @param entry
  //   * @param expectedAssignedUserNames
  //   * @param expectedUnassignedUserNames
  //   */
  //  void assertGroupHistoryEntry(final HistoryEntry entry, final String[] expectedAssignedUserNames,
  //      final String[] expectedUnassignedUserNames)
  //  {
  //    final List<PropertyDelta> list = entry.getDelta();
  //    assertEquals(1, list.size());
  //    final PropertyDelta delta = list.get(0);
  //    assertUserIds(expectedUnassignedUserNames, delta.getOldValue());
  //    assertUserIds(expectedAssignedUserNames, delta.getNewValue());
  //  }
  //
  //  /**
  //   * Checks if the history entry contains all and only all expected entries in old and new value.
  //   * 
  //   * @param entry
  //   * @param expectedAssignedGroupNames
  //   * @param expectedUnassignedGroupNames
  //   */
  //  void assertUserHistoryEntry(final HistoryEntry entry, final String[] expectedAssignedGroupNames,
  //      final String[] expectedUnassignedGroupNames)
  //  {
  //    final List<PropertyDelta> list = entry.getDelta();
  //    assertEquals(1, list.size());
  //    final PropertyDelta delta = list.get(0);
  //    assertGroupIds(expectedUnassignedGroupNames, delta.getOldValue());
  //    assertGroupIds(expectedAssignedGroupNames, delta.getNewValue());
  //  }

  /**
   * Convert expectedGroupNames in list of expected group ids: {2,4,7} Asserts that all group ids in groupssString are
   * expected and vice versa.
   *
   * @param expectedGroupNames
   * @param groupsString       csv of groups, e. g. {2,4,7}
   */
  void assertGroupIds(final String[] expectedGroupNames, final String groupsString)
  {
    if (expectedGroupNames == null) {
      assertTrue(StringUtils.isEmpty(groupsString));
    }
    final String[] expectedGroups = new String[expectedGroupNames.length];
    for (int i = 0; i < expectedGroupNames.length; i++) {
      expectedGroups[i] = getGroup(expectedGroupNames[i]).getId().toString();
    }
    assertIds(expectedGroups, groupsString);
  }

  /**
   * Convert expectedUserNames in list of expected users ids: {2,4,7} Asserts that all user ids in usersString are
   * expected and vice versa.
   *
   * @param expectedUserNames
   * @param groupsString      csv of groups, e. g. {2,4,7}
   */
  void assertUserIds(final String[] expectedUserNames, final String usersString)
  {
    if (expectedUserNames == null) {
      assertTrue(StringUtils.isEmpty(usersString));
      return;
    }
    final String[] expectedUsers = new String[expectedUserNames.length];
    for (int i = 0; i < expectedUserNames.length; i++) {
      expectedUsers[i] = getUser(expectedUserNames[i]).getId().toString();
    }
    assertIds(expectedUsers, usersString);
  }

  private void assertIds(final String[] expectedEntries, final String csvString)
  {
    final String[] entries = StringUtils.split(csvString, ',');
    for (final String expected : expectedEntries) {
      assertTrue("'" + expected + "' expected in: " + ArrayUtils.toString(entries),
          ArrayUtils.contains(entries, expected));
    }
    for (final String entry : entries) {
      assertTrue("'" + entry + "' doesn't expected in: " + ArrayUtils.toString(expectedEntries), ArrayUtils
          .contains(expectedEntries, entry));
    }
  }
}
