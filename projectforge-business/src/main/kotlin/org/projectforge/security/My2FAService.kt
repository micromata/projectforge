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

package org.projectforge.security

import mu.KotlinLogging
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.model.rest.RestPaths
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

/**
 * Is called for all requests by UserFilter and ensure valid 2FA for every request configured in [My2FARequestConfiguration].
 */
@Service
open class My2FAService {
  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  /**
   * @param 6 digits code displayed by the logged-in users Authenticator app.
   * @return error message or "OK" if code was successfully validated.
   */
  fun validateOTP(code: String): String {
    val authenticatorToken = userAuthenticationsService.getAuthenticatorToken()
    if (authenticatorToken == null) {
      log.warn { "Can't check OTP for user '${ThreadLocalUserContext.getUser()?.username}', no authenticator token configured." }
      return ERROR_2FA_NOT_CONFIGURED
    }
    if (!TimeBased2FA.standard.validate(authenticatorToken, code)) {
      SecurityLogging.logSecurityWarn(this::class.java, "2FA WRONG CODE", "The entered 2FA code was wrong.")
      return ERROR_2FA_WRONG_CODE
    }
    // Update last
    ThreadLocalUserContext.getUserContext().updateLastSuccessful2FA()
    return SUCCESS
  }

  companion object {
    const val ERROR_2FA_NOT_CONFIGURED = "2FA not configured."
    const val ERROR_2FA_WRONG_CODE = "Wrong code."
    const val SUCCESS = "OK"
  }
}
