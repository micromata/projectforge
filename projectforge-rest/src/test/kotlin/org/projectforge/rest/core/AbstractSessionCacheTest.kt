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

package org.projectforge.rest.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.framework.utils.NumberHelper

class AbstractSessionCacheTest {
    class TestSessionCache: AbstractSessionCache<String>(expireTimeInMillis = 100, clearEntriesIntervalInMillis = TICKS_PER_DAY) {
        override fun entryAsString(entry: String): String {
            return entry
        }

        public override fun refresh() {
            super.refresh()
        }
    }

    @Test
    fun cacheTest() {
        val cache = TestSessionCache()
        val sessionId1 = NumberHelper.getSecureRandomAlphanumeric(30)
        val sessionId2 = NumberHelper.getSecureRandomAlphanumeric(30)
        assertEquals(0, cache.size)
        assertEquals(0, cache.validSize)
        cache.registerSessionData(sessionId1, "Hurzel")
        cache.registerSessionData(sessionId2, "2")
        assertEquals("Hurzel", cache.getSessionData(sessionId1))
        assertEquals("2", cache.getSessionData(sessionId2))
        assertEquals(2, cache.size)
        assertEquals(2, cache.validSize)
        for (counter in 0..5) {
            Thread.sleep(50)
            assertEquals("Hurzel", cache.getSessionData(sessionId1))
        }
        assertEquals(2, cache.size)
        assertEquals(1, cache.validSize,"2nd entry is expired, only 1 entry was used.")
        assertEquals("Hurzel", cache.getSessionData(sessionId1))
        assertNull(cache.getSessionData(sessionId2), "Not used since 2 seconds, is expired.")
        cache.refresh()
        assertEquals(1, cache.size, "Expired entry should be removed now.")
    }
}
