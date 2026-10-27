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


package org.projectforge.business.fibu.kost.reallocation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BuchungssatzKost2ShareSplitTest {
    @Test
    fun `parts sum up to the total, the last one takes the rounding difference`() {
        val parts = BuchungssatzKost2ShareService.splitByMillis(BigDecimal("100.00"), listOf(1L, 1L, 1L))
        assertEquals(listOf(BigDecimal("33.33"), BigDecimal("33.33"), BigDecimal("33.34")), parts)
        assertEquals(BigDecimal("100.00"), parts.fold(BigDecimal.ZERO, BigDecimal::add))
    }

    @Test
    fun `debit amounts keep their sign`() {
        val parts = BuchungssatzKost2ShareService.splitByMillis(BigDecimal("-1000.00"), listOf(6L, 4L))
        assertEquals(listOf(BigDecimal("-600.00"), BigDecimal("-400.00")), parts)
        val odd = BuchungssatzKost2ShareService.splitByMillis(BigDecimal("-0.05"), listOf(1L, 1L))
        assertEquals(BigDecimal("-0.05"), odd.fold(BigDecimal.ZERO, BigDecimal::add))
    }

    @Test
    fun `zero weights`() {
        // The rest is zero (all time sheets moved): the whole amount goes to the target.
        assertEquals(
            listOf(BigDecimal("-500.00"), BigDecimal("0.00")),
            BuchungssatzKost2ShareService.splitByMillis(BigDecimal("-500"), listOf(5L, 0L)),
        )
        // No time at all: the last part takes everything.
        assertEquals(
            listOf(BigDecimal("0.00"), BigDecimal("12.34")),
            BuchungssatzKost2ShareService.splitByMillis(BigDecimal("12.34"), listOf(0L, 0L)),
        )
    }
}
