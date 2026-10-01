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

package org.projectforge.business.fibu

import org.projectforge.framework.time.PFDay
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The monthly totals the charts of the forecast export show (sheet 'Grafiken 1' of the Excel template), computed
 * by [ForecastExport.chartData] for the web frontend. The Excel derives these values by template formulas from the
 * detail sheets; here the same values are summed up while the detail sheets are filled (see [ForecastChartTotals]).
 * Every list has 12 entries, one per month of [months].
 */
class ForecastChartData(
    /** The 12 months of the forecast as `yyyy-MM`. */
    val months: List<String>,
    /**
     * The remaining forecast per month by position status (rows 2-6 of Forecast_Data): only the statuses of
     * [ForecastUtils.auftragsPositionsStatusToShow], in that order.
     */
    val forecastByStatus: Map<AuftragsStatus, List<BigDecimal>>,
    /** Invoiced net sums per month (IST, row 8 of Forecast_Data). */
    val ist: List<BigDecimal>,
    /** Invoiced net sums of the 12 months one year before. */
    val prevYear: List<BigDecimal>,
    /** Invoiced net sums of the 12 months two years before. */
    val prevPrevYear: List<BigDecimal>,
    /**
     * The plan per month (row 9 of Forecast_Data = row 7 of Planning_Data): max of the planning forecast and the
     * invoices before the planning date. Null, if no planning date was given.
     */
    val plan: List<BigDecimal>?,
    /** The date of the order book snapshot actually used as plan (the closest one to the requested date). */
    val planningDate: LocalDate?,
) {
    /**
     * Per month the max of IST and the remaining forecast ('Gesamt' of sheet 'Umsatz kumuliert'), the base of the
     * cumulated forecast curve. A stored field (not a computed getter), because the REST json mapper serializes
     * fields only.
     */
    val total: List<BigDecimal> = List(ist.size) { i ->
        val forecast = forecastByStatus.values.fold(BigDecimal.ZERO) { acc, values -> acc + values[i] }
        ist[i].max(forecast)
    }

    /**
     * The fields of the list filter the charts were asked for but did not apply, set by the REST layer
     * (`OrderEntityRest.forecastFilterUsage`), so the charts tab can show which of the list's criteria hold.
     */
    var ignoredFilterFields: List<String> = emptyList()

    /** The fields of the list filter replaced by a parameter of the charts (the period of performance). */
    var replacedFilterFields: List<String> = emptyList()

    /** The fields of the list filter applied only in part (the first of several payment types). */
    var partialFilterFields: List<String> = emptyList()
}

/**
 * Accumulates the month values while the forecast sheets are filled. Plan and invoice values are kept per project,
 * because the Excel only sums rows of projects shown in the forecast sheet (the visible/visibleID COUNTIF formulas);
 * [build] applies the same rule.
 */
internal class ForecastChartTotals {
    enum class InvoiceKind { IST, PLANNING, PREV_YEAR, PREV_PREV_YEAR }

    private val forecastByStatus = ForecastUtils.auftragsPositionsStatusToShow.associateWith { newMonths() }
    private val planningForecastByProject = mutableMapOf<Long?, Array<BigDecimal>>()
    private val invoicesByProject = InvoiceKind.entries.associateWith { mutableMapOf<Long?, Array<BigDecimal>>() }

    fun addForecast(status: AuftragsStatus, monthIndex: Int, value: BigDecimal) {
        forecastByStatus[status]?.add(monthIndex, value) // Other statuses aren't part of the Excel sums either.
    }

    fun addPlanningForecast(status: AuftragsStatus, projectId: Long?, monthIndex: Int, value: BigDecimal) {
        if (status !in forecastByStatus) {
            return
        }
        planningForecastByProject.getOrPut(projectId) { newMonths() }.add(monthIndex, value)
    }

    fun addInvoice(kind: InvoiceKind, projectId: Long?, monthIndex: Int, value: BigDecimal?) {
        value ?: return
        invoicesByProject[kind]!!.getOrPut(projectId) { newMonths() }.add(monthIndex, value)
    }

    /**
     * @param startDate The first month of the forecast.
     * @param visibleProjectIds The project ids of the forecast sheet rows (the visibleID column).
     * @param planningDate The planning date used, or null if no plan was calculated.
     */
    fun build(startDate: PFDay, visibleProjectIds: Set<Long>, planningDate: LocalDate?): ForecastChartData {
        fun sumVisible(map: Map<Long?, Array<BigDecimal>>): Array<BigDecimal> {
            val result = newMonths()
            map.filterKeys { it != null && it in visibleProjectIds }.values.forEach { values ->
                values.forEachIndexed { i, value -> result.add(i, value) }
            }
            return result
        }
        val plan = planningDate?.let {
            val planningForecast = sumVisible(planningForecastByProject)
            val planningIst = sumVisible(invoicesByProject[InvoiceKind.PLANNING]!!)
            List(MONTHS) { i -> planningForecast[i].max(planningIst[i]) }
        }
        return ForecastChartData(
            months = List(MONTHS) { i ->
                val month = startDate.plusMonths(i.toLong())
                "${month.year}-${month.monthValue.toString().padStart(2, '0')}"
            },
            forecastByStatus = forecastByStatus.mapValues { it.value.toList() },
            ist = sumVisible(invoicesByProject[InvoiceKind.IST]!!).toList(),
            prevYear = sumVisible(invoicesByProject[InvoiceKind.PREV_YEAR]!!).toList(),
            prevPrevYear = sumVisible(invoicesByProject[InvoiceKind.PREV_PREV_YEAR]!!).toList(),
            plan = plan,
            planningDate = planningDate,
        )
    }

    private fun Array<BigDecimal>.add(monthIndex: Int, value: BigDecimal) {
        if (monthIndex in indices) {
            this[monthIndex] += value
        }
    }

    companion object {
        private const val MONTHS = 12
        private fun newMonths() = Array(MONTHS) { BigDecimal.ZERO }
    }
}
