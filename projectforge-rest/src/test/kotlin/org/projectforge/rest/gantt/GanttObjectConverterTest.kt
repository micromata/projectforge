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

package org.projectforge.rest.gantt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.gantt.GanttObjectType
import org.projectforge.business.gantt.GanttRelationType
import java.math.BigDecimal
import java.time.LocalDate

class GanttObjectConverterTest {
    @Test
    fun `Gantt-only tree survives the round trip including its predecessors`() {
        val second = GanttObject(
            id = -3, title = "Second", duration = BigDecimal("2.5"), predecessorId = -2, predecessorOffset = 1,
            relationType = GanttRelationType.START_START, type = GanttObjectType.MILESTONE, visible = true,
        )
        val first = GanttObject(
            id = -2, title = "First", startDate = LocalDate.of(2026, 10, 1), endDate = LocalDate.of(2026, 10, 9),
            progress = 40, visible = true,
        )
        val root = GanttObject(id = -1, title = "Root", children = mutableListOf(first, second))

        val data = GanttObjectConverter.fromDTO(root)
        val back = GanttObjectConverter.toDTO(data.rootObject, taskDao = null)

        assertEquals("Root", back.title)
        assertEquals(listOf(-2L, -3L), back.children?.map { it.id })
        val backFirst = back.children!![0]
        assertEquals(LocalDate.of(2026, 10, 1), backFirst.startDate)
        assertEquals(LocalDate.of(2026, 10, 9), backFirst.endDate)
        assertEquals(40, backFirst.progress)
        assertNull(backFirst.task, "A Gantt-only object has no structure element.")
        val backSecond = back.children!![1]
        assertEquals(-2L, backSecond.predecessorId)
        assertEquals("First", backSecond.predecessorTitle)
        assertEquals(1, backSecond.predecessorOffset)
        assertEquals(0, BigDecimal("2.5").compareTo(backSecond.duration))
        assertEquals(GanttRelationType.START_START, backSecond.relationType)
        assertEquals(GanttObjectType.MILESTONE, backSecond.type)
        assertEquals(true, backSecond.visible)
        assertEquals(false, back.visible)
    }

    @Test
    fun `null root gives empty chart data`() {
        assertNull(GanttObjectConverter.fromDTO(null).rootObject)
    }
}
