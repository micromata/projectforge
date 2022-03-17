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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.user.UserDao
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.user.entities.PFUserDO

class My2FABruteForceProtectionTest {
  @Test
  fun timePenaltiesTest() {
    val protection = initProtection()
    val userDao = protection.userDao
    val user = PFUserDO()
    user.id = 42
    user.username = "kai"
    Mockito.`when`(userDao.internalGetOrLoad(42)).thenAnswer {
      return@thenAnswer user
    }

    Assertions.assertTrue(protection.isRetryAllowed(42))
    Assertions.assertNull(protection.getLastFailedTry(42))
    Assertions.assertFalse(user.deactivated)
    for (i in 0..11) {
      if (i in arrayOf(0, 1, 2, 4, 5, 7, 8, 10, 11)) {
        Assertions.assertTrue(protection.isRetryAllowed(42), "Retry should be allowed for try #$i.")
      } else {
        Assertions.assertFalse(protection.isRetryAllowed(42), "Retry shouldn't be allowed for try #$i.")
      }
      if (i > 0) {
        Assertions.assertTrue(
          System.currentTimeMillis() - protection.getLastFailedTry(42)!! < 10000,
          "Last failed retry shouldn't be older than seconds for try #i."
        )
      } else {
        Assertions.assertNull(protection.getLastFailedTry(42), "No last failure millis expected.")
      }
      protection.registerOTPFailure(42)
    }
    Mockito.verify(userDao, Mockito.times(1)).internalGetOrLoad(42)
    Mockito.verify(userDao, Mockito.times(1)).internalUpdate(user)
    Assertions.assertTrue(user.deactivated) // User should now be deactivated.
    // Simulate activation of user by an admin:
    user.deactivated = false
    Assertions.assertEquals(12, protection.getNumberOfFailures(42), "12 failed tries expected.")
    Assertions.assertFalse(protection.isRetryAllowed(42), "Retry shouldn't be allowed after 12 failed tries.")
    protection.userChangeListener.afterSaveOrModifify(user, OperationType.UPDATE)
    Assertions.assertNull(protection.getLastFailedTry(42), "No last failure millis expected, user should be cleared.")
    Assertions.assertEquals(0, protection.getNumberOfFailures(42), "No failed tries expected, user should be cleared.")
    Assertions.assertTrue(protection.isRetryAllowed(42), "Retry should be allowed, user is cleared.")
  }

  @Test
  fun waitingMillisTest() {
    val protection = My2FABruteForceProtection()
    arrayOf(0, 1, 2, 4, 5, 7, 8, 10, 11).forEach { counter ->
      Assertions.assertEquals(0L, protection.getWaitingMillis(counter), "Expected offset 0 for counter=$counter")
    }
    arrayOf(3, 6, 9).forEach { counter ->
      Assertions.assertEquals(
        AbstractCache.TICKS_PER_HOUR,
        protection.getWaitingMillis(counter),
        "Expected offset of ${AbstractCache.TICKS_PER_HOUR} for counter=$counter"
      )
    }
    arrayOf(12, 13).forEach { counter ->
      Assertions.assertEquals(
        Long.MAX_VALUE,
        protection.getWaitingMillis(counter),
        "Expected offset of ${Long.MAX_VALUE} for counter=$counter"
      )
    }
  }

  private fun initProtection(): My2FABruteForceProtection {
    val protection = My2FABruteForceProtection()
    protection.userDao = Mockito.mock(UserDao::class.java)
    protection.initialize()
    return protection
  }
}
