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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.Constants
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2DO
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.*

class TimesheetOverlapUtilsTest {
    @Test
    fun `disjoint time sheets keep their full duration`() {
        val a = createTimesheet(1, 0, 60) // 1 hour
        val b = createTimesheet(2, 120, 60) // 1 hour, starting after a
        val split = TimesheetOverlapUtils.splitDurations(listOf(a, b))
        Assertions.assertEquals(HOUR, split[1L])
        Assertions.assertEquals(HOUR, split[2L])
        Assertions.assertEquals(2 * HOUR, TimesheetOverlapUtils.unionDurationMillis(listOf(a, b)))
    }

    @Test
    fun `two fully overlapping time sheets are split 50-50`() {
        val a = createTimesheet(1, 0, 60)
        val b = createTimesheet(2, 0, 60)
        val split = TimesheetOverlapUtils.splitDurations(listOf(a, b))
        Assertions.assertEquals(HOUR / 2, split[1L])
        Assertions.assertEquals(HOUR / 2, split[2L])
        Assertions.assertEquals(HOUR, TimesheetOverlapUtils.unionDurationMillis(listOf(a, b)))
    }

    @Test
    fun `three fully overlapping time sheets are split into thirds`() {
        val sheets = listOf(createTimesheet(1, 0, 60), createTimesheet(2, 0, 60), createTimesheet(3, 0, 60))
        val split = TimesheetOverlapUtils.splitDurations(sheets)
        // Sum must be exactly one hour despite the remainder of the integer division.
        Assertions.assertEquals(HOUR, split.values.sum())
        Assertions.assertEquals(HOUR, TimesheetOverlapUtils.unionDurationMillis(sheets))
        // Each gets roughly a third.
        split.values.forEach { Assertions.assertTrue(it in (HOUR / 3)..(HOUR / 3 + 1)) }
    }

    @Test
    fun `partial overlap splits only the shared interval`() {
        // a: 0..60min, b: 30..90min -> overlap 30..60 (30min shared).
        val a = createTimesheet(1, 0, 60)
        val b = createTimesheet(2, 30, 60)
        val split = TimesheetOverlapUtils.splitDurations(listOf(a, b))
        // a: 30min alone + 15min (half of shared 30min) = 45min. Same for b.
        Assertions.assertEquals(45 * MINUTE, split[1L])
        Assertions.assertEquals(45 * MINUTE, split[2L])
        // Union = 0..90min = 90min.
        Assertions.assertEquals(90 * MINUTE, TimesheetOverlapUtils.unionDurationMillis(listOf(a, b)))
    }

    @Test
    fun `gross working time counts every sheet with a positive work fraction in full`() {
        // Two disjoint sheets (both full working time) each keep their whole hour.
        val a = createTimesheet(1, 0, 60)
        val b = createTimesheet(2, 120, 60)
        val gross = TimesheetOverlapUtils.grossWorkingDurations(listOf(a, b))
        Assertions.assertEquals(HOUR, gross[1L])
        Assertions.assertEquals(HOUR, gross[2L])
        Assertions.assertEquals(2 * HOUR, gross.values.sum())
    }

    @Test
    fun `gross working time counts overlapping working sheets only once`() {
        // Two fully overlapping working sheets: the union is one hour, so the gross total is 1h, not 2h.
        val a = createTimesheet(1, 0, 60)
        val b = createTimesheet(2, 0, 60)
        val gross = TimesheetOverlapUtils.grossWorkingDurations(listOf(a, b))
        Assertions.assertEquals(HOUR, gross.values.sum(), "Overlap counted once.")
    }

    @Test
    fun `gross working time counts travel time (fraction 0_5) in full`() {
        // Travel time has a work fraction of 0.5, but the gross figure still counts its full duration.
        val travel = createTimesheet(1, 0, 60, workFraction = BigDecimal("0.5"))
        val gross = TimesheetOverlapUtils.grossWorkingDurations(listOf(travel))
        Assertions.assertEquals(HOUR, gross[1L], "Travel time counts fully in gross working time.")
    }

    @Test
    fun `gross working time excludes zero-fraction sheets (cost type 33)`() {
        val cost33 = createTimesheet(1, 0, 60, workFraction = BigDecimal.ZERO)
        val gross = TimesheetOverlapUtils.grossWorkingDurations(listOf(cost33))
        Assertions.assertNull(gross[1L], "A zero-fraction sheet does not contribute to gross working time.")
        Assertions.assertEquals(0L, gross.values.sum())
    }

    @Test
    fun `a zero-fraction sheet does not reduce an overlapping working sheet`() {
        // A cost type "33" sheet (fraction 0) fully overlapping a working sheet: the working sheet keeps its
        // full hour, because the zero-fraction sheet is removed before the overlap split.
        val cost33 = createTimesheet(1, 0, 60, workFraction = BigDecimal.ZERO)
        val work = createTimesheet(2, 0, 60)
        val gross = TimesheetOverlapUtils.grossWorkingDurations(listOf(cost33, work))
        Assertions.assertNull(gross[1L])
        Assertions.assertEquals(HOUR, gross[2L], "Working sheet keeps its full hour; the 33 sheet doesn't split it.")
        Assertions.assertEquals(HOUR, gross.values.sum())
    }

    @Test
    fun `gross working time resolves the work fraction through the Kost2-Art`() {
        // The Kost2 has no own fraction, so it is taken from its Kost2-Art: 0 -> excluded, positive -> full.
        val cost33 = createTimesheetWithKost2Art(1, 0, 60, artWorkFraction = BigDecimal.ZERO)
        val travel = createTimesheetWithKost2Art(2, 120, 60, artWorkFraction = BigDecimal("0.5"))
        val gross = TimesheetOverlapUtils.grossWorkingDurations(listOf(cost33, travel))
        Assertions.assertNull(gross[1L], "Kost2-Art fraction 0 -> excluded.")
        Assertions.assertEquals(HOUR, gross[2L], "Kost2-Art fraction 0.5 -> counts fully.")
    }

    private fun createTimesheet(
        id: Long,
        startOffsetMinutes: Long,
        durationMinutes: Long,
        workFraction: BigDecimal? = null,
    ): TimesheetDO {
        return TimesheetDO().also {
            it.id = id
            it.startTime = Date(START_TIME.time + startOffsetMinutes * MINUTE)
            it.stopTime = Date(START_TIME.time + (startOffsetMinutes + durationMinutes) * MINUTE)
            if (workFraction != null) {
                it.kost2 = Kost2DO().also { kost2 -> kost2.workFraction = workFraction }
            }
        }
    }

    private fun createTimesheetWithKost2Art(
        id: Long,
        startOffsetMinutes: Long,
        durationMinutes: Long,
        artWorkFraction: BigDecimal,
    ): TimesheetDO {
        return createTimesheet(id, startOffsetMinutes, durationMinutes).also {
            it.kost2 = Kost2DO().also { kost2 ->
                kost2.kost2Art = Kost2ArtDO().also { art -> art.workFraction = artWorkFraction }
            }
        }
    }

    companion object {
        private const val MINUTE = Constants.MILLIS_PER_MINUTE
        private const val HOUR = Constants.MILLIS_PER_HOUR
        private val START_TIME =
            Date.from(LocalDateTime.of(2026, Month.JANUARY, 5, 8, 0).atZone(ZoneId.of("UTC")).toInstant())

        @JvmStatic
        @BeforeAll
        fun setUp() {
            // The gross-duration tests read TimesheetDO.workFraction, which resolves Kost2/Kost2-Art through
            // PfCaches. Install cacheless test instances so that static access does not fail on an uninitialized
            // lateinit; fully initialized Kost2/Kost2-Art objects are returned as-is (see getKost2IfNotInitialized).
            PfCaches.internalSetupForTestCases()
        }
    }
}
