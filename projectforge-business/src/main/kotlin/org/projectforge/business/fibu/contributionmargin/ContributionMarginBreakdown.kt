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

/** What the contribution margin is broken down by below the projects. */
enum class ContributionMarginLevel {
  /** Orders and their positions. */
  ORDER,

  /** Tasks (Strukturelemente) below the task of the project. */
  TASK,
}

enum class ContributionMarginRowType {
  PROJECT,
  ORDER,
  ORDER_POSITION,
  TASK,

  /** What isn't assignable to an order or task of the project, see [ContributionMarginResidual]. */
  RESIDUAL,
}

/** The monthly values of the 12 months of the period, as in [ContributionMarginData], and the sums. */
class ContributionMarginSeries(
  val revenue: List<BigDecimal>,
  /** Positive amounts. */
  val costs: List<BigDecimal>,
  val profit: List<BigDecimal>,
  /** Cumulated from the first month on, see [ContributionMarginData.percentage]. */
  val percentage: List<BigDecimal?>,
  /** Contribution margin of the same months one year earlier. */
  val prevYear: List<BigDecimal>,
  val prevYearPercentage: List<BigDecimal?>,
  /** The sums of the period and the comparison years. */
  val sums: ContributionMarginSums,
)

/**
 * What the residual of a project consists of in the period: the residual is the contribution margin of the
 * project (accounting records, preliminary values) less the calculated ones of its orders or tasks.
 */
class ContributionMarginResidual(
  /** Revenue of the invoice positions without an order position (or, by tasks, without a task). */
  val invoicesWithoutAssignment: BigDecimal,
  /** Costs (positive) of the time sheets without an order position. Always 0 by tasks. */
  val timesheetsWithoutAssignment: BigDecimal,
  /**
   * The rest: the difference between the accounting records and the calculated values, e.g. other costs on the
   * kost2 of the project (external services, travel expenses), salaries instead of the hourly rate, invoices
   * booked on other projects.
   */
  val accountingDifference: BigDecimal,
)

/**
 * One row of the breakdown, in tree order: a project, followed by its orders (each followed by its positions)
 * or by its task tree, and its residual.
 */
class ContributionMarginBreakdownRow(
  /** Unique within the breakdown, e.g. `p12/o34/op56`. */
  val id: String,
  /** The id of the parent row, null for a project. */
  val parentId: String?,
  val type: ContributionMarginRowType,
  /** 0 for a project. */
  val depth: Int,
  val hasChildren: Boolean,
  val projectId: Long,
  /** The order of an order or order position row. */
  val orderId: Long? = null,
  val orderPositionId: Long? = null,
  val taskId: Long? = null,
  /** E.g. the number and title of the order, the title of the task; null for the residual (translated by the client). */
  val label: String? = null,
  /** The cost number of the project (project rows only). */
  val kost: String? = null,
  val customer: String? = null,
  val projectGroup: String? = null,
  val series: ContributionMarginSeries,
  /** Residual rows only. */
  val residual: ContributionMarginResidual? = null,
)

/** The result of [ContributionMarginService.calculateBreakdown]. */
class ContributionMarginBreakdown(
  val level: ContributionMarginLevel,
  /** As [ContributionMarginData.months] and the following ones, for the chart of a row. */
  val months: List<String>,
  val preliminary: List<Boolean>,
  val lastMonth: String?,
  val valuesEnd: LocalDate?,
  /** The flat hourly rate time sheets are valued with, null if none is configured (no time sheet costs at all). */
  val hourlyRate: BigDecimal?,
  val targetPercentage: Int,
  val redThreshold: Int,
  val rows: List<ContributionMarginBreakdownRow>,
)
