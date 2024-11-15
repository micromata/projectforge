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

package org.projectforge.rest.core

import mu.KotlinLogging
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.ServerData
import org.projectforge.rest.utils.RequestLog
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.ValidationError
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import jakarta.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Caches the csrf token per session id's of the clients (for up to 4 Hours). Every hour, expired csrf tokens will be removed.
 */
@Service
open class SessionCsrfService
  : AbstractSessionCache<String>(
  expireTimeInMillis = 4 * TICKS_PER_HOUR,
  clearEntriesIntervalInMillis = TICKS_PER_HOUR
) {

  /**
   * Checks the CSRF token. If the user is logged in by an authenticationToken [RestAuthenticationInfo.loggedInByAuthenticationToken] and the CSRF token is missed no check will be done.
   * Therefore pure Rest clients may not care about the CSRF token.
   *
   * If null is returned, the caller may proceed. If not, the caller should simply return the ResponseAction. The
   * user will get an updaten CSRF token and should redo his last action.
   * @param request http request
   * @param postData PostData should contain serverData.csrfToken
   * @param logAction Displayed on failed csrf check if given. If not given: "Modification" will be used: $logAction of data declined.
   * @return null if CSRF check is OK, otherwise an ResponseEntity (status OK) with validation error and updated
   * csrf token from user's session.
   */
  fun validateCsrfToken(
    request: HttpServletRequest, postData: PostData<*>, logAction: String = "Modification"
  ): ResponseEntity<ResponseAction>? {
    val csrfToken = postData.serverData?.csrfToken
    if (csrfToken.isNullOrBlank() && ThreadLocalUserContext.userContext?.loggedInByAuthenticationToken == true) {
      if (log.isDebugEnabled) {
        log.debug { "User '${ThreadLocalUserContext.loggedInUser?.username}' logged in by rest call, not by session." }
      }
      return null
    }
    if (checkToken(request, csrfToken)) {
      // Check OK.
      return null
    }
    log.warn("Check of CSRF token failed, a validation error will be shown. $logAction of data declined: ${postData.data}. Expected token='${super.getSessionData(request)}', given token='$csrfToken'")
    val validationErrors = mutableListOf<ValidationError>()
    validationErrors.add(ValidationError.create("errorpage.csrfError"))
    postData.serverData = createServerData(request)
    return ResponseEntity.ok(
      ResponseAction(
        targetType = TargetType.UPDATE, merge = true,
        validationErrors = validationErrors
      ).addVariable("serverData", postData.serverData)
        .addVariable("validationErrors", validationErrors)
    )
  }

  fun createServerData(request: HttpServletRequest): ServerData {
    return ServerData(csrfToken = ensureAndGetToken(request))
  }

  private fun checkToken(request: HttpServletRequest, token: String?): Boolean {
    if (token.isNullOrEmpty() || token.trim().length < TOKEN_LENGTH) {
      log.info { "Token to short, check failed for session id '${RequestLog.getTruncatedSessionId(request)}'." }
      return false
    }
    val expected = ensureAndGetToken(request) // If no token is given or is expired, create a new one for next request.
    if (expected == token) {
      return true
    }
    log.info { "Token check failed for session id '${RequestLog.getTruncatedSessionId(request)}': expected='$expected', given='$token'" }
    return false
  }

  private fun ensureAndGetToken(request: HttpServletRequest): String {
    var token = super.getSessionData(request)
    if (token != null && token.length == TOKEN_LENGTH) {
      return token
    }
    token = NumberHelper.getSecureRandomAlphanumeric(TOKEN_LENGTH)
    log.debug { "No valid csrf token found in AbstractSessionCache, creating '$token' for session id '${RequestLog.getTruncatedSessionId(request)}'" }
    super.registerSessionData(request, token)
    return token
  }

  override fun entryAsString(entry: String): String {
    return "'${entry.substring(0..5)}...'"
  }

  companion object {
    const val TOKEN_LENGTH = 30
  }
}
