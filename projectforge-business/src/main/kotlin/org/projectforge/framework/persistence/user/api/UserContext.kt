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

package org.projectforge.framework.persistence.user.api

import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.slf4j.LoggerFactory
import java.io.Serializable

/**
 * User context for logged-in users. Contains the user etc.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class UserContext() : Serializable {
    /**
     * @return the user
     */
    var user: PFUserDO? = null
    val employeeId: Long?
        get() = EmployeeCache.instance.getEmployeeIdByUserId(user?.id)

    /**
     * If true, a 2FA authentification is required before any further access. This field may be set to true,
     * if a 2FA is required after login.
     */
    var new2FARequired: Boolean = false

    /**
     * Last successful two factor authentification for this user (in session/stay-login) in epoch ms.
     */
    var lastSuccessful2FA: Long? = null

    /**
     * Has the user configured his Authenticator-App? Null means, that this state isn't yet checkted.
     */
    var authenticatorAppConfigured: Boolean? = null

    /**
     * Sets current millis as last successful 2FA and [new2FARequired] to false.
     */
    fun updateLastSuccessful2FA() {
        this.lastSuccessful2FA = System.currentTimeMillis()
        new2FARequired = false
    }

    /**
     * See RestAuthenticationInfo of ProjectForge's rest module.
     */
    var loggedInByAuthenticationToken = false

    /**
     * Stores the given user in the context.
     *
     * @param user
     */
    constructor(user: PFUserDO) : this() {
        this.user = user
    }

    /**
     * Clear all fields (user etc.).
     *
     * @return this for chaining.
     */
    fun logout(): UserContext {
        user = null
        return this
    }

    /**
     * Refreshes the user stored in the user group cache. Ignore fields such as stayLoggedInKey, password and
     * passwordSalt.
     *
     * @return this for chaining.
     */
    fun refreshUser(): UserContext {
        val updatedUser = UserGroupCache.getInstance().getUser(user!!.id)
        if (updatedUser == null) {
            log.warn("Couldn't update user from UserCache, should only occur in maintenance mode!")
            return this
        }
        user = updatedUser
        return this
    }

    companion object {
        private val log = LoggerFactory.getLogger(UserContext::class.java)
        private const val serialVersionUID = 4934701869144478233L

        /**
         * Don't use this method. It's used for creating an UserContext without copying a user.
         *
         * @param user
         * @return The created UserContext.
         */
        @JvmStatic
        fun __internalCreateWithSpecialUser(user: PFUserDO): UserContext {
            return UserContext(user)
        }

        @JvmStatic
        fun createTestInstance(user: PFUserDO): UserContext {
            val ctx = UserContext()
            ctx.user = user
            return ctx
        }
    }
}
