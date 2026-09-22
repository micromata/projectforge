/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.business.user.*
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.UIColor
import org.projectforge.ui.UIToast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/user")
open class UserServicesRest {
  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var stayLoggedInTokenDao: StayLoggedInTokenDao

  @Autowired
  private lateinit var userDao: UserDao

  @Autowired
  private lateinit var sessionCsrfService: SessionCsrfService

  class AccessLogEntries(userAccessLogEntries: UserAccessLogEntries?) {
    val tokenType: UserTokenType? = userAccessLogEntries?.tokenType
    var entries = userAccessLogEntries?.sortedList()
  }

  @PostMapping("renewToken")
  fun renewToken(
    @RequestParam("token", required = true) tokenString: String,
    request: HttpServletRequest,
    @RequestBody postData: PostData<MyAccountPageRest.MyAccountData>
  ): ResponseEntity<*> {
    sessionCsrfService.validateCsrfToken(request, postData, "Renewing Token")?.let { return it }
    val tokenType = UserTokenType.valueOf(tokenString)
    userAuthenticationsService.renewToken(ThreadLocalUserContext.loggedInUserId!!, tokenType)
    val newToken = userAuthenticationsService.getToken(ThreadLocalUserContext.loggedInUserId!!, tokenType)

    renewToken(postData.data, tokenType, newToken)

    return ResponseEntity.ok(
      ResponseAction(
        message = ResponseAction.Message("user.authenticationToken.renew.successful"),
        targetType = TargetType.UPDATE
      )
        .addVariable("data", postData.data)
    )
  }

  /**
   * Deletes all stay-logged-in tokens of the logged-in user, so every device (including this one) has to log
   * in again. The session itself is left alone: the user is on their my-account page and didn't ask to be
   * thrown out of it.
   */
  @PostMapping("logoutAllDevices")
  fun logoutAllDevices(
    request: HttpServletRequest,
    @RequestBody postData: PostData<MyAccountPageRest.MyAccountData>
  ): ResponseEntity<*> {
    sessionCsrfService.validateCsrfToken(request, postData, "Logout of all devices")?.let { return it }
    val userId = ThreadLocalUserContext.loggedInUserId!!
    log.info("User #$userId logs out all stay-logged-in devices.")
    stayLoggedInTokenDao.deleteAll(userId)
    postData.data.stayLoggedInDevices = translate("login.stayLoggedIn.devices.none")
    return UIToast.createToastResponseEntity(
      translate("login.stayLoggedIn.invalidateAllStayLoggedInSessions.successfullDeleted"),
      color = UIColor.SUCCESS,
      mutableMapOf("data" to postData.data),
      merge = true,
      targetType = TargetType.UPDATE,
    )
  }

  /**
   * The same for another user, for admins only (user edit page).
   */
  @PostMapping("logoutAllDevicesOfUser")
  fun logoutAllDevicesOfUser(
    @RequestParam("userId", required = true) userId: Long,
    request: HttpServletRequest,
    @RequestBody postData: PostData<User>
  ): ResponseEntity<*> {
    log.info("Trying to log out all stay-logged-in devices of user #$userId.")
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    sessionCsrfService.validateCsrfToken(request, postData, "Logout of all devices")?.let { return it }
    stayLoggedInTokenDao.deleteAll(userId)
    postData.data.stayLoggedInDevices = 0
    postData.data.stayLoggedInLastAccessTimeAgo = null
    return UIToast.createToastResponseEntity(
      translate("login.stayLoggedIn.invalidateAllStayLoggedInSessions.successfullDeleted"),
      color = UIColor.SUCCESS,
      mutableMapOf("data" to postData.data),
      merge = true,
      targetType = TargetType.UPDATE,
    )
  }

  @GetMapping("tokenAccess")
  fun getTokenAccess(@RequestParam("token", required = true) tokenString: String): AccessLogEntries {
    val tokenType = UserTokenType.valueOf(tokenString)
    return AccessLogEntries(userAuthenticationsService.getUserAccessLogEntries(tokenType))
  }

  companion object {
    fun setToken(data: MyAccountPageRest.MyAccountData, tokenType: UserTokenType, tokenData: UserTokenData?) {
      when (tokenType) {
        UserTokenType.CALENDAR_REST -> {
          data.calendarExportToken = tokenData?.token
          data.calendarExportTokenCreationDate = getDateString(tokenData?.creationDate)
        }
        UserTokenType.DAV_TOKEN -> {
          data.davToken = tokenData?.token
          data.davTokenCreationDate = getDateString(tokenData?.creationDate)
        }
        UserTokenType.REST_CLIENT -> {
          data.restClientToken = tokenData?.token
          data.restClientTokenCreationDate = getDateString(tokenData?.creationDate)
        }
        else -> {
          throw UnsupportedOperationException()
        }
      }
    }

    fun renewToken(data: MyAccountPageRest.MyAccountData, tokenType: UserTokenType, newToken: String?) {
      setToken(data, tokenType, UserTokenData(newToken, tokenType, Date()))
    }

    fun getDateString(date: Date? = Date()): String {
      date ?: return ""
      return "${PFDateTime.from(date).format()} (${TimeAgo.getMessage(date)})"
    }
  }
}
