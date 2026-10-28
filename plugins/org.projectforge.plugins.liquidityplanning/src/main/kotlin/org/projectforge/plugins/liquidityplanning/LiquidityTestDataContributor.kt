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

import org.projectforge.business.testdata.TestDataContext
import org.projectforge.business.testdata.TestDataContributor
import org.projectforge.framework.time.RecurrenceFrequency
import org.projectforge.plugins.core.PluginAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

/**
 * The company's costs of the coming months: monthly series (salaries, rent, ...) and single entries. The
 * incoming payments come from the invoices.
 */
@Service
open class LiquidityTestDataContributor : TestDataContributor {
    @Autowired
    private lateinit var liquidityEntryDao: LiquidityEntryDao

    @Autowired
    private lateinit var liquiditySeriesDao: LiquiditySeriesDao

    override val order = 100

    override val pluginId = PluginAdminService.PLUGIN_LIQUIDITY_PLANNING_ID

    override fun seed(ctx: TestDataContext) {
        val month = YearMonth.from(ctx.today)
        listOf(
            Triple(28, -96500, "Salaries"),
            Triple(3, -8400, "Office rent Kassel"),
            Triple(15, -2350, "Cloud hosting"),
            Triple(10, -21800, "Social security contributions"),
        ).forEach { (day, amount, subject) ->
            val series = LiquiditySeriesDO().also {
                it.startDate = month.atDay(day)
                it.frequency = RecurrenceFrequency.MONTHLY
                it.amount = BigDecimal(amount)
                it.subject = subject
            }
            liquiditySeriesDao.insert(series, checkAccess = false)
        }
        listOf(
            Triple(ctx.month(2026, 11, 10), -18900, "VAT advance payment"),
            Triple(ctx.month(2026, 12, 10), -24600, "VAT advance payment"),
            Triple(ctx.month(2026, 11, 20), 54000, "Expected: Customer App design (Alpine Insurance)"),
            Triple(ctx.month(2026, 12, 22), 28800, "Expected: payment change request (Lumen Retail)"),
            Triple(ctx.month(2026, 10, 30), -12400, "Laptops for new colleagues"),
        ).forEach { (date, amount, subject) ->
            val entry = LiquidityEntryDO().also {
                it.dateOfPayment = date
                it.amount = BigDecimal(amount)
                it.subject = subject
            }
            liquidityEntryDao.insert(entry, checkAccess = false)
        }
    }
}
