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
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class UserXmlPreferencesTestFork : AbstractTestBase() {
    @Autowired
    private lateinit var userXmlPreferencesCache: UserXmlPreferencesCache

    @Test
    fun testUserDO() {
        val user1 = getUser("user1")
        val user2 = getUser("user2")
        logon(user1)
        userXmlPreferencesCache.putEntry(null, "msg", "Hurzel", true, user1.id!!)
        Assertions.assertEquals("Hurzel", userXmlPreferencesCache.getEntry(null, "msg", user1.id!!))
        assert(user1.id, "msg", true, "New entry should be modified.")
        userXmlPreferencesCache.refresh()
        assert(user1.id, "msg", false, "Entry should be flushed to db and shouldn't be modified.")
        userXmlPreferencesCache.putEntry(null, "value", 42, true, user1.id!!)
        assert(user1.id, "value", true, "New entry should be modified.")
        userXmlPreferencesCache.refresh()
        userXmlPreferencesCache.putEntry(null, "value", 42, true, user1.id!!)
        assert(user1.id, "value", false, "Entry has original value and shouldn't be modified.")
        userXmlPreferencesCache.putEntry(null, "application", "ProjectForge", false, user1.id!!)
        Assertions.assertEquals("ProjectForge", userXmlPreferencesCache.getEntry(null, "application", user1.id!!))
        try {
            userXmlPreferencesCache.putEntry(null, "msg", "Hurzel2", true, user2.id!!)
            Assertions.fail<Any>("User 1 should not have access to entry of user 2")
        } catch (ex: AccessException) {
            // OK
        }
        logon(TEST_ADMIN_USER)
        userXmlPreferencesCache.putEntry(null, "msg", "Hurzel2", true, user2.id!!)
        Assertions.assertEquals("Hurzel", userXmlPreferencesCache.getEntry(null, "msg", user1.id!!))
        logon(user2)
        Assertions.assertEquals("ProjectForge", userXmlPreferencesCache.getEntry(null, "application", user1.id!!))
    }

    private fun assert(userId: Long?, key: String, expectedModified: Boolean, msg: String? = null) {
        val data = userXmlPreferencesCache.ensureAndGetUserPreferencesData(userId!!)
        val value = data.getEntry(null, key)
        val dataKey = UserPrefCacheDataKey(null, key)
        if (expectedModified) {
            Assertions.assertTrue(userXmlPreferencesCache.isModified(data, dataKey, value), msg)
        } else {
            Assertions.assertFalse(userXmlPreferencesCache.isModified(data, dataKey, value), msg)
        }
    }
}
