/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.Validate
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.framework.persistence.utils.SQLHelper.ensureUniqueResult
import org.projectforge.framework.utils.NumberHelper.getSecureRandomUrlSaveString
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class UserAuthenticationsDao : BaseDao<UserAuthenticationsDO>(UserAuthenticationsDO::class.java) {
    @Autowired
    private lateinit var userDao: UserDao

    internal var userTokenCache: UserTokenCache? = null

    /**
     * return true for admin users.
     */
    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return accessChecker.isUserMemberOfAdminGroup(user)
    }

    override fun hasAccess(user: PFUserDO, obj: UserAuthenticationsDO, oldObj: UserAuthenticationsDO?,
                           operationType: OperationType,
                           throwException: Boolean): Boolean {
        Validate.notNull(obj)
        Validate.notNull(obj.user)
        return hasLoggedInUserAccess(obj.user!!.id, throwException)
    }

    private fun hasLoggedInUserAccess(ownerUserId: Int, throwException: Boolean = true): Boolean {
        if (accessChecker.isLoggedInUserMemberOfAdminGroup() || ownerUserId == ThreadLocalUserContext.getUserId()) {
            return true
        }
        if (throwException) {
            throw AccessException("access.exception.violation", "AccessToken")
        }
        return false
    }

    override fun afterSaveOrModify(obj: UserAuthenticationsDO?) {
        if (userTokenCache != null) {
            throw UnsupportedOperationException("Not yet implmented: userTokenCache.refresh...")
        }
    }

    override fun newInstance(): UserAuthenticationsDO {
        return UserAuthenticationsDO()
    }

    /**
     * @param authentications
     * @param userId   If null, then user will be set to null;
     * @see BaseDao.getOrLoad
     */
    open fun setUser(authentications: UserAuthenticationsDO, userId: Int) {
        val user = userDao.getOrLoad(userId)
        authentications.user = user
    }

    /**
     * @return Stored or created authentications object for given user.
     * @throws AccessException if the logged-in user neither doesn't match the given user nor is admin user.
     */
    open fun getByUserId(userId: Int): UserAuthenticationsDO? {
        return ensureAuthentications(userId)
    }

    open fun getUserByToken(userId: Int, type: UserTokenType, token: String?): PFUserDO? {
        if (token.isNullOrBlank() || token.trim().length < 10) {
            log.warn("Token for user $userId too short, aborting.")
            return null
        }
        val queryName = when (type) {
            UserTokenType.CALENDAR_REST -> UserAuthenticationsDO.FIND_USER_BY_USERNAME_AND_CALENDAR_TOKEN
            UserTokenType.DAV_TOKEN -> UserAuthenticationsDO.FIND_USER_BY_USERNAME_AND_DAV_TOKEN
            UserTokenType.REST_CLIENT -> UserAuthenticationsDO.FIND_USER_BY_USERNAME_AND_REST_CLIENT_TOKEN
            UserTokenType.STAY_LOGGED_IN_KEY -> UserAuthenticationsDO.FIND_USER_BY_USERNAME_AND_STAY_LOGGED_IN_KEY
        }
        val user = ensureUniqueResult(em
                .createNamedQuery(queryName, PFUserDO::class.java)
                .setParameter("id", userId)
                .setParameter("token", token))
        if (user != null && !user.hasSystemAccess()) {
            log.warn("Deleted user tried to login (via token '$type'): $user")
            return null
        }
        return user
    }

    /**
     * Returns the user's authentication token if exists (must be not blank with a size >= 10). If not, a new token key
     * will be generated.
     * @throws AccessException if logged in user isn't either admin user nor owner of this token.
     */
    open fun getToken(userId: Int, type: UserTokenType): String? {
        if (ThreadLocalUserContext.getUserId() != userId) { // Only admin users are able to renew authentication token of other users:
            accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        }
        val authentications = ensureAuthentications(userId)
        return authentications.getToken(type)
    }

    /**
     * Returns the user's authentication token if exists (must be not blank with a size >= 10). If not, a new token key
     * will be generated.
     */
    open fun getToken(username: String, type: UserTokenType): String? {
        val authentications = ensureAuthentications(username)
        return authentications.getToken(type)
    }

    private fun checkAndFixAuthenticationTokens(authentication: UserAuthenticationsDO) {
        authentication.user?.id?.let { userId ->
            var changed = checkAndFixToken(authentication, userId, UserTokenType.CALENDAR_REST)
            changed = changed || checkAndFixToken(authentication, userId, UserTokenType.DAV_TOKEN)
            changed = changed || checkAndFixToken(authentication, userId, UserTokenType.REST_CLIENT)
            changed = changed || checkAndFixToken(authentication, userId, UserTokenType.STAY_LOGGED_IN_KEY)
            if (changed) {
                update(authentication)
            }
        }
    }

    private fun checkAndFixToken(authentications: UserAuthenticationsDO, userId: Int, type: UserTokenType): Boolean {
        val token = authentications.getToken(type)
        if (token.isNullOrBlank() || token.trim().length < 10) {
            log.info("Authentication token '$type' renewed for user: $userId")
            authentications.setToken(type, createAuthenticationToken())
            return true
        }
        return false
    }

    /**
     * Renews the user's authentication token (random string sequence).
     */
    open fun renewToken(userId: Int, type: UserTokenType) {
        accessChecker.checkRestrictedOrDemoUser() // Demo users are also not allowed to do this.
        val authentications = getByUserId(userId)
        if (authentications == null) {
            log.warn("No user authentications object found for user $userId. Nothing to renew for token '$type'.")
            return
        }
        authentications.setToken(type, createAuthenticationToken())
        update(authentications)
        log.info("Authentication token '$type' renewed for user: $userId")
    }

    private fun createAuthenticationToken(): String? {
        return getSecureRandomUrlSaveString(AUTHENTICATION_TOKEN_LENGTH.toInt())
    }

    /**
     * @return Stored or created authentications object for given user.
     * @throws AccessException if the logged-in user neither doesn't match the given user nor is admin user.
     */
    private fun ensureAuthentications(userId: Int): UserAuthenticationsDO {
        hasLoggedInUserAccess(userId)
        var authentications = ensureUniqueResult(em
                .createNamedQuery(UserAuthenticationsDO.FIND_BY_USER_ID, UserAuthenticationsDO::class.java)
                .setParameter("userId", userId))
        if (authentications == null) {
            authentications = UserAuthenticationsDO()
            setUser(authentications, userId)
            authentications.calendarExportToken = createAuthenticationToken()
            authentications.davToken = createAuthenticationToken()
            authentications.restClientToken = createAuthenticationToken()
            authentications.stayLoggedInKey = createAuthenticationToken()
            save(authentications)
        } else {
            checkAndFixAuthenticationTokens(authentications)
        }
        return authentications
    }

    private fun ensureAuthentications(username: String): UserAuthenticationsDO {
        val userId = UserGroupCache.tenantInstance.getUser(username)?.id
                ?: throw IllegalArgumentException("User with username 'username' not found.")
        return ensureAuthentications(userId)
    }

    companion object {
        private val log = LoggerFactory.getLogger(UserAuthenticationsDao::class.java)
        private val AUTHENTICATION_TOKEN_LENGTH: Short = 20
    }
}
