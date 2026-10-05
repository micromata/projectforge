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
 * The rows behind the contribution margin of the period (the sheets Monats-DB, Rechnungen and DB-Zeitberichte of
 * the DB Excel): the sums per month and project, the invoices and the time sheet costs. Only the period itself,
 * not the comparison years.
 */
class ContributionMarginDetails(
  val months: List<ContributionMarginMonthRow>,
  val invoices: List<ContributionMarginInvoiceRow>,
  val timesheets: List<ContributionMarginTimesheetRow>,
) {
  companion object {
    val EMPTY = ContributionMarginDetails(emptyList(), emptyList(), emptyList())
  }
}

/** The result of [ContributionMarginService.calculateWithDetails]. */
class ContributionMarginResult(val data: ContributionMarginData, val details: ContributionMarginDetails)

/** The sums of a project in a month. */
class ContributionMarginMonthRow(
  /** `yyyy-MM`. */
  val month: String,
  val projectId: Long,
  val kost: String?,
  val customer: String?,
  val project: String?,
  val revenue: BigDecimal,
  /** Positive amounts, as [ContributionMarginProject.costs]. */
  val costs: BigDecimal,
  val profit: BigDecimal,
  val percentage: BigDecimal?,
  /** True, if the month contains preliminary values (unbooked invoices, time sheets). */
  val preliminary: Boolean,
)

/** One invoice position of a project in the period. */
class ContributionMarginInvoiceRow(
  val invoiceId: Long?,
  val date: LocalDate?,
  val number: Int?,
  val positionNumber: Short?,
  val projectId: Long?,
  val kost: String?,
  val customer: String?,
  val project: String?,
  val subject: String?,
  val netSum: BigDecimal,
  val status: String?,
  val orderId: Long?,
  /** The order position as `<order number>.<position number>`. */
  val order: String?,
  /** The date of the accounting record, null if not booked (yet). */
  val bookedDate: LocalDate?,
  /** True, if the invoice counts as preliminary revenue (not booked yet). */
  val preliminary: Boolean,
)

/** The time sheet costs (hours × hourly rate) of a kost2 in a month, preliminary by definition. */
class ContributionMarginTimesheetRow(
  /** `yyyy-MM`. */
  val month: String,
  val projectId: Long,
  val kost2: String?,
  val customer: String?,
  val project: String?,
  val hours: BigDecimal?,
  /** Positive amount. */
  val costs: BigDecimal,
)
