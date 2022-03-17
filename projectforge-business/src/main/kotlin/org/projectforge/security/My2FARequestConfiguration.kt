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

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

/**
 * For configuration of required 2FA. Uri's for which 2FA is required may be definied as coma separated regex's. Some sets of
 * uri expressions may be used by name:
 * - ADMIN: WRITE:{user};/wa/userEdit;/wa/groupEdit;/wa/admin;/react/change.*Password;/wa/license;/wa/access;/react/logViewer/-1;/react/system;/react/configuration;/wa/wicket/bookmarkable/org.projectforge.web.admin
 * - HR: /wa/employee;/wa/wicket/bookmarkable/org.projectforge.plugins.eed
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

  companion object {
    internal val shortCuts =
      mutableMapOf(
        "ADMIN" to
            "WRITE:user;WRITE:group;/wa/userEdit;/wa/groupEdit;/wa/admin;/react/change.*Password;/wa/license;/wa/access;/react/logViewer/-1;/react/system;/react/configuration;/wa/wicket/bookmarkable/org.projectforge.web.admin",
        "HR" to "WRITE:employee;/wa/employee;/wa/wicket/bookmarkable/org.projectforge.plugins.eed",
        "FINANCE" to
            "WRITE:incomingInvoice;WRITE:outgoingInvoice;/wa/report;/wa/accounting;/wa/datev;/wa/liquidity;/react/account;/react/cost1;/react/cost2;/wa/incomingInvoice;/wa/outgoingInvoice",
        "ORGA" to
            "WRITE:incomingMail;WRITE:outgoingMail;WRITE:contract;/wa/incomingMail;/react/outgoingMail;/wa/outgoingMail;/react/incomingMail;/wa/contractMail;/react/contract",
        "SCRIPT" to "/react/script",
        "MY_ACCOUNT" to "/react/tokenInfo;/react/myAccount;/rs/tokenInfo;/rs/user/renewToken",
        "PASSWORD" to "/react/change.*Password",
        "ALL" to "/",
      )
  }
}
