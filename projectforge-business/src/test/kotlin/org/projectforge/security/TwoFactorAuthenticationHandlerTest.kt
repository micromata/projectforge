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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.model.rest.RestPaths

class TwoFactorAuthenticationHandlerTest {
  @Test
  fun matchesTest() {
    var handler = getHandler("PASSWORD", "", "ADMIN; MY_ACCOUNT", "", "HR;FINANCE;ORGA;SCRIPTING", "/")
    Assertions.assertEquals(AbstractCache.TICKS_PER_MINUTE, handler.matches("/react/changePassword")?.expiryMillis)
    Assertions.assertEquals(AbstractCache.TICKS_PER_MINUTE, handler.matches("/react/changeWlanPassword")?.expiryMillis)
    Assertions.assertEquals(AbstractCache.TICKS_PER_HOUR, handler.matches("/wa/userEdit")?.expiryMillis)
    Assertions.assertEquals(AbstractCache.TICKS_PER_HOUR, handler.matches("/rs/user/${RestPaths.SAVE}")?.expiryMillis)
    Assertions.assertEquals(AbstractCache.TICKS_PER_HOUR, handler.matches("/react/myAccount")?.expiryMillis)
    Assertions.assertEquals(AbstractCache.TICKS_PER_DAY * 30, handler.matches("/wa/outgoingInvoice")?.expiryMillis)
    Assertions.assertEquals(AbstractCache.TICKS_PER_DAY * 90, handler.matches("/unknown-url")?.expiryMillis)
    handler = getHandler("PASSWORD; ACCESS", "", "ADMIN;MY_ACCOUNT")
    Assertions.assertNull(handler.matches("/unknown-url")?.expiryMillis)
  }

  private fun getHandler(
    minutes1: String?,
    minutes10: String?,
    hours1: String? = null,
    hours8: String? = null,
    days30: String? = null,
    days90: String? = null
  ): TwoFactorAuthenticationHandler {
    val config = TwoFactorAuthenticationConfiguration()
    config.expiryPeriodMinutes1 = minutes1
    config.expiryPeriodMinutes10 = minutes10
    config.expiryPeriodHours1 = hours1
    config.expiryPeriodHours8 = hours8
    config.expiryPeriodDays30 = days30
    config.expiryPeriodDays90 = days90
    val handler = TwoFactorAuthenticationHandler()
    handler.configuration = config
    handler.postConstruct()
    return handler
  }
}
