/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.PropertyOpType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.utils.CollectionUtils
import org.projectforge.framework.persistence.utils.CollectionUtils.joinToString
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.test.HistoryEntryHolder
import org.projectforge.business.test.HistoryTester
import org.projectforge.business.test.HistoryTester.Companion.assertHistoryEntry
import org.springframework.beans.factory.annotation.Autowired

class GroupDaoTest : AbstractTestBase() {
    class TestContext(val users: Array<PFUserDO>, val groups: Array<GroupDO>)

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var userDao: UserDao

    @Test
    fun testAddUserWithHistory() {
        val loggedInUser = logon(ADMIN_USER)
        val users = createTestUsers(userDao, "$PREFIX.user.addUser")
        val hist = createHistoryTester()
        val group = GroupDO()
        group.name = "$PREFIX.group.addUser"
        group.addUser(users[0])
        group.addUser(users[1])
        group.addUser(users[2])
        groupDao.insert(group)
        groupDao.loadHistory(group).sortedEntries.let { entries ->
            Assertions.assertEquals(1, entries.size)
            assertHistoryEntry(entries[0], GroupDO::class, group.id, EntityOpType.Insert, loggedInUser)
        }
        userDao.loadHistory(users[0]).sortedEntries.let { entries ->
            Assertions.assertEquals(2, entries.size)
            entries[0].let { entry ->
                HistoryTester.assertHistoryAttr(
                    entry,
                    propertyName = "assignedGroups",
                    value = group.id.toString(), // assignedGroup
                    oldValue = null,
                    opType = PropertyOpType.Update,
                    propertyTypeClass = GroupDO::class,
                )
            }
            assertHistoryEntry(entries[1], PFUserDO::class, users[0].id, EntityOpType.Insert, loggedInUser)
        }
        // var recent = getRecentHistoryEntries(4)
        hist.loadRecentHistoryEntries(4, 3)

        group.assignedUsers!!.remove(users[0]) // Unassign users[0] from group 1.
        group.assignedUsers!!.remove(users[1]) // Unassign users[1] from group 1.
        group.addUser(users[3]) // Assign users[3] to group 1.
        groupDao.update(group)
        userDao.loadHistory(users[0]).sortedEntries.let { entries ->
            Assertions.assertEquals(
                3,
                entries.size
            ) // 1. user inserted[3], 2. group assigned[2], 3. group unassigned[0],
            entries[0].let { entry -> // group 1 unassigned
                HistoryTester.assertHistoryAttr(
                    entry,
                    propertyName = "assignedGroups",
                    value = null,
                    oldValue = group.id.toString(), // unassignedGroup
                    opType = PropertyOpType.Update,
                    propertyTypeClass = GroupDO::class,
                )
            }
            entries[1].let { entry -> // group 1 assigned
                HistoryTester.assertHistoryAttr(
                    entry,
                    value = group.id.toString(), // assignedGroup
                    oldValue = null,
                    propertyName = "assignedGroups",
                    opType = PropertyOpType.Update,
                    propertyTypeClass = GroupDO::class,
                )
            }
            assertHistoryEntry(
                entries[2],
                PFUserDO::class,
                users[0].id,
                EntityOpType.Insert,
                loggedInUser,
            ) // user inserted
        }
        groupDao.loadHistory(group).sortedEntries.let { entries ->
            Assertions.assertEquals(
                2,
                entries.size
            ) // 1. group inserted, 2. assigned: users[3], unassigned: users[0], users[1]
            entries[0].let { entry -> // group 1 assigned
                HistoryTester.assertHistoryAttr(
                    entry,
                    propertyName = "assignedUsers",
                    value = users[3].id.toString(),          // assigned: users[3]
                    oldValue = "${users[0].id},${users[1].id}", // unassigned: users[0], users[1]
                    opType = PropertyOpType.Update,
                    propertyTypeClass = PFUserDO::class,
                )
            }
            assertHistoryEntry(
                entries[1],
                GroupDO::class,
                group.id,
                EntityOpType.Insert,
                loggedInUser,
            ) // user inserted
        }

        hist.loadRecentHistoryEntries(4, 4)
    }

    @Test
    fun testSetAssignedUsers() {
        logon(ADMIN_USER)
        val group = GroupDO()
        val users = createTestUsers(userDao, "$PREFIX.user.setAssignedUsers")
        groupDao.setAssignedUsers(group, mutableSetOf(users[1], users[2]))
        Assertions.assertEquals(
            CollectionUtils.joinToStringOfIds(setOf(users[1], users[2])),
            CollectionUtils.joinToStringOfIds(group.assignedUsers)
        )

        group.assignedUsers = mutableSetOf(users[0], users[1])
        groupDao.setAssignedUsers(group, mutableSetOf(users[1], users[2]))
        Assertions.assertEquals(
            CollectionUtils.joinToStringOfIds(setOf(users[1], users[2])),
            CollectionUtils.joinToStringOfIds(group.assignedUsers)
        )
    }

    @Test
    fun testAssignGroupByIdsWithHistory() {
        logon(ADMIN_USER)
        val users = createTestUsers(userDao, "$PREFIX.user.assignGroupByIds")
        val groups = createTestGroups(groupDao, "$PREFIX.group.assignGroupByIds")
        val testContext = TestContext(users, groups)
        // Start with first assignment of group[0] and group[1] to user[0]:
        val hist = createHistoryTester()
        groupDao.assignGroupByIds(
            users[0],
            groupsToAssign = asIds(groups, arrayOf(0, 1)),
            groupsToUnassign = null
        )
        assertAssignedUsersOfAllGroups(
            testContext,
            userMatrix = arrayOf(arrayOf(0), arrayOf(0), arrayOf(), arrayOf())
        )
        hist.loadRecentHistoryEntries(3, 3)
            .recentEntries?.let { entries ->
                assertUserAndGroupsHistoryEntries(testContext, entries, users[0], arrayOf(0, 1), null)
            }

        // printGroupUserMatrix(testContext) // print current state for debugging:
        // Current users of groups: group[0]: 0, group[1]: 0,
        // Current groups of users: user[0]: 0,1,
        groupDao.assignGroupByIds(
            users[1],
            groupsToAssign = asIds(groups, arrayOf(0, 1, 2)),
            groupsToUnassign = null
        )
        // printGroupUserMatrix(testContext) // print current state for debugging:
        // Current users of groups: group[0]: 0,1, group[1]: 0,1, group[2]: 1,
        // Current groups of users: user[0]: 0,1, user[1]: 0,1,2,
        assertAssignedUsersOfAllGroups(
            testContext,
            userMatrix = arrayOf(arrayOf(0, 1), arrayOf(0, 1), arrayOf(1), arrayOf())
        )
        hist.loadRecentHistoryEntries(4, 4)
            .recentEntries?.let { entries ->
                assertUserAndGroupsHistoryEntries(testContext, entries, users[1], arrayOf(0, 1, 2), null)
            }

        groupDao.assignGroupByIds(
            users[1],
            groupsToAssign = asIds(groups, arrayOf(3)),
            groupsToUnassign = asIds(groups, arrayOf(0, 1))
        )
        // printGroupUserMatrix(testContext) // print current state for debugging:
        // Current users of groups: group[0]: 0, group[1]: 0, group[2]: 1, group[3]: 1,
        // Current groups of users: user[0]: 0,1, user[1]: 2,3,
        assertAssignedUsersOfAllGroups(
            testContext,
            userMatrix = arrayOf(arrayOf(0), arrayOf(0), arrayOf(1), arrayOf(1))
        )
        hist.loadRecentHistoryEntries(4, 4)
            .recentEntries?.let { entries ->
                assertUserAndGroupsHistoryEntries(testContext, entries, users[1], arrayOf(3), arrayOf(0, 1))
            }

        // NOP: No changes:
        groupDao.assignGroupByIds(
            users[1],
            groupsToAssign = asIds(groups, arrayOf(3)),
            groupsToUnassign = asIds(groups, arrayOf(0, 1))
        )
        assertAssignedUsersOfAllGroups(
            testContext,
            userMatrix = arrayOf(arrayOf(0), arrayOf(0), arrayOf(1), arrayOf(1))
        )
        hist.loadRecentHistoryEntries(0)

        suppressErrorLogs {
            // -12 is unkonwn, exception expected.
            try {
                groupDao.assignGroupByIds(
                    users[1], groupsToAssign = listOf(testContext.groups[2].id, -12),
                    groupsToUnassign = asIds(groups, arrayOf(0, 1))
                )
                Assertions.fail { "IllegalArgumentException expected tue to unknown group." }
            } catch (ex: RuntimeException) {
                // Expected.
            }
            // -18 is unkonwn, exception expected.
            try {
                groupDao.assignGroupByIds(
                    users[1], groupsToAssign = listOf(testContext.groups[2].id),
                    groupsToUnassign = listOf(testContext.groups[0].id, -18)
                )
                Assertions.fail { "IllegalArgumentException expected tue to unknown group." }
            } catch (ex: RuntimeException) {
                // Expected.
            }
        }
    }

    @Suppress("unused")
    private fun printGroupUserMatrix(testContext: TestContext) {
        val dbGroups = mutableListOf<GroupDO>()
        val sb = StringBuilder()
        sb.append("userMatrix = arrayOf(")//arrayOf(0, 1), arrayOf(0, 1), arrayOf(1), arrayOf())")
        print("Current users of groups: ")
        testContext.groups.forEachIndexed { _, groupDO ->
            groupDao.find(groupDO.id)?.let { group ->
                if (!group.assignedUsers.isNullOrEmpty()) {
                    dbGroups.add(group)
                    print(
                        "group[${asIndex(testContext.groups, group.id)}]: ${
                            asIndices(testContext.users, group.assignedUsers)
                        }, "
                    )
                    sb.append("arrayOf(").append(asIndices(testContext.users, group.assignedUsers)).append("), ")
                }
            }
        }
        println()
        print("Current groups of users: ")
        testContext.users.forEachIndexed { _, userDO ->
            val assignedGroups = dbGroups.filter { it.assignedUsers?.any { it.id == userDO.id } == true }
            if (assignedGroups.isNotEmpty()) {
                print(
                    "user[${asIndex(testContext.users, userDO.id)}]: ${
                        asIndices(testContext.groups, assignedGroups)
                    }, "
                )
            }
        }
        println()
        println(sb.toString().removeSuffix(", ") + ")")
    }

    private fun assertUserAndGroupsHistoryEntries(
        testContext: TestContext,
        entries: Collection<HistoryEntryHolder>,
        expectedUser: PFUserDO,
        expectedAssignedGroups: Array<Int?>?,
        expectedUnassignedGroups: Array<Int?>?,
    ) {
        // Order of entries for GroupDO is not guaranteed.
        expectedAssignedGroups?.filterNotNull()?.forEach { groupIdx ->
            entries.find { it.entry.entityId == testContext.groups[groupIdx].id }?.let { holder ->
                assertAssignedUsersHistoryEntry(
                    testContext.groups[groupIdx].id,
                    holder,
                    newValue = expectedUser.id.toString()
                )
            }
        }
        if (expectedAssignedGroups?.isNotEmpty() == true) {
            entries.find { it.entry.entityId == expectedUser.id }?.let { holder ->
                assertAssignedGroupsHistoryEntry(
                    expectedUser.id,
                    holder,
                    newValue = asIdsString(testContext.groups, expectedAssignedGroups),
                    oldValue = asIdsString(testContext.groups, expectedUnassignedGroups),
                )
            }
        }
    }

    private fun assertAssignedUsersHistoryEntry(
        id: Long?,
        holder: HistoryEntryHolder,
        newValue: String? = null,
        oldValue: String? = null,
    ) {
        val modUser = ThreadLocalUserContext.requiredLoggedInUser
        assertHistoryEntry(holder, GroupDO::class, id, EntityOpType.Update, modUser, 1)
        HistoryTester.assertHistoryAttr(
            holder,
            propertyName = "assignedUsers",
            value = newValue,
            oldValue = oldValue,
            opType = PropertyOpType.Update,
            propertyTypeClass = PFUserDO::class,
        )
    }

    private fun assertAssignedGroupsHistoryEntry(
        id: Long?,
        holder: HistoryEntryHolder,
        newValue: String? = null,
        oldValue: String? = null,
    ) {
        val modUser = ThreadLocalUserContext.requiredLoggedInUser
        assertHistoryEntry(holder, PFUserDO::class, id, EntityOpType.Update, modUser, 1)
        HistoryTester.assertHistoryAttr(
            holder,
            propertyName = "assignedGroups",
            value = newValue,
            oldValue = oldValue,
            opType = PropertyOpType.Update,
            propertyTypeClass = GroupDO::class,
        )
    }

    /**
     * @param userMatrix Each row represents the assigned users of a group.
     */
    private fun assertAssignedUsersOfAllGroups(
        testContext: TestContext,
        userMatrix: Array<Array<Int?>>
    ) {
        testContext.groups.forEachIndexed { index, group ->
            val userIdx = userMatrix[index]
            assertAssignedUsers(testContext, group, userIdx)
        }
    }

    private fun assertAssignedUsers(testContext: TestContext, group: GroupDO, userIndices: Array<Int?>) {
        val dbGroup = groupDao.find(group.id)
        val assignedUsers = CollectionUtils.joinToStringOfIds(dbGroup?.assignedUsers)
        val expectedUsers = asIdsString(testContext.users, userIndices)
        Assertions.assertEquals(
            expectedUsers,
            assignedUsers,
            "assignedUsers of group ${asIndex(testContext.groups, group.id)}"
        )
    }

    private fun <T : IdObject<*>> asIndex(col: Array<T>, id: Long?): Int {
        return col.indexOfFirst { it.id == id }
    }

    private fun <T : IdObject<Long>> asIndices(col: Array<T>, id: Collection<T?>?): String {
        return id?.map { asIndex(col, it?.id) }?.sorted()?.joinToString(",") ?: ""
    }


    companion object {
        private val PREFIX = GroupDaoTest::class.simpleName

        fun <T : IdObject<Long>> asIds(list: Array<T>, indices: Array<Int?>?): List<Long>? {
            indices ?: return null
            return indices.filterNotNull().map { list[it].id!! }.sorted()
        }

        fun <T : IdObject<Long>> asIdsString(list: Array<T>, indices: Array<Int?>?): String? {
            return joinToString(asIds(list, indices))
        }

        fun createTestUsers(userDao: UserDao, prefix: String): Array<PFUserDO> {
            val users = mutableListOf<PFUserDO>()
            for (i in 0..3) {
                PFUserDO().let {
                    it.username = "$prefix.user$i"
                    userDao.insert(it)
                    users.add(it)
                }
            }
            return users.toTypedArray()
        }

        fun createTestGroups(groupDao: GroupDao, prefix: String): Array<GroupDO> {
            val groups = mutableListOf<GroupDO>()
            for (i in 0..3) {
                GroupDO().let {
                    it.name = "$prefix.group$i"
                    groupDao.insert(it)
                    groups.add(it)
                }
            }
            return groups.toTypedArray()
        }
    }
}
