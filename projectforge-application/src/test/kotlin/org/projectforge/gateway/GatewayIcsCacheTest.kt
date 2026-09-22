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

package org.projectforge.gateway

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.gateway.sync.GatewayIcsCache

class GatewayIcsCacheTest {

    @Test
    fun putAndGet() {
        val cache = GatewayIcsCache()
        cache.put(42L, "encryptedQ1", "BEGIN:VCALENDAR\nEND:VCALENDAR")
        assertEquals("BEGIN:VCALENDAR\nEND:VCALENDAR", cache.get(42L, "encryptedQ1"))
        assertEquals(1, cache.size())
    }

    @Test
    fun overwriteExistingEntry() {
        val cache = GatewayIcsCache()
        cache.put(42L, "q1", "old")
        cache.put(42L, "q1", "new")
        assertEquals("new", cache.get(42L, "q1"))
        assertEquals(1, cache.size())
    }

    @Test
    fun differentUsersAndQueries() {
        val cache = GatewayIcsCache()
        cache.put(1L, "q1", "ics-1-q1")
        cache.put(1L, "q2", "ics-1-q2")
        cache.put(2L, "q1", "ics-2-q1")

        assertEquals("ics-1-q1", cache.get(1L, "q1"))
        assertEquals("ics-1-q2", cache.get(1L, "q2"))
        assertEquals("ics-2-q1", cache.get(2L, "q1"))
        assertEquals(3, cache.size())
    }

    @Test
    fun getMissingReturnsNull() {
        val cache = GatewayIcsCache()
        assertNull(cache.get(99L, "nonexistent"))
    }

    @Test
    fun clearRemovesAll() {
        val cache = GatewayIcsCache()
        cache.put(1L, "q1", "data")
        cache.put(2L, "q2", "data")
        cache.clear()
        assertEquals(0, cache.size())
        assertNull(cache.get(1L, "q1"))
    }
}
