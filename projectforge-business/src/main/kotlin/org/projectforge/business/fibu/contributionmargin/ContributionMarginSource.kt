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

import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.scripting.support.BookedInvoices
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The contribution margin amounts of projects in [from]..[until], as loaded by
 * [ContributionMarginService.load]: the accounting records, and for the days from [preliminaryBegin] on the
 * unbooked invoices and the time sheets × [hourlyRate]. Shared by the order book's contribution margin tab
 * and scripts, so both count the same amounts.
 */
class ContributionMarginSource(
  val entries: List<ContributionMarginEntry>,
  val from: LocalDate,
  val until: LocalDate,
  /** The last day of the last month with imported accounting records, or null if there are none. */
  val bookingImportEnd: LocalDate?,
  /** The first day whose values are preliminary, or null if the accounting records cover the whole range. */
  val preliminaryBegin: LocalDate?,
  /** The configured hourly rate of the time sheets, or null if not configured (no time sheet costs). */
  val hourlyRate: BigDecimal?,
  /** The invoices found in the accounting records. */
  val bookedInvoices: BookedInvoices,
) {
  /** Whether the invoice counts as preliminary revenue, see [ContributionMarginCalculator.isPreliminaryRevenue]. */
  fun isPreliminaryRevenue(invoice: RechnungDO): Boolean {
    return ContributionMarginCalculator.isPreliminaryRevenue(invoice, preliminaryBegin, until, bookedInvoices)
  }
}
