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

package org.projectforge.business.fibu.contributionmargin

import java.math.BigDecimal
import java.time.LocalDate

/**
 * The contribution margin (DB1) of a set of projects over 12 months, with the same months of the two
 * previous years for comparison (see [ContributionMarginService.calculate]).
 *
 * All amounts are net. Costs are given as positive amounts, so profit = revenue - costs.
 */
class ContributionMarginData(
  /** The 12 months of the period as "yyyy-MM". */
  val months: List<String>,
  /** Revenue per month of [months]. */
  val revenue: List<BigDecimal>,
  /** Costs per month of [months] (positive amounts). */
  val costs: List<BigDecimal>,
  /** Contribution margin per month of [months]. */
  val profit: List<BigDecimal>,
  /**
   * Contribution margin in % of the revenue per month of [months], cumulated from the first month on (one
   * decimal, 0 for a loss), null as long as there is no positive revenue.
   */
  val percentage: List<BigDecimal?>,
  /**
   * Per month of [months]: whether the month lies after the last imported accounting records, so its
   * values are preliminary (unbooked invoices as revenue, time sheets × hourly rate as costs).
   */
  val preliminary: List<Boolean>,
  /**
   * The last month of [months] with values ("yyyy-MM"): the previous month at the latest, as the current one
   * isn't complete yet. Null if the period begins in the current month or later.
   */
  val lastMonth: String?,
  /** The last day the sums of the period cover ([lastMonth]'s end), or null if it has no values yet. */
  val valuesEnd: LocalDate?,
  /**
   * The last day the sums of the same months one year earlier cover: the end of those 12 months, the end of the
   * previous month at the latest (not cut at [valuesEnd]). Null if they have no values yet.
   */
  val prevYearValuesEnd: LocalDate?,
  /** The same as [prevYearValuesEnd] for the months two years earlier. */
  val prevPrevYearValuesEnd: LocalDate?,
  /** Contribution margin of the same months one year earlier. */
  val prevYear: List<BigDecimal>,
  /** Contribution margin in % of the same months one year earlier, cumulated like [percentage]. */
  val prevYearPercentage: List<BigDecimal?>,
  /** Contribution margin of the same months two years earlier. */
  val prevPrevYear: List<BigDecimal>,
  /** One row per project with any amount in the period or the two previous ones, sorted by customer and project. */
  val projects: List<ContributionMarginProject>,
  /** The sums over all [projects]. */
  val total: ContributionMarginSums,
  /** The last day of the last imported month of the accounting records, or null if there are none at all. */
  val bookingImportEnd: LocalDate?,
  /** The flat hourly rate time sheets are valued with, or null if none is configured. */
  val hourlyRate: BigDecimal?,
  /** True if preliminary months exist, but no hourly rate is configured, so their costs lack the time sheets. */
  val hourlyRateMissing: Boolean,
  /** The target contribution margin in %: green from here on, yellow below (see [redThreshold]). */
  val targetPercentage: Int,
  /** The contribution margin in % below which it is shown red. */
  val redThreshold: Int,
) {
  /** Number of the filtered orders without a project, which can't be taken into account. */
  var ordersWithoutProject: Int = 0

  /** Set by the REST layer: the list's filter criteria not applied at all. */
  var ignoredFilterFields: List<String> = emptyList()

  /** Set by the REST layer: the list's filter criteria replaced by the start date. */
  var replacedFilterFields: List<String> = emptyList()
}

/**
 * Revenue, costs and contribution margin of the period, and the contribution margin of the same period of
 * the two previous years.
 */
open class ContributionMarginSums(
  val revenue: BigDecimal,
  /** Positive amount. */
  val costs: BigDecimal,
  val prevYearProfit: BigDecimal,
  val prevPrevYearProfit: BigDecimal,
  /** Revenue of the same period one year earlier. */
  val prevYearRevenue: BigDecimal,
  /** Revenue of the same period two years earlier. */
  val prevPrevYearRevenue: BigDecimal,
) {
  // Stored, not computed getters: the REST object mapper serializes fields only.
  val profit: BigDecimal = revenue - costs

  /** Contribution margin in % of the revenue (one decimal), or null without positive revenue. */
  val percentage: BigDecimal? = ContributionMarginCalculator.percentage(revenue, profit)

  /** Contribution margin in % of the revenue of the same period one year earlier, like [percentage]. */
  val prevYearPercentage: BigDecimal? = ContributionMarginCalculator.percentage(prevYearRevenue, prevYearProfit)

  /** Contribution margin in % of the revenue of the same period two years earlier, like [percentage]. */
  val prevPrevYearPercentage: BigDecimal? =
    ContributionMarginCalculator.percentage(prevPrevYearRevenue, prevPrevYearProfit)
}

class ContributionMarginProject(
  val projectId: Long,
  /** The cost number of the project, e.g. 5.123.04. */
  val kost: String?,
  val customer: String?,
  val project: String?,
  revenue: BigDecimal,
  costs: BigDecimal,
  prevYearProfit: BigDecimal,
  prevPrevYearProfit: BigDecimal,
  prevYearRevenue: BigDecimal,
  prevPrevYearRevenue: BigDecimal,
) : ContributionMarginSums(revenue, costs, prevYearProfit, prevPrevYearProfit, prevYearRevenue, prevPrevYearRevenue)
