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

package org.projectforge.business.vacation.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VacationDOTest {
    @Test
    fun `periods overlap if they share at least one day`() {
        val vacation = vacation("2030-02-04", "2030-02-08")
        assertTrue(vacation.hasOverlap(vacation("2030-02-06", "2030-02-12")), "Overlapping end.")
        assertTrue(vacation.hasOverlap(vacation("2030-02-01", "2030-02-04")), "Ending on the first day.")
        assertTrue(vacation.hasOverlap(vacation("2030-02-08", "2030-02-10")), "Starting on the last day.")
        assertTrue(vacation.hasOverlap(vacation("2030-02-05", "2030-02-06")), "Inside.")
        assertTrue(vacation.hasOverlap(vacation("2030-01-01", "2030-03-01")), "Around.")
    }

    @Test
    fun `periods don't overlap if one ends before the other starts`() {
        val vacation = vacation("2030-02-04", "2030-02-08")
        assertFalse(vacation.hasOverlap(vacation("2030-01-20", "2030-02-03")), "Before.")
        assertFalse(vacation.hasOverlap(vacation("2030-02-09", "2030-02-15")), "After.")
        assertFalse(vacation.hasOverlap(VacationDO()), "Without dates.")
    }

    private fun vacation(start: String, end: String): VacationDO {
        return VacationDO().also {
            it.startDate = LocalDate.parse(start)
            it.endDate = LocalDate.parse(end)
        }
    }
}
