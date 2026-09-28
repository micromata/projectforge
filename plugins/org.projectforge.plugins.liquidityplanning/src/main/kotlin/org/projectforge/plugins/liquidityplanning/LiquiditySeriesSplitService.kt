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

import org.projectforge.framework.time.RecurrenceFrequency
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Splits a recurring [LiquiditySeriesDO] at an effective date — the "valid from a date on" edit mode of the
 * series editor. Instead of mutating the master in place (which would retroactively change every still-virtual
 * past occurrence too), the series is cut in two: the original keeps its values and is ended just before the
 * cut (its [LiquiditySeriesDO.count] set to the number of occurrences before the effective date), and a
 * continuation series carries the edited rule and template from the cut on. Because occurrences are suppressed
 * per `(seriesId, seriesDate)`, the two series coexist cleanly and the history stays correct.
 *
 * The count/grid math ([countBefore], [cappedOldCount], [alignedStart]) is pure so it can be unit-tested
 * without a database; [createContinuationAndMigrate] does the two writes (insert the continuation, re-link
 * already-materialized future occurrences onto it) in one transaction.
 *
 * @author Kai Reinhard
 */
@Service
open class LiquiditySeriesSplitService(
    // All constructor-injected (Spring uses the single constructor) so the whole service — the pure grid math
    // and [createContinuationAndMigrate] — can be unit-tested with a real projector and mocked collaborators,
    // no Spring context.
    private val liquiditySeriesProjector: LiquiditySeriesProjector,
    private val liquiditySeriesDao: LiquiditySeriesDao,
    private val liquidityEntryDao: LiquidityEntryDao,
    private val liquidityMaterializationService: LiquidityMaterializationService,
) {

    /**
     * The number of occurrences of [original] whose anchor day is strictly before [effectiveFrom], bounded by a
     * finite [LiquiditySeriesDO.count]. The occurrence grid is monotonically increasing, so this is the ordinal
     * of the first occurrence on or after the effective date — i.e. how many installments stay with the old
     * series.
     */
    // `open` so CGLIB routes the call to the injected target: the @Transactional method below makes this
    // bean a CGLIB proxy, and a final (plain `fun`) method would run against the uninitialized proxy
    // instance instead, where the constructor-injected projector is null.
    open fun countBefore(original: LiquiditySeriesDO, effectiveFrom: LocalDate): Int {
        val start = original.startDate ?: return 0
        val count = original.count
        var n = 0
        while (count == null || n < count) {
            if (!liquiditySeriesProjector.occurrenceDate(start, original.intervalMonths, n).isBefore(effectiveFrom)) {
                break
            }
            n++
        }
        return n
    }

    /** The [LiquiditySeriesDO.count] the ended old series gets: `n`, but never more than an existing finite count. */
    open fun cappedOldCount(original: LiquiditySeriesDO, n: Int): Int {
        return original.count?.let { minOf(n, it) } ?: n
    }

    /** The continuation's start day: the n-th occurrence on the original grid, so it aligns with the old anchors. */
    open fun alignedStart(original: LiquiditySeriesDO, n: Int): LocalDate {
        return liquiditySeriesProjector.occurrenceDate(original.startDate!!, original.intervalMonths, n)
    }

    /**
     * Inserts the continuation series (the edited rule and template, anchored at [alignedStart]) and re-links
     * every already-materialized occurrence of the old series at or after the cut onto it, so those frozen rows
     * follow the new series and are not projected twice. Returns the new series' id.
     *
     * The continuation deliberately keeps the original recurrence grid ([LiquiditySeriesDO.frequency] and
     * [LiquiditySeriesDO.intervalMonths]) rather than any edited interval: it shares the old anchors, so a
     * re-linked occurrence's `seriesDate` still lands on the grid and stays suppressed. Only the template
     * (amount, subject, comment, auto-set-paid) and the installment count carry the edited values.
     */
    @Transactional
    open fun createContinuationAndMigrate(
        oldId: Long,
        edited: EditedTemplate,
        original: LiquiditySeriesDO,
        n: Int,
    ): Long {
        val start = alignedStart(original, n)
        val continuation = LiquiditySeriesDO()
        continuation.startDate = start
        continuation.frequency = original.frequency ?: RecurrenceFrequency.MONTHLY
        continuation.intervalMonths = original.intervalMonths.coerceAtLeast(1)
        continuation.count = original.count?.let { it - n } ?: edited.count
        continuation.amount = edited.amount
        continuation.subject = edited.subject
        continuation.comment = edited.comment
        continuation.autoSetPaid = edited.autoSetPaid
        liquiditySeriesDao.insert(continuation)
        val newId = continuation.id ?: return oldId
        liquidityEntryDao.select(deleted = null, checkAccess = false)
            .filter { it.seriesId == oldId && it.seriesDate?.isBefore(start) == false }
            .forEach { entry ->
                entry.seriesId = newId
                liquidityEntryDao.update(entry, checkAccess = false)
            }
        // Materialize the continuation's occurrence 0 (its aligned start) unless a migrated row already took
        // that anchor. This restores the invariant the full-text-filtered list relies on — every series has
        // its first occurrence as a real, indexed row (see LiquidityEntityRest.virtualRowsForList and
        // onAfterSaveOrUpdate, which does the same for a freshly created series). Without it a continuation
        // whose occurrences are all still virtual has no Lucene-visible row, so a search matching its subject
        // would find none of its occurrences even though they share that subject.
        liquidityMaterializationService.materializeAnchor(newId, start)
        return newId
    }

    /**
     * The edited template values, snapshotted from the posted series so the continuation carries them while the
     * separately reloaded original row keeps its own. The recurrence grid (frequency, interval) is intentionally
     * not part of this — the continuation stays on the original grid (see [createContinuationAndMigrate]).
     */
    class EditedTemplate(
        val count: Int?,
        val amount: BigDecimal?,
        val subject: String?,
        val comment: String?,
        val autoSetPaid: Boolean,
    ) {
        constructor(series: LiquiditySeriesDO) : this(
            count = series.count,
            amount = series.amount,
            subject = series.subject,
            comment = series.comment,
            autoSetPaid = series.autoSetPaid,
        )
    }
}
