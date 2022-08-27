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

package org.projectforge.security

import mu.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.time.TimeUnit
import org.projectforge.security.webauthn.WebAuthnSupport
import org.projectforge.sms.SmsSenderConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

@Service
open class My2FAService {
  @Autowired
  private lateinit var authenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var groupService: GroupService

  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var smsSenderConfig: SmsSenderConfig

  @Autowired
  private lateinit var bruteForceProtection: My2FABruteForceProtection

  @Autowired
  private lateinit var my2FARequestConfiguration: My2FARequestConfiguration

  @Autowired
  private lateinit var webAuthnSupport: WebAuthnSupport

  val smsConfigured
    get() = smsSenderConfig.isSmsConfigured() || SystemStatus.isDevelopmentMode()

  private var mail2FADisabledGroupIds: MutableList<Int>? = null

  enum class Unit { MINUTES, HOURS, DAYS }

  @PostConstruct
  private fun init() {
    setDisabled(my2FARequestConfiguration.disableEmail2FAForGroups)
  }

  /**
   * User's are requested to configure at least one 2FA.
   * @return false if user is requested to configure at least one 2FA and true, if the user has configured 2FA.
   */
  val userConfigured2FA: Boolean
    get() {
      ThreadLocalUserContext.user?.let { user ->
        if (!authenticationsService.getAuthenticatorToken().isNullOrBlank()) {
          return true
        }
        if (smsConfigured && !user.mobilePhone.isNullOrBlank()) {
          return true
        }
        if (webAuthnSupport.isAvailableForUser(user.id)) {
          return true
        }
      }
      return false
    }

  internal fun setDisabled(groupNames: String?) {
    if (groupNames.isNullOrBlank()) {
      mail2FADisabledGroupIds = null
      return
    }
    val foundGroupNames = mutableListOf<String>()
    val list = mutableListOf<Int>()
    groupNames.split(";,:").forEach { groupName ->
      if (groupName.isNotBlank()) {
        val allGroups = groupService.allGroups
        val group = allGroups.find { it.name == groupName }
        if (group == null) {
          log.error { "Group with name '$groupName' not foud in data base. Will be ignored for parameter projectforge.2fa.disableMail2FAForGroups=${my2FARequestConfiguration.disableEmail2FAForGroups}" }
        } else {
          list.add(group.id)
          foundGroupNames.add(group.name ?: "???")
        }
      }
      mail2FADisabledGroupIds = list
    }
    log.info("2FA by e-mail is disabled for users of group(s): ${foundGroupNames.joinToString()} ")
  }

  /**
   * Does also Brute force protection. Does also a OTP check against Authenticator-App-Token, if exist.
   * @param code 6 digits code entered by the use.
   * @param expectedTokens Token sent by the system (sms or mail), must match the code.
   * @return result code. (SUCCESS, if any of the given expectedToken or Authenticator-App-Token matches.
   */
  fun validateOTP(code: String, vararg expectedToken: String?): OTPCheckResult {
    preCheck(code)?.let { result -> // User is blocked.
      return result
    }
    if (code.isBlank()) {
      return OTPCheckResult.CODE_EMPTY
    }
    var failed = false
    expectedToken.forEach { token ->
      if (!token.isNullOrBlank()) {
        if (token == code) {
          bruteForceProtection.registerOTPSuccess()
          log.info { "Successful OTP check (via SMS or e-mail)." }
          ThreadLocalUserContext.userContext!!.updateLastSuccessful2FA()
          return OTPCheckResult.SUCCESS
        } else {
          failed = true
        }
      }
    }
    val result = _validateAuthenticatorOTP(code, true)
    if (result == OTPCheckResult.FAILED || result == OTPCheckResult.NOT_CONFIGURED && failed) {
      // At least one code check failed:
      bruteForceProtection.registerOTPFailure()
    }
    return result
  }

  /**
   * Does also Brute force protection.
   * @param code 6 digits code displayed by the logged-in users Authenticator app.
   * @param suppressNoTokenWarnings If true, no warnings will be shown, if authenticator token isn't available for the logged-in user.
   * @return result code (BLOCKED with message, FAILED, NOT_CONFIGURED or SUCCESS)
   */
  fun validateAuthenticatorOTP(code: String, suppressNoTokenWarnings: Boolean = false): OTPCheckResult {
    val result = _validateAuthenticatorOTP(code, suppressNoTokenWarnings)
    if (result == OTPCheckResult.FAILED) {
      bruteForceProtection.registerOTPFailure()
    }
    return result
  }

  /**
   * Does not increase the Brute force counter.
   * @param code 6 digits code displayed by the logged-in users Authenticator app.
   * @param suppressNoTokenWarnings If true, no warnings will be shown, if authenticator token isn't available for the logged-in user.
   * @return result code (BLOCKED with message, FAILED, NOT_CONFIGURED or SUCCESS)
   */
  private fun _validateAuthenticatorOTP(code: String, suppressNoTokenWarnings: Boolean = false): OTPCheckResult {
    preCheck(code)?.let { result -> // User is blocked.
      return result
    }
    val authenticatorToken = userAuthenticationsService.getAuthenticatorToken()
    if (authenticatorToken == null) {
      if (!suppressNoTokenWarnings) {
        log.warn { "Can't check OTP for user '${ThreadLocalUserContext.user?.username}', no authenticator token configured." }
      }
      return OTPCheckResult.NOT_CONFIGURED
    }
    if (code.isBlank()) {
      return OTPCheckResult.CODE_EMPTY
    }
    if (!TimeBased2FA.standard.validate(authenticatorToken, code)) {
      SecurityLogging.logSecurityWarn(this::class.java, "2FA WRONG CODE", "The entered 2FA code was wrong.")
      return OTPCheckResult.FAILED
    }
    log.info { "Successful OTP check (via authenticator app)." }
    // Update last
    ThreadLocalUserContext.userContext!!.updateLastSuccessful2FA()
    bruteForceProtection.registerOTPSuccess()
    return OTPCheckResult.SUCCESS
  }

  private fun preCheck(code: String): OTPCheckResult? {
    if (code.length < 4) {
      return OTPCheckResult.FAILED // Code can't match. Brute force protection not required.
    }
    bruteForceProtection.getBlockedResult()?.let { result -> // User is blocked.
      return result
    }
    return null
  }

  /**
   * Checks if the last successful 2FA of the logged-in user isn't older than the given time period.
   * @return True, if a successful 2FA was done in the specified time period or false, if not.
   */
  @JvmOverloads
  fun checklastSuccessful2FA(
    timePeriod: Long,
    unit: Unit,
    user: UserContext? = ThreadLocalUserContext.userContext
  ): Boolean {
    val lastSuccessful2FA = user?.lastSuccessful2FA ?: return false
    val timeAgo = when (unit) {
      Unit.MINUTES -> timePeriod * TimeUnit.MINUTE.millis
      Unit.HOURS -> timePeriod * TimeUnit.HOUR.millis
      Unit.DAYS -> timePeriod * TimeUnit.DAY.millis
    }
    return System.currentTimeMillis() - timeAgo < lastSuccessful2FA
  }

  /**
   * @return true if the logged in user is member of any group configured via [My2FARequestConfiguration.disableEmail2FAForGroups].
   */
  fun isMail2FADisabledForUser(userContext: UserContext? = null): Boolean {
    val disabledGroupIds = mail2FADisabledGroupIds
    if (disabledGroupIds.isNullOrEmpty()) {
      return false
    }
    val user = userContext?.user ?: ThreadLocalUserContext.user
    requireNotNull(user)
    val userGroups =
      UserGroupCache.getInstance().getUserGroups(user) ?: return false // User without groups shouldn't occur.
    return userGroups.intersect(disabledGroupIds)
      .isNotEmpty() // return true, if at least one disabledGroupIds is part of user group id's.
  }

  companion object {
    fun getLastSuccessful2FAAsTimeAgo(): String? {
      return ThreadLocalUserContext.userContext?.lastSuccessful2FA?.let {
        TimeAgo.getMessage(Date(it), maxUnit = TimeUnit.DAY)
      }
    }
  }
}
