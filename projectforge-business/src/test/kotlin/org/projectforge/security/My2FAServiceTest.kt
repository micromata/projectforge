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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.TimeUnit
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class My2FAServiceTest : AbstractTestBase() {
  @Autowired
  private lateinit var my2FAService: My2FAService

  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  @Test
  fun matchesTest() {
    logon(TEST_USER)
    userAuthenticationsService.clearAuthenticatorToken()
    Assertions.assertNull(userAuthenticationsService.getAuthenticatorToken())
    Assertions.assertEquals(
      OTPCheckResult.NOT_CONFIGURED,
      my2FAService.validateAuthenticatorOTP("123456"),
      "User has no authenticator token, so OTP validation should fail."
    )
    userAuthenticationsService.createNewAuthenticatorToken() // Will update last successful 2FA (otherwise use will not see his 2FA settings.
    ThreadLocalUserContext.userContext!!.lastSuccessful2FA = null // So delete it for the next test.
    Assertions.assertEquals(
      OTPCheckResult.FAILED,
      my2FAService.validateAuthenticatorOTP("123456"),
      "Invalid token, so OTP validation should fail."
    )
    val handler =
      My2FARequestHandlerTest.getHandler("PASSWORD", "", "ADMIN; MY_ACCOUNT", "", "HR;FINANCE;ORGA;SCRIPT", "/")
    Assertions.assertEquals(
      0L,
      handler.getRemainingPeriod4WriteAccess("user"),
      "2FA required, but no 2FA yet done by the logged-in user."
    )
    val secretKey = userAuthenticationsService.getAuthenticatorToken()
    Assertions.assertNotNull(secretKey)
    val code = TimeBased2FA.standard.getTOTPCode(secretKey!!)
    Assertions.assertEquals(
      OTPCheckResult.SUCCESS,
      my2FAService.validateAuthenticatorOTP(code),
      "OTP validation should work."
    )
    My2FARequestHandlerTest.checkRemainingPeriod(handler.getRemainingPeriod4WriteAccess("user"), 1)
  }

  /**
   * The rule behind projectforge.2fa.loginExpiryDays: with stay-logged-in, a 2FA is required again only after the
   * configured number of days. Pinned down here, because [org.projectforge.login.LoginService.checkStayLoggedIn]
   * relies on it - a null lastSuccessful2FA (the last2FA cookie is gone) must keep demanding a second factor, it must
   * not silently pass.
   */
  @Test
  fun lastSuccessful2FAExpiryTest() {
    val userContext = UserContext(PFUserDO(), nofresh = true)
    Assertions.assertFalse(
      my2FAService.checklastSuccessful2FA(30, My2FAService.Unit.DAYS, userContext),
      "No 2FA done at all (e. g. the last2FA cookie is missing after a stay-logged-in restore): 2FA required.",
    )
    userContext.lastSuccessful2FA = System.currentTimeMillis() - 29 * TimeUnit.DAY.millis
    Assertions.assertTrue(
      my2FAService.checklastSuccessful2FA(30, My2FAService.Unit.DAYS, userContext),
      "Last 2FA is 29 days old, so it's still valid for 30 days.",
    )
    userContext.lastSuccessful2FA = System.currentTimeMillis() - 31 * TimeUnit.DAY.millis
    Assertions.assertFalse(
      my2FAService.checklastSuccessful2FA(30, My2FAService.Unit.DAYS, userContext),
      "Last 2FA is 31 days old, so a new 2FA is required.",
    )
  }

  /**
   * projectforge.2fa.ssoAmrValues: a login via SSO counts as a 2FA done at auth_time, if the amr claim says so.
   */
  @Test
  fun ssoLoginCountsAsSecondFactorTest() {
    val config = My2FARequestConfiguration()
    val now = System.currentTimeMillis()
    val authTime = now - 8 * TimeUnit.HOUR.millis
    val mfa = listOf("pwd", "otp")
    Assertions.assertNull(config.getLastSuccessful2FAAfterSsoLogin("user", mfa, authTime, now), "Not configured: SSO is only the login.")
    config.internalSetSsoAmrValues4TestCases("MFA, otp")
    config.internalSetExemptUsers4TestCases("exempt")
    Assertions.assertNull(config.getLastSuccessful2FAAfterSsoLogin("Exempt", mfa, authTime, now), "Exempt users need no 2FA anyway.")
    Assertions.assertNull(config.getLastSuccessful2FAAfterSsoLogin("user", listOf("pwd"), authTime, now), "Password only.")
    Assertions.assertNull(config.getLastSuccessful2FAAfterSsoLogin("user", null, authTime, now), "No amr.")
    Assertions.assertNull(config.getLastSuccessful2FAAfterSsoLogin("user", mfa, null, now), "No auth_time.")
    Assertions.assertEquals(authTime, config.getLastSuccessful2FAAfterSsoLogin("user", mfa, authTime, now))
    Assertions.assertEquals(now, config.getLastSuccessful2FAAfterSsoLogin("user", mfa, now + TimeUnit.HOUR.millis, now), "Future auth_time.")

    logon(TEST_USER)
    val handler = My2FARequestHandlerTest.getHandler("/min1", "/min10", "/hour1", "/hour8", "/day30")
    ThreadLocalUserContext.userContext!!.lastSuccessful2FA = config.getLastSuccessful2FAAfterSsoLogin("user", mfa, authTime, now)
    for (uri in listOf("/min1", "/min10", "/hour1", "/hour8")) {
      Assertions.assertEquals(0L, handler.getRemainingPeriod(uri), "$uri: 2FA of ProjectForge required after SSO.")
    }
    Assertions.assertTrue(
      handler.getRemainingPeriod("/day30")!! > 29 * TimeUnit.DAY.millis,
      "A period of 30 days is fulfilled by the SSO login (8 hours ago).",
    )
    ThreadLocalUserContext.userContext!!.lastSuccessful2FA = null
    Assertions.assertEquals(0L, handler.getRemainingPeriod("/day30"), "Without a counted SSO login, nothing is fulfilled.")
  }

  @Test
  fun disabledTest() {
    my2FAService.setDisabled(null)
    logon(TEST_ADMIN_USER)
    Assertions.assertFalse(my2FAService.isMail2FADisabledForUser(), "No groups disabled for 2FA via mail.")
    my2FAService.setDisabled("PF_Admin")
    Assertions.assertTrue(my2FAService.isMail2FADisabledForUser(), "Admins disabled for 2FA via mail.")
    logon(TEST_FINANCE_USER)
    Assertions.assertFalse(my2FAService.isMail2FADisabledForUser(), "Admins disabled for 2FA via mail.")
  }
}
