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

package org.projectforge.plugins.liquidityplanning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.projectforge.framework.time.RecurrenceFrequency
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Unit tests of [LiquiditySeriesSplitService]. The service is built with a real [LiquiditySeriesProjector]
 * (so the grid math is exercised for real) and mocked collaborators (no database), which its all-constructor
 * injection makes possible. Two groups: the pure split math ([LiquiditySeriesSplitService.countBefore],
 * [LiquiditySeriesSplitService.cappedOldCount], [LiquiditySeriesSplitService.alignedStart]) and the
 * write path ([LiquiditySeriesSplitService.createContinuationAndMigrate]).
 *
 * @author Kai Reinhard
 */
class LiquiditySeriesSplitServiceTest {
    private val liquiditySeriesDao = mock<LiquiditySeriesDao>()
    private val liquidityEntryDao = mock<LiquidityEntryDao>()
    private val materializationService = mock<LiquidityMaterializationService>()
    private val service = LiquiditySeriesSplitService(
        LiquiditySeriesProjector(),
        liquiditySeriesDao,
        liquidityEntryDao,
        materializationService,
    )

    @BeforeEach
    fun setUp() {
        // Inserting the continuation assigns it the id the migration and materialization then use.
        whenever(liquiditySeriesDao.insert(any<LiquiditySeriesDO>(), any<Boolean>())).thenAnswer { invocation: InvocationOnMock ->
            invocation.getArgument<LiquiditySeriesDO>(0).id = CONTINUATION_ID
            CONTINUATION_ID
        }
    }

    @Test
    fun `countBefore counts the occurrences strictly before the effective date`() {
        val start = LocalDate.of(2025, 1, 15)
        val series = series(startDate = start, intervalMonths = 1, count = null)
        // On the start: nothing is before it → the whole series would move to the continuation.
        assertEquals(0, service.countBefore(series, start))
        // On the third anchor (Mar 15): Jan and Feb stay behind → N = 2.
        assertEquals(2, service.countBefore(series, LocalDate.of(2025, 3, 15)))
        // Between anchors (Mar 16): the March occurrence is already before it → N = 3.
        assertEquals(3, service.countBefore(series, LocalDate.of(2025, 3, 16)))
    }

    @Test
    fun `countBefore is bounded by a finite count`() {
        val start = LocalDate.of(2025, 1, 15)
        val series = series(startDate = start, intervalMonths = 1, count = 3)
        // The series ends after 3 installments (Jan/Feb/Mar); a far-future cut cannot count past them.
        assertEquals(3, service.countBefore(series, LocalDate.of(2030, 1, 1)))
    }

    @Test
    fun `countBefore honours an interval greater than one`() {
        val start = LocalDate.of(2025, 1, 15)
        // Every second month: anchors Jan, Mar, May, Jul, …
        val series = series(startDate = start, intervalMonths = 2, count = null)
        // May 15 is the third anchor → Jan and Mar are before it → N = 2.
        assertEquals(2, service.countBefore(series, LocalDate.of(2025, 5, 15)))
        // April falls between anchors: only Jan and Mar precede it → N = 2 as well.
        assertEquals(2, service.countBefore(series, LocalDate.of(2025, 4, 1)))
    }

    @Test
    fun `cappedOldCount never exceeds an existing finite count`() {
        val endless = series(startDate = LocalDate.of(2025, 1, 15), intervalMonths = 1, count = null)
        assertEquals(4, service.cappedOldCount(endless, 4))
        val finite = series(startDate = LocalDate.of(2025, 1, 15), intervalMonths = 1, count = 3)
        // A cut computed beyond the series' own end is capped at the end.
        assertEquals(3, service.cappedOldCount(finite, 5))
        assertEquals(2, service.cappedOldCount(finite, 2))
    }

    @Test
    fun `alignedStart lands on the n-th anchor of the original grid`() {
        val series = series(startDate = LocalDate.of(2025, 1, 31), intervalMonths = 1, count = null)
        // Month-end drift is preserved by the projector: n = 1 → Feb 28 (not the 31st).
        assertEquals(LocalDate.of(2025, 2, 28), service.alignedStart(series, 1))
        assertEquals(LocalDate.of(2025, 3, 31), service.alignedStart(series, 2))
    }

    @Test
    fun `createContinuationAndMigrate builds the continuation on the original grid with the edited template`() {
        val original = series(startDate = LocalDate.of(2025, 1, 15), intervalMonths = 1, count = null)
        whenever(materializedRows()).thenReturn(mutableListOf())
        val edited = LiquiditySeriesSplitService.EditedTemplate(
            count = null,
            amount = BigDecimal("110.00"),
            subject = "rent raised",
            comment = "from April on",
            autoSetPaid = true,
        )

        // Cut at the 4th anchor (Apr 15): three installments stay with the old series.
        val newId = service.createContinuationAndMigrate(OLD_ID, edited, original, n = 3)

        assertEquals(CONTINUATION_ID, newId)
        val captor = argumentCaptor<LiquiditySeriesDO>()
        verify(liquiditySeriesDao).insert(captor.capture(), any())
        val continuation = captor.firstValue
        // Keeps the original recurrence grid …
        assertEquals(LocalDate.of(2025, 4, 15), continuation.startDate)
        assertEquals(1, continuation.intervalMonths)
        assertEquals(RecurrenceFrequency.MONTHLY, continuation.frequency)
        // … but carries the edited template, and an endless original leaves the continuation endless.
        assertEquals(BigDecimal("110.00"), continuation.amount)
        assertEquals("rent raised", continuation.subject)
        assertEquals("from April on", continuation.comment)
        assertEquals(true, continuation.autoSetPaid)
        assertNull(continuation.count)
    }

    @Test
    fun `createContinuationAndMigrate re-links only the materialized occurrences at or after the cut`() {
        val original = series(startDate = LocalDate.of(2025, 1, 15), intervalMonths = 1, count = null)
        val before = entry(OLD_ID, LocalDate.of(2025, 1, 15))
        val atCut = entry(OLD_ID, LocalDate.of(2025, 4, 15))
        val after = entry(OLD_ID, LocalDate.of(2025, 6, 15))
        val foreign = entry(OTHER_SERIES_ID, LocalDate.of(2025, 6, 15))
        whenever(materializedRows()).thenReturn(mutableListOf(before, atCut, after, foreign))

        service.createContinuationAndMigrate(OLD_ID, endlessTemplate(), original, n = 3)

        // The occurrence before the cut and a foreign series' row keep their series id …
        assertEquals(OLD_ID, before.seriesId)
        assertEquals(OTHER_SERIES_ID, foreign.seriesId)
        // … while the ones on and after the cut move to the continuation.
        assertEquals(CONTINUATION_ID, atCut.seriesId)
        assertEquals(CONTINUATION_ID, after.seriesId)
        verify(liquidityEntryDao).update(eq(atCut), any())
        verify(liquidityEntryDao).update(eq(after), any())
        verify(liquidityEntryDao, times(2)).update(any(), any())
    }

    @Test
    fun `createContinuationAndMigrate materializes the continuation's occurrence 0 so it stays searchable`() {
        val original = series(startDate = LocalDate.of(2025, 1, 15), intervalMonths = 1, count = null)
        // No materialized future rows: every continuation occurrence is still virtual, so without this the
        // continuation would have no Lucene-visible row and drop out of a full-text-filtered list.
        whenever(materializedRows()).thenReturn(mutableListOf())

        service.createContinuationAndMigrate(OLD_ID, endlessTemplate(), original, n = 3)

        verify(materializationService).materializeAnchor(CONTINUATION_ID, LocalDate.of(2025, 4, 15))
    }

    @Test
    fun `createContinuationAndMigrate caps a finite continuation at the remaining installments`() {
        val original = series(startDate = LocalDate.of(2025, 1, 15), intervalMonths = 1, count = 10)
        whenever(materializedRows()).thenReturn(mutableListOf())

        service.createContinuationAndMigrate(OLD_ID, endlessTemplate(), original, n = 3)

        val captor = argumentCaptor<LiquiditySeriesDO>()
        verify(liquiditySeriesDao).insert(captor.capture(), any())
        // A finite original hands the remaining installments (10 − 3) to the continuation, ignoring the
        // edited count (which only applies when the original is endless).
        assertEquals(7, captor.firstValue.count)
        verify(materializationService, times(1)).materializeAnchor(any(), any())
    }

    /** Stubs/verifies the single `select(deleted = null, checkAccess = false)` the migration reads through. */
    private fun materializedRows(): MutableList<LiquidityEntryDO> {
        return liquidityEntryDao.select(deleted = anyOrNull(), checkAccess = any(), useRoot = anyOrNull())
            as MutableList<LiquidityEntryDO>
    }

    private fun endlessTemplate() = LiquiditySeriesSplitService.EditedTemplate(
        count = null,
        amount = BigDecimal("110.00"),
        subject = "rent raised",
        comment = null,
        autoSetPaid = false,
    )

    private fun series(startDate: LocalDate, intervalMonths: Int, count: Int?): LiquiditySeriesDO {
        val series = LiquiditySeriesDO()
        series.startDate = startDate
        series.intervalMonths = intervalMonths
        series.count = count
        series.amount = BigDecimal("100.00")
        series.subject = "rent"
        return series
    }

    private fun entry(seriesId: Long, seriesDate: LocalDate): LiquidityEntryDO {
        val entry = LiquidityEntryDO()
        entry.seriesId = seriesId
        entry.seriesDate = seriesDate
        entry.dateOfPayment = seriesDate
        return entry
    }

    companion object {
        private const val OLD_ID = 100L
        private const val CONTINUATION_ID = 200L
        private const val OTHER_SERIES_ID = 999L
    }
}
