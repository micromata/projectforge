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
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import org.projectforge.business.login.Login
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.*
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.loggedInUserId
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.framework.utils.Crypt.decrypt
import org.projectforge.framework.utils.Crypt.encrypt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class UserDao : BaseDao<PFUserDO>(PFUserDO::class.java) {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    private var userPasswordDao: UserPasswordDao? = null

    override val defaultSortProperties: Array<SortProperty>
        get() = DEFAULT_SORT_PROPERTIES

    val defaultFilter: QueryFilter
        get() {
            val queryFilter = QueryFilter(null)
            queryFilter.add(eq("deleted", false))
            return queryFilter
        }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDao.createQueryFilter
     */
    override fun createQueryFilter(filter: BaseSearchFilter?): QueryFilter {
        return QueryFilter(filter)
    }

    override fun getList(filter: BaseSearchFilter): List<PFUserDO> {
        val myFilter = if (filter is PFUserFilter) {
            filter
        } else {
            PFUserFilter(filter)
        }
        val queryFilter = createQueryFilter(myFilter)
        if (myFilter.deactivatedUser != null) {
            queryFilter.add(eq("deactivated", myFilter.deactivatedUser))
        }
        if (Login.getInstance().hasExternalUsermanagementSystem()) {
            // Check hasExternalUsermngmntSystem because otherwise the filter is may-be preset for an user and the user can't change the filter
            // (because the fields aren't visible).
            if (myFilter.restrictedUser != null) {
                queryFilter.add(eq("restrictedUser", myFilter.restrictedUser))
            }
            if (myFilter.localUser != null) {
                queryFilter.add(eq("localUser", myFilter.localUser))
            }
        }
        if (myFilter.hrPlanning != null) {
            queryFilter.add(eq("hrPlanning", myFilter.hrPlanning))
        }
        var list = getList(queryFilter)
        if (myFilter.isAdminUser != null) {
            val origList = list
            list = LinkedList()
            for (user in origList) {
                if (myFilter.isAdminUser == accessChecker.isUserMemberOfAdminGroup(user, false)) {
                    list.add(user)
                }
            }
        }
        return list
    }

    fun getUserRights(userId: Long?): List<UserRightDO>? {
        return userGroupCache.getUserRights(userId)
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDao.afterSaveOrModify
     */
    override fun afterSaveOrModify(obj: PFUserDO) {
        if (!obj.isMinorChange) {
            userGroupCache.setExpired()
        }
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDao.hasAccess
     */
    override fun hasAccess(
        user: PFUserDO, obj: PFUserDO?, oldObj: PFUserDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        return accessChecker.isUserMemberOfAdminGroup(user, throwException)
    }

    /**
     * @return false, if no admin user and the context user is not at minimum in one groups assigned to the given user or
     * false. Also deleted and deactivated users are only visible for admin users.
     * @see org.projectforge.framework.persistence.api.BaseDao.hasUserSelectAccess
     * @see AccessChecker.areUsersInSameGroup
     */
    override fun hasUserSelectAccess(user: PFUserDO, obj: PFUserDO, throwException: Boolean): Boolean {
        var result = accessChecker.isUserMemberOfAdminGroup(user)
                || accessChecker.isUserMemberOfGroup(
            user, ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.CONTROLLING_GROUP
        )
        log.debug("UserDao hasSelectAccess. Check user member of admin, finance or controlling group: $result")
        if (!result && obj.hasSystemAccess()) {
            result = accessChecker.areUsersInSameGroup(user, obj)
            log.debug(
                ("UserDao hasSelectAccess. Caller user: " + user.username + " Check user: " + obj.username
                        + " Check user in same group: " + result)
            )
        }
        if (throwException && !result) {
            throw AccessException(user, AccessType.GROUP, OperationType.SELECT)
        }
        return result
    }

    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDao.hasInsertAccess
     */
    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return accessChecker.isUserMemberOfAdminGroup(user, false)
    }

    /**
     * Update user after login success.
     *
     * @param user the user
     */
    fun updateUserAfterLoginSuccess(user: PFUserDO) {
        persistenceService.runInTransaction { context ->
            context.criteriaUpdate(PFUserDO::class.java) { cb, root, update ->
                update.set(root.get("lastLogin"), Date())
                update.set(root.get("loginFailures"), 0)
                update.where(cb.equal(root.get<Long>("id"), user.id));
            }
        }
    }

    fun updateIncrementLoginFailure(username: String) {
        persistenceService.runInTransaction { context ->
            context.criteriaUpdate(PFUserDO::class.java) { cb, root, update ->
                update.set(root.get<Int>("loginFailures"), cb.sum(root.get("loginFailures"), 1))
                update.where(cb.equal(root.get<String>("username"), username));
            }
        }
    }

    /**
     * Does an user with the given username already exists? Works also for existing users (if username was modified).
     */
    fun doesUsernameAlreadyExist(user: PFUserDO): Boolean {
        Validate.notNull(user)
        var dbUser: PFUserDO? = null
        dbUser = if (user.id == null) {
            // New user
            getInternalByName(user.username)
        } else {
            // user already exists. Check maybe changed username:
            persistenceService.selectNamedSingleResult(
                PFUserDO.FIND_OTHER_USER_BY_USERNAME,
                PFUserDO::class.java,
                Pair("username", user.username),
                Pair("id", user.id),
            )
        }
        return dbUser != null
    }

    fun getInternalByName(username: String?): PFUserDO? {
        return persistenceService.selectNamedSingleResult(
            PFUserDO.FIND_BY_USERNAME,
            PFUserDO::class.java,
            Pair("username", username),
        )
    }

    /**
     * User can modify own setting, this method ensures that only such properties will be updated, the user's are allowed
     * to.
     *
     * @param user
     */
    fun updateMyAccount(user: PFUserDO) {
        accessChecker.checkRestrictedOrDemoUser()
        val contextUser = ThreadLocalUserContext.loggedInUser
        Validate.isTrue(user.id == contextUser!!.id)
        val dbUser = internalGetById(user.id)
        dbUser!!.timeZone = user.timeZone
        dbUser.dateFormat = user.dateFormat
        dbUser.excelDateFormat = user.excelDateFormat
        dbUser.timeNotation = user.timeNotation
        dbUser.locale = user.locale
        dbUser.personalPhoneIdentifiers = user.personalPhoneIdentifiers
        dbUser.sshPublicKey = user.sshPublicKey
        dbUser.firstname = user.firstname
        dbUser.lastname = user.lastname
        dbUser.description = user.description
        dbUser.gpgPublicKey = user.gpgPublicKey
        dbUser.sshPublicKey = user.sshPublicKey
        val result = internalUpdate(dbUser)
        if (result != EntityCopyStatus.NONE) {
            log.info("Object updated: $dbUser")
            copyValues(user, contextUser)
        } else {
            log.info("No modifications detected (no update needed): $dbUser")
        }
        userGroupCache.updateUser(contextUser)
    }

    /**
     * Gets history entries of super and adds all history entries of the UserRightDO children.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.getDisplayHistoryEntries
     */
    override fun getDisplayHistoryEntries(obj: PFUserDO): MutableList<DisplayHistoryEntry> {
        val list = super.getDisplayHistoryEntries(obj)
        if (!hasLoggedInUserHistoryAccess(obj, false)) {
            return list
        }
        if (CollectionUtils.isNotEmpty(obj.rights)) {
            for (right in obj.rights!!) {
                val entries = internalGetDisplayHistoryEntries(right)
                for (entry in entries) {
                    val propertyName = entry.propertyName
                    if (propertyName != null) {
                        entry.displayPropertyName =
                            right.rightIdString + ":" + entry.propertyName // Prepend number of positon.
                    } else {
                        entry.displayPropertyName = right.rightIdString.toString()
                    }
                }
                mergeList(list, entries)
            }
        }
        list.sortWith({ o1, o2 ->
            (o2.timestamp.compareTo(
                o1.timestamp
            ))
        })
        return list
    }

    override fun hasHistoryAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return accessChecker.isUserMemberOfAdminGroup(user, throwException)
    }

    /**
     * Re-index all dependent objects only if the username, first or last name was changed.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.wantsReindexAllDependentObjects
     */
    override fun wantsReindexAllDependentObjects(obj: PFUserDO, dbObj: PFUserDO): Boolean {
        if (!super.wantsReindexAllDependentObjects(obj, dbObj)) {
            return false
        }
        return !StringUtils.equals(obj.username, dbObj.username)
                || !StringUtils.equals(obj.firstname, dbObj.firstname)
                || !StringUtils.equals(obj.lastname, dbObj.lastname)
    }

    override fun newInstance(): PFUserDO {
        return PFUserDO()
    }

    fun findByUsername(username: String?): List<PFUserDO> {
        return persistenceService.executeNamedQuery(
            PFUserDO.FIND_BY_USERNAME,
            PFUserDO::class.java,
            Pair("username", username),
        )
    }

    /**
     * Encrypts the given data with the user's password hash. If the user changes his password, decryption isn't possible
     * anymore.
     *
     * @param data The data to encrypt.
     * @return The encrypted data.
     */
    fun encrypt(data: String): String? {
        val password = getPasswordOfUser(loggedInUserId!!) ?: return null
        return encrypt(password, data)
    }

    /**
     * Decrypts the given data with the user's password hash. If the user changes his password, decryption isn't possible
     * anymore.
     *
     * @param encrypted The data to encrypt.
     * @return The decrypted data.
     */
    fun decrypt(encrypted: String?): String? {
        return decrypt(encrypted, loggedInUserId!!)
    }

    /**
     * Decrypts the given data with the user's password hash. If the user changes his password, decryption isn't possible
     * anymore.
     *
     * @param encrypted The data to encrypt.
     * @param userId    Use the password of the given user (used by CookieService, because user isn't yet logged-in).
     * @return The decrypted data.
     * @see UserDao.decrypt
     */
    fun decrypt(encrypted: String?, userId: Long): String? {
        val password = getPasswordOfUser(userId) ?: return null
        return decrypt(password, encrypted)
    }

    private fun getPasswordOfUser(userId: Long): String? {
        if (userPasswordDao == null) {
            userPasswordDao = applicationContext.getBean(UserPasswordDao::class.java)
        }
        val passwordObj = userPasswordDao!!.internalGetByUserId(userId)
        if (passwordObj == null) {
            log.warn("Can't encrypt data. Password for user $userId not found.")
            return null
        }
        if (StringUtils.isBlank(passwordObj.passwordHash)) {
            log.warn("Can't encrypt data. Password of user '$userId not found.")
            return null
        }
        return passwordObj.passwordHash
    }

    companion object {
        val DEFAULT_SORT_PROPERTIES = arrayOf(SortProperty("firstname"), SortProperty("lastname"))
    }
}
