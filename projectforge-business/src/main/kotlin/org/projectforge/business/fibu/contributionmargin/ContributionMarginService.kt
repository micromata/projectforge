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

import jakarta.annotation.PostConstruct
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungCache
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.scripting.support.BookedInvoices
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationJsonValidators
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.between
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ge
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The contribution margin (DB1) of projects: revenue and costs from the accounting records (by the kost2 of
 * the project), completed for the months not imported yet by the unbooked invoices (revenue) and the time
 * sheets × a flat hourly rate (costs). See [ContributionMarginCalculator] for the calculation itself.
 *
 * The accounting records are readable for finance and controlling only, so the data is loaded without the
 * DAO's access checks; the access is checked here instead: finance and controlling may see all projects,
 * project managers and assistants only their own ones ([isOwnProject]).
 */
@Service
open class ContributionMarginService {
  @Autowired
  private lateinit var buchungssatzDao: BuchungssatzDao

  @Autowired
  private lateinit var caches: PfCaches

  @Autowired
  private lateinit var kostCache: KostCache

  @Autowired
  private lateinit var persistenceService: PfPersistenceService

  @Autowired
  private lateinit var rechnungCache: RechnungCache

  @Autowired
  private lateinit var rechnungDao: RechnungDao

  @Autowired
  private lateinit var timesheetDao: TimesheetDao

  @Autowired
  private lateinit var userGroupCache: UserGroupCache

  @PostConstruct
  private fun postConstruct() {
    ConfigurationJsonValidators.register(ConfigurationParam.FIBU_CONTRIBUTION_MARGIN) { json ->
      validate(ContributionMarginConfig.parse(json))
    }
  }

  /** The settings of the contribution margin (configuration parameter `fibu.contributionMargin`). */
  open fun config(): ContributionMarginConfig {
    return ContributionMarginConfig.parse(
      Configuration.instance.getStringValue(ConfigurationParam.FIBU_CONTRIBUTION_MARGIN)
    )
  }

  /** The translated errors of the given settings, each prefixed by the label of its field. */
  private fun validate(config: ContributionMarginConfig): List<String> {
    return config.validate { kost -> projectIdOfKost(kost) != null }.map { error ->
      val label = translate("${ContributionMarginConfig.I18N_PREFIX}.${error.field}")
      val prefix = error.index?.let { "$label ${it + 1}" } ?: label
      "$prefix: ${translateMsg(error.i18nKey, error.param)}"
    }
  }

  /** Whether the user may see the contribution margins of any projects. */
  open fun hasAccess(user: PFUserDO? = ThreadLocalUserContext.loggedInUser): Boolean {
    user ?: return false
    return isFibu(user) || userGroupCache.isUserMemberOfGroup(
      user,
      ProjectForgeGroup.PROJECT_MANAGER,
      ProjectForgeGroup.PROJECT_ASSISTANT,
    )
  }

  open fun checkAccess(user: PFUserDO? = ThreadLocalUserContext.loggedInUser) {
    if (!hasAccess(user)) {
      throw AccessException(user, "access.exception.noAccess")
    }
  }

  /** The given projects the user may see the contribution margin of. */
  open fun allowedProjectIds(
    projectIds: Collection<Long>,
    user: PFUserDO? = ThreadLocalUserContext.loggedInUser,
  ): Set<Long> {
    user ?: return emptySet()
    if (isFibu(user)) {
      return projectIds.toSet()
    }
    if (!hasAccess(user)) {
      return emptySet()
    }
    return projectIds.filter { id -> caches.getProjekt(id)?.let { isOwnProject(user, it) } == true }.toSet()
  }

  /**
   * The contribution margin of the given projects for the 12 months from the month of [startDate] on, with
   * the two previous years. The access to the projects must be checked by the caller ([allowedProjectIds]).
   */
  open fun calculate(projectIds: Collection<Long>, startDate: LocalDate): ContributionMarginData {
    return calculate(projectIds, startDate, withDetails = false).data
  }

  /** As [calculate], plus the rows behind it ([ContributionMarginDetails]). */
  open fun calculateWithDetails(projectIds: Collection<Long>, startDate: LocalDate): ContributionMarginResult {
    return calculate(projectIds, startDate, withDetails = true)
  }

  private fun calculate(projectIds: Collection<Long>, startDate: LocalDate, withDetails: Boolean): ContributionMarginResult {
    val config = config()
    val bookingImportEnd = bookingImportEnd()
    val calculator = ContributionMarginCalculator(startDate, bookingImportEnd, PFDay.now().localDate)
    val source = load(projectIds, calculator.prevPrevYearBegin, calculator.periodEnd, bookingImportEnd, config)
    val data = calculator.calculate(source.entries, ::projectInfo, source.hourlyRate, config)
    if (!withDetails) {
      return ContributionMarginResult(data, ContributionMarginDetails.EMPTY)
    }
    val details = calculator.details(source.entries, ::projectInfo, { kostCache.getKost2(it)?.formattedNumber }, source.hourlyRate)
    val invoices = if (calculator.periodValuesEnd < calculator.periodBegin) {
      emptyList()
    } else {
      invoiceRows(selectInvoices(projectIds, calculator.periodBegin, calculator.periodValuesEnd), source)
    }
    return ContributionMarginResult(data, ContributionMarginDetails(details.months, invoices, details.timesheets))
  }

  /** One row per position of the invoices (planned ones excluded, as they aren't issued yet). */
  private fun invoiceRows(invoices: List<RechnungDO>, source: ContributionMarginSource): List<ContributionMarginInvoiceRow> {
    return invoices.filter { it.status != RechnungStatus.GEPLANT }.flatMap { invoice ->
      val projectId = invoice.projekt?.id
      val info = projectId?.let { projectInfo(it) }
      val customer = caches.getKundeIfNotInitialized(invoice.kunde)?.name ?: invoice.kundeText ?: info?.customer
      val bookedDate = source.bookedInvoices.bookedDate(invoice)?.localDate
      val preliminary = source.isPreliminaryRevenue(invoice)
      val status = invoice.status?.i18nKey?.let { translate(it) }
      val positions = rechnungCache.getRechnungInfo(invoice.id)?.positions ?: emptyList()
      positions.map { pos ->
        val orderPos = rechnungCache.getOrderPositionInfoOfInvoicePos(pos.id)
        ContributionMarginInvoiceRow(
          invoiceId = invoice.id,
          date = invoice.datum,
          number = invoice.nummer,
          positionNumber = pos.number,
          projectId = projectId,
          kost = info?.kost,
          customer = customer,
          project = info?.project,
          subject = invoice.betreff,
          netSum = pos.netSum,
          status = status,
          orderId = orderPos?.auftragId,
          order = orderPos?.let { "${it.auftragNummer}.${it.number}" },
          bookedDate = bookedDate,
          preliminary = preliminary,
        )
      }
    }.sortedWith(compareBy({ it.date }, { it.number }, { it.positionNumber }))
  }

  /**
   * The contribution margin amounts of the given projects (all projects if null) in [from]..[until]: the
   * accounting records, completed after the last imported month by the unbooked invoices and the time
   * sheets × the configured hourly rate. Without access checks: the caller is responsible (e.g. scripts,
   * running as superuser, or [calculate]).
   *
   * Ends with the previous month at the latest, whatever [until] is: the current month isn't complete yet
   * ([ContributionMarginSource.until] is the actual end).
   */
  open fun load(projectIds: Collection<Long>?, from: LocalDate, until: LocalDate): ContributionMarginSource {
    return load(projectIds, from, until, bookingImportEnd(), config())
  }

  private fun load(
    projectIds: Collection<Long>?,
    from: LocalDate,
    requestedUntil: LocalDate,
    bookingImportEnd: LocalDate?,
    config: ContributionMarginConfig,
  ): ContributionMarginSource {
    val until = ContributionMarginCalculator.calculationEnd(requestedUntil, PFDay.now().localDate)
    val isRevenueAccount = config.isRevenueAccount
    val hourlyRate = config.effectiveHourlyRate
    val preliminaryBegin = ContributionMarginCalculator.preliminaryBegin(bookingImportEnd, from, until)
    val kost2ToProject = kost2ToProject(projectIds, config)
    val records = if (projectIds == null) {
      selectRecords(null, from, until)
    } else {
      selectRecords(kost2ToProject.keys, from, until)
    }
    records.forEach { it.konto = caches.getKontoIfNotInitialized(it.konto) }
    val accountNumber: (BuchungssatzDO) -> Int? = { it.konto?.nummer }
    val entries = ContributionMarginCalculator.recordEntries(records, kost2ToProject, accountNumber, isRevenueAccount)
      .toMutableList()
    // An invoice may be booked into an earlier month than its date (e.g. the month of service) and on any
    // kost2, so the booked invoices are looked up in all records of up to a year before the preliminary days.
    val bookedInvoices = if (projectIds == null || preliminaryBegin == null) {
      BookedInvoices(records, isRevenueAccount)
    } else {
      val allRecords = selectRecords(null, maxOf(from, preliminaryBegin.minusYears(1)), until)
      allRecords.forEach { it.konto = caches.getKontoIfNotInitialized(it.konto) }
      BookedInvoices(allRecords, isRevenueAccount)
    }
    if (preliminaryBegin != null) {
      val invoices = selectInvoices(projectIds, preliminaryBegin, until)
      entries += ContributionMarginCalculator.invoiceEntries(invoices, preliminaryBegin, until, bookedInvoices)
      if (hourlyRate != null) {
        entries += timesheetEntries(kost2ToProject, preliminaryBegin, until, hourlyRate)
      }
    }
    log.info {
      "Contribution margin of ${projectIds?.size ?: "all"} projects in $from..$until: ${records.size} accounting records, ${entries.size} entries, booking import end $bookingImportEnd."
    }
    return ContributionMarginSource(
      entries = entries,
      from = from,
      until = until,
      bookingImportEnd = bookingImportEnd,
      preliminaryBegin = preliminaryBegin,
      hourlyRate = hourlyRate,
      bookedInvoices = bookedInvoices,
    )
  }

  /**
   * The last day of the last month with imported accounting records (of any project), or null if there
   * are none. Taken over all projects, so a project without bookings in the last month isn't mistaken
   * for one whose records aren't imported yet.
   */
  open fun bookingImportEnd(): LocalDate? {
    val period = persistenceService.selectSingleResult(
      "select max(t.year * 100 + t.month) from BuchungssatzDO t where t.deleted = false",
      Int::class.javaObjectType,
    ) ?: return null
    return YearMonth.of(period / 100, period % 100).atEndOfMonth()
  }

  /**
   * The project of each kost2 (of the given projects, of all if null): its own project, or for a kost2
   * without one the project assigned by [ContributionMarginConfig.kost2Assignments].
   */
  private fun kost2ToProject(projectIds: Collection<Long>?, config: ContributionMarginConfig): Map<Long, Long> {
    val projectIdSet = projectIds?.toSet()
    val result = mutableMapOf<Long, Long>()
    kostCache.getAllKost2(includeDeleted = true).forEach { kost2 ->
      val kost2Id = kost2.id ?: return@forEach
      val projectId = kost2.projekt?.id ?: return@forEach
      if (projectIdSet == null || projectId in projectIdSet) {
        result[kost2Id] = projectId
      }
    }
    config.kost2Assignments.filter { !it.isBlank }.forEach { assignment ->
      val kost2 = ContributionMarginConfig.parseKostNumber(assignment.kost2)
      val project = ContributionMarginConfig.parseKostNumber(assignment.project)
      if (kost2 == null || project == null) {
        log.warn { "Contribution margin: invalid kost2 assignment ${assignment.kost2}=${assignment.project}." }
        return@forEach
      }
      val projectId = projectIdOfKost(project)
      if (projectId == null) {
        log.warn { "Contribution margin: project ${assignment.project} of the kost2 assignment not found." }
        return@forEach
      }
      if (projectIdSet != null && projectId !in projectIdSet) {
        return@forEach
      }
      val (kost2Nummernkreis, kost2Bereich, kost2Teilbereich) = kost2
      val kost2List = kostCache.getKost2List(kost2Nummernkreis, kost2Bereich, kost2Teilbereich, includeDeleted = true)
        .filter { it.projekt == null }
      if (kost2List.isEmpty()) {
        log.warn { "Contribution margin: no kost2 ${assignment.kost2}.* without a project to assign." }
      }
      kost2List.forEach { kost2 -> kost2.id?.let { result[it] = projectId } }
    }
    return result
  }

  /** The project of the given kost (three parts), found by its kost2 (also deleted ones). */
  private fun projectIdOfKost(kost: List<Int>): Long? {
    val (nummernkreis, bereich, teilbereich) = kost
    return kostCache.getKost2List(nummernkreis, bereich, teilbereich, includeDeleted = true)
      .firstNotNullOfOrNull { it.projekt?.id }
  }

  private fun selectInvoices(projectIds: Collection<Long>?, from: LocalDate, until: LocalDate): List<RechnungDO> {
    if (projectIds == null) {
      val queryFilter = queryFilter()
      queryFilter.add(between("datum", from, until))
      return rechnungDao.select(queryFilter, checkAccess = false)
    }
    return projectIds.chunked(CHUNK_SIZE).flatMap { chunk ->
      val queryFilter = queryFilter()
      queryFilter.add(isIn("projekt.id", chunk))
      queryFilter.add(between("datum", from, until))
      rechnungDao.select(queryFilter, checkAccess = false)
    }
  }

  private fun timesheetEntries(
    kost2ToProject: Map<Long, Long>,
    from: LocalDate,
    until: LocalDate,
    hourlyRate: BigDecimal,
  ): List<ContributionMarginEntry> {
    val zoneId = ThreadLocalUserContext.zoneId
    val fromTime = Date.from(from.atStartOfDay(zoneId).toInstant())
    val untilTime = Date.from(until.plusDays(1).atStartOfDay(zoneId).toInstant())
    return kost2ToProject.keys.chunked(CHUNK_SIZE).flatMap { chunk ->
      val queryFilter = queryFilter()
      queryFilter.add(isIn("kost2.id", chunk))
      queryFilter.add(ge("startTime", fromTime))
      queryFilter.add(QueryFilter.lt("startTime", untilTime))
      timesheetDao.select(queryFilter, checkAccess = false).mapNotNull { timesheet ->
        val startTime = timesheet.startTime ?: return@mapNotNull null
        val kost2Id = timesheet.kost2?.id
        val projectId = kost2ToProject[kost2Id] ?: return@mapNotNull null
        val date = startTime.toInstant().atZone(zoneId).toLocalDate()
        val user = caches.getUserIfNotInitialized(timesheet.user)
        ContributionMarginEntry(
          projectId,
          date,
          costs = ContributionMarginCalculator.timesheetCosts(timesheet.duration, hourlyRate),
          type = ContributionMarginEntryType.TIMESHEET,
          kost2Id = kost2Id,
          text = "Timesheet ${user?.getFullname()}: ${PFDay.from(date).format()}",
        )
      }
    }
  }

  /** The accounting records of the given kost2 (of all if null) in [from]..[until]. */
  private fun selectRecords(kost2Ids: Collection<Long>?, from: LocalDate, until: LocalDate): List<BuchungssatzDO> {
    if (kost2Ids == null) {
      val queryFilter = queryFilter()
      queryFilter.add(between("datum", from, until))
      return buchungssatzDao.select(queryFilter, checkAccess = false)
    }
    return kost2Ids.chunked(CHUNK_SIZE).flatMap { chunk ->
      val queryFilter = queryFilter()
      queryFilter.add(isIn("kost2.id", chunk))
      queryFilter.add(between("datum", from, until))
      buchungssatzDao.select(queryFilter, checkAccess = false)
    }
  }

  private fun projectInfo(projectId: Long): ContributionMarginProjectInfo? {
    val project = caches.getProjekt(projectId) ?: return null
    return ContributionMarginProjectInfo(
      kost = project.kost,
      customer = caches.getKundeIfNotInitialized(project.kunde)?.name,
      project = project.name,
    )
  }

  private fun isFibu(user: PFUserDO): Boolean {
    return userGroupCache.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)
  }

  /** As the project manager access of the order book (`AuftragRight`), plus the project manager himself. */
  private fun isOwnProject(user: PFUserDO, project: ProjektDO): Boolean {
    return userGroupCache.isUserMemberOfGroup(user.id, project.projektManagerGroupId)
        || project.projectManagerId == user.id
        || project.headOfBusinessManagerId == user.id
        || project.salesManagerId == user.id
  }

  private fun queryFilter(): QueryFilter {
    val queryFilter = QueryFilter()
    queryFilter.deleted = false
    queryFilter.maxRows = MAX_ROWS
    return queryFilter
  }

  companion object {
    /** The ids per IN clause (PostgreSQL's limit of bind parameters). */
    private const val CHUNK_SIZE = 1000

    private const val MAX_ROWS = 1_000_000
  }
}
