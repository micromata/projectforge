/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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
    var handler = getHandler("PASSWORD", "", "ADMIN; MY_ACCOUNT", "", "HR;FINANCE;ORGA;SCRIPT", "/")
    Assertions.assertEquals(AbstractCache.TICKS_PER_MINUTE, handler.matchesUri("/rs/changePassword")?.expiryMillis)
    Assertions.assertEquals(
      AbstractCache.TICKS_PER_MINUTE,
      handler.matchesUri("/rs/changeWlanPassword")?.expiryMillis
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
    handler = getHandler("PASSWORD;", "", "ADMIN;MY_ACCOUNT")
    Assertions.assertNull(handler.matchesUri("/unknown-url")?.expiryMillis)

    Assertions.assertNull(handler.matchesUri("/rs/user/autosearch"))
    Assertions.assertNull(handler.matchesUri("/rs/user/autosearch?search=hurzel"))
  }

  @Test
  fun getReducedPathsTest() {
    Assertions.assertArrayEquals(arrayOf(""), My2FARequestHandler.getParentPaths("").toTypedArray())
    Assertions.assertArrayEquals(arrayOf("/"), My2FARequestHandler.getParentPaths("/").toTypedArray())
    Assertions.assertArrayEquals(arrayOf("//", "/"), My2FARequestHandler.getParentPaths("//").toTypedArray())
    Assertions.assertArrayEquals(arrayOf("/rs"), My2FARequestHandler.getParentPaths("/rs").toTypedArray())
    Assertions.assertArrayEquals(
      arrayOf("/rs/", "/rs"),
      My2FARequestHandler.getParentPaths("/rs/").toTypedArray()
    )
    Assertions.assertArrayEquals(
      arrayOf("/rs/user", "/rs"),
      My2FARequestHandler.getParentPaths("/rs/user").toTypedArray()
    )
    Assertions.assertArrayEquals(
      arrayOf("/rs/user/save", "/rs/user", "/rs"),
      My2FARequestHandler.getParentPaths("/rs/user/save").toTypedArray()
    )
    Assertions.assertArrayEquals(
      arrayOf("/rs//user/save", "/rs//user", "/rs/", "/rs"),
      My2FARequestHandler.getParentPaths("/rs//user/save").toTypedArray()
    )
  }

  @Test
  fun remainingPeriodTest() {
    val handler = getHandler("PASSWORD", "", "ADMIN; MY_ACCOUNT", "", "HR;FINANCE;ORGA;SCRIPT", "/")
    val user = PFUserDO()
    ThreadLocalUserContext.setUser(user)
    Assertions.assertEquals(0L, handler.getRemainingPeriod4WriteAccess("user"))
    ThreadLocalUserContext.userContext!!.updateLastSuccessful2FA()
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
      handler.registerShortCutValues(
        My2FAShortCut.ADMIN,
        "WRITE:user;WRITE:group;/wa/userEdit;/wa/groupEdit;/wa/admin",
        "/rs/change.*Password",
        "/rs/user",
        "/wa/license;/wa/access;/react/logViewer/-1;/react/system;/react/configuration;/wa/wicket/bookmarkable/org.projectforge.web.admin"
      )
      handler.registerShortCutValues(My2FAShortCut.HR, "WRITE:employee;/wa/employee;/wa/wicket/bookmarkable/org.projectforge.plugins.eed")
      handler.registerShortCutValues(
        My2FAShortCut.FINANCE,
        "WRITE:incomingInvoice;WRITE:outgoingInvoice;/wa/report;/wa/accounting;/wa/datev;/wa/liquidity;/react/account;/react/cost1;/react/cost2;/wa/incomingInvoice;/wa/outgoingInvoice"
      )
      handler.registerShortCutValues(
        My2FAShortCut.ORGA,
        "WRITE:incomingMail;WRITE:outgoingMail;WRITE:contract;/wa/incomingMail;/react/outgoingMail;/wa/outgoingMail;/react/incomingMail;/wa/contractMail;/react/contract"
      )
      handler.registerShortCutValues(My2FAShortCut.SCRIPT, "/react/script")
      handler.registerShortCutValues(My2FAShortCut.MY_ACCOUNT, "/react/tokenInfo;/react/myAccount;/rs/tokenInfo;/rs/user/renewToken")
      handler.registerShortCutValues(My2FAShortCut.PASSWORD, "/rs/change.*Password")
      handler.registerShortCutValues(My2FAShortCut.ADMIN, "/rs/groupAccess/")
      handler.registerShortCutValues(My2FAShortCut.ALL, "/")
      handler.internalSet4UnitTests(config)
      return handler
    }
  }
}
