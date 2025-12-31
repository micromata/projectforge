/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.password.PasswordQualityService
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.I18nKeyAndParams
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.business.test.AbstractTestBase
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
        val id: Serializable = userService.insert(user, false)
        user = userService.find(id, false)
        Assertions.assertEquals("UserTest", user.username)
        Assertions.assertEquals("Description", user.description)
        user.description = "Description\ntest"
        userService.update(user)
        user = userService.find(id, false)
        Assertions.assertEquals("Description\ntest", user.description)
        userService.update(user)
        user = userService.find(id, false)
    }

    @Test
    fun testPasswordHandling() {
        val user = PFUserDO()
        user.username = "UserTest-Passwords"
        user.description = "Description"
        val id = userService.insert(user, false)
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
        val minPwLenEntry = configurationDao.getEntry(ConfigurationParam.MIN_PASSWORD_LENGTH)!!
        minPwLenEntry.longValue = 10
        configurationDao.update(minPwLenEntry, checkAccess = false)
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

    @Test
    fun testUniqueUsernameDO() {
        var user = createTestUser("42")
        val userId1 = userService.insert(user, false)
        user = createTestUser("100")
        val userId2 = userService.insert(user, false)

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
        userService.insert(user, false)

        val dbUser = userService.find(userId2, false)
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

        user = userService.find(userId2, false)
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

    companion object {
        private val STRONGOLDPW = "ja6gieyai8quie0Eey!ooS8eMonah:".toCharArray()

        private const val MESSAGE_KEY_PASSWORD_MIN_LENGTH_ERROR = "user.changePassword.error.notMinLength"

        private const val MESSAGE_KEY_PASSWORD_CHARACTER_ERROR = "user.changePassword.error.noCharacter"

        private const val MESSAGE_KEY_PASSWORD_NONCHAR_ERROR = "user.changePassword.error.noNonCharacter"

        private const val MESSAGE_KEY_PASSWORD_OLD_EQ_NEW_ERROR = "user.changePassword.error.oldPasswdEqualsNew"
    }
}
