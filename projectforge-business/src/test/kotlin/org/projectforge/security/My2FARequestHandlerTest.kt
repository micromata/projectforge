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
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.model.rest.RestPaths

class My2FARequestHandlerTest {
  @Test
  fun matchesTest() {
    var handler = getHandler("PASSWORD", "", "ADMIN; MY_ACCOUNT", "", "HR;FINANCE;ORGA;SCRIPTING", "/")
    Assertions.assertEquals(AbstractCache.TICKS_PER_MINUTE, handler.matchesUri("/react/changePassword")?.expiryMillis)
    Assertions.assertEquals(
      AbstractCache.TICKS_PER_MINUTE,
      handler.matchesUri("/react/changeWlanPassword")?.expiryMillis
    )
    Assertions.assertEquals(AbstractCache.TICKS_PER_HOUR, handler.matchesUri("/wa/userEdit")?.expiryMillis)
    Assertions.assertEquals(AbstractCache.TICKS_PER_HOUR, handler.matchesUri("/wa/userEdit/124")?.expiryMillis)
    Assertions.assertEquals(AbstractCache.TICKS_PER_HOUR, handler.matchesUri("/wa/userEdit/125")?.expiryMillis)
    Assertions.assertEquals(
      AbstractCache.TICKS_PER_HOUR,
      handler.matchesUri("/rs/user/${RestPaths.SAVE}")?.expiryMillis
    )
    Assertions.assertEquals(AbstractCache.TICKS_PER_HOUR, handler.matchesUri("/react/myAccount")?.expiryMillis)
    Assertions.assertEquals(AbstractCache.TICKS_PER_DAY * 30, handler.matchesUri("/wa/outgoingInvoice")?.expiryMillis)
    Assertions.assertEquals(AbstractCache.TICKS_PER_DAY * 90, handler.matchesUri("/unknown-url")?.expiryMillis)
    handler = getHandler("PASSWORD; ACCESS", "", "ADMIN;MY_ACCOUNT")
    Assertions.assertNull(handler.matchesUri("/unknown-url")?.expiryMillis)
  }

  @Test
  fun getReducedPathsTest() {
    Assertions.assertArrayEquals(arrayOf(""), My2FARequestHandler.getParentPaths("").toTypedArray())
    Assertions.assertArrayEquals(arrayOf("/"), My2FARequestHandler.getParentPaths("/").toTypedArray())
    Assertions.assertArrayEquals(arrayOf("/", "//"), My2FARequestHandler.getParentPaths("//").toTypedArray())
    Assertions.assertArrayEquals(arrayOf("/rs"), My2FARequestHandler.getParentPaths("/rs").toTypedArray())
    Assertions.assertArrayEquals(
      arrayOf("/rs", "/rs/"),
      My2FARequestHandler.getParentPaths("/rs/").toTypedArray()
    )
    Assertions.assertArrayEquals(
      arrayOf("/rs", "/rs/user"),
      My2FARequestHandler.getParentPaths("/rs/user").toTypedArray()
    )
    Assertions.assertArrayEquals(
      arrayOf("/rs", "/rs/user", "/rs/user/save"),
      My2FARequestHandler.getParentPaths("/rs/user/save").toTypedArray()
    )
    Assertions.assertArrayEquals(
      arrayOf("/rs", "/rs/", "/rs//user", "/rs//user/save"),
      My2FARequestHandler.getParentPaths("/rs//user/save").toTypedArray()
    )
  }

  @Test
  fun remainingPeriodTest() {
    val handler = getHandler("PASSWORD", "", "ADMIN; MY_ACCOUNT", "", "HR;FINANCE;ORGA;SCRIPTING", "/")
    val user = PFUserDO()
    ThreadLocalUserContext.setUser(user)
    Assertions.assertEquals(0L, handler.getRemainingPeriod4WriteAccess("user"))
    ThreadLocalUserContext.getUserContext().updateLastSuccessful2FA()
    checkRemainingPeriod(handler.getRemainingPeriod4WriteAccess("user"), 1)
    checkRemainingPeriod(handler.getRemainingPeriod("/wa/incomingInvoice"), 720)
    Assertions.assertNull(handler.getRemainingPeriod4WriteAccess("timesheet"))
  }

  companion object {
    internal fun checkRemainingPeriod(remainingPeriod: Long?, hours: Long) {
      val ms = hours * 3600 * 1000
      val epsilon = 10000 // Tolerance (accepting 10s of test duration)
      Assertions.assertNotNull(remainingPeriod)
      Assertions.assertTrue(
        remainingPeriod!! > ms - epsilon,
        "Remaining period should almost $hours hours ($ms ms). Tolerance for test is 10 seconds (>${ms - epsilon}). Actual value: $remainingPeriod"
      ) //
      Assertions.assertTrue(
        remainingPeriod <= ms,
        "Remaining period should be less than $hours hours ($ms ms). Actual value: $remainingPeriod"
      ) //
    }

    internal fun getHandler(
      minutes1: String?,
      minutes10: String?,
      hours1: String? = null,
      hours8: String? = null,
      days30: String? = null,
      days90: String? = null
    ): My2FARequestHandler {
      val config = My2FARequestConfiguration()
      config.expiryPeriodMinutes1 = minutes1
      config.expiryPeriodMinutes10 = minutes10
      config.expiryPeriodHours1 = hours1
      config.expiryPeriodHours8 = hours8
      config.expiryPeriodDays30 = days30
      config.expiryPeriodDays90 = days90
      val handler = My2FARequestHandler()
      handler.configuration = config
      handler.postConstruct()
      return handler
    }
  }
}
