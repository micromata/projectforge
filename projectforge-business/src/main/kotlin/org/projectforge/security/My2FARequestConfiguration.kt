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

package org.projectforge.security

import org.projectforge.framework.access.AccessChecker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

/**
 * For configuration of required 2FA. Uri's for which 2FA is required may be defined as coma separated regex's. Some sets of
 * uri expressions may be used by name, see org.projectforge.rest.config.ProjectForge2FAInitialization:
 * - ADMIN: WRITE:{user};/wa/userEdit;/wa/groupEdit;/wa/admin;/react/change.*Password;/wa/license;/wa/access;/react/system;/react/configuration;/wa/wicket/bookmarkable/org.projectforge.web.admin
 * - FINANCE: /wa/report;/wa/accounting;/wa/datev;/wa/liquidity;/react/account;/react/cost1;/react/cost2;/wa/incomingInvoice;/wa/outgoingInvoice
 * - ORGA: /wa/incomingMail;/react/outgoingMail;/wa/outgoingMail;/react/incomingMail;/wa/contractMail;/react/contract
 * - SCRIPTING: /wa/script
 * - MY_ACCOUNT: /react/tokenInfo;/react/myyAccount
 * - PASSWORD: /react/change.*Password
 *
 * You may also use WRITE:<entity-name> for specifying all write access calls (Rest) of an entity.
 *
 * Examples:
 * - 'admin;/wa/employee;WRITE:user': Means All uri's for admin's and uri /wa/employee.
 * - '/': Means all uri's matches.
 *
 * Second factors may be definied with different periods of expiry. If an uri matches multiples periods; the shortest period is used.
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
}
