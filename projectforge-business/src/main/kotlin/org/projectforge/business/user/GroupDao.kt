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

import mu.KotlinLogging
import org.apache.commons.lang3.Validate
import org.projectforge.business.login.Login
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class GroupDao : BaseDao<GroupDO>(GroupDO::class.java) {
    @Autowired
    private val userDao: UserDao? = null

    private val doHistoryUpdate = true

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    override val defaultSortProperties: Array<SortProperty>?
        get() = DEFAULT_SORT_PROPERTIES


    // private final GroupsProvider groupsProvider = new GroupsProvider();
    init {
        this.supportAfterUpdate = true
    }

    override fun getList(filter: BaseSearchFilter): List<GroupDO> {
        val myFilter = if (filter is GroupFilter) {
            filter
        } else {
            GroupFilter(filter)
        }
        val queryFilter = QueryFilter(myFilter)
        if (Login.getInstance().hasExternalUsermanagementSystem()) {
            // Check hasExternalUsermngmntSystem because otherwise the filter is may-be preset for an user and the user can't change the filter
            // (because the fields aren't visible).
            if (myFilter.localGroup != null) {
                queryFilter.add(eq("localGroup", myFilter.localGroup))
            }
        }
        return getList(queryFilter)
    }

    /**
     * Does a group with the given name already exists? Works also for existing users (if group name was modified).
     */
    fun doesGroupnameAlreadyExist(group: GroupDO): Boolean {
        Validate.notNull(group)
        val dbGroup = if (group.id == null) {
            // New group
            getByName(group.name)
        } else {
            // group already exists. Check maybe changed name:
            persistenceService.selectNamedSingleResult(
                GroupDO.FIND_OTHER_GROUP_BY_NAME,
                GroupDO::class.java,
                Pair("name", group.name),
                Pair("id", group.id),
            )
        }
        return dbGroup != null
    }

    /**
     * Please note: Any existing assigned user in group object is ignored!
     *
     * @param assignedUsers Full list of all users which have to assigned to this group.
     */
    @Throws(AccessException::class)
    fun setAssignedUsers(group: GroupDO, assignedUsers: Collection<PFUserDO>) {
        val origAssignedUsers = group.assignedUsers
        if (origAssignedUsers != null) {
            val it = origAssignedUsers.iterator()
            while (it.hasNext()) {
                val user = it.next()
                if (!assignedUsers.contains(user)) {
                    it.remove()
                }
            }
        }
        for (user in assignedUsers) {
            val dbUser = userDao!!.internalGetById(user.id)
                ?: throw RuntimeException(
                    ("User '"
                            + user.id
                            + "' not found. Could not add this unknown user to new group: "
                            + group.name)
                )
            if (origAssignedUsers == null || !origAssignedUsers.contains(dbUser)) {
                group.addUser(dbUser)
            }
        }
    }

    /**
     * Creates for every user an history entry if the user is part of this new group.
     */
    override fun afterSave(group: GroupDO) {
        val groupList: MutableCollection<GroupDO> = ArrayList()
        groupList.add(group)
        if (group.assignedUsers != null) {
            // Create history entry of PFUserDO for all assigned users:
            for (user in group.assignedUsers!!) {
                createHistoryEntry(user, null, groupList)
            }
        }
    }

    /**
     * Creates for every user an history if the user is assigned or unassigned from this updated group.
     */
    fun afterUpdate(group: GroupDO, dbGroup: GroupDO) {
        if (doHistoryUpdate) {
            val origAssignedUsers: Set<PFUserDO>? = dbGroup.assignedUsers
            val assignedUsers: Set<PFUserDO>? = group.assignedUsers
            val assignedList: MutableCollection<PFUserDO> = ArrayList() // List of new assigned users.
            val unassignedList: MutableCollection<PFUserDO> = ArrayList() // List of unassigned users.
            for (user in group.assignedUsers!!) {
                if (!origAssignedUsers!!.contains(user)) {
                    assignedList.add(user)
                }
            }
            for (user in dbGroup.assignedUsers!!) {
                if (!assignedUsers!!.contains(user)) {
                    unassignedList.add(user)
                }
            }
            val groupList: MutableCollection<GroupDO> = ArrayList()
            groupList.add(group)
            // Create history entry of PFUserDO for all new assigned users:
            for (user in assignedList) {
                createHistoryEntry(user, null, groupList)
            }
            // Create history entry of PFUserDO for all unassigned users:
            for (user in unassignedList) {
                createHistoryEntry(user, groupList, null)
            }
        }
    }

    /**
     * Assigns groups to and unassigns groups from given user.
     *
     * @param groupsToAssign   Groups to assign (nullable).
     * @param groupsToUnassign Groups to unassign (nullable).
     * @throws AccessException
     */
    @JvmOverloads
    fun assignGroups(
        user: PFUserDO,
        groupsToAssign: Set<GroupDO>,
        groupsToUnassign: Set<GroupDO>?,
        updateUserGroupCache: Boolean = true
    ) {
        val groupIdsToAssign: MutableSet<Int?> = HashSet()
        val groupIdsToUnassign: MutableSet<Int?> = HashSet()
        if (groupIdsToAssign != null) {
            for (group in groupsToAssign) {
                groupIdsToAssign.add(group.id)
            }
        }
        if (groupsToUnassign != null) {
            for (group in groupsToUnassign) {
                groupIdsToUnassign.add(group.id)
            }
        }
        assignGroupByIds(user, groupIdsToAssign, groupIdsToUnassign, updateUserGroupCache)
    }

    /**
     * Assigns groups to and unassigns groups from given user.
     *
     * @param groupIdsToAssign   Groups to assign (nullable).
     * @param groupIdsToUnassign Groups to unassign (nullable).
     * @throws AccessException
     */
    fun assignGroupByIds(
        user: PFUserDO,
        groupIdsToAssign: Set<Int?>?,
        groupIdsToUnassign: Set<Int?>?,
        updateUserGroupCache: Boolean
    ) {
        val assignedGroups = mutableListOf<GroupDO>()
        val unassignedGroups = mutableListOf<GroupDO>()
        persistenceService.runInTransaction { context ->
            val dbUser = context.selectById(PFUserDO::class.java, user.id, attached = true)
                ?: throw RuntimeException("User with id ${user.id} not found.")
            if (groupIdsToAssign != null) {
                for (groupId in groupIdsToAssign) {
                    val dbGroup = context.selectById(GroupDO::class.java, groupId, attached = true)
                        ?: throw RuntimeException("Group with id $groupId not found.")
                    log.error("******* not yet migrated: HistoryBaseDaoAdapter.wrapHistoryUpdate(dbGroup)")
                    // TODO: HistoryBaseDaoAdapter.wrapHistoryUpdate(dbGroup) {
                    var assignedUsers: MutableSet<PFUserDO>? = dbGroup.assignedUsers
                    if (assignedUsers == null) {
                        assignedUsers = HashSet()
                        dbGroup.assignedUsers = assignedUsers
                    }
                    if (!assignedUsers.contains(dbUser)) {
                        log.info("Assigning user '" + dbUser.username + "' to group '" + dbGroup.name + "'.")
                        assignedUsers.add(dbUser)
                        assignedGroups.add(dbGroup)
                        dbGroup.setLastUpdate() // Needed, otherwise GroupDO is not detected for hibernate history!
                    } else {
                        log.info("User '" + dbUser.username + "' already assigned to group '" + dbGroup.name + "'.")
                    }
                    context.update(dbGroup)
                }
            }
            if (groupIdsToUnassign != null) {
                for (groupId in groupIdsToUnassign) {
                    val dbGroup = context.selectById(GroupDO::class.java, groupId, attached = true)
                        ?: throw RuntimeException("Group with id $groupId not found.")
                    log.error("******* not yet migrated: HistoryBaseDaoAdapter.wrapHistoryUpdate(dbGroup)")
                    //  TODO: HistoryBaseDaoAdapter.wrapHistoryUpdate(dbGroup) {
                    val assignedUsers = dbGroup.assignedUsers
                    if (assignedUsers != null && assignedUsers.contains(dbUser)) {
                        log.info("Unassigning user '" + dbUser.username + "' from group '" + dbGroup.name + "'.")
                        assignedUsers.remove(dbUser)
                        unassignedGroups.add(dbGroup)
                        dbGroup.setLastUpdate() // Needed, otherwise GroupDO is not detected for hibernate history!
                    } else {
                        log.info("User '" + dbUser.username + "' is not assigned to group '" + dbGroup.name + "' (can't unassign).")
                    }
                    context.update(dbGroup)
                    // }
                }
            }

            createHistoryEntry(user, unassignedGroups, assignedGroups)
            if (updateUserGroupCache) {
                userGroupCache.setExpired()
            }
        }
    }

    private fun createHistoryEntry(
        user: PFUserDO, unassignedList: Collection<GroupDO>?,
        assignedList: Collection<GroupDO>?
    ) {
        var unassignedList = unassignedList
        var assignedList = assignedList
        if (unassignedList != null && unassignedList.size == 0) {
            unassignedList = null
        }
        if (assignedList != null && assignedList.size == 0) {
            assignedList = null
        }
        if (unassignedList == null && assignedList == null) {
            return
        }
        createHistoryEntry(user, user.id, "assignedGroups", GroupDO::class.java, unassignedList, assignedList)
    }

    /**
     * Prevents changing the group name for ProjectForge groups.
     */
    override fun onChange(obj: GroupDO, dbObj: GroupDO) {
        for (group in ProjectForgeGroup.entries) {
            if (group.getName() == dbObj.name) {
                // A group of ProjectForge will be changed.
                if (group.getName() != obj.name) {
                    // The group's name must be unmodified!
                    log.warn(
                        "Preventing the change of ProjectForge's group '" + group.getName() + "' in '" + obj.name + "'."
                    )
                    obj.name = group.getName()
                }
                break
            }
        }
    }

    override fun afterSaveOrModify(group: GroupDO) {
        userGroupCache.setExpired()
    }

    override fun afterDelete(obj: GroupDO) {
        userGroupCache.setExpired()
    }

    /**
     * return Always true, no generic select access needed for group objects.
     */
    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    /**
     * @return false, if no admin user and the context user is not member of the group. Also deleted groups are only
     * visible for admin users.
     */
    override fun hasUserSelectAccess(user: PFUserDO, obj: GroupDO, throwException: Boolean): Boolean {
        Validate.notNull(obj)
        var result = accessChecker.isUserMemberOfAdminGroup(user)
        if (result) {
            return true
        }
        if (accessChecker.isUserMemberOfGroup(
                user, ProjectForgeGroup.FINANCE_GROUP,
                ProjectForgeGroup.CONTROLLING_GROUP
            )
        ) {
            return true
        }
        if (!obj.deleted) {
            Validate.notNull(user)
            result = userGroupCache.isUserMemberOfGroup(user.id, obj.id)
        }
        if (result) {
            return true
        }
        if (throwException) {
            throw AccessException(AccessType.GROUP, OperationType.SELECT)
        }
        return result
    }

    override fun hasAccess(
        user: PFUserDO, obj: GroupDO?, oldObj: GroupDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        return accessChecker.isUserMemberOfAdminGroup(user, throwException)
    }

    override fun hasHistoryAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return accessChecker.isUserMemberOfAdminGroup(user, throwException)
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDao.hasInsertAccess
     */
    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return accessChecker.isUserMemberOfAdminGroup(user)
    }

    override fun newInstance(): GroupDO {
        return GroupDO()
    }

    fun getByName(name: String?): GroupDO? {
        if (name == null) {
            return null
        }
        return persistenceService.selectNamedSingleResult(
            GroupDO.FIND_BY_NAME,
            GroupDO::class.java,
            Pair("name", name),
        )
    }

    companion object {
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf(
            "assignedUsers.username",
            "assignedUsers.firstname",
            "assignedUsers.lastname"
        )

        private val DEFAULT_SORT_PROPERTIES = arrayOf(SortProperty("name"))
    }
}
