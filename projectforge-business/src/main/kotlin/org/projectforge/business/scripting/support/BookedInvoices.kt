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

package org.projectforge.business.scripting.support

import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.framework.time.PFDay

/**
 * Index of the outgoing invoices that already appear in the accounting records, by their document number
 * ([BuchungssatzDO.beleg]). An invoice may be booked into another month than its date (e.g. the month of
 * service), so scripts use this index to not count it a second time as preliminary revenue.
 *
 * Only records on revenue accounts are considered, so the document number of a cost or payment record
 * can't accidentally match an invoice number.
 */
class BookedInvoices(records: Collection<BuchungssatzDO>, revenueAccounts: IntRange = REVENUE_ACCOUNTS) {
  /** Document number (e.g. `17209` or `17209-S`) → earliest booking date. */
  val bookingDates: Map<String, PFDay>

  init {
    val map = mutableMapOf<String, PFDay>()
    records.forEach { record ->
      val date = record.datum ?: return@forEach
      val account = record.konto?.nummer ?: return@forEach
      if (account !in revenueAccounts) {
        return@forEach
      }
      val day = PFDay.from(date)
      tokens(record.beleg).forEach { token ->
        val existing = map[token]
        if (existing == null || day < existing) {
          map[token] = day
        }
      }
    }
    bookingDates = map
  }

  val size: Int
    get() = bookingDates.size

  /**
   * The earliest booking date of the given invoice, or null if it isn't booked (yet). Matched by
   * [RechnungDO.belegNummer], so a cancellation invoice matches only its own `-S` number, not the original's.
   */
  fun bookedDate(invoice: RechnungDO): PFDay? {
    val number = invoice.belegNummer ?: return null
    return bookingDates[number.uppercase()]
  }

  fun isBooked(invoice: RechnungDO): Boolean = bookedDate(invoice) != null

  companion object {
    val REVENUE_ACCOUNTS = 4000..4799

    // Cancellations are booked as `17209-S`, older ones also as `17209S`.
    private val TOKEN_REGEX = Regex("(\\d+)(-?S)?")

    /**
     * The invoice numbers a document number may contain: `17209`, `RE17209`, `17209/1` → `17209`;
     * `17209-S`, `17209S` → `17209-S`.
     */
    fun tokens(beleg: String?): List<String> {
      beleg ?: return emptyList()
      return TOKEN_REGEX.findAll(beleg.uppercase()).map { match ->
        val number = match.groupValues[1]
        if (match.groupValues[2].isEmpty()) number else "$number${RechnungDO.CANCELLATION_SUFFIX}"
      }.toList()
    }
  }
}
