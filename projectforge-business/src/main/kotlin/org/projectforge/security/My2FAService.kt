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
import org.projectforge.SystemStatus
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.TimeUnit
import org.projectforge.sms.SmsSenderConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger {}

@Service
open class My2FAService {
  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var smsSenderConfig: SmsSenderConfig

  val smsConfigured
    get() = smsSenderConfig.isSmsConfigured() || SystemStatus.isDevelopmentMode()

  enum class Unit { MINUTES, HOURS, DAYS }

  /**
   * @param 6 digits code displayed by the logged-in users Authenticator app.
   * @param suppressNoTokenWarnings If true, no warnings will be shown, if authenticator token isn't available for the logged-in user.
   * @return error message or "OK" if code was successfully validated.
   */
  fun validateOTP(code: String, suppressNoTokenWarnings: Boolean = false): String {
    val authenticatorToken = userAuthenticationsService.getAuthenticatorToken()
    if (authenticatorToken == null) {
      if (!suppressNoTokenWarnings) {
        log.warn { "Can't check OTP for user '${ThreadLocalUserContext.getUser()?.username}', no authenticator token configured." }
      }
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

  /**
   * Checks if the last successful 2FA of the logged-in user isn't older than the given time period.
   * @return True, if a successful 2FA was done in the specified time period or false, if not.
   */
  fun checklastSuccessful2FA(timePeriod: Long, unit: Unit): Boolean {
    val user = ThreadLocalUserContext.getUserContext()
    val lastSuccessful2FA = user?.lastSuccessful2FA ?: return false
    val timeAgo = when (unit) {
      Unit.MINUTES -> timePeriod * TimeUnit.MINUTE.millis
      Unit.HOURS -> timePeriod * TimeUnit.HOUR.millis
      Unit.DAYS -> timePeriod * TimeUnit.DAY.millis
    }
    return System.currentTimeMillis() - timeAgo < lastSuccessful2FA
  }

  companion object {
    fun getLastSuccessful2FAAsTimeAgo(): String? {
      return ThreadLocalUserContext.getUserContext()?.lastSuccessful2FA?.let {
        TimeAgo.getMessage(Date(it), maxUnit = TimeUnit.DAY)
      }
    }

    const val ERROR_2FA_NOT_CONFIGURED = "2FA not configured."
    const val ERROR_2FA_WRONG_CODE = "Wrong code."
    const val SUCCESS = "OK"
  }
}
