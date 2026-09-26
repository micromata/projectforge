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

import org.projectforge.plugins.liquidityplanning.LiquiditySeriesDO
import org.projectforge.plugins.liquidityplanning.LiquiditySeriesDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOEntityRest
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
    )
