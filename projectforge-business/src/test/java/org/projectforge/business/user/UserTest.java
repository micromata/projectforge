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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.password.PasswordQualityService;
import org.projectforge.framework.i18n.I18nKeyAndParams;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest extends AbstractTestBase {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserTest.class);
  private static final String STRONGOLDPW = "ja6gieyai8quie0Eey!ooS8eMonah:";

  private static final String MESSAGE_KEY_PASSWORD_QUALITY_ERROR = "user.changePassword.error.passwordQualityCheck";

  private static final String MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR = "user.changePassword.error.notMinLength";

  private static final String MESSAGE_KEY_PASSWORD_CHARACTER_ERROR = "user.changePassword.error.noCharacter";

  private static final String MESSAGE_KEY_PASSWORD_NONCHAR_ERROR = "user.changePassword.error.noNonCharacter";

  private static final String MESSAGE_KEY_PASSWORD_OLD_EQ_NEW_ERROR = "user.changePassword.error.oldPasswdEqualsNew";

  @Autowired
  private GroupService groupService;

  @Autowired
  private PasswordQualityService passwordQualityService;

  @Test
  public void testUserDO() {
    logon(AbstractTestBase.TEST_ADMIN_USER);
    final PFUserDO user = userService.getByUsername(AbstractTestBase.TEST_ADMIN_USER);
    assertEquals(user.getUsername(), AbstractTestBase.TEST_ADMIN_USER);
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final PFUserDO user1 = getUser("user1");
    final String groupnames = groupService.getGroupnames(user1.getId());
    assertEquals("group1; group2", groupnames, "Groupnames");
    assertEquals(true, userGroupCache.isUserMemberOfGroup(user1.getId(), getGroupId("group1")));
    assertEquals(false, userGroupCache.isUserMemberOfGroup(user1.getId(), getGroupId("group3")));
    final GroupDO group = groupService.getGroup(getGroupId("group1"));
    assertEquals("group1", group.getName());
    final PFUserDO admin = getUser(AbstractTestBase.ADMIN);
    assertEquals(true, userGroupCache.isUserMemberOfAdminGroup(admin.getId()), "Administrator");
    assertEquals(false, userGroupCache.isUserMemberOfAdminGroup(user1.getId()), "Not administrator");
  }

  @Test
  public void testGetUserDisplayname() {
    final PFUserDO user = new PFUserDO();
    user.setUsername("hurzel");
    assertEquals("hurzel", user.getUserDisplayName(), "getUserDisplayname");
    user.setLastname("Reinhard");
    assertEquals("Reinhard", user.getFullname(), "getFullname");
    assertEquals("Reinhard (hurzel)", user.getUserDisplayName(), "getUserDisplayname");
    user.setFirstname("Kai");
    assertEquals("Kai Reinhard", user.getFullname(), "getFullname");
    assertEquals("Kai Reinhard (hurzel)", user.getUserDisplayName(), "getUserDisplayname");
  }

  @Test
  public void testSaveAndUpdate() {
    logon(AbstractTestBase.TEST_ADMIN_USER);
    PFUserDO user = new PFUserDO();
    user.setUsername("UserTest");
    user.setPassword("Hurzel");
    user.setDescription("Description");
    final Serializable id = userService.save(user);
    user = userService.internalGetById(id);
    assertEquals("UserTest", user.getUsername());
    assertNull(user.getPassword()); // Not SHA, should be ignored.
    assertEquals("Description", user.getDescription());
    assertEquals(Integer.valueOf(1), user.getTenant() != null ? user.getTenant().getPk() : Integer.valueOf(-1));
    user.setDescription("Description\ntest");
    user.setPassword("secret");
    userService.update(user);
    user = userService.internalGetById(id);
    assertEquals("Description\ntest", user.getDescription());
    assertEquals(Integer.valueOf(1), user.getTenant() != null ? user.getTenant().getPk() : Integer.valueOf(-1));
    assertNull(user.getPassword()); // Not SHA, should be ignored.
    user.setPassword("SHA{...}");
    userService.update(user);
    user = userService.internalGetById(id);
    assertEquals("SHA{...}", user.getPassword());
    assertEquals(Integer.valueOf(1), user.getTenant() != null ? user.getTenant().getPk() : Integer.valueOf(-1));
  }

  @Test
  public void testCopyValues() {
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

  /**
   * Test password quality.
   */
  @Test
  public void testPasswordQuality() {
    List<I18nKeyAndParams> passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, null);
    assertTrue(passwordQualityMessages.contains(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR, 10)),
            "Empty password not allowed.");

    passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, "");
    assertTrue(passwordQualityMessages.contains(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR, 10)),
            "Empty password not allowed.");

    passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, "abcd12345");
    assertTrue(passwordQualityMessages.contains(new
                    I18nKeyAndParams(MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR, 10)),
            "Password with less than " + "10" + " characters not allowed.");

    passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, "ProjectForge");
    assertTrue(passwordQualityMessages.contains(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_NONCHAR_ERROR)),
            "Password must have one non letter at minimum.");

    passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, "1234567890");
    assertTrue(passwordQualityMessages.contains(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_CHARACTER_ERROR)),
            "Password must have one non letter at minimum.");

    passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, "12345678901");
    assertTrue(passwordQualityMessages.contains(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_CHARACTER_ERROR)),
            "Password must have one non letter at minimum.");

    passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, STRONGOLDPW);
    assertTrue(passwordQualityMessages.contains(new I18nKeyAndParams(MESSAGE_KEY_PASSWORD_OLD_EQ_NEW_ERROR)),
            "Password must New password should not be the same as the old one.");

    assertTrue(passwordQualityService.checkPasswordQuality(STRONGOLDPW, "kabcdjh!id").isEmpty(),
            "Password OK.");

    assertTrue(passwordQualityService.checkPasswordQuality(STRONGOLDPW, "kjh8iabcddsf").isEmpty(),
            "Password OK.");

    assertTrue(passwordQualityService.checkPasswordQuality(STRONGOLDPW, "  5     g ").isEmpty(),
            "Password OK.");
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

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void testUniqueUsernameDO() {
    final Serializable[] ids = new Integer[2];

    emf.runInTrans(emgr -> {
      PFUserDO user = createTestUser("42");
      ids[0] = userService.save(user);
      user = createTestUser("100");
      ids[1] = userService.save(user);
      return null;
    });
    emf.runInTrans(emgr -> {
      final PFUserDO user = createTestUser("42");
      assertTrue(userService.doesUsernameAlreadyExist(user), "Username should already exist.");
      user.setUsername("5");
      assertFalse(userService.doesUsernameAlreadyExist(user), "Signature should not exist.");
      userService.save(user);
      return null;
    });
    emf.runInTrans(emgr -> {
      final PFUserDO dbUser = userService.internalGetById(ids[1]);
      final PFUserDO user = new PFUserDO();
      user.copyValuesFrom(dbUser);
      assertFalse(userService.doesUsernameAlreadyExist(user), "Username does not exist.");
      user.setUsername("42");
      assertTrue(userService.doesUsernameAlreadyExist(user), "Username does already exist.");
      user.setUsername("4711");
      assertFalse(userService.doesUsernameAlreadyExist(user), "Username does not exist.");
      //userService.update(user);
      return null;
    });
    emf.runInTrans(emgr -> {
      final PFUserDO user = userService.internalGetById(ids[1]);
      assertFalse(userService.doesUsernameAlreadyExist(user), "Signature does not exist.");
      return null;
    });
  }

  private PFUserDO createTestUser(final String username) {
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
   * Convert expectedGroupNames in list of expected group ids: {2,4,7} Assertions. that all group ids in groupssString are
   * expected and vice versa.
   *
   * @param expectedGroupNames
   * @param groupsString       csv of groups, e. g. {2,4,7}
   */
  void assertGroupIds(final String[] expectedGroupNames, final String groupsString) {
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
   * Convert expectedUserNames in list of expected users ids: {2,4,7} Assertions. that all user ids in usersString are
   * expected and vice versa.
   *
   * @param expectedUserNames
   * @param usersString       csv of groups, e. g. {2,4,7}
   */
  void assertUserIds(final String[] expectedUserNames, final String usersString) {
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

  private void assertIds(final String[] expectedEntries, final String csvString) {
    final String[] entries = StringUtils.split(csvString, ',');
    for (final String expected : expectedEntries) {
      assertTrue(ArrayUtils.contains(entries, expected),
              "'" + expected + "' expected in: " + ArrayUtils.toString(entries));
    }
    for (final String entry : entries) {
      assertTrue(ArrayUtils.contains(expectedEntries, entry),
              "'" + entry + "' doesn't expected in: " + ArrayUtils.toString(expectedEntries));
    }
  }
}
