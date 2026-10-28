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


package org.projectforge.business.timesheet

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2DO

class TimesheetKost2MapperTest {
    private val art1 = Kost2ArtDO().also { it.id = 1L }
    private val art2 = Kost2ArtDO().also { it.id = 2L }

    @Test
    fun `the cost unit of the same type is found`() {
        val target1 = kost2(10L, art1)
        val target2 = kost2(11L, art2)
        assertSame(target2, TimesheetKost2Mapper.sameKost2Art(kost2(1L, art2), listOf(target1, target2)))
        assertNull(TimesheetKost2Mapper.sameKost2Art(kost2(1L, art2), listOf(target1)), "No cost unit of the type.")
    }

    @Test
    fun `a cost unit without type matches one without type only`() {
        val withoutType = kost2(10L, null)
        val withType = kost2(11L, art1)
        assertSame(withoutType, TimesheetKost2Mapper.sameKost2Art(kost2(1L, null), listOf(withType, withoutType)))
        assertNull(TimesheetKost2Mapper.sameKost2Art(kost2(1L, null), listOf(withType)))
        assertNull(TimesheetKost2Mapper.sameKost2Art(kost2(1L, art1), listOf(withoutType)))
    }

    @Test
    fun `no cost unit or nothing available`() {
        assertNull(TimesheetKost2Mapper.sameKost2Art(null, listOf(kost2(10L, null), kost2(11L, art1))))
        assertNull(TimesheetKost2Mapper.sameKost2Art(kost2(1L, art1), null))
        assertNull(TimesheetKost2Mapper.sameKost2Art(kost2(1L, art1), emptyList()))
    }

    private fun kost2(id: Long, art: Kost2ArtDO?) = Kost2DO().also { it.id = id; it.kost2Art = art }
}
