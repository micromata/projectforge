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

package org.projectforge.security

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.framework.access.AccessChecker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

/**
 * For configuration of required 2FA. Uri's for which 2FA is required may be defined as coma separated regex's. Some sets of
 * uri expressions may be used by name, see org.projectforge.rest.config.ProjectForge2FAInitialization:
 * - ADMIN: WRITE:user;WRITE:group;WRITE:access;WRITE:configuration;/rs/user;/rs/group;/rs/access;/rs/system;...
 * - FINANCE: WRITE:employeeSalary;/rs/accountingRecord;/rs/datevRecordImport;/rs/account;/rs/cost1;/rs/incomingInvoice;...
 * - ORGA: WRITE:incomingMail;WRITE:outgoingMail;WRITE:contract;/rs/incomingMail;/rs/outgoingMail;/rs/contract;...
 * - SCRIPTING: WRITE:script;/rs/script;...
 * - MY_ACCOUNT: /rs/myAccount;...
 * - PASSWORD: /rs/changePassword;/rs/changeWlanPassword
 *
 * You may also use WRITE:<entity-name> for specifying all write access calls (Rest) of an entity.
 *
 * Examples:
 * - 'admin;/rs/employee;WRITE:user': Means All uri's for admin's and uri /rs/employee.
 * - '/': Means all uri's matches.
 *
 * Second factors may be defined with different periods of expiry. If an uri matches multiples periods; the shortest period is used.
 */
@Configuration
open class My2FARequestConfiguration {
  @Autowired
  lateinit var accessChecker: AccessChecker

  /**
   * If given, an e-mail as 2nd factor isn't allowed / provided for users of these groups (coma separated). Recommended is PF_Admin
   */
  @Value("\${projectforge.2fa.disableMail2FAForGroups}")
  var disableEmail2FAForGroups: String? = null
    private set // internal for test cases

  /**
   * If given, 2FA is required at least after given days, if stay-logged-in functionality is used. Without
   * stay-logged-in 2FA is required on every log-in.
   */
  @Value("\${projectforge.2fa.loginExpiryDays}")
  var loginExpiryDays: Int? = null
    internal set // internal for test cases

  /**
   * Maximum age in minutes of an SSO session (OIDC login). If given, a login via SSO is treated as a second factor
   * done this long ago (worst case: the shared SSO session is just about to expire). Periods of
   * `projectforge.2fa.expiryPeriod.*` that are longer than this are fulfilled by the SSO login, equal or shorter ones
   * demand a second factor of ProjectForge. If not given (or 0), SSO is only the login and a second factor of
   * ProjectForge is always required.
   */
  @Value("\${projectforge.2fa.ssoSessionMaxAgeMinutes:}")
  var ssoSessionMaxAgeMinutes: Int? = null
    internal set // internal for test cases

  /**
   * Warns about values of [ssoSessionMaxAgeMinutes] that most probably aren't meant.
   */
  @PostConstruct
  private fun checkSsoSessionMaxAge() {
    val minutes = ssoSessionMaxAgeMinutes ?: return
    if (minutes < 0) {
      log.warn { "projectforge.2fa.ssoSessionMaxAgeMinutes=$minutes is negative and ignored: SSO is only the login." }
    } else if (minutes in 1..10) {
      log.warn {
        "projectforge.2fa.ssoSessionMaxAgeMinutes=$minutes is very short: a SSO login may then authorize the mobile app " +
            "without a second factor of its own (needs one of at most 10 minutes)."
      }
    } else if (minutes > 7 * 24 * 60) {
      log.warn { "projectforge.2fa.ssoSessionMaxAgeMinutes=$minutes is longer than 7 days: is the SSO session really that long?" }
    }
  }

  /**
   * The time (millis) to take as `lastSuccessful2FA` of a user just logged in via SSO, see [ssoSessionMaxAgeMinutes].
   * @return null, if SSO doesn't count as second factor (not configured, or [username] is exempt anyway).
   */
  fun getLastSuccessful2FAAfterSsoLogin(username: String?, now: Long = System.currentTimeMillis()): Long? {
    val minutes = ssoSessionMaxAgeMinutes?.takeIf { it > 0 } ?: return null
    if (isExempt(username)) {
      return null
    }
    return now - minutes * 60_000L
  }

  /**
   * Login names (comma separated, case insensitive) that need no second factor, neither after login nor for
   * registering the mobile app. Only meant for the account Apple's app review logs in with: it can't read a mail or
   * an authenticator app. Empty by default; remove the entry after the review.
   */
  @Value("\${projectforge.2fa.exemptUsers:}")
  var exemptUsers: String = ""
    internal set // internal for test cases

  /**
   * Exempt users are a special case of one instance (the app review), never meant for a productive one: hard to miss
   * in the log of every start.
   */
  @PostConstruct
  private fun logExemptUsers() {
    val usernames = exemptUsers.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (usernames.isEmpty()) {
      return
    }
    EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.VERY_IMPORTANT, EmphasizedLogSupport.Alignment.LEFT)
      .setLogLevel(EmphasizedLogSupport.LogLevel.WARN)
      .log("projectforge.2fa.exemptUsers is set: these users log in WITHOUT a second factor:")
      .log("  ${usernames.joinToString(", ")}")
      .log("Only meant for the app review (e.g. Apple's), never for a productive instance.")
      .log("Remove the entry from projectforge.properties after the review.")
      .logEnd()
  }

  /**
   * @return true, if [username] is listed in [exemptUsers].
   */
  fun isExempt(username: String?): Boolean {
    if (username.isNullOrBlank()) {
      return false
    }
    return exemptUsers.split(',').any { it.isNotBlank() && it.trim().equals(username, ignoreCase = true) }
  }

  /**
   * List of regular expressions. For matching uri's a 2nd factor is mandatory and the 2nd factor expires after 1 minute.
   * Empty means (no 2nd factor).
   */
  @Value("\${projectforge.2fa.expiryPeriod.minutes1}")
  var expiryPeriodMinutes1: String? = null
    internal set // internal for test cases

  /**
   * List of regular expressions. For matching uri's a 2nd factor is mandatory.
   * Empty means (no 2nd factor required at all).
   */
  @Value("\${projectforge.2fa.expiryPeriod.minutes10}")
  var expiryPeriodMinutes10: String? = null
    internal set // internal for test cases

  /**
   * List of regular expressions. For matching uri's a 2nd factor is mandatory.
   * Empty means (no 2nd factor required at all).
   */
  @Value("\${projectforge.2fa.expiryPeriod.hours1}")
  var expiryPeriodHours1: String? = null
    internal set // internal for test cases

  /**
   * List of regular expressions. For matching uri's a 2nd factor is mandatory.
   * Empty means (no 2nd factor required at all).
   */
  @Value("\${projectforge.2fa.expiryPeriod.hours8}")
  var expiryPeriodHours8: String? = null
    internal set // internal for test cases

  /**
   * List of regular expressions. For matching uri's a 2nd factor is mandatory.
   * Empty means (no 2nd factor required at all).
   */
  @Value("\${projectforge.2fa.expiryPeriod.days30}")
  var expiryPeriodDays30: String? = null
    internal set // internal for test cases

  /**
   * List of regular expressions. For matching uri's a 2nd factor is mandatory.
   * Empty means (no 2nd factor required at all).
   */
  @Value("\${projectforge.2fa.expiryPeriod.days90}")
  var expiryPeriodDays90: String? = null
    internal set // internal for test cases

  /**
   * For 2FA via mailing OTP, you should configure, that in addition to the OTP the login
   * password is required for security reasons.
   * Empty -> no password, All -> for every user, Admin -> only for admin users.
   */
  //@Value("\${projectforge.2fa.loginPasswordRequired4Mail2FA}")
  //var loginPasswordRequired4Mail2FA: String? = null
  //  internal set // internal for test cases

  /**
   * Checks, if the login password is required for the logged-in user on OTP check (if configured).
   * @see loginPasswordRequired4Mail2FA
   */
  fun checkLoginPasswordRequired4Mail2FA(): Boolean {
    return false
    /*val value = loginPasswordRequired4Mail2FA?.lowercase() ?: return false
    return when (value) {
      "admin" -> accessChecker.isLoggedInUserMemberOfAdminGroup
      "" -> false
      else -> true
    }*/
  }

  fun internalSet4TestCases(
    expiryPeriodMinutes1: String? = null,
    expiryPeriodMinutes10: String? = null,
    expiryPeriodHours1: String? = null,
    expiryPeriodHours8: String? = null,
  ) {
    this.expiryPeriodMinutes1 = expiryPeriodMinutes1
    this.expiryPeriodMinutes10 = expiryPeriodMinutes10
    this.expiryPeriodHours1 = expiryPeriodHours1
    this.expiryPeriodHours8 = expiryPeriodHours8

  }

  /** For test cases of other modules, see [exemptUsers]. */
  fun internalSetExemptUsers4TestCases(usernames: String) {
    exemptUsers = usernames
  }

  /** For test cases of other modules, see [loginExpiryDays]. */
  fun internalSetLoginExpiryDays4TestCases(days: Int?) {
    loginExpiryDays = days
  }

  /** For test cases of other modules, see [ssoSessionMaxAgeMinutes]. */
  fun internalSetSsoSessionMaxAgeMinutes4TestCases(minutes: Int?) {
    ssoSessionMaxAgeMinutes = minutes
  }
}
