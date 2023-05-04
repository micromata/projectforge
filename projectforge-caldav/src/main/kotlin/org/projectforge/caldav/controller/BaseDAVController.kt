/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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
import mu.KotlinLogging
import org.projectforge.business.user.UserAuthenticationsDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.caldav.model.User
import org.projectforge.caldav.model.UsersHome
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.security.RegisterUser4Thread
import org.projectforge.security.SecurityLogging
import org.springframework.beans.factory.annotation.Autowired

private val log = KotlinLogging.logger {}

/**
 * Created by blumenstein on 21.11.16.
 */
open class BaseDAVController : BaseDAVAuthenticationController() {
  @Autowired
  private lateinit var userAuthenticationsDao: UserAuthenticationsDao

  @JvmField
  var usersHome: UsersHome? = null

  @AccessControlList
  fun getUserPrivs(target: User?, currentUser: User?): List<Priviledge> {
    log.debug { "getUserPrivs" }
    val result = mutableListOf<Priviledge>()
    if (target != null && target.id == currentUser?.id) {
      result.add(Priviledge.ALL)
    } else {
      return AccessControlledResource.NONE
    }
    return result
  }

  @ChildrenOf
  @Users
  fun getUsers(usersHome: UsersHome?): Collection<User> {
    val contextUser = ThreadLocalUserContext.user
    if (contextUser == null) {
      log.error("No user authenticated, can't get list of users.")
      return emptyList()
    }
    log.info("Trying to get list of users. Return only logged-in one due to security reasons.")
    val user = User()
    user.id = contextUser.id.toLong()
    user.username = contextUser.username
    return listOf(user)
  }

  override fun checkAuthentication(username: String?, token: String?): Boolean {
    ensureAutowire()
    log.debug { "checkAuthentication" }
    if (username.isNullOrBlank()) {
      log.info { "username not given, can't authenticate user." }
      return false
    }
    if (token.isNullOrBlank()) {
      log.info { "DAV token (password) not given, can't authenticate user." }
      return false
    }
    log.debug { "Trying to authenticate user '$username'..." }
    val authenticatedUser = userAuthenticationsDao.getUserByToken(username, UserTokenType.DAV_TOKEN, token)
    if (authenticatedUser == null) {
      val msg = "Can't authenticate user '$username' by given token. User name and/or token invalid."
      log.error(msg)
      SecurityLogging.logSecurityWarn(
        this::class.java,
        "${UserTokenType.DAV_TOKEN.name} AUTHENTICATION FAILED",
        msg
      )
      return false
    }
    log.debug { "Registering authenticated user '$username'" }
    RegisterUser4Thread.registerUser(authenticatedUser)
    return true
  }
}
