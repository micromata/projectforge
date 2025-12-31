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
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
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
        UserTokenType.STAY_LOGGED_IN_KEY -> {
          data.stayLoggedInKey = tokenData?.token
          data.stayLoggedInKeyCreationDate = getDateString(tokenData?.creationDate)
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
