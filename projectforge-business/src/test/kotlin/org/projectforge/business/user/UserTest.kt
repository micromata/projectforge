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

package org.projectforge.business.user

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.password.PasswordQualityService
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.I18nKeyAndParams
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.test.AbstractTestBase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable

class UserTest : AbstractTestBase() {
    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var passwordQualityService: PasswordQualityService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var userPasswordDao: UserPasswordDao

    override fun afterAll() {
        recreateDataBase()
    }

    @Test
    fun testUserDO() {
        logon(TEST_ADMIN_USER)
        val user = userService.getInternalByUsername(TEST_ADMIN_USER)
        Assertions.assertEquals(user.username, TEST_ADMIN_USER)
        val user1 = getUser("user1")
        val groupnames = groupService.getGroupnames(user1.id)
        Assertions.assertEquals("group1; group2", groupnames, "Groupnames")
        Assertions.assertEquals(true, userGroupCache.isUserMemberOfGroup(user1.id, getGroupId("group1")))
        Assertions.assertEquals(false, userGroupCache.isUserMemberOfGroup(user1.id, getGroupId("group3")))
        val group = groupService.getGroup(getGroupId("group1"))
        Assertions.assertEquals("group1", group.name)
        val admin = getUser(ADMIN)
        Assertions.assertEquals(true, userGroupCache.isUserMemberOfAdminGroup(admin.id), "Administrator")
        Assertions.assertEquals(false, userGroupCache.isUserMemberOfAdminGroup(user1.id), "Not administrator")
    }

    @Test
    fun testGetUserDisplayname() {
        val user = PFUserDO()
        user.username = "hurzel"
        Assertions.assertEquals("hurzel", user.userDisplayName, "getUserDisplayname")
        user.lastname = "Reinhard"
        Assertions.assertEquals("Reinhard", user.getFullname(), "getFullname")
        Assertions.assertEquals("Reinhard (hurzel)", user.userDisplayName, "getUserDisplayname")
        user.firstname = "Kai"
        Assertions.assertEquals("Kai Reinhard", user.getFullname(), "getFullname")
        Assertions.assertEquals("Kai Reinhard (hurzel)", user.userDisplayName, "getUserDisplayname")
    }

    @Test
    fun testSaveAndUpdate() {
        logon(TEST_ADMIN_USER)
        var user = PFUserDO()
        user.username = "UserTest"
        user.description = "Description"
        val id: Serializable = userService.save(user)
        user = userService.internalGetById(id)
        Assertions.assertEquals("UserTest", user.username)
        Assertions.assertEquals("Description", user.description)
        user.description = "Description\ntest"
        userService.update(user)
        user = userService.internalGetById(id)
        Assertions.assertEquals("Description\ntest", user.description)
        userService.update(user)
        user = userService.internalGetById(id)
    }

    @Test
    fun testPasswordHandling() {
        val user = PFUserDO()
        user.username = "UserTest-Passwords"
        user.description = "Description"
        val id = userService.save(user)
        userPasswordDao.encryptAndSavePassword(id, "secret".toCharArray(), false)
        val passwordObj = userPasswordDao.internalGetByUserId(id)
        Assertions.assertNotNull(passwordObj!!.passwordHash) // Not SHA, should be ignored.
        Assertions.assertTrue(passwordObj.passwordHash!!.startsWith("SHA{"))
    }

    /**
     * Test password quality.
     */
    @Test
    fun testPasswordQuality() {
        val minPwLenEntry = configurationDao.getEntry(ConfigurationParam.MIN_PASSWORD_LENGTH)
        minPwLenEntry.intValue = 10
        configurationDao.internalUpdate(minPwLenEntry)
        var passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, null)
        Assertions.assertTrue(
            passwordQualityMessages.contains(
                I18nKeyAndParams(
                    MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR, 10
                )
            ),
            "Empty password not allowed."
        )

        passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, "".toCharArray())
        Assertions.assertTrue(
            passwordQualityMessages.contains(
                I18nKeyAndParams(
                    MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR, 10
                )
            ),
            "Empty password not allowed."
        )

        passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, "abcd12345".toCharArray())
        Assertions.assertTrue(
            passwordQualityMessages.contains(
                I18nKeyAndParams(
                    MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR, 10
                )
            ),
            "Password with less than " + "10" + " characters not allowed."
        )

        passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, "ProjectForge".toCharArray())
        Assertions.assertTrue(
            passwordQualityMessages.contains(
                I18nKeyAndParams(
                    MESSAGE_KEY_PASSWORD_NONCHAR_ERROR
                )
            ),
            "Password must have one non letter at minimum."
        )

        passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, "1234567890".toCharArray())
        Assertions.assertTrue(
            passwordQualityMessages.contains(
                I18nKeyAndParams(
                    MESSAGE_KEY_PASSWORD_CHARACTER_ERROR
                )
            ),
            "Password must have one non letter at minimum."
        )

        passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, "12345678901".toCharArray())
        Assertions.assertTrue(
            passwordQualityMessages.contains(
                I18nKeyAndParams(
                    MESSAGE_KEY_PASSWORD_CHARACTER_ERROR
                )
            ),
            "Password must have one non letter at minimum."
        )

        passwordQualityMessages = passwordQualityService.checkPasswordQuality(STRONGOLDPW, STRONGOLDPW)
        Assertions.assertTrue(
            passwordQualityMessages.contains(
                I18nKeyAndParams(
                    MESSAGE_KEY_PASSWORD_OLD_EQ_NEW_ERROR
                )
            ),
            "Password must New password should not be the same as the old one."
        )

        Assertions.assertTrue(
            passwordQualityService.checkPasswordQuality(STRONGOLDPW, "kabcdjh!id".toCharArray()).isEmpty(),
            "Password OK."
        )

        Assertions.assertTrue(
            passwordQualityService.checkPasswordQuality(STRONGOLDPW, "kjh8iabcddsf".toCharArray()).isEmpty(),
            "Password OK."
        )

        Assertions.assertTrue(
            passwordQualityService.checkPasswordQuality(STRONGOLDPW, "  5     g ".toCharArray()).isEmpty(),
            "Password OK."
        )
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
    @Test
    fun testUniqueUsernameDO() {

        var user = createTestUser("42")
        val userId1 = userService.save(user)
        user = createTestUser("100")
        val userId2 = userService.save(user)

        user = createTestUser("42")
        Assertions.assertTrue(
            userService.doesUsernameAlreadyExist(user),
            "Username should already exist."
        )
        user.username = "5"
        Assertions.assertFalse(
            userService.doesUsernameAlreadyExist(user),
            "Signature should not exist."
        )
        userService.save(user)

        val dbUser = userService.internalGetById(userId2)
        user = PFUserDO()
        user.copyValuesFrom(dbUser)
        Assertions.assertFalse(
            userService.doesUsernameAlreadyExist(user),
            "Username does not exist."
        )
        user.username = "42"
        Assertions.assertTrue(
            userService.doesUsernameAlreadyExist(user),
            "Username does already exist."
        )
        user.username = "4711"
        Assertions.assertFalse(
            userService.doesUsernameAlreadyExist(user),
            "Username does not exist."
        )

        user = userService.internalGetById(userId2)
        Assertions.assertFalse(
            userService.doesUsernameAlreadyExist(user),
            "Signature does not exist."
        )
    }

    private fun createTestUser(username: String): PFUserDO {
        val user = PFUserDO()
        user.username = username
        return user
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
    fun assertGroupIds(expectedGroupNames: Array<String?>?, groupsString: String) {
        if (expectedGroupNames == null) {
            Assertions.assertTrue(StringUtils.isEmpty(groupsString))
        }
        val expectedGroups = arrayOfNulls<String>(expectedGroupNames!!.size)
        for (i in expectedGroupNames.indices) {
            expectedGroups[i] = getGroup(expectedGroupNames[i]).id.toString()
        }
        assertIds(expectedGroups, groupsString)
    }

    /**
     * Convert expectedUserNames in list of expected users ids: {2,4,7} Assertions. that all user ids in usersString are
     * expected and vice versa.
     *
     * @param expectedUserNames
     * @param usersString       csv of groups, e. g. {2,4,7}
     */
    fun assertUserIds(expectedUserNames: Array<String?>?, usersString: String) {
        if (expectedUserNames == null) {
            Assertions.assertTrue(StringUtils.isEmpty(usersString))
            return
        }
        val expectedUsers = arrayOfNulls<String>(expectedUserNames.size)
        for (i in expectedUserNames.indices) {
            expectedUsers[i] = getUser(expectedUserNames[i]).id.toString()
        }
        assertIds(expectedUsers, usersString)
    }

    private fun assertIds(expectedEntries: Array<String?>, csvString: String) {
        val entries = StringUtils.split(csvString, ',')
        for (expected in expectedEntries) {
            Assertions.assertTrue(
                ArrayUtils.contains(entries, expected),
                "'" + expected + "' expected in: " + ArrayUtils.toString(entries)
            )
        }
        for (entry in entries) {
            Assertions.assertTrue(
                ArrayUtils.contains(expectedEntries, entry),
                "'" + entry + "' doesn't expected in: " + ArrayUtils.toString(expectedEntries)
            )
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(UserTest::class.java)
        private val STRONGOLDPW = "ja6gieyai8quie0Eey!ooS8eMonah:".toCharArray()

        private const val MESSAGE_KEY_PASSWORD_QUALITY_ERROR = "user.changePassword.error.passwordQualityCheck"

        private const val MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR = "user.changePassword.error.notMinLength"

        private const val MESSAGE_KEY_PASSWORD_CHARACTER_ERROR = "user.changePassword.error.noCharacter"

        private const val MESSAGE_KEY_PASSWORD_NONCHAR_ERROR = "user.changePassword.error.noNonCharacter"

        private const val MESSAGE_KEY_PASSWORD_OLD_EQ_NEW_ERROR = "user.changePassword.error.oldPasswdEqualsNew"
    }
}
