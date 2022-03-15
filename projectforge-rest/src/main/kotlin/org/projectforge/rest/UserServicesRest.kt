/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.user.UserAccessLogEntries
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/user")
open class UserServicesRest {
  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

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
    response: HttpServletResponse,
    @RequestBody postData: PostData<MyAccountPageRest.MyAccountData>
  ): ResponseEntity<*> {
    sessionCsrfService.validateCsrfToken(request, postData, "Renewing Token")?.let { return it }
    val tokenType = UserTokenType.valueOf(tokenString)
    userAuthenticationsService.renewToken(ThreadLocalUserContext.getUserId(), tokenType)
    val newToken = userAuthenticationsService.getToken(ThreadLocalUserContext.getUserId(), tokenType)

    when (tokenType) {
      UserTokenType.CALENDAR_REST -> postData.data.calendarExportToken = newToken
      UserTokenType.DAV_TOKEN -> postData.data.davToken = newToken
      UserTokenType.REST_CLIENT -> postData.data.restClientToken = newToken
      UserTokenType.STAY_LOGGED_IN_KEY -> postData.data.stayLoggedInKey = newToken
      else -> {
        throw UnsupportedOperationException()
      }
    }

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
}
