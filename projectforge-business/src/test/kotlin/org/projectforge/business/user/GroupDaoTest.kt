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
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.HistoryServiceTest.Companion.assertAttrEntry
import org.projectforge.framework.persistence.history.HistoryServiceTest.Companion.assertHistoryEntry
import org.projectforge.framework.persistence.history.PropertyOpType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.utils.CollectionUtils
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class GroupDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var userDao: UserDao

    @Test
    fun testAddUserWithHistory() {
        val loggedInUser = logon(ADMIN_USER)
        val users = createTestUsers(userDao, "$PREFIX.user.addUser")
        var lastStats = countHistoryEntries()
        val group = GroupDO()
        group.name = "$PREFIX.group.addUser"
        group.addUser(users[0])
        group.addUser(users[1])
        group.addUser(users[2])
        groupDao.saveInTrans(group)
        groupDao.getHistoryEntries(group).let { entries ->
            Assertions.assertEquals(1, entries.size)
            assertHistoryEntry(GroupDO::class, group.id, EntityOpType.Insert, loggedInUser, entries[0])
        }
        userDao.getHistoryEntries(users[0]).let { entries ->
            Assertions.assertEquals(2, entries.size)
            entries[0].let { entry ->
                assertAttrEntry(
                    GroupDO::class.qualifiedName,
                    value = group.id.toString(), // assignedGroup
                    oldValue = null,
                    propertyName = "assignedGroups",
                    PropertyOpType.Update,
                    entry.attributes,
                )
            }
            assertHistoryEntry(PFUserDO::class, users[0].id, EntityOpType.Insert, loggedInUser, entries[1])
        }
        var recent = getRecentHistoryEntries(4)
        lastStats = assertNumberOfNewHistoryEntries(lastStats, 4, 3)

        group.assignedUsers!!.remove(users[0]) // Unassign users[0] from group 1.
        group.assignedUsers!!.remove(users[1]) // Unassign users[1] from group 1.
        group.addUser(users[3]) // Assign users[3] to group 1.
        groupDao.updateInTrans(group)
        userDao.getHistoryEntries(users[0]).let { entries ->
            Assertions.assertEquals(
                3,
                entries.size
            ) // 1. user inserted[3], 2. group assigned[2], 3. group unassigned[0],
            entries[0].let { entry -> // group 1 unassigned
                assertAttrEntry(
                    GroupDO::class.qualifiedName,
                    value = null,
                    oldValue = group.id.toString(), // unassignedGroup
                    propertyName = "assignedGroups",
                    PropertyOpType.Update,
                    entry.attributes,
                )
            }
            entries[1].let { entry -> // group 1 assigned
                assertAttrEntry(
                    GroupDO::class.qualifiedName,
                    value = group.id.toString(), // assignedGroup
                    oldValue = null,
                    propertyName = "assignedGroups",
                    PropertyOpType.Update,
                    entry.attributes,
                )
            }
            assertHistoryEntry(
                PFUserDO::class,
                users[0].id,
                EntityOpType.Insert,
                loggedInUser,
                entries[2]
            ) // user inserted
        }
        groupDao.getHistoryEntries(group).let { entries ->
            Assertions.assertEquals(
                2,
                entries.size
            ) // 1. group inserted, 2. assigned: users[3], unassigned: users[0], users[1]
            entries[0].let { entry -> // group 1 assigned
                assertAttrEntry(
                    PFUserDO::class.qualifiedName,
                    value = users[3].id.toString(),          // assigned: users[3]
                    oldValue = "${users[0].id},${users[1].id}", // unassigned: users[0], users[1]
                    propertyName = "assignedUsers",
                    PropertyOpType.Update,
                    entry.attributes,
                )
            }
            assertHistoryEntry(
                GroupDO::class,
                group.id,
                EntityOpType.Insert,
                loggedInUser,
                entries[1]
            ) // user inserted
        }

        lastStats = assertNumberOfNewHistoryEntries(lastStats, 4, 4)
    }

    @Test
    fun testAssignGroupByIdsInTransWithHistory() {
        val loggedInUser = logon(ADMIN_USER)
        val users = createTestUsers(userDao, "$PREFIX.user.assignGroupByIdsInTrans")
        val groups = createTestGroups(groupDao, "$PREFIX.group.assignGroupByIdsInTrans")
        // Start with first assignment of group[0] and group[1] to user[0]:
        var lastStats = countHistoryEntries()
        groupDao.assignGroupByIdsInTrans(users[0], asIds(groups, 0, 1), null)
        assertAssignedUsersOfAllGroups(
            groups, users,
            arrayOf(intArrayOf(0), intArrayOf(0), intArrayOf(), intArrayOf())
        )
        lastStats = assertNumberOfNewHistoryEntries(lastStats, 3, 3)
        getRecentHistoryEntries(3).let { entries ->
            // Order of entries for GroupDO is not guaranteed.
            entries.find { it.entry.entityId == groups[0].id }?.let { holder ->
                assertAssignedUsersHistoryEntry(
                    groups[0].id,
                    holder,
                    newValue = asIdsString(users, 0)
                )
            }
            entries.find { it.entry.entityId == groups[1].id }?.let { holder ->
                assertAssignedUsersHistoryEntry(
                    groups[1].id,
                    holder,
                    newValue = asIdsString(users, 0)
                )
            }
            entries.find { it.entry.entityId == users[0].id }?.let { holder ->
                assertAssignedGroupsHistoryEntry(
                    users[0].id,
                    holder,
                    newValue = asIdsString(groups, 0, 1)
                )
            }
        }
        // Assigning and unassigning of group/users is done by GroupDao.saveOrUpdate(group) or by GroupDao.assignGroupByIdsInTrans().
        // groupDao.assignGroupByIdsInTrans(user5, setOf(group1.id, group2.id), null)
        //lastStats = assertNumberOfNewHistoryEntries(lastStats, 3, 2)
        //getRecentHistoryEntries(3)

        // TODO: Test of already existing groups as well as unknown groups.
    }

    private fun assertAssignedUsersHistoryEntry(
        id: Long?,
        holder: HistoryEntryWithEntity,
        newValue: String? = null,
        oldValue: String? = null,
    ) {
        val modUser = ThreadLocalUserContext.requiredLoggedInUser
        val attrs =
            assertHistoryEntry(GroupDO::class, id, EntityOpType.Update, modUser, holder.entry, 1)
        assertAttrEntry(
            PFUserDO::class.qualifiedName,
            value = newValue,
            oldValue = oldValue,
            propertyName = "assignedUsers",
            PropertyOpType.Update,
            attrs,
        )
    }

    private fun assertAssignedGroupsHistoryEntry(
        id: Long?,
        holder: HistoryEntryWithEntity,
        newValue: String? = null,
        oldValue: String? = null,
    ) {
        val modUser = ThreadLocalUserContext.requiredLoggedInUser
        val attrs =
            assertHistoryEntry(PFUserDO::class, id, EntityOpType.Update, modUser, holder.entry, 1)
        assertAttrEntry(
            GroupDO::class.qualifiedName,
            value = newValue,
            oldValue = oldValue,
            propertyName = "assignedGroups",
            PropertyOpType.Update,
            attrs,
        )
    }

    private fun assertAssignedUsersOfAllGroups(
        groups: Array<GroupDO>,
        users: Array<PFUserDO>,
        userMatrix: Array<IntArray>
    ) {
        groups.forEachIndexed { index, group ->
            val userIdx = userMatrix[index]
            assertAssignedUsers(group, users, *userIdx)
        }
    }

    private fun assertAssignedUsers(group: GroupDO, users: Array<PFUserDO>, vararg userIdx: Int) {
        val dbGroup = groupDao.getById(group.id)
        val assignedUsers = CollectionUtils.joinToIdString(dbGroup?.assignedUsers)
        val expectedUsers = asIdsString(users, idx = userIdx)
        Assertions.assertEquals(expectedUsers, assignedUsers, "assignedUsers of group ${group.id}")
    }

    companion object {
        private const val PREFIX = "GroupDaoTest"

        fun <T : IdObject<Long>> asIds(list: Array<T>, vararg idx: Int): List<Long> {
            return idx.map { list[it].id!! }.sorted()
        }

        fun <T : IdObject<Long>> asIdsString(list: Array<T>, vararg idx: Int): String {
            return asIds(list, *idx).joinToString(",")
        }

        fun createTestUsers(userDao: UserDao, prefix: String): Array<PFUserDO> {
            val users = mutableListOf<PFUserDO>()
            for (i in 0..3) {
                PFUserDO().let {
                    it.username = "$prefix.user$i"
                    userDao.saveInTrans(it)
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
                    groupDao.saveInTrans(it)
                    groups.add(it)
                }
            }
            return groups.toTypedArray()
        }
    }
}
