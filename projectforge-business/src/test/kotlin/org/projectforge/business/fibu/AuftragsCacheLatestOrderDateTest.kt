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


package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class AuftragsCacheLatestOrderDateTest {
    @Test
    fun `youngest of the order dates, including the period of performance of the positions`() {
        val order = order(1, projektId = 10, angebotsDatum = LocalDate.of(2025, 3, 1)).also {
            it.entscheidungsDatum = LocalDate.of(2025, 4, 1)
            it.periodOfPerformanceEnd = LocalDate.of(2025, 6, 30)
        }
        assertEquals(LocalDate.of(2025, 6, 30), AuftragsCache.latestOrderDate(order, null))
        val positions = listOf(
            position(LocalDate.of(2025, 9, 30)),
            position(LocalDate.of(2026, 1, 31), deleted = true),
        )
        assertEquals(LocalDate.of(2025, 9, 30), AuftragsCache.latestOrderDate(order, positions))
    }

    @Test
    fun `creation date only if no other date is given`() {
        val created = Date.from(LocalDate.of(2024, 5, 15).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant())
        val order = order(1, projektId = 10).also { it.created = created }
        assertEquals(LocalDate.of(2024, 5, 15), AuftragsCache.latestOrderDate(order, null))
        assertNull(AuftragsCache.latestOrderDate(order(2, projektId = 10), null))
    }

    @Test
    fun `max per project, ignoring deleted and replaced orders`() {
        val orders = listOf(
            order(1, projektId = 10, angebotsDatum = LocalDate.of(2025, 1, 1)),
            order(2, projektId = 10, angebotsDatum = LocalDate.of(2025, 2, 1)),
            order(3, projektId = 10, angebotsDatum = LocalDate.of(2026, 1, 1)).also { it.deleted = true },
            order(4, projektId = 10, angebotsDatum = LocalDate.of(2026, 2, 1)).also {
                it.status = AuftragsStatus.ERSETZT
            },
            order(5, projektId = 20, angebotsDatum = LocalDate.of(2024, 1, 1)),
            order(6, projektId = null, angebotsDatum = LocalDate.of(2026, 3, 1)),
        )
        val positions = mapOf(5L to listOf(position(LocalDate.of(2025, 12, 31))))
        val result = AuftragsCache.buildLatestOrderDateByProjektId(orders, positions)
        assertEquals(LocalDate.of(2025, 2, 1), result[10])
        assertEquals(LocalDate.of(2025, 12, 31), result[20])
        assertFalse(result.containsKey(30))
        assertEquals(2, result.size)
    }

    private fun order(id: Long, projektId: Long?, angebotsDatum: LocalDate? = null) = OrderInfo().also {
        it.id = id
        it.projektId = projektId
        it.angebotsDatum = angebotsDatum
        it.status = AuftragsStatus.BEAUFTRAGT
    }

    private fun position(periodOfPerformanceEnd: LocalDate, deleted: Boolean = false) = OrderPositionInfo().also {
        it.periodOfPerformanceEnd = periodOfPerformanceEnd
        it.deleted = deleted
    }
}
