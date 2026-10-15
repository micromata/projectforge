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

import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Projects the occurrences of the recurring [LiquiditySeriesDO]s into virtual [LiquidityEntryDO]s for a given
 * horizon, so lists and the forecast can show a series without every occurrence being stored. An occurrence is
 * suppressed once it has been materialized (a real entry with the same `(seriesId, seriesDate)` exists), which
 * is how "changes affect only future unpaid occurrences" falls out for free: a materialized occurrence is
 * frozen and no longer projected.
 *
 * The virtual rows carry a synthetic negative id (see [encodeVirtualId]) that doubles as their React key and
 * their mass-update handle; their decodable identity for save/materialization are the real `seriesId` +
 * `seriesDate` fields, which travel in the row JSON.
 *
 * @author Kai Reinhard
 */
@Service
open class LiquiditySeriesProjector {
    @Autowired
    private lateinit var liquiditySeriesDao: LiquiditySeriesDao

    @Autowired
    private lateinit var liquidityEntryDao: LiquidityEntryDao

    /**
     * Every virtual occurrence of any (non-deleted) series whose anchor day falls in `[horizonStart,
     * horizonEnd]` and that has not yet been materialized. Reads all series and — to suppress occurrences
     * whose materialized row was soft-deleted (a deliberately skipped installment) — all series-linked
     * entries including the deleted ones, without access checks (the caller has already been access-checked).
     */
    open fun project(horizonStart: LocalDate, horizonEnd: LocalDate): List<LiquidityEntryDO> {
        val series = liquiditySeriesDao.selectAllNotDeleted(checkAccess = false)
        if (series.isEmpty()) {
            return emptyList()
        }
        val occupied = liquidityEntryDao.select(deleted = null, checkAccess = false)
            .mapNotNullTo(HashSet()) { entry ->
                val id = entry.seriesId
                val date = entry.seriesDate
                if (id != null && date != null) SeriesAnchor(id, date) else null
            }
        return project(horizonStart, horizonEnd, series, occupied)
    }

    /**
     * The pure projection over the given series and set of already-materialized anchors — extracted so it can
     * be unit-tested without a database. Occurrences are anchored to `startDate.plusMonths(n * interval)` so
     * a month-end start (e.g. the 31st) does not drift over the year.
     */
    open fun project(
        horizonStart: LocalDate,
        horizonEnd: LocalDate,
        series: List<LiquiditySeriesDO>,
        occupiedAnchors: Set<SeriesAnchor>,
    ): List<LiquidityEntryDO> {
        val result = ArrayList<LiquidityEntryDO>()
        for (s in series) {
            val seriesId = s.id ?: continue
            val start = s.startDate ?: continue
            val count = s.count
            var n = 0
            while (true) {
                if (count != null && n >= count) {
                    break
                }
                val occDate = occurrenceDate(start, s.intervalMonths, n)
                if (occDate.isAfter(horizonEnd)) {
                    break
                }
                if (!occDate.isBefore(horizonStart) && !occupiedAnchors.contains(SeriesAnchor(seriesId, occDate))) {
                    result.add(newVirtualEntry(s, seriesId, occDate, n))
                }
                n++
                // Endless series are still bounded by horizonEnd (the isAfter break above); this only guards
                // against a pathological interval of 0.
                if (s.intervalMonths < 1) {
                    break
                }
            }
        }
        return result
    }

    private fun newVirtualEntry(
        series: LiquiditySeriesDO,
        seriesId: Long,
        occDate: LocalDate,
        ordinal: Int,
    ): LiquidityEntryDO {
        val entry = LiquidityEntryDO()
        entry.id = encodeVirtualId(seriesId, ordinal)
        entry.dateOfPayment = occDate
        entry.amount = series.amount
        entry.subject = series.subject
        entry.comment = series.comment
        entry.autoSetPaid = series.autoSetPaid
        entry.paid = null
        entry.seriesId = seriesId
        entry.seriesDate = occDate
        return entry
    }

    /** The anchor day of the n-th occurrence, always measured from the start so month-ends do not drift. */
    fun occurrenceDate(startDate: LocalDate, intervalMonths: Int, ordinal: Int): LocalDate {
        val interval = intervalMonths.coerceAtLeast(1)
        return PFDay.from(startDate).plusMonths((ordinal.toLong() * interval)).localDate
    }

    /** The stable identity of an occurrence: which series and which anchor day. */
    data class SeriesAnchor(val seriesId: Long, val seriesDate: LocalDate)

    /** A decoded virtual id: which series and which ordinal it stood for. */
    data class VirtualRef(val seriesId: Long, val ordinal: Int)

    companion object {
        /**
         * The ordinal-encoding radix of the synthetic id. Bounds a series to fewer than this many projected
         * occurrences, comfortably above any list horizon (24 months) or forecast window (600 days).
         */
        private const val ORDINAL_RADIX = 10000L

        /** True for a projected (virtual) row: real primary keys are always positive. */
        fun isVirtual(id: Long?): Boolean = id != null && id < 0

        /** The synthetic id of the n-th occurrence of a series — negative, so it can never collide with a real pk. */
        fun encodeVirtualId(seriesId: Long, ordinal: Int): Long = -(seriesId * ORDINAL_RADIX + ordinal)

        /** Decodes a synthetic id back into its series id and ordinal, or `null` if the id is not virtual. */
        fun decodeVirtualId(id: Long?): VirtualRef? {
            if (!isVirtual(id)) {
                return null
            }
            val q = -id!!
            return VirtualRef(seriesId = q / ORDINAL_RADIX, ordinal = (q % ORDINAL_RADIX).toInt())
        }
    }
}
