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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Turns a virtual occurrence of a [LiquiditySeriesDO] into a real, frozen [LiquidityEntryDO] — the
 * copy-on-write step shared by the two ways a user can touch an occurrence: opening its prefilled form (the
 * REST `newBaseDO` prefill only builds the transient template; the actual row is written when the user saves)
 * and selecting it for a mass update ([materialize], which persists immediately before the update loop runs).
 *
 * @author Kai Reinhard
 */
@Service
open class LiquidityMaterializationService {
    @Autowired
    private lateinit var liquiditySeriesDao: LiquiditySeriesDao

    @Autowired
    private lateinit var liquidityEntryDao: LiquidityEntryDao

    @Autowired
    private lateinit var liquiditySeriesProjector: LiquiditySeriesProjector

    /**
     * The transient template an occurrence's edit form starts from: the series' values plus its anchor. `id`
     * stays null (it is a new, unsaved row) and `dateOfPayment` starts at the anchor day, which the user may
     * then move without changing the occurrence's identity ([LiquidityEntryDO.seriesDate] is the anchor).
     * Returns `null` if the series is gone.
     */
    open fun buildPrefill(seriesId: Long, seriesDate: LocalDate): LiquidityEntryDO? {
        val series = liquiditySeriesDao.find(seriesId, checkAccess = false) ?: return null
        return buildOccurrence(series, seriesId, seriesDate)
    }

    /**
     * A transient occurrence for a virtual id, without persisting it — for computing a mass-update statistics
     * preview over a selection that still contains virtual rows. Returns `null` if the id is not a valid
     * virtual occurrence of an existing series.
     */
    open fun preview(virtualId: Long): LiquidityEntryDO? {
        return resolve(virtualId)?.let { (series, seriesDate) -> buildOccurrence(series, series.id!!, seriesDate) }
    }

    /**
     * Materializes the occurrence a virtual id stands for and returns the persisted row, so a mass update can
     * treat it like any other selected entry. Decodes the id with the same helper the projector encodes with,
     * re-derives the anchor day from the series (never trusting a date from the client), and skips an ordinal
     * beyond a finite series' `count`. If the occurrence is already materialized (its anchor is taken) that
     * existing row is returned instead of a duplicate. Returns `null` if the id is not a valid virtual
     * occurrence.
     */
    open fun materialize(virtualId: Long): LiquidityEntryDO? {
        val (series, seriesDate) = resolve(virtualId) ?: return null
        val seriesId = series.id ?: return null
        findMaterialized(seriesId, seriesDate)?.let { return it }
        val entry = buildOccurrence(series, seriesId, seriesDate)
        liquidityEntryDao.insert(entry, checkAccess = false)
        return entry
    }

    /** Decodes a virtual id to its series and anchor day, honouring a finite series' installment count. */
    private fun resolve(virtualId: Long): Pair<LiquiditySeriesDO, LocalDate>? {
        val ref = LiquiditySeriesProjector.decodeVirtualId(virtualId) ?: return null
        val series = liquiditySeriesDao.find(ref.seriesId, checkAccess = false) ?: return null
        val count = series.count
        if (count != null && ref.ordinal >= count) {
            return null
        }
        val start = series.startDate ?: return null
        return series to liquiditySeriesProjector.occurrenceDate(start, series.intervalMonths, ref.ordinal)
    }

    /** A fresh, unsaved occurrence carrying the series' template values and its anchor identity. */
    private fun buildOccurrence(series: LiquiditySeriesDO, seriesId: Long, seriesDate: LocalDate): LiquidityEntryDO {
        val entry = LiquidityEntryDO()
        entry.dateOfPayment = seriesDate
        entry.amount = series.amount
        entry.subject = series.subject
        entry.comment = series.comment
        entry.autoSetPaid = series.autoSetPaid
        entry.paid = null
        entry.seriesId = seriesId
        entry.seriesDate = seriesDate
        return entry
    }

    /** The already-materialized row for an anchor, if any (only non-deleted rows can be updated further). */
    private fun findMaterialized(seriesId: Long, seriesDate: LocalDate): LiquidityEntryDO? {
        return liquidityEntryDao.selectAllNotDeleted(checkAccess = false)
            .firstOrNull { it.seriesId == seriesId && it.seriesDate == seriesDate }
    }
}
