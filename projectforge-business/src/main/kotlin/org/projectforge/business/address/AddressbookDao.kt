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

package org.projectforge.business.address

import org.apache.commons.lang3.StringUtils
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.service.UserService
import org.projectforge.common.StringHelper
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.user
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.util.CollectionUtils

/**
 * @author Florian blumenstein
 */
@Repository
open class AddressbookDao : BaseDao<AddressbookDO>(AddressbookDO::class.java) {
    @Autowired
    private val userDao: UserDao? = null

    @Autowired
    private val groupService: GroupService? = null

    @Autowired
    private val userService: UserService? = null

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    init {
        userRightId = UserRightId.MISC_ADDRESSBOOK
    }

    fun setOwner(ab: AddressbookDO, userId: Int) {
        val user = userDao!!.getOrLoad(userId)
        ab.owner = user
    }

    override fun newInstance(): AddressbookDO {
        return AddressbookDO()
    }

    override fun getList(filter: BaseSearchFilter): List<AddressbookDO> {
        val myFilter = if (filter is AddressbookFilter) filter
        else {
            AddressbookFilter(filter)
        }
        val user = user
        val queryFilter = QueryFilter(myFilter)
        queryFilter.addOrder(asc("title"))
        val list = getList(queryFilter)
        if (myFilter.isDeleted) {
            // No further filtering, show all deleted calendars.
            return list
        }
        val result: MutableList<AddressbookDO> = ArrayList()
        val right = userRight as AddressbookRight?
        val userId = user!!.id
        val adminAccessOnly = (myFilter.isAdmin
                && accessChecker.isUserMemberOfAdminGroup(user))
        for (ab in list) {
            val isOwn = right!!.isOwner(user, ab)
            if (isOwn) {
                // User is owner.
                if (adminAccessOnly) {
                    continue
                }
                if (myFilter.isAll || myFilter.isOwn) {
                    // Calendar matches the filter:
                    result.add(ab!!)
                }
            } else {
                // User is not owner.
                if (myFilter.isAll || myFilter.isOthers || adminAccessOnly) {
                    if ((myFilter.isFullAccess && right.hasFullAccess(ab, userId))
                        || (myFilter.isReadonlyAccess && right.hasReadonlyAccess(ab, userId))
                    ) {
                        // Calendar matches the filter:
                        if (!adminAccessOnly) {
                            result.add(ab!!)
                        }
                    } else if (adminAccessOnly) {
                        result.add(ab!!)
                    }
                }
            }
        }
        return result
    }

    val allAddressbooksWithFullAccess: List<AddressbookDO>
        /**
         * Gets a list of all addressbooks with full access of the current logged-in user as well as the addressbooks owned by the
         * current logged-in user.
         *
         * @return
         */
        get() {
            val filter = AddressbookFilter()
            filter.setOwnerType(AddressbookFilter.OwnerType.ALL)
            filter.setFullAccess(true).setReadonlyAccess(false)
            val resultList = getList(filter)?.toMutableList() ?: mutableListOf()
            if (resultList.none { it.id == GLOBAL_ADDRESSBOOK_ID }) {
                // Add global addressbook if not already in list.
                resultList.add(globalAddressbook)
            }
            return resultList
        }

    val globalAddressbook: AddressbookDO
        get() = internalGetById(GLOBAL_ADDRESSBOOK_ID)!!

    /**
     * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
     *
     * @param ab
     * @param fullAccessGroups
     */
    fun setFullAccessGroups(ab: AddressbookDO, fullAccessGroups: Collection<GroupDO?>?) {
        ab.fullAccessGroupIds = groupService!!.getGroupIds(fullAccessGroups)
    }

    fun getSortedFullAccessGroups(ab: AddressbookDO): Collection<GroupDO> {
        return groupService!!.getSortedGroups(ab.fullAccessGroupIds)
    }

    /**
     * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
     *
     * @param ab
     * @param fullAccessUsers
     */
    fun setFullAccessUsers(ab: AddressbookDO, fullAccessUsers: Collection<PFUserDO?>) {
        ab.fullAccessUserIds = userService!!.getUserIds(fullAccessUsers)
    }

    fun getSortedFullAccessUsers(ab: AddressbookDO): Collection<PFUserDO> {
        return userService!!.getSortedUsers(ab.fullAccessUserIds)
    }

    /**
     * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
     *
     * @param ab
     * @param readonlyAccessGroups
     */
    fun setReadonlyAccessGroups(ab: AddressbookDO, readonlyAccessGroups: Collection<GroupDO?>?) {
        ab.readonlyAccessGroupIds = groupService!!.getGroupIds(readonlyAccessGroups)
    }

    fun getSortedReadonlyAccessGroups(ab: AddressbookDO): Collection<GroupDO> {
        return groupService!!.getSortedGroups(ab.readonlyAccessGroupIds)
    }

    /**
     * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
     *
     * @param ab
     * @param readonlyAccessUsers
     */
    fun setReadonlyAccessUsers(ab: AddressbookDO, readonlyAccessUsers: Collection<PFUserDO?>) {
        ab.readonlyAccessUserIds = userService!!.getUserIds(readonlyAccessUsers)
    }

    fun getSortedReadonlyAccessUsers(ab: AddressbookDO): Collection<PFUserDO> {
        return userService!!.getSortedUsers(ab.readonlyAccessUserIds)
    }

    override fun getDisplayHistoryEntries(obj: AddressbookDO): MutableList<DisplayHistoryEntry> {
        val list = super.getDisplayHistoryEntries(obj)
        if (CollectionUtils.isEmpty(list)) {
            return list
        }
        for (entry in list) {
            if (entry.propertyName == null) {
                continue
            } else if (entry.propertyName!!.endsWith("GroupIds")) {
                val oldValue = entry.oldValue
                if (StringUtils.isNotBlank(oldValue) && "null" != oldValue) {
                    val oldGroupNames = groupService!!.getGroupNames(oldValue)
                    entry.oldValue = StringHelper.listToString(oldGroupNames, ", ", true)
                }
                val newValue = entry.newValue
                if (StringUtils.isNotBlank(newValue) && "null" != newValue) {
                    val newGroupNames = groupService!!.getGroupNames(newValue)
                    entry.newValue = StringHelper.listToString(newGroupNames, ", ", true)
                }
            } else if (entry.propertyName!!.endsWith("UserIds")) {
                val oldValue = entry.oldValue
                if (StringUtils.isNotBlank(oldValue) && "null" != oldValue) {
                    val oldGroupNames = userService!!.getUserNames(oldValue)
                    entry.oldValue = StringHelper.listToString(oldGroupNames, ", ", true)
                }
                val newValue = entry.newValue
                if (StringUtils.isNotBlank(newValue) && "null" != newValue) {
                    val newGroupNames = userService!!.getUserNames(newValue)
                    entry.newValue = StringHelper.listToString(newGroupNames, ", ", true)
                }
            }
        }
        return list
    }

    override fun onDelete(obj: AddressbookDO) {
        super.onDelete(obj)
        persistenceService.runInTransaction { context ->
            val addressList = context.query(
                "SELECT a FROM AddressDO a WHERE :addressbook MEMBER OF a.addressbookList",
                AddressDO::class.java,
                Pair("addressbook", obj),
                attached = true,
            )
            addressList.forEach { address ->
                if (address.addressbookList!!.size == 1 && address.addressbookList!!.contains(obj)) {
                    // Add global address book, if no other address book is left:
                    address.addressbookList!!.add(globalAddressbook)
                }
                address.addressbookList!!.remove(obj)
                context.update(address)
            }
            addressList
        }
    }

    companion object {
        const val GLOBAL_ADDRESSBOOK_ID: Int = 1
        val ADDITIONAL_SEARCH_FIELDS = arrayOf("usersgroups", "owner.username", "owner.firstname", "owner.lastname")
    }
}
