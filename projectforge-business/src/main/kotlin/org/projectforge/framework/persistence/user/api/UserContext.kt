/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.Validate
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.PFUserDO.Companion.createCopyWithoutSecretFields
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.slf4j.LoggerFactory
import java.io.Serializable

/**
 * User context for logged-in users. Contains the user and the current tenant (if any) etc.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class UserContext() : Serializable {
    /**
     * @return the user
     */
    var user: PFUserDO? = null
        private set
    val employeeId: Int?
        get() = userGroupCache.getEmployeeId(user?.id)
    /**
     * @return the currentTenant
     */
    var currentTenant: TenantDO? = null
    /**
     * @return the stayLoggedIn
     */
    var isStayLoggedIn = false
    private lateinit var userGroupCache: UserGroupCache

    /**
     * See RestAuthenticationInfo of ProjectForge's rest module.
     */
    var loggedInByAuthenticationToken = false

    /**
     * Stores the given user in the context. If the user contains secret fields (such as password etc.) a copy without
     * such fields is stored.
     *
     * @param user
     */
    @JvmOverloads
    constructor(user: PFUserDO, userGroupCache: UserGroupCache? = null): this() {
        Validate.notNull(user)
        this.user = user
        this.userGroupCache = userGroupCache ?: UserGroupCache.tenantInstance
        if (user.hasSecretFieldValues()) {
            log.warn(
                    "Should instantiate UserContext with user containing secret values (makes now a copy of the given user).")
            this.user = createCopyWithoutSecretFields(user)
        } else {
            this.user = user
        }
        currentTenant = user.tenant
    }

    /**
     * Clear all fields (user etc.).
     *
     * @return this for chaining.
     */
    fun logout(): UserContext {
        user = null
        currentTenant = null
        isStayLoggedIn = false
        return this
    }

    /**
     * Refreshes the user stored in the user group cache. Ignore fields such as stayLoggedInKey, password and
     * passwordSalt.
     *
     * @return this for chaining.
     */
    fun refreshUser(): UserContext {
        val updatedUser = userGroupCache.getUser(user!!.id)
        if (updatedUser == null) {
            log.warn("Couldn't update user from UserCache, should only occur in maintenance mode!")
            return this
        }
        if (user!!.hasSecretFieldValues()) {
            log.warn(
                    "Oups, userCache contains user (id=" + user!!.id + ") with secret values, please contact developers.")
            user = createCopyWithoutSecretFields(updatedUser)
        } else {
            user = updatedUser
        }
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
        fun __internalCreateWithSpecialUser(user: PFUserDO, userGroupCache: UserGroupCache): UserContext {
            return UserContext(user, userGroupCache)
        }

        @JvmStatic
        fun createTestInstance(user: PFUserDO): UserContext {
            val ctx = UserContext() // Can't user UserContext(PFUserDO) constructor: UserGroupCache of tenant not yet initialized.
            ctx.user = user
            return ctx
        }
    }
}
