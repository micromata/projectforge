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
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.test.TestConfiguration
import org.projectforge.business.test.TestSetup
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.Serializable

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage: UserGroupCacheTest </path/userGroupCache1.json> </path/userGroupCache2.json>")
        System.exit(1)
    }
    TestSetup.init()
    println("Working directory: ${File(".").absolutePath}")
    val file1 = UserGroupCacheTest.getFile(args[0])
    val file2 = UserGroupCacheTest.getFile(args[1])
    if (file1 == null || file2 == null) {
        System.exit(1)
    }
    val json1 = file1!!.readText()
    val json2 = file2!!.readText()
    val userGroupCache1 = JsonUtils.fromJson(json1, UserGroupCacheDebug.Data::class.java)
    val userGroupCache2 = JsonUtils.fromJson(json2, UserGroupCacheDebug.Data::class.java)
    if (userGroupCache1 == null) {
        println("Error: userGroupCache1 is null in file: $file1")
        System.exit(1)
    }
    if (userGroupCache2 == null) {
        println("Error: userGroupCache2 is null in file: $file1")
        System.exit(1)
    }
    println(UserGroupCacheDebug.internalCompareWith(userGroupCache1!!, userGroupCache2!!))
}

class UserGroupCacheTest : AbstractTestBase() {
    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Test
    fun testUserMemberOfAtLeastOneGroup() {
        logon(TEST_ADMIN_USER)
        var group1: GroupDO? = GroupDO()
        group1!!.name = "testusergroupcache1"
        var assignedUsers = mutableSetOf<PFUserDO>()
        group1.assignedUsers = assignedUsers
        assignedUsers.add(getUser(TEST_USER))
        var id: Serializable = groupDao.insert(group1)
        group1 = groupDao.find(id)

        var group2: GroupDO? = GroupDO()
        group2!!.name = "testusergroupcache2"
        assignedUsers = HashSet()
        group2.assignedUsers = assignedUsers
        assignedUsers.add(getUser(TEST_ADMIN_USER))
        id = groupDao.insert(group2)
        group2 = groupDao.find(id)

        Assertions.assertFalse(
            userGroupCache.isUserMemberOfAtLeastOneGroup(
                getUser(
                    TEST_ADMIN_USER
                ).id
            )
        )
        Assertions.assertFalse(
            userGroupCache.isUserMemberOfAtLeastOneGroup(
                getUser(
                    TEST_ADMIN_USER
                ).id, group1!!.id
            )
        )
        Assertions.assertTrue(userGroupCache.isUserMemberOfAtLeastOneGroup(getUser(TEST_ADMIN_USER).id, group2!!.id))
        Assertions.assertTrue(
            userGroupCache.isUserMemberOfAtLeastOneGroup(getUser(TEST_ADMIN_USER).id, group1.id, group2.id)
        )
        Assertions.assertTrue(
            userGroupCache.isUserMemberOfAtLeastOneGroup(
                getUser(TEST_ADMIN_USER).id, null, group1.id,
                group2.id
            )
        )
        Assertions.assertTrue(
            userGroupCache.isUserMemberOfAtLeastOneGroup(
                getUser(TEST_ADMIN_USER).id, null, group1.id,
                null, group2.id, null
            )
        )
        Assertions.assertTrue(
            userGroupCache.isUserMemberOfAtLeastOneGroup(getUser(TEST_ADMIN_USER).id, group2.id, group1.id)
        )
    }

    companion object {
        internal fun getFile(path: String): File? {
            val file = File(path)
            if (!file.exists() || !file.canRead()) {
                println("file: '$path' not found or not readable.")
                return null
            }
            return file
        }
    }
}
