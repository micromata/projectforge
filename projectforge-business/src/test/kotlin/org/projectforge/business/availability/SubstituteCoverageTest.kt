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

package org.projectforge.business.availability

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.availability.SubstituteCoverage.Absence
import java.time.LocalDate

class SubstituteCoverageTest {
    private fun june(day: Int): LocalDate = LocalDate.of(2030, 6, day)

    private fun uncovered(vararg absencesBySubstitute: List<Absence>): List<Int> =
        SubstituteCoverage.uncoveredDays(june(1), june(30), absencesBySubstitute.toList()).map { it.dayOfMonth }.toList()

    @Test
    fun `no substitute or one never away covers all days`() {
        assertTrue(uncovered().isEmpty(), "No substitutes: nobody to miss.")
        assertTrue(uncovered(listOf(Absence(june(1), june(30))), emptyList()).isEmpty(), "One is on duty all the time.")
    }

    @Test
    fun `the days all substitutes are away`() {
        assertEquals(
            listOf(5, 6, 9, 10),
            uncovered(
                listOf(Absence(june(5), june(10))),
                listOf(Absence(june(1), june(6)), Absence(june(9), june(12))),
            ),
        )
        assertEquals((1..30).toList(), uncovered(listOf(Absence(june(1), june(30)))), "A single one away all month.")
    }

    @Test
    fun `absences beside the period or without dates count for nothing`() {
        assertTrue(uncovered(listOf(Absence(june(1).minusDays(5), june(1).minusDays(1)))).isEmpty())
        assertTrue(uncovered(listOf(Absence(null, june(10)))).isEmpty())
    }
}
