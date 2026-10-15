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

package org.projectforge.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.SystemStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.rest.dto.UserLdapValues
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserRightDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightVO
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.persistence.api.UserRightService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import java.util.Date

class UserEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var userEntityRest: UserEntityRest

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var userRightDao: UserRightDao

    @Autowired
    private lateinit var userRightService: UserRightService

    private fun validate(dto: User) =
        userEntityRest.validate(
            PFUserDO().also { dto.copyTo(it) },
            PostData(dto, watchFieldsTriggered = null, serverData = null),
        )
            ?.map { it.fieldId }

    @Test
    fun `a taken username and invalid alternative emails are refused`() {
        logon(TEST_ADMIN_USER)
        val dto = User(username = TEST_USER, alternativeEmails = "valid@example.org, not-an-email")
        val fields = validate(dto)
        assertTrue(fields!!.contains("username"), "$fields")
        assertTrue(fields.contains("alternativeEmails"), "$fields")
        assertEquals(1, fields.count { it == "alternativeEmails" }, "only the invalid address: $fields")
    }

    @Test
    fun `an email address of another user is refused with its owner`() {
        logon(TEST_ADMIN_USER)
        val other = initTestDB.addUser(PFUserDO().also {
            it.username = "UserEntityRestTest-owner"
            it.alternativeEmails = "owner-alt@example.org"
        })
        userGroupCache.forceReload()
        val fields = validate(User(username = "UserEntityRestTest-foreign", alternativeEmails = "Owner-Alt@example.org"))
        assertTrue(fields!!.contains("alternativeEmails"), "${other.username}: $fields")
    }

    @Test
    fun `a non administrator learns nothing about other users by validation`() {
        logon(TEST_USER)
        // A taken username and an invalid address: both would be named to an administrator.
        val fields = validate(User(username = TEST_ADMIN_USER, alternativeEmails = "not-an-email"))
        assertTrue(fields.isNullOrEmpty(), "$fields")
    }

    @Test
    fun `a save keeps the values the form doesn't carry`() {
        logon(TEST_ADMIN_USER)
        val lastLogin = Date(1_700_000_000_000)
        val lastPasswordChange = Date(1_700_000_100_000)
        val user = initTestDB.addUser(PFUserDO().also {
            it.username = "UserEntityRestTest-keep"
            it.lastLogin = lastLogin
            it.loginFailures = 2
            it.lastPasswordChange = lastPasswordChange
            it.lastWlanPasswordChange = lastPasswordChange
            it.firstDayOfWeekValue = 7
        })
        // What the next form posts: its own fields only, the timestamps not at all.
        val dto = User(id = user.id, username = user.username, description = "changed")
        userDao.update(userEntityRest.transformForDB(dto), checkAccess = false)

        val saved = userDao.find(user.id, checkAccess = false)!!
        assertEquals("changed", saved.description)
        assertEquals(lastLogin.time, saved.lastLogin?.time)
        assertEquals(2, saved.loginFailures)
        assertEquals(lastPasswordChange.time, saved.lastPasswordChange?.time)
        assertEquals(lastPasswordChange.time, saved.lastWlanPasswordChange?.time)
        assertEquals(7, saved.firstDayOfWeekValue)
    }

    @Test
    fun `a save of an existing user sets no password, whatever the client posts`() {
        logon(TEST_ADMIN_USER)
        val user = initTestDB.addUser("UserEntityRestTest-nopassword")
        val created = userDao.find(user.id, checkAccess = false)!!.created
        // Posted without the creation date: the save would take the user for a new one.
        val dto = User(id = user.id, username = user.username)
        dto.created = null
        dto.password = "Pf-Probe-7319-xQ!z"
        val userDO = userEntityRest.transformForDB(dto)
        assertEquals(created?.time, userDO.created?.time, "the creation date is the stored one")

        userEntityRest.onAfterSave(MockHttpServletRequest(), userDO, PostData(dto, watchFieldsTriggered = null, serverData = null))
        assertNull(userDao.find(user.id, checkAccess = false)!!.lastPasswordChange, "no password set")
    }

    @Test
    fun `incomplete posix values name their fields`() {
        // Posix accounts count as configured in development mode.
        SystemStatus.internalSet4JunitTests(true)
        logon(TEST_ADMIN_USER)
        val dto = User(username = "UserEntityRestTest-posix")
        dto.ldapValues = UserLdapValues(loginShell = "/bin/bash", sambaNTPassword = null)
        val fields = validate(dto)
        assertTrue(fields!!.containsAll(listOf("ldapValues.uidNumber", "ldapValues.gidNumber", "ldapValues.homeDirectory")), "$fields")
    }

    @Test
    fun `the configurable rights depend on the assigned groups`() {
        logon(TEST_ADMIN_USER)
        val none = userEntityRest.rights(UserEntityRest.RightsRequest())
        val financeGroup = getGroup(FINANCE_GROUP)
        val finance = userEntityRest.rights(
            UserEntityRest.RightsRequest(assignedGroups = listOf(Group(id = financeGroup.id)))
        )
        assertNotEquals(none.map { it.rightId }, finance.map { it.rightId })
        finance.filterNot { it.booleanType }.forEach {
            assertTrue(it.values.isNotEmpty(), "right ${it.rightId} offers no value")
        }
    }

    @Test
    fun `a value the form holds survives a new answer`() {
        logon(TEST_ADMIN_USER)
        val groups = listOf(Group(id = getGroup(FINANCE_GROUP).id))
        val right = userEntityRest.rights(UserEntityRest.RightsRequest(assignedGroups = groups))
            .first { !it.booleanType && it.values.size > 1 }
        val chosen = right.values.last().value
        val answer = userEntityRest.rights(
            UserEntityRest.RightsRequest(
                assignedGroups = groups,
                userRights = mutableListOf(
                    org.projectforge.rest.dto.UserRightDto(right.rightId).also {
                        it.value = org.projectforge.business.user.UserRightValue.valueOf(chosen)
                    })
            )
        )
        assertEquals(chosen, answer.first { it.rightId == right.rightId }.value)
    }

    @Test
    fun `only an administrator may ask for the rights`() {
        logon(TEST_USER)
        assertThrows<AccessException> { userEntityRest.rights(UserEntityRest.RightsRequest()) }
        assertThrows<AccessException> { userEntityRest.settingsOptions() }
    }

    @Test
    fun `a copy drops the ldap ids of its original`() {
        val dto = User(id = 1, username = "UserEntityRestTest-clone")
        dto.ldapValues = UserLdapValues(uidNumber = 4711, sambaSIDNumber = 4711, homeDirectory = "/home/x", gidNumber = 100)
        userEntityRest.prepareClone(dto)
        assertNull(dto.ldapValues?.uidNumber)
        assertNull(dto.ldapValues?.sambaSIDNumber)
        assertNull(dto.ldapValues?.homeDirectory)
        assertEquals(100, dto.ldapValues?.gidNumber)
    }

    @Test
    fun `the edit dto carries the flags and rights of the form`() {
        SystemStatus.internalSet4JunitTests(true)
        logon(TEST_ADMIN_USER)
        val dto = userEntityRest.newBaseDTO(null)
        assertEquals(true, dto.posixConfigured)
        assertTrue(dto.userRights != null)
    }

    @Test
    fun `a save leaving a group clears the rights only that group offered`() {
        logon(TEST_ADMIN_USER)
        val financeId = getGroup(FINANCE_GROUP).id!!
        val user = initTestDB.addUser("UserEntityRestTest-rights")
        groupDao.assignGroupByIds(user, setOf(financeId), emptySet())
        val right = userRightService.getRight(UserRightId.FIBU_EINGANGSRECHNUNGEN)
        userRightDao.updateUserRights(user, listOf(UserRightVO(right).also { it.value = UserRightValue.READWRITE }))
        userGroupCache.forceReload()
        assertEquals(UserRightValue.READWRITE, storedValue(user, UserRightId.FIBU_EINGANGSRECHNUNGEN))

        // The form without the finance group: the right isn't offered anymore, so it isn't posted either.
        val dto = User(id = user.id, username = user.username, assignedGroups = mutableListOf())
        dto.userRights = mutableListOf()
        userEntityRest.onAfterSaveOrUpdate(MockHttpServletRequest(), user, PostData(dto, watchFieldsTriggered = null, serverData = null))

        assertTrue(userGroupCache.getUserGroups(user).isNullOrEmpty())
        assertNull(storedValue(user, UserRightId.FIBU_EINGANGSRECHNUNGEN))
    }

    @Test
    fun `a save joining a group keeps the right set along with it`() {
        logon(TEST_ADMIN_USER)
        val financeId = getGroup(FINANCE_GROUP).id!!
        val user = initTestDB.addUser("UserEntityRestTest-join")
        val right = userRightService.getRight(UserRightId.FIBU_EINGANGSRECHNUNGEN)
        // A row of the right exists already (as after an earlier membership): the update path.
        groupDao.assignGroupByIds(user, setOf(financeId), emptySet())
        userRightDao.updateUserRights(user, listOf(UserRightVO(right).also { it.value = UserRightValue.READONLY }))
        groupDao.assignGroupByIds(user, emptySet(), setOf(financeId))
        userGroupCache.forceReload()

        val dto = User(id = user.id, username = user.username, assignedGroups = mutableListOf(Group(id = financeId)))
        dto.userRights = mutableListOf(
            org.projectforge.rest.dto.UserRightDto(UserRightId.FIBU_EINGANGSRECHNUNGEN.id).also {
                it.value = UserRightValue.READWRITE
            })
        userEntityRest.onAfterSaveOrUpdate(MockHttpServletRequest(), user, PostData(dto, watchFieldsTriggered = null, serverData = null))

        assertEquals(UserRightValue.READWRITE, storedValue(user, UserRightId.FIBU_EINGANGSRECHNUNGEN))
    }

    private fun storedValue(user: PFUserDO, rightId: UserRightId): UserRightValue? =
        userRightDao.select(user).find { it.rightIdString == rightId.id }?.value
}
