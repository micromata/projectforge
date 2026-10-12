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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2DO

/**
 * Parsing and matching of the overlap groups ([TimesheetOverlapGroups]), without data base.
 */
class TimesheetOverlapGroupsTest {
    @Test
    fun `groups and their prefixes`() {
        val groups = TimesheetOverlapGroups.parse("6.100, 6.300 ; 6.200")
        assertEquals(0, groups.groupOf(kost2(6, 100, 0, 1)))
        assertEquals(0, groups.groupOf(kost2(6, 300, 12, 4)))
        assertEquals(1, groups.groupOf(kost2(6, 200, 0, 1)))
        assertNull(groups.groupOf(kost2(6, 400, 0, 1)))
        assertNull(groups.groupOf(kost2(5, 100, 0, 1)))
        assertNull(groups.groupOf(null))
    }

    @Test
    fun `prefixes are compared segment-wise`() {
        val groups = TimesheetOverlapGroups.parse("6.1")
        assertNull(groups.groupOf(kost2(6, 100, 0, 1)), "6.1 must not match 6.100")
        assertEquals(0, groups.groupOf(kost2(6, 1, 0, 1)))
        val full = TimesheetOverlapGroups.parse("6.100.00.01")
        assertEquals(0, full.groupOf(kost2(6, 100, 0, 1)))
        assertNull(full.groupOf(kost2(6, 100, 0, 2)))
    }

    @Test
    fun `the longest matching prefix decides`() {
        val groups = TimesheetOverlapGroups.parse("6;6.200")
        assertEquals(0, groups.groupOf(kost2(6, 100, 0, 1)))
        assertEquals(1, groups.groupOf(kost2(6, 200, 0, 1)))
        val reversed = TimesheetOverlapGroups.parse("6.100,6.300;6.300.01")
        assertEquals(0, reversed.groupOf(kost2(6, 300, 0, 1)))
        assertEquals(1, reversed.groupOf(kost2(6, 300, 1, 1)))
    }

    @Test
    fun `wildcards, invalid and duplicate prefixes`() {
        assertEquals(0, TimesheetOverlapGroups.parse("6.*").groupOf(kost2(6, 100, 0, 1)))
        // Invalid prefixes are ignored, the valid ones of the same group still count.
        val groups = TimesheetOverlapGroups.parse("abc,6.100;6.1.2.3.4;;6.200;6.100")
        assertEquals(0, groups.groupOf(kost2(6, 100, 0, 1)), "duplicate: the first occurrence wins")
        assertEquals(3, groups.groupOf(kost2(6, 200, 0, 1)))
        assertNull(groups.groupOf(kost2(6, 1, 2, 3)))
        assertNull(TimesheetOverlapGroups.parse(null).groupOf(kost2(6, 100, 0, 1)))
        assertNull(TimesheetOverlapGroups.parse(" ").groupOf(kost2(6, 100, 0, 1)))
    }

    private fun kost2(nummernkreis: Int, bereich: Int, teilbereich: Int, kost2Art: Long): Kost2DO {
        return Kost2DO().also {
            it.nummernkreis = nummernkreis
            it.bereich = bereich
            it.teilbereich = teilbereich
            it.kost2Art = Kost2ArtDO().withId(kost2Art)
        }
    }
}
