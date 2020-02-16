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

import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * Caches authentication tokens (5 minutes).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
internal class UserTokenCache() : AbstractCache(), UserChangedListener {

    @Autowired
    private lateinit var userAuthenticationTokenDao: UserAuthenticationsDao

    @Autowired
    private lateinit var userDao: UserDao

    private val authenticationCache = mutableMapOf<Int, UserAuthenticationsDO>()

    fun getAuthenticationToken(userId: Int, type: UserTokenType): String? {
        checkRefresh()
        val authentications = getAuthentications(userId) ?: return null
        return authentications.getToken(type)
    }

    fun getAuthenticationToken(username: String, type: UserTokenType): String? {
        checkRefresh()
        val userId = UserGroupCache.tenantInstance.getUser(username)?.id ?: return null
        return getAuthenticationToken(userId, type)
    }

    private fun getAuthentications(userId: Int): UserAuthenticationsDO? {
        checkRefresh()
        var authentications = authenticationCache[userId]
        if (authentications == null) {
            authentications = userAuthenticationTokenDao.getByUserId(userId) ?: return null
            userAuthenticationTokenDao.decryptAllTokens(authentications) // Decrypt all tokens for faster access.
            authenticationCache[userId] = authentications
        }
        return authentications
    }


    /**
     * Clears authentication token.
     *
     * @param user
     * @param operationType
     */
    override fun afterUserChanged(user: PFUserDO, operationType: OperationType?) {
        synchronized(authenticationCache) {
            this.authenticationCache.remove(user.id)
        }
    }

    /**
     * This method will be called by CacheHelper and is synchronized.
     */
    override fun refresh() {
        synchronized(authenticationCache) {
            authenticationCache.clear()
        }
    }

    @PostConstruct
    fun postConstruct() {
        userAuthenticationTokenDao.userTokenCache = this
        userDao.register(this)
    }

    companion object {
        private val log = LoggerFactory.getLogger(UserTokenCache::class.java)
    }

    init {
        setExpireTimeInMinutes(5)
    }
}
