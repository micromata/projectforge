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

package org.projectforge.caldav.controller

import io.milton.annotations.AccessControlList
import io.milton.annotations.ChildrenOf
import io.milton.annotations.Users
import io.milton.resource.AccessControlledResource
import io.milton.resource.AccessControlledResource.Priviledge
import org.projectforge.caldav.model.User
import org.projectforge.caldav.model.UsersHome
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.slf4j.LoggerFactory

/**
 * Created by blumenstein on 21.11.16.
 */
open class BaseDAVController: BaseDAVAuthenticationController() {

    @JvmField
    var usersHome: UsersHome? = null
    @AccessControlList
    fun getUserPrivs(target: User?, currentUser: User?): List<Priviledge> {
        val result = mutableListOf<Priviledge>()
        if (target != null && currentUser != null && currentUser.id == target.id) {
            result.add(Priviledge.ALL)
        } else {
            return AccessControlledResource.NONE
        }
        return result
    }

    @ChildrenOf
    @Users
    fun getUsers(usersHome: UsersHome?): Collection<User> {
        val userId = ThreadLocalUserContext.getUserId()
        if (userId == null) {
            log.error("No user authenticated, can't get list of users.")
            return emptyList()
        }
        log.info("Trying to get list of users. Return only logged-in one due to security reasons.")
        val user = User()
        user.id = ThreadLocalUserContext.getUserId().toLong()
        user.username = ThreadLocalUserContext.getUser().username
        return listOf(user)
    }

    companion object {
        private val log = LoggerFactory.getLogger(BaseDAVController::class.java)
    }
}
