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

import org.apache.commons.lang3.StringUtils
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class UserRightDao protected constructor() : BaseDao<UserRightDO>(UserRightDO::class.java) {
    @Autowired
    private lateinit var userRightService: UserRightService

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    fun getList(user: PFUserDO?): List<UserRightDO> {
        val filter = UserRightFilter()
        filter.user = user
        return getList(filter)
    }

    fun internalGetAllOrdered(): List<UserRightDO> {
        return persistenceService.namedQuery(
            UserRightDO.FIND_ALL_ORDERED,
            UserRightDO::class.java
        )
    }

    @JvmOverloads
    fun updateUserRights(user: PFUserDO, list: List<UserRightVO>, updateUserGroupCache: Boolean = true) {
        val dbList = getList(user)
        // evict all entities from the session cache to avoid that the update is already done in the copy method
        val userGroupCache = userGroupCache
        val userGroups = userGroupCache.getUserGroupDOs(user)
        list.forEach { rightVO ->
            var rightDO: UserRightDO? = null
            dbList?.forEach { dbItem ->
                val rightid = userRightService.getRightId(dbItem?.rightIdString)
                if (rightid === rightVO.right.id) {
                    rightDO = dbItem
                }
            }
            if (rightDO == null) {
                if ((rightVO.isBooleanValue && rightVO.value == UserRightValue.FALSE)
                    || rightVO.value == null
                ) {
                    // Right has no value and is not yet in data base.
                    // Do nothing.
                } else {
                    // Create new right instead of updating an existing one.
                    rightDO = UserRightDO(user, rightVO.right.id).setUser(user)
                    rightDO?.let {
                        copy(it, rightVO)
                        save(it)
                    }
                }
            } else {
                rightDO?.let {
                    copy(it, rightVO)
                    val rightId = userRightService.getRightId(it.rightIdString)
                    val right = userRightService.getRight(rightId)
                    if (!right.isAvailable(user, userGroups)
                        || !right.isAvailable(user, userGroups, it.value)
                    ) {
                        it.setValue(null)
                    }
                    update(it)
                }
            }
        }
        // Set unavailable rights to null (if exists):
        dbList?.forEach { rightDO ->
            rightDO?.let {
                val rightId = it.rightIdString
                val right = userRightService.getRight(rightId)
                if (right != null && (!right.isAvailable(user, userGroups)
                            || !right.isAvailable(user, userGroups, it.value))
                ) {
                    it.setValue(null)
                    update(it)
                }
            }
        }
        if (updateUserGroupCache) {
            userGroupCache.setExpired()
        }
    }

    override fun afterUpdate(obj: UserRightDO, dbObj: UserRightDO?, isModified: Boolean) {
        super.afterUpdate(obj, dbObj, isModified)
        if (isModified) {
            userGroupCache!!.setExpired()
        }
    }

    override fun afterSave(obj: UserRightDO) {
        super.afterSave(obj)
        userGroupCache!!.setExpired()
    }

    private fun copy(dest: UserRightDO, src: UserRightVO) {
        if (src.right.isBooleanType) {
            if (src.isBooleanValue) {
                dest.setValue(UserRightValue.TRUE)
            } else {
                dest.setValue(UserRightValue.FALSE)
            }
        } else {
            dest.setValue(src.value)
        }
    }

    fun getUserRights(user: PFUserDO?): List<UserRightVO> {
        val list: MutableList<UserRightVO> = ArrayList()
        if (user?.id == null) {
            return list
        }
        val dbList = getList(user)
        val userGroupCache = userGroupCache
        val userGroups = userGroupCache.getUserGroupDOs(user)
        for (right in userRightService.orderedRights) {
            if (!right.isAvailable(user, userGroups)) {
                continue
            }
            val rightVO = UserRightVO(right)
            for (rightDO in dbList!!) {
                rightDO?.let {
                    if (it.rightIdString == right.id.id) {
                        rightVO.setValue(it.value)
                    }
                }
            }
            list.add(rightVO)
        }
        return list
    }

    override fun getList(filter: BaseSearchFilter): List<UserRightDO> {
        val queryFilter = QueryFilter(filter)
        val myFilter = filter as UserRightFilter
        if (myFilter.user != null) {
            queryFilter.add(eq("user", myFilter.user))
        }
        queryFilter.createJoin("user")
        queryFilter.addOrder(asc("user.username")).addOrder(asc("rightIdString"))
        val list = getList(queryFilter)
        return list
    }

    /**
     * User must member of group finance or controlling.
     */
    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.ADMIN_GROUP)
    }

    /**
     * @see .hasUserSelectAccess
     */
    override fun hasUserSelectAccess(user: PFUserDO, obj: UserRightDO, throwException: Boolean): Boolean {
        return hasUserSelectAccess(user, throwException)
    }

    /**
     * User must member of group admin.
     */
    override fun hasAccess(
        user: PFUserDO, obj: UserRightDO?, oldObj: UserRightDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.ADMIN_GROUP)
    }

    override fun newInstance(): UserRightDO {
        return UserRightDO()
    }

    companion object {
        val ADDITIONAL_SEARCH_FIELDS = arrayOf("user.username", "user.firstname", "user.lastname")
    }
}
