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

import org.projectforge.framework.i18n.translate
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The detail rows behind the forecast charts (the sheets Projektübersicht, Forecast_Data and Rechnungen /
 * Rechnungen Vorjahr / Rechnungen Vorvorjahr of the Excel export), collected while the pipeline runs (see
 * [ForecastTablesCollector]). Only rows of projects shown in the forecast are part of it, the same rule the charts
 * follow (see [ForecastChartTotals.build]).
 */
class ForecastTables(
    /** The 12 months of the forecast as `yyyy-MM`, see [ForecastChartData.months]. */
    val months: List<String>,
    val projects: List<ForecastProjectRow>,
    val positions: List<ForecastPositionRow>,
    val invoices: List<ForecastInvoiceRow>,
) {
    companion object {
        val EMPTY = ForecastTables(emptyList(), emptyList(), emptyList(), emptyList())
    }
}

/** The result of [ForecastExport.statistics]: the charts and the rows behind them. */
class ForecastStatistics(val chart: ForecastChartData, val tables: ForecastTables)

/** One row of the project overview: the sums of one project. */
class ForecastProjectRow(
    /** [ForecastExportContext.PROJECT_ID_NONE] for the invoices without any project. */
    val projectId: Long,
    val customer: String?,
    val project: String?,
    /** The remaining forecast plus the invoices (IST) of the 12 months. */
    val forecast: BigDecimal,
    /** Null, if no plan was calculated. */
    val plan: BigDecimal?,
    val prevYear: BigDecimal,
    val prevPrevYear: BigDecimal,
)

/** One order position of the forecast (one row of Forecast_Data). */
class ForecastPositionRow(
    /** Null for the pseudo rows (invoices without order resp. without project). */
    val orderId: Long?,
    val orderNumber: Int?,
    val positionNumber: Short?,
    val projectId: Long?,
    val customer: String?,
    val project: String?,
    val title: String?,
    /** Only given, if it differs from the order's title. */
    val positionTitle: String?,
    val art: String?,
    val paymentType: String?,
    val orderStatus: String,
    val positionStatus: String,
    val personDays: BigDecimal?,
    val netSum: BigDecimal,
    val probability: BigDecimal,
    val weightedNetSum: BigDecimal,
    val invoicedSum: BigDecimal,
    val toBeInvoicedSum: BigDecimal,
    val periodOfPerformanceBegin: LocalDate?,
    val periodOfPerformanceEnd: LocalDate?,
    val forecastType: String,
    /** The 12 months of the remaining forecast, null for no value (absolute value below 1, as in the Excel). */
    val months: List<BigDecimal?>,
    /** The remaining forecast after the 12 months. */
    val remaining: BigDecimal,
    val difference: BigDecimal,
    val warning: String?,
    /** True for the rows representing invoices without order or without project. */
    val pseudo: Boolean,
)

/** One invoice position (one row of the invoice sheets). */
class ForecastInvoiceRow(
    val invoiceId: Long?,
    val invoiceNumber: Int?,
    val positionNumber: Short?,
    val date: LocalDate?,
    /** [ForecastExportContext.PROJECT_ID_NONE] for invoices without any project. */
    val projectId: Long,
    val customer: String?,
    val project: String?,
    val subject: String?,
    val positionText: String?,
    val orderId: Long?,
    /** The order position as `<order number>.<position number>`. */
    val order: String?,
    val netSum: BigDecimal,
    /** Index 0..11 of the month within the 12 months of [kind]. */
    val monthIndex: Int,
    val kind: Kind,
) {
    enum class Kind { IST, PREV_YEAR, PREV_PREV_YEAR }
}

/**
 * Collects the rows of [ForecastTables] while the forecast sheets are filled. The per-project plan sums are taken
 * from [ForecastChartTotals], which collects them anyway.
 */
internal class ForecastTablesCollector {
    private val positions = mutableListOf<ForecastPositionRow>()
    private val invoices = mutableListOf<ForecastInvoiceRow>()

    fun addPosition(row: ForecastPositionRow) {
        positions.add(row)
    }

    fun addInvoice(row: ForecastInvoiceRow) {
        invoices.add(row)
    }

    /**
     * @param visibleProjectIds The project ids of the forecast sheet rows, see [ForecastChartTotals.build].
     * @param planByProject The plan per project, null if no plan was calculated.
     * @param labelOf Customer and project name of a project without any row carrying them.
     */
    fun build(
        months: List<String>,
        visibleProjectIds: Set<Long>,
        planByProject: Map<Long, BigDecimal>?,
        labelOf: (projectId: Long) -> Pair<String?, String?>?,
    ): ForecastTables {
        val visibleInvoices = invoices.filter { it.projectId in visibleProjectIds }
        return ForecastTables(
            months = months,
            projects = projects(visibleProjectIds, visibleInvoices, planByProject, labelOf),
            positions = positions.toList(),
            invoices = visibleInvoices,
        )
    }

    private class ProjectSums {
        var customer: String? = null
        var project: String? = null
        var forecast: BigDecimal = BigDecimal.ZERO
        var prevYear: BigDecimal = BigDecimal.ZERO
        var prevPrevYear: BigDecimal = BigDecimal.ZERO

        fun label(customer: String?, project: String?) {
            if (this.customer.isNullOrBlank()) this.customer = customer
            if (this.project.isNullOrBlank()) this.project = project
        }
    }

    /** The same sums as the sheet Projektübersicht, see [ForecastExport.fillProjectOverviewSheet]. */
    private fun projects(
        visibleProjectIds: Set<Long>,
        visibleInvoices: List<ForecastInvoiceRow>,
        planByProject: Map<Long, BigDecimal>?,
        labelOf: (projectId: Long) -> Pair<String?, String?>?,
    ): List<ForecastProjectRow> {
        val map = mutableMapOf<Long, ProjectSums>()
        positions.forEach { row ->
            // Unlike the Excel sheet, positions of orders without project are summed up with the invoices without
            // project, so the overview adds up to the charts.
            val projectId = row.projectId ?: ForecastExportContext.PROJECT_ID_NONE
            val sums = map.getOrPut(projectId) { ProjectSums() }
            row.months.forEach { value -> value?.let { sums.forecast += it } }
            if (row.projectId != null) {
                sums.label(row.customer, row.project)
            } else {
                sums.label(null, translate("fibu.auftrag.forecast.withoutProject"))
            }
        }
        visibleInvoices.forEach { row ->
            val sums = map.getOrPut(row.projectId) { ProjectSums() }
            when (row.kind) {
                ForecastInvoiceRow.Kind.IST -> sums.forecast += row.netSum
                ForecastInvoiceRow.Kind.PREV_YEAR -> sums.prevYear += row.netSum
                ForecastInvoiceRow.Kind.PREV_PREV_YEAR -> sums.prevPrevYear += row.netSum
            }
            sums.label(row.customer, row.project)
        }
        planByProject?.keys?.filter { it in visibleProjectIds }?.forEach { map.getOrPut(it) { ProjectSums() } }
        map.forEach { (id, sums) ->
            val incomplete = sums.customer.isNullOrBlank() || sums.project.isNullOrBlank()
            if (id != ForecastExportContext.PROJECT_ID_NONE && incomplete) {
                labelOf(id)?.let { (customer, project) -> sums.label(customer, project) }
            }
        }
        return map.entries
            .sortedWith(compareBy({ it.value.customer ?: "" }, { it.value.project ?: "" }))
            .map { (id, sums) ->
                ForecastProjectRow(
                    projectId = id,
                    customer = sums.customer,
                    project = sums.project,
                    forecast = sums.forecast,
                    plan = planByProject?.let { it[id] ?: BigDecimal.ZERO },
                    prevYear = sums.prevYear,
                    prevPrevYear = sums.prevPrevYear,
                )
            }
    }
}
