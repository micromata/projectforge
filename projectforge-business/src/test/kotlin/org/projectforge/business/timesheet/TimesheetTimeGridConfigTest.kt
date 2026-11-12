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
import org.junit.jupiter.api.Test
import org.projectforge.framework.time.PFDateTime
import java.time.ZoneId

class TimesheetTimeGridConfigTest {
    @Test
    fun `valid values are taken, anything else falls back to 15`() {
        assertEquals(5, TimesheetTimeGridConfig.parse(5))
        assertEquals(30, TimesheetTimeGridConfig.parse(30))
        assertEquals(15, TimesheetTimeGridConfig.parse(null))
        assertEquals(15, TimesheetTimeGridConfig.parse(7))
        assertEquals(15, TimesheetTimeGridConfig.parse(0))
    }

    @Test
    fun `roundToGrid rounds to the nearest step`() {
        fun at(hour: Int, minute: Int, second: Int = 0) =
            PFDateTime.withDate(2031, 3, 10, hour, minute, second, zoneId = ZoneId.of("Europe/Berlin"))
        assertEquals(at(8, 15).utilDate, TimesheetTimeGridConfig.roundToGrid(at(8, 10), 15).utilDate)
        assertEquals(at(8, 0).utilDate, TimesheetTimeGridConfig.roundToGrid(at(8, 7, 20), 15).utilDate)
        assertEquals(at(9, 0).utilDate, TimesheetTimeGridConfig.roundToGrid(at(8, 53), 15).utilDate)
        assertEquals(at(8, 45).utilDate, TimesheetTimeGridConfig.roundToGrid(at(8, 45, 40), 15).utilDate)
    }
}
