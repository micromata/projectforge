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

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.history.*
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.loggedInUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @author Florian blumenstein
 */
@Service
open class AddressbookDao : BaseDao<AddressbookDO>(AddressbookDO::class.java) {
    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var historyFormatUtils: HistoryFormatUtils

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    init {
        userRightId = UserRightId.MISC_ADDRESSBOOK
    }

    fun setOwner(ab: AddressbookDO, userId: Long) {
        val user = userDao.findOrLoad(userId)
        ab.owner = user
    }

    override fun newInstance(): AddressbookDO {
        return AddressbookDO()
    }

    override fun select(filter: BaseSearchFilter): List<AddressbookDO> {
        val myFilter = if (filter is AddressbookFilter) filter
        else {
            AddressbookFilter(filter)
        }
        val user = loggedInUser
        val queryFilter = QueryFilter(myFilter)
        queryFilter.addOrder(asc("title"))
        val list = select(queryFilter)
        if (myFilter.deleted) {
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
                    result.add(ab)
                }
            } else {
                // User is not owner.
                if (myFilter.isAll || myFilter.isOthers || adminAccessOnly) {
                    if ((myFilter.isFullAccess && right.hasFullAccess(ab, userId))
                        || (myFilter.isReadonlyAccess && right.hasReadonlyAccess(ab, userId))
                    ) {
                        // Calendar matches the filter:
                        if (!adminAccessOnly) {
                            result.add(ab)
                        }
                    } else if (adminAccessOnly) {
                        result.add(ab)
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
            val resultList = select(filter).toMutableList()
            if (resultList.none { it.id == GLOBAL_ADDRESSBOOK_ID }) {
                // Add global addressbook if not already in list.
                resultList.add(globalAddressbook)
            }
            return resultList
        }

    val globalAddressbook: AddressbookDO
        get() = globalAddressbookOrNull!!

    val globalAddressbookOrNull: AddressbookDO?
        get() = find(GLOBAL_ADDRESSBOOK_ID, checkAccess = false)

    override fun customizeDisplayHistoryEntry(context: HistoryLoadContext) {
        historyFormatUtils.replaceGroupAndUserIdsValues(context.requiredDisplayHistoryEntry)
    }

    override fun onDelete(obj: AddressbookDO) {
        persistenceService.runInTransaction { context ->
            val addressList = context.executeQuery(
                "SELECT a FROM AddressDO a WHERE :addressbook MEMBER OF a.addressbookList",
                AddressDO::class.java,
                Pair("addressbook", obj),
                attached = true,
            )
            addressList.forEach { address ->
                if (address.addressbookList?.size == 1 && address.addressbookList!!.contains(obj)) {
                    // Add global address book, if no other address book is left:
                    address.addressbookList!!.add(globalAddressbook)
                }
                address.addressbookList?.remove(obj)
                context.update(address)
            }
        }
    }

    companion object {
        const val GLOBAL_ADDRESSBOOK_ID: Long = 1
        const val GLOBAL_ADDRESSBOOK_TITLE = "Global"
        const val GLOBAL_ADDRESSBOOK_DESCRIPTION = "The global addressbook"
        val ADDITIONAL_SEARCH_FIELDS = arrayOf("usersgroups", "owner.username", "owner.firstname", "owner.lastname")
    }
}
