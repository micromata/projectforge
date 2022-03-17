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
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletResponse

class My2FABruteForceProtectionTest {
  @Test
  fun bruteForceTest() {
    val userDao = Mockito.mock(UserDao::class.java)
    val protection = My2FABruteForceProtection()
    protection.getWaitingMillis(0)
    Mockito.`when`(userDao.internalGetOrLoad(42)).thenAnswer {
      println("yeah")
    }
    Mockito.verify(userDao, Mockito.times(1)).internalGetOrLoad(0)
  }

  @Test
  fun timePenaltiesTest() {
    val protection = My2FABruteForceProtection()
    arrayOf(0, 1, 2, 4, 5, 7, 8, 10, 11).forEach { counter ->
      Assertions.assertEquals(0L, protection.getWaitingMillis(counter), "Expected offset 0 for counter=$counter")
    }
    arrayOf(3, 6, 9).forEach { counter ->
      Assertions.assertEquals(AbstractCache.TICKS_PER_HOUR, protection.getWaitingMillis(counter), "Expected offset of ${AbstractCache.TICKS_PER_HOUR} for counter=$counter")
    }
    arrayOf(12, 13).forEach { counter ->
      Assertions.assertEquals(Long.MAX_VALUE, protection.getWaitingMillis(counter), "Expected offset of ${Long.MAX_VALUE} for counter=$counter")
    }
  }
}
