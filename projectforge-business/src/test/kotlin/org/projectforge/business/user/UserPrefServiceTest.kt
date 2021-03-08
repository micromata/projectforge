/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class UserPrefServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var userPrefCache: UserPrefCache

    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Test
    fun entriesTest() {
        logon(TEST_USER)
        val area = "UserPrefCacheTest"
        val name = NumberHelper.getSecureRandomAlphanumeric(20)
        val name2 = NumberHelper.getSecureRandomAlphanumeric(20)
        val entry = userPrefService.getEntry(area, name, String::class.java)
        assertNull(entry)
        userPrefService.putEntry(area, name, "Hurzel")
        userPrefService.putEntry(area, name2, 42)
        assertEquals("Hurzel", userPrefService.getEntry(area, name, String::class.java))
        logoff()
        logon(TEST_USER2)
        assertNull(userPrefService.getEntry(area, name, String::class.java))
        userPrefService.putEntry(area, name, "Hurzel2")
        userPrefService.putEntry(area, name2, 88)
        logoff()
        userPrefCache.flushToDB(getUserId(TEST_USER))
        userPrefCache.setExpired()
        logon(TEST_USER)
        assertEquals("Hurzel", userPrefService.getEntry(area, name, String::class.java))
        assertEquals(42, userPrefService.getEntry(area, name2, Int::class.java))
        logon(TEST_USER2)
        assertEquals("Hurzel2", userPrefService.getEntry(area, name, String::class.java))
        assertEquals(88, userPrefService.getEntry(area, name2, Int::class.java))

        val prefNames = userPrefDao.getPrefNames(area)
        assertEquals(2, prefNames.size, "Got prefnames ${prefNames.joinToString { it }}")
        assertTrue(prefNames.contains(name))
        assertTrue(prefNames.contains(name2))

        logon(TEST_USER)
        var prefs = userPrefDao.getUserPrefs(getUserId(TEST_USER))
        //println(ToStringUtil.toJsonString(prefs))
        assertEquals("^JSON:\"Hurzel\"", prefs.find { it.area == area && it.name == name }!!.valueString)
        assertEquals("^JSON:42", prefs.find { it.area == area && it.name == name2 }!!.valueString)

        logon(TEST_USER2)
        userPrefCache.flushToDB(getUserId(TEST_USER2))
        prefs = userPrefDao.getUserPrefs(getUserId(TEST_USER2))
        assertEquals("^JSON:\"Hurzel2\"", prefs.find { it.area == area && it.name == name }!!.valueString)
        assertEquals("^JSON:88", prefs.find { it.area == area && it.name == name2 }!!.valueString)
        //println(ToStringUtil.toJsonString(userPrefDao.getUserPrefs(getUserId(TEST_USER2))))
    }
}
