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

package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import java.time.LocalDate
import java.util.Locale

class OrderStatisticsCacheTest {
    private var now = 0L

    private val cache = OrderStatisticsCache().also { it.clock = { now } }

    @Test
    fun `the same key is calculated once, another user gets an own calculation`() {
        var calculations = 0
        val calculate = { ++calculations }
        assertEquals(1, cache.get(key(userId = 1), calculate))
        assertEquals(1, cache.get(key(userId = 1), calculate))
        assertEquals(2, cache.get(key(userId = 2), calculate), "Never the result of another user.")
        assertEquals(3, cache.get(key(userId = 1, kind = OrderStatisticsCache.Kind.CONTRIBUTION_MARGIN), calculate))
        assertEquals(4, cache.get(key(userId = 1, startDate = LocalDate.of(2025, 1, 1)), calculate))
        assertEquals(1, cache.get(key(userId = 1), calculate))
    }

    @Test
    fun `entries expire and are cleared`() {
        var calculations = 0
        val calculate = { ++calculations }
        cache.get(key(), calculate)
        now += OrderStatisticsCache.TTL_MILLIS - 1
        assertEquals(1, cache.get(key(), calculate))
        now += 1
        assertEquals(2, cache.get(key(), calculate), "Expired.")
        cache.clear()
        assertEquals(0, cache.size)
        assertEquals(3, cache.get(key(), calculate))
    }

    @Test
    fun `a failed calculation is not cached`() {
        assertThrows<IllegalStateException> { cache.get(key()) { throw IllegalStateException("failed") } }
        assertEquals(0, cache.size)
        assertEquals("ok", cache.get(key()) { "ok" })
    }

    @Test
    fun `the least recently used entry is dropped`() {
        repeat(OrderStatisticsCache.MAX_ENTRIES + 5) { cache.get(key(userId = it.toLong())) { it } }
        assertEquals(OrderStatisticsCache.MAX_ENTRIES, cache.size)
    }

    @Test
    fun `the filter key ignores the order of the criteria and the favorite`() {
        fun filter(vararg fields: String, name: String? = null) = MagicFilter(name = name).also { filter ->
            fields.forEach { field ->
                filter.entries.add(MagicFilterEntry(field).also { it.value.values = arrayOf("b", "a") })
            }
        }
        assertEquals(
            OrderStatisticsCache.filterKey(filter("customers", "projects")),
            OrderStatisticsCache.filterKey(filter("projects", "customers", name = "Favorite")),
        )
        val sorted = filter("customers").also { it.entries[0].value.values = arrayOf("a", "b") }
        assertEquals(OrderStatisticsCache.filterKey(filter("customers")), OrderStatisticsCache.filterKey(sorted))
    }

    private fun key(
        userId: Long = 1,
        kind: OrderStatisticsCache.Kind = OrderStatisticsCache.Kind.FORECAST,
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
    ) = OrderStatisticsCache.Key(userId, Locale.GERMAN, kind, "", startDate)
}
