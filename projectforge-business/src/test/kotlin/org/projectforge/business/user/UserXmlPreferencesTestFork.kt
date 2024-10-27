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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.access.AccessException
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class UserXmlPreferencesTestFork : AbstractTestBase() {
    @Autowired
    private lateinit var userXmlPreferencesDao: UserXmlPreferencesDao

    @Autowired
    private lateinit var userXmlPreferencesCache: UserXmlPreferencesCache

    @Test
    fun testUserDO() {
        val user1 = getUser("user1")
        val user2 = getUser("user2")
        logon(user1)
        userXmlPreferencesCache.putEntry(user1.id!!, "msg", "Hurzel", true)
        Assertions.assertEquals("Hurzel", userXmlPreferencesCache.getEntry(user1.id!!, "msg"))
        assert(user1.id, "msg", true,"New entry should be modified.")
        userXmlPreferencesCache.refresh()
        assert(user1.id, "msg", false,"Entry should be flushed to db and shouldn't be modified.")
        userXmlPreferencesCache.putEntry(user1.id!!, "value", 42, true)
        assert(user1.id, "value", true,"New entry should be modified.")
        userXmlPreferencesCache.refresh()
        userXmlPreferencesCache.putEntry(user1.id!!, "value", 42, true)
        assert(user1.id, "value", false,"Entry has original value and shouldn't be modified.")
        userXmlPreferencesCache.putEntry(user1.id!!, "application", "ProjectForge", false)
        Assertions.assertEquals("ProjectForge", userXmlPreferencesCache.getEntry(user1.id!!, "application"))
        try {
            userXmlPreferencesCache.putEntry(user2.id!!, "msg", "Hurzel2", true)
            Assertions.fail<Any>("User 1 should not have access to entry of user 2")
        } catch (ex: AccessException) {
            // OK
        }
        logon(TEST_ADMIN_USER)
        userXmlPreferencesCache.putEntry(user2.id!!, "msg", "Hurzel2", true)
        Assertions.assertEquals("Hurzel", userXmlPreferencesCache.getEntry(user1.id!!, "msg"))
        logon(user2)
        Assertions.assertEquals("ProjectForge", userXmlPreferencesCache.getEntry(user1.id!!, "application"))
    }

    private fun assert(userId: Long?, key: String, expectedModified: Boolean, msg: String? = null) {
        val data = userXmlPreferencesCache.ensureAndGetUserPreferencesData(userId!!)
        val value = data.getEntry(key)
        if (expectedModified) {
            Assertions.assertTrue(userXmlPreferencesDao.isModified(data, key, value), msg)
        } else {
            Assertions.assertFalse(userXmlPreferencesDao.isModified(data, key, value), msg)
        }
    }
}
