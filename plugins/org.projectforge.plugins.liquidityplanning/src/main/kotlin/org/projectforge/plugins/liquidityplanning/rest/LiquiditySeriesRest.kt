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

package org.projectforge.plugins.liquidityplanning.rest

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.projectforge.plugins.liquidityplanning.LiquiditySeriesDO
import org.projectforge.plugins.liquidityplanning.LiquiditySeriesDao
import org.projectforge.plugins.liquidityplanning.LiquiditySeriesSplitService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.saveOrUpdate
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The focused editor of a recurring [LiquiditySeriesDO], reached from the read-only "part of series …" link
 * on any occurrence (`/liquidity/series/{id}`). Edit-only on purpose: a series has no list of its own, so
 * this only serves load and `saveorupdate` (plus soft-delete = stop the projection). Editing the series
 * template affects only the future, still-virtual occurrences; materialized (touched/paid) occurrences are
 * frozen real rows and unaffected — which is why there is no "this/future/all" prompt.
 *
 * Guarded by the same right as the entries ([org.projectforge.plugins.liquidityplanning.LiquidityplanningPluginUserRightId.PLUGIN_LIQUIDITY_PLANNING],
 * via [LiquiditySeriesDao]).
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/liquiditySeries")
class LiquiditySeriesRest :
    AbstractDOEntityRest<LiquiditySeriesDO, LiquiditySeriesDao>(
        LiquiditySeriesDao::class.java,
        "plugins.liquidityplanning.series.title",
    ) {

    @Autowired
    private lateinit var splitService: LiquiditySeriesSplitService

    /**
     * The "valid from a date on" save of the series editor (see [LiquiditySeriesDO.effectiveFrom]). Instead of
     * the plain in-place update the standard `saveorupdate` route does — which would retroactively rewrite
     * every still-virtual past occurrence too — this cuts the series at the effective date: a continuation
     * series carries the edited rule and template from the cut on, the original keeps its values and is ended
     * just before it (`count = N`). Because occurrences are suppressed per `(seriesId, seriesDate)`, the two
     * series coexist cleanly and the history stays correct.
     *
     * Falls back to the ordinary upsert when there is nothing to split: no effective date, an unsaved series,
     * or an effective date on or before the start (which would leave an empty `count = 0` history series). The
     * continuation insert and the migration of already-materialized future occurrences run in one transaction
     * ([LiquiditySeriesSplitService.createContinuationAndMigrate]); ending the old series is the separate
     * write that follows — the same two-step shape the entry form's `repeat` block has.
     */
    @AccessChecked("DAO: find/insert/saveOrUpdate of LiquiditySeriesDao (PLUGIN_LIQUIDITY_PLANNING)")
    @PostMapping("split")
    fun split(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<LiquiditySeriesDO>,
    ): ResponseEntity<ResponseAction> {
        sessionCsrfService.validateCsrfToken(request, postData, "Split series")?.let { return it }
        val edited = transformForDB(postData.data)
        val effectiveFrom = edited.effectiveFrom
        val oldId = edited.id
        val original = oldId?.let { baseDao.find(it) }
        val originalStart = original?.startDate
        // Nothing to split — a whole-series edit, a still-unsaved series, or a cut at/before the start:
        // let the standard upsert write the edited values in place (the historic behaviour).
        if (original == null || originalStart == null || effectiveFrom == null || !effectiveFrom.isAfter(originalStart)) {
            return saveOrUpdate(request, baseDao, edited, postData, this, validate(edited))
        }
        val n = splitService.countBefore(original, effectiveFrom)
        splitService.createContinuationAndMigrate(
            oldId,
            LiquiditySeriesSplitService.EditedTemplate(edited),
            original,
            n,
        )
        // End the old series just before the cut, keeping its original template values (reloaded fresh, so
        // the edited values the client posted do not leak into it). The standard upsert gives the ordinary
        // ResponseAction that redirects to the entry list.
        val oldSeries = baseDao.find(oldId)!!
        oldSeries.count = splitService.cappedOldCount(original, n)
        return saveOrUpdate(request, baseDao, oldSeries, postData, this, validate(oldSeries))
    }
}
