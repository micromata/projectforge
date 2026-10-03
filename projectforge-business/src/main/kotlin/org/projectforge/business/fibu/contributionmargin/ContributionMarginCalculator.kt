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
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.scripting.support.BookedInvoices
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/** Where an amount of the contribution margin comes from. */
enum class ContributionMarginEntryType {
  /** An imported accounting record (final). */
  RECORD,

  /** An unbooked invoice of a month without imported accounting records (preliminary revenue). */
  INVOICE,

  /** A time sheet × hourly rate of a month without imported accounting records (preliminary costs). */
  TIMESHEET,
}

/**
 * An amount of the contribution margin: revenue (positive) or costs (negative as booked, a credit positive)
 * of a project at a date.
 */
class ContributionMarginEntry(
  val projectId: Long,
  val date: LocalDate,
  val revenue: BigDecimal = BigDecimal.ZERO,
  val costs: BigDecimal = BigDecimal.ZERO,
  val type: ContributionMarginEntryType = ContributionMarginEntryType.RECORD,
  /** The kost2 of the accounting record or time sheet; null for invoices. */
  val kost2Id: Long? = null,
  /** The account number of the accounting record. */
  val account: Int? = null,
  /** A description of the invoice or time sheet. */
  val text: String? = null,
)

/** What a row of the project table shows of a project beside its amounts. */
class ContributionMarginProjectInfo(val kost: String?, val customer: String?, val project: String?)

/**
 * The calculation of [ContributionMarginData], free of any data base access (see [ContributionMarginService]
 * for loading the data).
 *
 * The period are the 12 months from the month of [startDate] on; the comparison periods are the same months
 * one and two years earlier.
 *
 * Values of the accounting records are final. For the months after [bookingImportEnd], the preliminary
 * entries (unbooked invoices, time sheets × hourly rate) are added, see [invoiceEntries] and [timesheetCosts].
 * They are loaded by [ContributionMarginService.load].
 *
 * The calculation ends with the month before [today]: the current month isn't complete yet, so there are no
 * values for it nor for the following months (see [calculationEnd]).
 */
class ContributionMarginCalculator(startDate: LocalDate, val bookingImportEnd: LocalDate?, today: LocalDate) {
  val start: YearMonth = YearMonth.from(startDate)

  val months: List<YearMonth> = (0L until 12L).map { start.plusMonths(it) }

  val periodBegin: LocalDate = start.atDay(1)

  val periodEnd: LocalDate = months.last().atEndOfMonth()

  /** First day of the comparison period, two years before [periodBegin]. */
  val prevPrevYearBegin: LocalDate = start.minusYears(2).atDay(1)

  /** The last day of the period with values: the end of the previous month at the latest. */
  val periodValuesEnd: LocalDate = calculationEnd(periodEnd, today)

  /** The first day whose values are preliminary, or null if the accounting records cover the whole period. */
  val preliminaryBegin: LocalDate?
    get() = preliminaryBegin(bookingImportEnd, periodBegin, periodValuesEnd)

  fun calculate(
    entries: Collection<ContributionMarginEntry>,
    projectInfo: (Long) -> ContributionMarginProjectInfo?,
    hourlyRate: BigDecimal?,
    config: ContributionMarginConfig,
  ): ContributionMarginData {
    // yearsBack (0..2) → month → project id → sums
    val sums = Array(3) { mutableMapOf<YearMonth, MutableMap<Long, Amount>>() }
    entries.forEach { entry ->
      val month = YearMonth.from(entry.date)
      val yearsBack = (0..2).firstOrNull { month.plusYears(it.toLong()) in months } ?: return@forEach
      sums[yearsBack].getOrPut(month.plusYears(yearsBack.toLong())) { mutableMapOf() }
        .getOrPut(entry.projectId) { Amount() }
        .add(entry)
    }
    fun monthly(yearsBack: Int) = months.map { month ->
      sums[yearsBack][month]?.values?.fold(Amount()) { acc, amount -> acc.add(amount) } ?: Amount()
    }
    fun byProject(yearsBack: Int): Map<Long, Amount> = sums[yearsBack].values
      .flatMap { it.entries }
      .groupingBy { it.key }
      // A new accumulator per project: fold(Amount()) would share one mutable instance between all projects.
      .fold({ _, _ -> Amount() }) { _, acc, entry -> acc.add(entry.value) }

    val current = monthly(0)
    val currentByProject = byProject(0)
    val prevByProject = byProject(1)
    val prevPrevByProject = byProject(2)
    val projects = (currentByProject.keys + prevByProject.keys + prevPrevByProject.keys).map { projectId ->
      val info = projectInfo(projectId)
      val amount = currentByProject[projectId] ?: Amount()
      ContributionMarginProject(
        projectId = projectId,
        kost = info?.kost,
        customer = info?.customer,
        project = info?.project,
        revenue = amount.revenue,
        costs = -amount.costs,
        prevYearProfit = prevByProject[projectId]?.profit ?: BigDecimal.ZERO,
        prevPrevYearProfit = prevPrevByProject[projectId]?.profit ?: BigDecimal.ZERO,
        prevYearRevenue = prevByProject[projectId]?.revenue ?: BigDecimal.ZERO,
      )
    }.sortedWith(compareBy<ContributionMarginProject>({ it.customer ?: "" }, { it.project ?: "" }, { it.kost ?: "" }))
    val preliminaryBegin = preliminaryBegin
    // The months after the calculation end have no values (yet).
    val hasValues = months.map { it.atDay(1) <= periodValuesEnd }
    val prevYear = monthly(1)
    return ContributionMarginData(
      months = months.map { it.toString() },
      revenue = current.map { it.revenue },
      costs = current.map { -it.costs },
      profit = current.map { it.profit },
      percentage = cumulativePercentage(current).mapIndexed { i, percentage -> percentage.takeIf { hasValues[i] } },
      preliminary = months.mapIndexed { i, month ->
        hasValues[i] && preliminaryBegin != null && month.atEndOfMonth() >= preliminaryBegin
      },
      lastMonth = months.lastOrNull { it.atDay(1) <= periodValuesEnd }?.toString(),
      prevYear = prevYear.map { it.profit },
      prevYearPercentage = cumulativePercentage(prevYear),
      prevPrevYear = monthly(2).map { it.profit },
      projects = projects,
      total = ContributionMarginSums(
        revenue = projects.sumOf { it.revenue },
        costs = projects.sumOf { it.costs },
        prevYearProfit = projects.sumOf { it.prevYearProfit },
        prevPrevYearProfit = projects.sumOf { it.prevPrevYearProfit },
        prevYearRevenue = projects.sumOf { it.prevYearRevenue },
      ),
      bookingImportEnd = bookingImportEnd,
      hourlyRate = hourlyRate,
      hourlyRateMissing = preliminaryBegin != null && hourlyRate == null,
      targetPercentage = config.targetPercentage,
      redThreshold = config.redThreshold,
    )
  }

  /**
   * The DB % of each month cumulated from the begin of the period on (revenue and profit summed up to and
   * including the month), so it settles in the course of the period instead of jumping from month to month.
   */
  private fun cumulativePercentage(monthly: List<Amount>): List<BigDecimal?> {
    val sum = Amount()
    return monthly.map { sum.add(it); percentage(sum.revenue, sum.profit) }
  }

  private class Amount(var revenue: BigDecimal = BigDecimal.ZERO, var costs: BigDecimal = BigDecimal.ZERO) {
    val profit: BigDecimal
      get() = revenue + costs

    fun add(entry: ContributionMarginEntry): Amount {
      revenue += entry.revenue
      costs += entry.costs
      return this
    }

    fun add(other: Amount): Amount {
      revenue += other.revenue
      costs += other.costs
      return this
    }
  }

  companion object {
    private val HUNDRED = BigDecimal(100)

    /**
     * The end of a calculation up to [until]: the last day of the month before [today] at the latest, as
     * there are no values for the current month and later ones yet (neither booked nor preliminary).
     */
    fun calculationEnd(until: LocalDate, today: LocalDate): LocalDate {
      return minOf(until, today.withDayOfMonth(1).minusDays(1))
    }

    /**
     * The first day of [from]..[until] after the imported accounting records, or null if they cover the whole
     * range. Without any imported records, the whole range is preliminary.
     */
    fun preliminaryBegin(bookingImportEnd: LocalDate?, from: LocalDate, until: LocalDate): LocalDate? {
      if (until < from) {
        return null // Nothing to calculate at all.
      }
      val begin = bookingImportEnd?.plusDays(1) ?: return from
      return if (begin > until) null else maxOf(begin, from)
    }

    /**
     * Whether the invoice is preliminary revenue: dated in [preliminaryBegin]..[until], issued (not planned)
     * and not in the accounting records yet (possibly booked into an earlier month).
     */
    fun isPreliminaryRevenue(
      invoice: RechnungDO,
      preliminaryBegin: LocalDate?,
      until: LocalDate,
      bookedInvoices: BookedInvoices,
    ): Boolean {
      preliminaryBegin ?: return false
      val date = invoice.datum ?: return false
      return date >= preliminaryBegin && date <= until && invoice.status != RechnungStatus.GEPLANT
          && !bookedInvoices.isBooked(invoice)
    }

    /** The preliminary revenue ([isPreliminaryRevenue]) of the invoices with a project as entries. */
    fun invoiceEntries(
      invoices: Collection<RechnungDO>,
      preliminaryBegin: LocalDate?,
      until: LocalDate,
      bookedInvoices: BookedInvoices,
    ): List<ContributionMarginEntry> {
      return invoices.mapNotNull { invoice ->
        val projectId = invoice.projekt?.id ?: return@mapNotNull null
        if (!isPreliminaryRevenue(invoice, preliminaryBegin, until, bookedInvoices)) {
          return@mapNotNull null
        }
        ContributionMarginEntry(
          projectId,
          invoice.datum!!,
          revenue = invoice.info.netSum,
          type = ContributionMarginEntryType.INVOICE,
          text = "Invoice #${invoice.nummer}",
        )
      }
    }

    private val MILLIS_PER_HOUR = BigDecimal(3_600_000)

    /**
     * Contribution margin in % of the revenue (one decimal), 0 for a loss, or null without positive revenue.
     */
    fun percentage(revenue: BigDecimal, profit: BigDecimal): BigDecimal? {
      if (revenue <= BigDecimal.ZERO) {
        return null
      }
      return profit.multiply(HUNDRED).divide(revenue, 1, RoundingMode.HALF_UP).max(BigDecimal.ZERO.setScale(1))
    }

    /**
     * The accounting records of the given projects as entries: records on a revenue account as revenue,
     * all others as costs. The amount is taken as booked, so costs are negative and credits positive.
     *
     * @param kost2ToProject The project id of each kost2 id; records of other kost2 are skipped.
     * @param accountNumber The account number of a record.
     */
    fun recordEntries(
      records: Collection<BuchungssatzDO>,
      kost2ToProject: Map<Long, Long>,
      accountNumber: (BuchungssatzDO) -> Int?,
      isRevenueAccount: (Int) -> Boolean,
    ): List<ContributionMarginEntry> {
      return records.mapNotNull { record ->
        val date = record.datum ?: return@mapNotNull null
        val amount = record.betrag ?: return@mapNotNull null
        val projectId = kost2ToProject[record.kost2?.id ?: return@mapNotNull null] ?: return@mapNotNull null
        val account = accountNumber(record)
        val isRevenue = account != null && isRevenueAccount(account)
        ContributionMarginEntry(
          projectId,
          date,
          revenue = if (isRevenue) amount else BigDecimal.ZERO,
          costs = if (isRevenue) BigDecimal.ZERO else amount,
          kost2Id = record.kost2?.id,
          account = account,
        )
      }
    }

    /** The costs of a time sheet (negative): its duration in hours (two decimals) × hourly rate. */
    fun timesheetCosts(durationMillis: Long, hourlyRate: BigDecimal): BigDecimal {
      return -BigDecimal(durationMillis).divide(MILLIS_PER_HOUR, 2, RoundingMode.HALF_UP)
        .multiply(hourlyRate)
        .setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Parses comma separated account ranges like `4000-4799, 4900` into a predicate. Without valid ranges
     * (see [accountRanges]), no account matches.
     */
    fun parseAccountRanges(value: String?): (Int) -> Boolean {
      val ranges = accountRanges(value) ?: emptyList()
      return { account -> ranges.any { account in it } }
    }

    /**
     * The comma (or semicolon) separated account ranges like `4000-4799, 4900`, or null if any part isn't
     * a number or a range, or there is none.
     */
    fun accountRanges(value: String?): List<IntRange>? {
      val ranges = value?.split(',', ';')?.filter { it.isNotBlank() }?.map { part ->
        val bounds = part.split('-').map { it.trim() }
        val from = bounds[0].toIntOrNull() ?: return null
        val to = when (bounds.size) {
          1 -> from
          2 -> bounds[1].toIntOrNull() ?: return null
          else -> return null
        }
        if (to < from) {
          return null
        }
        from..to
      }
      return ranges?.takeIf { it.isNotEmpty() }
    }
  }
}
