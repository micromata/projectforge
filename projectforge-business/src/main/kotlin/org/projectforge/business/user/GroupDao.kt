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

import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
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
import org.projectforge.framework.persistence.utils.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class GroupDao : BaseDao<GroupDO>(GroupDO::class.java) {
    @Autowired
    private lateinit var userDao: UserDao

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    override val defaultSortProperties: Array<SortProperty>?
        get() = DEFAULT_SORT_PROPERTIES


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
     * Used by GroupEditPage (Wicket), only used by Group-Task-Access-Wizard.
     *
     * @param newAssignedUsers Full list of all users which have to assigned to this group.
     */
    @Throws(AccessException::class)
    fun setAssignedUsers(group: GroupDO, newAssignedUsers: Collection<PFUserDO>) {
        // Can't use group.assignedUsers = newAssignedUsers, because Hibernate doesn't recognize the change, if group is attached.
        group.assignedUsers?.removeIf { user ->
            newAssignedUsers.none { it.id == user.id }
        }
        group.assignedUsers = group.assignedUsers ?: mutableSetOf()
        newAssignedUsers.forEach { user ->
            val dbUser = userDao.internalGetById(user.id)
                ?: throw RuntimeException(
                    ("User '"
                            + user.id
                            + "' not found. Could not add this unknown user to new group: "
                            + group.name)
                )
            if (group.assignedUsers?.none { it.id == dbUser.id } == true) {
                group.addUser(dbUser)
            }
        }
    }

    /**
     * Creates for every user a history entry if the user is part of this new group.
     */
    override fun afterSave(obj: GroupDO) {
        val groupList = listOf(obj)
        // Create history entry of PFUserDO for all assigned users:
        persistenceService.runInTransaction { context -> // Assure a transaction is running.
            obj.assignedUsers?.forEach { user ->
                insertHistoryEntry(user, assignedList = groupList, unassignedList = null)
            }
        }
    }

    override fun afterUpdate(obj: GroupDO, dbObj: GroupDO?) {
        if (dbObj == null) {
            log.error { "Oups, shouldn't occur. dbObj is null. Can't determine assigned and unassigned user's in afterUpdate for creating history entries." }
        }
        CollectionUtils.compareCollections(obj.assignedUsers, dbObj?.assignedUsers).let { result ->
            result.added?.forEach { user ->
                insertHistoryEntry(user, assignedList = listOf(obj), unassignedList = null)
            }
            result.removed?.forEach { user ->
                insertHistoryEntry(user, assignedList = null, unassignedList = listOf(obj))
            }
        }
    }

    /**
     * Assigns groups to and unassigns groups from given user.
     *
     * @param groupsToAssign   Group id's to assign (nullable). If some groups already exists, they will be ignored.
     * @param groupsToUnassign Group Id's to unassign (nullable). If some groups already unassigned, they will be ignored.
     * @param updateUserGroupCache If true, the userGroupCache will be updated. Defualt is true.
     * @throws AccessException
     */
    @JvmOverloads
    fun assignGroupByIds(
        user: PFUserDO,
        groupsToAssign: Collection<Long?>?,
        groupsToUnassign: Collection<Long?>?,
        updateUserGroupCache: Boolean = true,
    ) {
        persistenceService.runInTransaction { context ->
            var assignedGroups: MutableList<GroupDO>? = null
            var unassignedGroups: MutableList<GroupDO>? = null
            val dbUser = context.selectById(PFUserDO::class.java, user.id, attached = true)
                ?: throw RuntimeException("User with id ${user.id} not found.")
            groupsToAssign?.filterNotNull()?.forEach { groupId ->
                val dbGroup = context.selectById(GroupDO::class.java, groupId, attached = true)
                    ?: throw RuntimeException("Group with id $groupId not found.")
                dbGroup.assignedUsers = dbGroup.assignedUsers ?: mutableSetOf()
                dbGroup.assignedUsers!!.let { assignedUsers ->
                    if (assignedUsers.none { it.id == dbUser.id }) { // Check if the user isn't yet assigned. Use id check instead of equals.
                        log.info("Assigning user '" + dbUser.username + "' to group '" + dbGroup.name + "'.")
                        assignedUsers.add(dbUser) // dbGroup is attached! Change is saved automatically by Hibernate on transaction commit.
                        dbGroup.setLastUpdate()   // Last update of group isn't set automatically without calling groupDao.saveOrUpdate.
                        assignedGroups = assignedGroups ?: mutableListOf()
                        assignedGroups!!.add(dbGroup)
                    } else {
                        log.info("User '" + dbUser.username + "' already assigned to group '" + dbGroup.name + "'.")
                    }
                }
            }
            groupsToUnassign?.filterNotNull()?.forEach { groupId ->
                val dbGroup = context.selectById(GroupDO::class.java, groupId, attached = true)
                    ?: throw RuntimeException("Group with id $groupId not found.")
                dbGroup.assignedUsers?.let { assignedUsers ->
                    if (assignedUsers.any { it.id == dbUser.id }) { // Check if the user is assigned. Use id check instead of equals.
                        log.info("Unassigning user '" + dbUser.username + "' from group '" + dbGroup.name + "'.")
                        assignedUsers.remove(dbUser) // dbGroup is attached! Change is saved automatically by Hibernate on transaction commit.
                        dbGroup.setLastUpdate()      // Last update of group isn't set automatically without calling groupDao.saveOrUpdate.
                        unassignedGroups = unassignedGroups ?: mutableListOf()
                        unassignedGroups!!.add(dbGroup)
                    } else {
                        log.info("User '" + dbUser.username + "' is not assigned to group '" + dbGroup.name + "' (can't unassign).")
                    }
                }
            }

            // Now, write history entries:
            assignedGroups?.forEach { group ->
                insertHistoryEntry(group, unassignedList = null, assignedList = listOf(user))
            }
            unassignedGroups?.forEach { group ->
                insertHistoryEntry(group, unassignedList = listOf(user), assignedList = null)
            }
            insertHistoryEntry(user, assignedList = assignedGroups, unassignedList = unassignedGroups)
            if (updateUserGroupCache) {
                userGroupCache.setExpired()
            }
        }
    }

    /**
     * Creates a history entry for the given user by setting the history entry attribute 'assignedGroups'.
     */
    private fun insertHistoryEntry(
        user: PFUserDO,
        assignedList: Collection<GroupDO>?,
        unassignedList: Collection<GroupDO>?,
    ) {
        if (unassignedList.isNullOrEmpty() && assignedList.isNullOrEmpty()) {
            return
        }
        insertUpdateHistoryEntry(
            user,
            "assignedGroups",
            GroupDO::class.java,
            oldValue = unassignedList,
            newValue = assignedList,
        )
    }

    /**
     * Creates a history entry for the given group by setting the history entry attribute [GroupDO.assignedUsers].
     */
    private fun insertHistoryEntry(
        group: GroupDO,
        unassignedList: Collection<PFUserDO>?,
        assignedList: Collection<PFUserDO>?,
    ) {
        if (unassignedList.isNullOrEmpty() && assignedList.isNullOrEmpty()) {
            return
        }
        insertUpdateHistoryEntry(
            group,
            "assignedUsers",
            PFUserDO::class.java,
            oldValue = unassignedList,
            newValue = assignedList,
        )
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
        var result = accessChecker.isUserMemberOfAdminGroup(user)
        if (result) {
            return true
        }
        if (accessChecker.isUserMemberOfGroup(
                user,
                ProjectForgeGroup.FINANCE_GROUP,
                ProjectForgeGroup.CONTROLLING_GROUP,
            )
        ) {
            return true
        }
        if (!obj.deleted) {
            result = userGroupCache.isUserMemberOfGroup(user.id, obj.id)
        }
        if (result) {
            return true
        }
        if (throwException) {
            throw AccessException(AccessType.GROUP, OperationType.SELECT)
        }
        return false
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
        name ?: return null
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
