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

import org.projectforge.framework.persistence.api.BaseDao
import org.springframework.stereotype.Service

/**
 * Persists the recurring [LiquiditySeriesDO] rules. Guarded by the same right as the entries themselves
 * ([LiquidityplanningPluginUserRightId.PLUGIN_LIQUIDITY_PLANNING]); the series has no list of its own (it is
 * reached only through the focused series editor), so this DAO only serves load / save / soft-delete.
 *
 * @author Kai Reinhard
 */
@Service
open class LiquiditySeriesDao : BaseDao<LiquiditySeriesDO>(LiquiditySeriesDO::class.java) {
    init {
        userRightId = LiquidityplanningPluginUserRightId.PLUGIN_LIQUIDITY_PLANNING
    }

    override fun newInstance(): LiquiditySeriesDO {
        return LiquiditySeriesDO()
    }
}
