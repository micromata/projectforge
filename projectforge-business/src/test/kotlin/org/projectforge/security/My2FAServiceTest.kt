/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
