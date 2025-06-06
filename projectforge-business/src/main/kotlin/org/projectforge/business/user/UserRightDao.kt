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

import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserRightDao protected constructor() : BaseDao<UserRightDO>(UserRightDO::class.java) {
    @Autowired
    private lateinit var userRightService: UserRightService

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    /**
     * Returns the list of user rights for the given user. The rights will not be evaluated if they are available for the user.
     */
    fun select(user: PFUserDO?, checkAccess: Boolean = true): List<UserRightDO> {
        if (checkAccess) {
            accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        }
        return persistenceService.executeNamedQuery(
            UserRightDO.FIND_ALL_BY_USER_ID,
            UserRightDO::class.java,
            "userId" to user?.id,
        )
    }

    fun queryAll(userId: Long): List<UserRightDO> {
        return persistenceService.runReadOnly {
            persistenceService.executeNamedQuery(
                UserRightDO.FIND_ALL_BY_USER_ID,
                UserRightDO::class.java,
                "userId" to userId,
            )
        }
    }

    /**
     * Returns all rights of the database: select from UserRightDO order by user.id, rightIdString
     *
     */
    fun selectAllOrdered(): List<UserRightDO> {
        return persistenceService.executeNamedQuery(
            UserRightDO.FIND_ALL_ORDERED,
            UserRightDO::class.java
        )
    }

    /**
     * Evaluates the rights of the user and updates the database. The rights will be evaluated if they are available for the user.
     * If a right is not available for the user, the value of the right will be set to null.
     */
    @JvmOverloads
    fun updateUserRights(user: PFUserDO, list: List<UserRightVO>, updateUserGroupCache: Boolean = true) {
        persistenceService.runInTransaction { _ ->
            val dbList = select(user)
            // evict all entities from the session cache to avoid that the update is already done in the copy method
            val userGroupCache = userGroupCache
            val userGroups = userGroupCache.getUserGroupDOs(user)
            val historyUserComment = user.historyUserComment // Save the history user comment also for the rights.
            list.forEach { rightVO ->
                var rightDO: UserRightDO? = null
                dbList.forEach { dbItem ->
                    val rightId = userRightService.getRightId(dbItem.rightIdString)
                    if (rightId == rightVO.right.id) {
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
                        rightDO = UserRightDO(user, rightVO.right.id).withUser(user).also {
                            copy(it, rightVO)
                            it.historyUserComment = historyUserComment // Save the history user comment also for the rights.
                            insert(it)
                        }
                    }
                } else {
                    rightDO!!.let {
                        copy(it, rightVO)
                        val rightId = userRightService.getRightId(it.rightIdString)
                        val right = userRightService.getRight(rightId)
                        if (!right.isAvailable(user, userGroups)
                            || !right.isAvailable(user, userGroups, it.value)
                        ) {
                            it.value = null
                        }
                        it.historyUserComment = historyUserComment // Save the history user comment also for the rights.
                        update(it)
                    }
                }
            }
            // Set unavailable rights to null (if exists):
            dbList.forEach { rightDO ->
                rightDO.let {
                    val rightId = it.rightIdString
                    val right = userRightService.getRight(rightId)
                    if (right != null && (!right.isAvailable(user, userGroups)
                                || !right.isAvailable(user, userGroups, it.value))
                    ) {
                        it.value = null
                        it.historyUserComment = historyUserComment // Save the history user comment also for the rights.
                        update(it)
                    }
                }
            }
            if (updateUserGroupCache) {
                userGroupCache.setExpired()
            }
        }
    }

    override fun afterUpdate(obj: UserRightDO, dbObj: UserRightDO?, isModified: Boolean) {
        if (isModified) {
            userGroupCache.setExpired()
        }
    }

    override fun afterInsert(obj: UserRightDO) {
        userGroupCache.setExpired()
    }

    private fun copy(dest: UserRightDO, src: UserRightVO) {
        if (src.right.isBooleanType) {
            if (src.isBooleanValue) {
                dest.value = UserRightValue.TRUE
            } else {
                dest.value = UserRightValue.FALSE
            }
        } else {
            dest.value = src.value
        }
    }

    fun getUserRights(user: PFUserDO?): List<UserRightVO> {
        val list: MutableList<UserRightVO> = ArrayList()
        if (user?.id == null) {
            return list
        }
        val dbList = select(user)
        val userGroupCache = userGroupCache
        val userGroups = userGroupCache.getUserGroupDOs(user)
        for (right in userRightService.orderedRights) {
            if (!right.isAvailable(user, userGroups)) {
                continue
            }
            val rightVO = UserRightVO(right)
            for (rightDO in dbList) {
                rightDO.let {
                    if (it.rightIdString == right.id.id) {
                        rightVO.setValue(it.value)
                    }
                }
            }
            list.add(rightVO)
        }
        return list
    }

    override fun select(filter: BaseSearchFilter): List<UserRightDO> {
        throw UnsupportedOperationException("Not implemented.")
       /* val queryFilter = QueryFilter(filter)
        val myFilter = filter as UserRightFilter
        if (myFilter.user != null) {
            queryFilter.add(eq("user", myFilter.user))
        }
        queryFilter.createJoin("user")
        queryFilter.addOrder(asc("user.username")).addOrder(asc("rightIdString"))
        val list = select(queryFilter)
        return list*/
    }

    /**
     * User must be member of group finance or controlling.
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
     * User must be member of group admin.
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
