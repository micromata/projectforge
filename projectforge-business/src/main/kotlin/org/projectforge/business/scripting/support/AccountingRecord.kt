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
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.TimePeriod
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Helper class for working with financial data and aggregating them.
 */
open class AccountingRecord(
  var date: PFDay,
  val businessUnit: String?,
  val customer: String?,
  val customerGroup: String,
  val project: String,
  val projectId: Long,
  val kost2String: String,
  val cost2: Int? = null,
  val account: Int? = null,
  val projectManagerGroup: String? = null,
  var revenue: BigDecimal = BigDecimal.ZERO,
  var costs: BigDecimal = BigDecimal.ZERO,
  var text: String? = null,
  var type: TYPE = TYPE.RECORD
) {
  enum class TYPE { RECORD, INVOICE, TIMESHEET }

  constructor(record: AccountingRecord) : this(
    date = record.date,
    businessUnit = record.businessUnit,
    customer = record.customer,
    customerGroup = record.customerGroup,
    project = record.project,
    projectId = record.projectId,
    kost2String = record.kost2String,
    cost2 = record.cost2,
    account = record.account,
    projectManagerGroup = record.projectManagerGroup,
  )

  val year: Int
    get() = date.year

  val month: Int
    get() = date.monthValue

  val profit
    get() = revenue + costs // Costs are given as negative amounts.

  val percentage
    get() = if (revenue > BigDecimal.ZERO && profit > BigDecimal.ZERO) {
      profit.divide(revenue, 2, RoundingMode.HALF_UP)
    } else {
      BigDecimal.ZERO
    }

  fun toKey(): AccountingRecordMonthKey {
    return AccountingRecordMonthKey(projectId, year, month)
  }

  fun add(record: AccountingRecord): AccountingRecord {
    revenue += record.revenue
    costs += record.costs
    return this
  }

  companion object {
    fun groupByProject(records: List<AccountingRecord>, von: PFDay, bis: PFDay): Map<Long, AccountingRecord> {
      return records.filter { it.date in von..bis }.groupingBy { it.projectId }
        .aggregateTo(mutableMapOf()) { _, accumulator: AccountingRecord?, element, first ->
          if (first) // first element
            AccountingRecord(element).add(element)
          else
            accumulator!!.add(element)
        }
    }

    fun groupByMonth(records: List<AccountingRecord>): Map<AccountingRecordMonthKey, AccountingRecord> {
      return records.groupingBy { it.toKey() }
        .aggregateTo(mutableMapOf()) { _, accumulator: AccountingRecord?, element, first ->
          if (first) // first element
            AccountingRecord(element).add(element)
          else
            accumulator!!.add(element)
        }
    }

    /**
     * Erzeuge eine ProjektBuchung aus einem Buchungssatz.
     */
    fun create(record: BuchungssatzDO, debits: Boolean, businessUnit: String, customerGroup: String): AccountingRecord {
      val date = PFDay.from(record.datum!!)
      val cost2 = record.kost2!!
      val projectId = cost2.projekt!!.id!!
      val kost2String = cost2.formattedNumber
      val project = cost2.projekt!!
      val customerName = project.kunde?.name ?: "---"
      var revenue = BigDecimal.ZERO
      var costs = BigDecimal.ZERO
      val amount = record.betrag
      if (amount != null) {
        if (debits) {
          revenue = amount
        } else {
          costs = amount // Betrag ist bereits als Ausgabe negativ, als Einnahme (z. B. Gutschrift) positiv.
        }
      }
      return AccountingRecord(
        date = date,
        businessUnit = businessUnit,
        customer = customerName,
        customerGroup = customerGroup,
        project = project.name ?: "???",
        projectId = projectId,
        kost2String = kost2String,
        revenue = revenue,
        costs = costs,
        cost2 = cost2.nummer,
        account = record.konto?.nummer,
        projectManagerGroup = project.projektManagerGroup?.name
      )
    }

    fun create(invoice: RechnungDO, businessUnit: String, customerGroup: String): AccountingRecord {
      return AccountingRecord(
        PFDay.fromOrNow(invoice.datum),
        businessUnit = businessUnit,
        customer = invoice.projekt?.kunde?.name ?: invoice.kundeAsString,
        customerGroup = customerGroup,
        project = invoice.projekt?.name ?: "???",
        projectId = invoice.projekt!!.id!!,
        kost2String = invoice.projekt!!.kost,
        revenue = invoice.info.netSum,
        projectManagerGroup = invoice.projekt?.projektManagerGroup?.name,
        text = "Invoice #${invoice.nummer}",
        type = TYPE.INVOICE,
      )
    }

    fun create(ts: TimesheetDO, businessUnit: String, customerGroup: String, hourlyRate: BigDecimal): AccountingRecord {
      val project = ts.kost2!!.projekt!!
      val projectName = project.name ?: "---"
      val customerName = project.kunde?.name ?: "---"
      return AccountingRecord(
        PFDay.from(ts.startTime!!),
        businessUnit = businessUnit,
        customer = customerName,
        customerGroup = customerGroup,
        project = projectName,
        projectId = project.id!!,
        kost2String = ts.kost2!!.formattedNumber,
        costs = -BigDecimal(ts.duration).divide(TimePeriod.MILLIS_PER_HOUR, 2, RoundingMode.HALF_UP)
          .multiply(hourlyRate)
          .setScale(2, RoundingMode.HALF_UP),
        projectManagerGroup = project.projektManagerGroup?.name,
        text = "Timesheet ${ts.user?.getFullname()}: ${PFDay.from(ts.startTime!!).format()}",
        type = TYPE.TIMESHEET,
      )
    }
  }
}
