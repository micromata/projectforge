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
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsCache
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.OrderPositionInfo
import org.projectforge.business.fibu.OrderAccessFacts
import org.projectforge.business.fibu.OrderSelectAccess
import org.projectforge.business.fibu.customergroup.BusinessUnit
import org.projectforge.business.fibu.customergroup.CustomerGroupService
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungCache
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.contributionmargin.ContributionMarginAllocator.GroupKey
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.reallocation.BuchungssatzKost2ShareDao
import org.projectforge.business.fibu.kost.reallocation.BuchungssatzKost2ShareService
import org.projectforge.business.scripting.support.BookedInvoices
import org.projectforge.business.task.TaskTree
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
 * project managers and assistants only their own ones ([isOwnProject]) and those of the orders they are a
 * contact person of or granted by an order access grant ([allowedProjectIds]), as in the order book
 * (`AuftragRight`).
 */
@Service
open class ContributionMarginService {
  @Autowired
  private lateinit var auftragsCache: AuftragsCache

  @Autowired
  private lateinit var buchungssatzDao: BuchungssatzDao

  @Autowired
  private lateinit var shareDao: BuchungssatzKost2ShareDao

  @Autowired
  private lateinit var shareService: BuchungssatzKost2ShareService

  @Autowired
  private lateinit var caches: PfCaches

  @Autowired
  private lateinit var contributionMarginCache: ContributionMarginCache

  @Autowired
  private lateinit var kostCache: KostCache

  @Autowired
  private lateinit var persistenceService: PfPersistenceService

  @Autowired
  private lateinit var rechnungCache: RechnungCache

  @Autowired
  private lateinit var rechnungDao: RechnungDao

  @Autowired
  private lateinit var taskTree: TaskTree

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

  /**
   * The projects of the given orders the user may see the contribution margin of: their own projects
   * ([isOwnProject]) and the projects of the orders the user is a contact person of or granted (as `AuftragRight`).
   * The orders must be the ones the user may see (selected with access check).
   */
  open fun allowedProjectIds(
    orders: Collection<AuftragDO>,
    user: PFUserDO? = ThreadLocalUserContext.loggedInUser,
  ): Set<Long> {
    user ?: return emptySet()
    val ordersByProject = orders.filter { it.projekt?.id != null }.groupBy { it.projekt!!.id!! }
    if (isFibu(user)) {
      return ordersByProject.keys
    }
    if (!hasAccess(user)) {
      return emptySet()
    }
    return ordersByProject.filter { (id, projectOrders) ->
      projectOrders.any { it.isContact(user.id) } || caches.getProjekt(id)?.let { isOwnProject(user, it) } == true
          || projectOrders.any { OrderSelectAccess.isGranted(user, OrderAccessFacts.of(it)) }
    }.keys
  }

  /**
   * The contribution margin of the given projects for the 12 months from the month of [startDate] on, with
   * the two previous years. The access to the projects must be checked by the caller ([allowedProjectIds]).
   */
  open fun calculate(projectIds: Collection<Long>, startDate: LocalDate): ContributionMarginData {
    return calculate(projectIds, startDate, withDetails = false).data
  }

  /**
   * The bubbles of the contribution margin bubble chart: those of [own] (the user's projects, see
   * [allowedProjectIds]) with labels, and those of [others] (projects the user may not see) anonymous, see
   * [ContributionMarginBubbleBuilder.bubbles]. Own and other projects never share a bubble, so the sums of the
   * others can't be derived from a bubble and the user's own projects.
   */
  open fun bubbles(
    own: ContributionMarginData,
    others: ContributionMarginData?,
    grouping: ContributionMarginBubbleGrouping,
  ): ContributionMarginBubbles {
    val customerGroups = CustomerGroupService.instanceOrNull
    val customerGroupOf: (Long) -> String? = { projectId -> customerGroups?.groupNameOf(caches.getProjekt(projectId)) }
    val unitOf: (Long) -> BusinessUnit? = { projectId -> customerGroups?.businessUnitOfProject(projectId) }
    val colorOf: (Long) -> String? = { projectId -> unitOf(projectId)?.color?.takeIf { it.isNotBlank() } }
    val ownProjectIds = own.projects.map { it.projectId }.toSet()
    val otherProjects = others?.projects?.filter { it.projectId !in ownProjectIds }.orEmpty()
    val bubbles = ContributionMarginBubbleBuilder.bubbles(own.projects, true, grouping, customerGroupOf, colorOf) +
        ContributionMarginBubbleBuilder.bubbles(otherProjects, false, grouping, customerGroupOf, colorOf)
    val usedColors = bubbles.mapNotNull { it.color }.toSet()
    val units = (own.projects + otherProjects)
      .mapNotNull { unitOf(it.projectId) }
      .distinctBy { it.key }
      .mapNotNull { unit ->
        val color = unit.color?.takeIf { it in usedColors } ?: return@mapNotNull null
        ContributionMarginBubbleUnit(unit.name ?: unit.key ?: "", color)
      }
      .sortedBy { it.name.lowercase() }
    return ContributionMarginBubbles(bubbles, own.targetPercentage, own.redThreshold, units)
  }

  /** As [calculate], plus the rows behind it ([ContributionMarginDetails]). */
  open fun calculateWithDetails(projectIds: Collection<Long>, startDate: LocalDate): ContributionMarginResult {
    return calculate(projectIds, startDate, withDetails = true)
  }

  /**
   * The parts of the projects come from the [ContributionMarginCache], only the missing ones are loaded. The
   * sums are calculated of all parts, they are sums over the projects.
   */
  private fun calculate(projectIds: Collection<Long>, startDate: LocalDate, withDetails: Boolean): ContributionMarginResult {
    val config = config()
    val bookingImportEnd = bookingImportEnd()
    val calculator = ContributionMarginCalculator(startDate, bookingImportEnd, PFDay.now().localDate)
    val context = context(calculator, bookingImportEnd)
    val parts = contributionMarginCache.get(context, projectIds.toSet()) { missing ->
      loadParts(missing, calculator, bookingImportEnd, config)
    }.values
    val entries = parts.flatMap { it.entries }
    val hourlyRate = config.effectiveHourlyRate
    val data = calculator.calculate(entries, ::projectInfo, hourlyRate, config)
    if (!withDetails) {
      return ContributionMarginResult(data, ContributionMarginDetails.EMPTY)
    }
    val details = calculator.details(entries, ::projectInfo, { kostCache.getKost2(it)?.formattedNumber }, hourlyRate)
    val invoices = parts.flatMap { it.invoiceRows }
      .sortedWith(compareBy({ it.date }, { it.number }, { it.positionNumber }))
    return ContributionMarginResult(data, ContributionMarginDetails(details.months, invoices, details.timesheets))
  }

  /**
   * The contribution margin of the given projects broken down by orders or tasks (Strukturelemente), for the same
   * period and comparison years as [calculate]: the calculated values of the orders or tasks
   * ([allocatedEntries]) and the residual of each project ([ContributionMarginAllocator.breakdown]). The access to
   * the projects must be checked by the caller ([allowedProjectIds]).
   */
  open fun calculateBreakdown(
    projectIds: Collection<Long>,
    startDate: LocalDate,
    level: ContributionMarginLevel,
  ): ContributionMarginBreakdown {
    val config = config()
    val bookingImportEnd = bookingImportEnd()
    val calculator = ContributionMarginCalculator(startDate, bookingImportEnd, PFDay.now().localDate)
    val context = context(calculator, bookingImportEnd)
    val ids = projectIds.toSet()
    val projectEntries = contributionMarginCache.get(context, ids) { missing ->
      loadParts(missing, calculator, bookingImportEnd, config)
    }.values.flatMap { it.entries }
    val allocatedEntries = contributionMarginCache.get(context.copy(breakdown = true), ids) { missing ->
      val entries = allocatedEntries(missing, calculator.prevPrevYearBegin, calculator.periodValuesEnd, config)
        .groupBy { it.projectId }
      missing.associateWith { ContributionMarginCache.ProjectPart(entries[it].orEmpty(), emptyList()) }
    }.values.flatMap { it.entries }
    val data = calculator.calculate(projectEntries, ::projectInfo, config.effectiveHourlyRate, config)
    // Many entries share their task (time sheets), so the paths are looked up once per task and project.
    val paths = mutableMapOf<Pair<Long?, Long>, List<GroupKey>>()
    val path: (ContributionMarginEntry) -> List<GroupKey> = when (level) {
      ContributionMarginLevel.ORDER -> { entry -> orderPath(entry.orderPositionId) }
      ContributionMarginLevel.TASK -> { entry ->
        paths.getOrPut(entry.taskId to entry.projectId) { taskPath(entry.taskId, entry.projectId) }
      }
    }
    val rows = ContributionMarginAllocator.breakdown(
      calculator, projectEntries, allocatedEntries, path, ::projectInfo, ::groupLabel, ::groupSortKey,
    )
    return ContributionMarginBreakdown(
      level = level,
      months = data.months,
      preliminary = data.preliminary,
      lastMonth = data.lastMonth,
      valuesEnd = data.valuesEnd,
      hourlyRate = data.hourlyRate,
      targetPercentage = data.targetPercentage,
      redThreshold = data.redThreshold,
      rows = rows,
    )
  }

  private fun context(calculator: ContributionMarginCalculator, bookingImportEnd: LocalDate?) =
    ContributionMarginCache.Context(
      startMonth = calculator.start,
      today = calculator.today,
      bookingImportEnd = bookingImportEnd,
      config = Configuration.instance.getStringValue(ConfigurationParam.FIBU_CONTRIBUTION_MARGIN),
      zoneId = ThreadLocalUserContext.zoneId,
      locale = ThreadLocalUserContext.locale,
    )

  /**
   * The calculated amounts of the given projects in [from]..[until] with their order positions and tasks:
   * - revenue: the positions of the issued invoices, at the booking date if booked (possibly in another month than
   *   the invoice's date, as in the accounting records), else at the invoice's date; with the invoiced order
   *   position and its task,
   * - costs: the time sheets × the configured hourly rate (none without one), assigned to the order positions of
   *   the nearest task upwards ([ContributionMarginAllocator.nearestPositions]).
   */
  private fun allocatedEntries(
    projectIds: Collection<Long>,
    from: LocalDate,
    until: LocalDate,
    config: ContributionMarginConfig,
  ): List<ContributionMarginEntry> {
    if (until < from) {
      return emptyList()
    }
    // An invoice may be booked into a month before or after its date.
    val bookedInvoices = bookedInvoices(from.minusMonths(2), until, config.isRevenueAccount)
    val allKost2ToProject = kost2ToProject(null, config)
    val projectIdSet = projectIds.toSet()
    val invoiceEntries = selectInvoices(projectIds, allKost2ToProject, from.minusMonths(1), until.plusMonths(1))
      .filter { it.status != RechnungStatus.GEPLANT }
      .flatMap { invoice ->
        val date = bookedInvoices.bookedDate(invoice)?.localDate ?: invoice.datum ?: return@flatMap emptyList()
        if (date < from || date > until) {
          return@flatMap emptyList()
        }
        positionsOf(invoice).flatMap { pos ->
          val orderPosition = rechnungCache.getOrderPositionInfoOfInvoicePos(pos.id)
          ContributionMarginCalculator.invoiceShares(invoice, pos, allKost2ToProject, projectIdSet).map { share ->
            ContributionMarginEntry(
              share.projectId,
              date,
              revenue = share.netSum,
              type = ContributionMarginEntryType.INVOICE,
              kost2Id = share.kost2Id,
              text = "Invoice #${invoice.nummer}.${pos.number}",
              orderPositionId = orderPosition?.id,
              taskId = orderPosition?.taskId,
            )
          }
        }
      }
    val hourlyRate = config.effectiveHourlyRate ?: return invoiceEntries
    val positionsOfTask = mutableMapOf<Long, List<ContributionMarginAllocator.Position>>()
    val nearest = mutableMapOf<Pair<Long?, Long>, List<ContributionMarginAllocator.Position>>()
    val timesheetEntries = timesheetEntries(kost2ToProject(projectIds, config), from, until, hourlyRate)
      .flatMap { entry ->
        val positions = nearest.getOrPut(entry.taskId to entry.projectId) {
          ContributionMarginAllocator.nearestPositions(
            entry.taskId,
            entry.projectId,
            parentOf = { taskTree.getTaskNodeById(it)?.parentId },
            positionsOf = { taskId ->
              positionsOfTask.getOrPut(taskId) {
                taskTree.getOrderPositionEntries(taskId).orEmpty().mapNotNull(::position)
              }
            },
          )
        }
        ContributionMarginAllocator.allocate(entry, positions)
      }
    log.info {
      "Contribution margin breakdown of ${projectIds.size} projects in $from..$until: ${invoiceEntries.size} invoice positions, ${timesheetEntries.size} time sheet entries."
    }
    return invoiceEntries + timesheetEntries
  }

  private fun position(info: OrderPositionInfo): ContributionMarginAllocator.Position? {
    val id = info.id ?: return null
    val order = info.auftrag
    return ContributionMarginAllocator.Position(
      id = id,
      orderId = info.auftragId,
      projectId = order?.projektId,
      personDays = info.personDays,
      netSum = info.dbNetSum,
      inactive = info.deleted || order?.deleted == true || info.status in INACTIVE_STATUS
          || order?.status in INACTIVE_STATUS,
    )
  }

  /**
   * The invoices booked on revenue accounts in the booking periods of [from]..[until] (of all projects, see
   * [load]). Only the few columns needed are loaded, as these are the records of more than two years.
   */
  private fun bookedInvoices(from: LocalDate, until: LocalDate, isRevenueAccount: (Int) -> Boolean): BookedInvoices {
    val records = persistenceService.executeQuery(
      "select t.beleg, t.year, t.month, t.datum, k.nummer from BuchungssatzDO t join t.konto k" +
          " where t.deleted = false and t.year * 100 + t.month between :from and :until",
      Array<Any?>::class.java,
      "from" to from.year * 100 + from.monthValue,
      "until" to until.year * 100 + until.monthValue,
    ).map { row ->
      BuchungssatzDO().also { record ->
        record.beleg = row[0] as String?
        record.year = row[1] as Int?
        record.month = row[2] as Int?
        record.datum = row[3] as LocalDate?
        record.konto = KontoDO().also { it.nummer = row[4] as Int? }
      }
    }
    return BookedInvoices(records, isRevenueAccount)
  }

  /** The order and the order position of the entry; empty without an order position. */
  private fun orderPath(orderPositionId: Long?): List<GroupKey> {
    val position = auftragsCache.getOrderPositionInfo(orderPositionId ?: return emptyList()) ?: return emptyList()
    val orderId = position.auftragId ?: return emptyList()
    return listOf(
      GroupKey(ContributionMarginRowType.ORDER, orderId),
      GroupKey(ContributionMarginRowType.ORDER_POSITION, position.id ?: return emptyList()),
    )
  }

  /**
   * The tasks from the task of the project down to [taskId]; from the top of the task tree on if the task isn't
   * below the project's one (or the project has none). Empty without a task.
   */
  private fun taskPath(taskId: Long?, projectId: Long): List<GroupKey> {
    taskId ?: return emptyList()
    val projectTaskId = caches.getProjekt(projectId)?.task?.id
    // Below the ancestor: the path starts with its child; otherwise with a child of the root.
    val path = taskTree.getPath(taskId, projectTaskId).mapNotNull { it.id }
    val ids = when {
      projectTaskId == null -> path
      taskId == projectTaskId -> listOf(projectTaskId)
      path.firstOrNull()?.let { taskTree.getTaskNodeById(it)?.parentId } == projectTaskId -> listOf(projectTaskId) + path
      else -> path
    }
    return ids.map { GroupKey(ContributionMarginRowType.TASK, it) }
  }

  private fun groupLabel(key: GroupKey): String? = when (key.type) {
    ContributionMarginRowType.ORDER -> auftragsCache.getOrderInfo(key.id)?.let { order ->
      listOfNotNull(order.nummer?.toString(), order.titel).joinToString(" ")
    }

    ContributionMarginRowType.ORDER_POSITION -> auftragsCache.getOrderPositionInfo(key.id)?.let { position ->
      listOfNotNull("${position.auftragNummer}.${position.number}", position.titel).joinToString(" ")
    }

    ContributionMarginRowType.TASK -> taskTree.getTaskById(key.id)?.title
    else -> null
  }

  /** Orders and positions by their numbers, tasks by their titles. */
  private fun groupSortKey(key: GroupKey): String = when (key.type) {
    ContributionMarginRowType.ORDER -> auftragsCache.getOrderInfo(key.id)?.nummer?.let { "%010d".format(it) }
    ContributionMarginRowType.ORDER_POSITION -> auftragsCache.getOrderPositionInfo(key.id)?.number
      ?.let { "%05d".format(it) }

    ContributionMarginRowType.TASK -> taskTree.getTaskById(key.id)?.title?.lowercase()
    else -> null
  } ?: ""

  /** The entries and invoice rows of [projectIds], one part per project (empty ones too). */
  private fun loadParts(
    projectIds: Set<Long>,
    calculator: ContributionMarginCalculator,
    bookingImportEnd: LocalDate?,
    config: ContributionMarginConfig,
  ): Map<Long, ContributionMarginCache.ProjectPart> {
    val allKost2ToProject = kost2ToProject(null, config)
    val source = load(
      projectIds, calculator.prevPrevYearBegin, calculator.periodValuesEnd, bookingImportEnd, config, allKost2ToProject,
    )
    val invoiceRows = if (calculator.periodValuesEnd < calculator.periodBegin) {
      emptyList()
    } else {
      val invoices = selectInvoices(projectIds, allKost2ToProject, calculator.periodBegin, calculator.periodValuesEnd)
      invoiceRows(invoices, source, allKost2ToProject, projectIds)
    }
    val entriesByProject = source.entries.groupBy { it.projectId }
    val rowsByProject = invoiceRows.groupBy { it.projectId }
    return projectIds.associateWith { id ->
      ContributionMarginCache.ProjectPart(entriesByProject[id].orEmpty(), rowsByProject[id].orEmpty())
    }
  }

  /** One row per position of the invoices (planned ones excluded, as they aren't issued yet). */
  private fun invoiceRows(
    invoices: List<RechnungDO>,
    source: ContributionMarginSource,
    kost2ToProject: Map<Long, Long>,
    projectIds: Collection<Long>,
  ): List<ContributionMarginInvoiceRow> {
    val projectIdSet = projectIds.toSet()
    return invoices.filter { it.status != RechnungStatus.GEPLANT }.flatMap { invoice ->
      val bookedDate = source.bookedInvoices.bookedDate(invoice)?.localDate
      val preliminary = source.isPreliminaryRevenue(invoice)
      val status = invoice.status?.i18nKey?.let { translate(it) }
      positionsOf(invoice).flatMap { pos ->
        val orderPos = rechnungCache.getOrderPositionInfoOfInvoicePos(pos.id)
        ContributionMarginCalculator.invoiceShares(invoice, pos, kost2ToProject, projectIdSet).map { share ->
          val projectId = share.projectId
          val info = projectInfo(projectId)
          ContributionMarginInvoiceRow(
            invoiceId = invoice.id,
            date = invoice.datum,
            number = invoice.nummer,
            positionNumber = pos.number,
            projectId = projectId,
            kost = info?.kost,
            customer = caches.getKundeIfNotInitialized(invoice.kunde)?.name ?: invoice.kundeText ?: info?.customer,
            project = info?.project,
            projectGroup = info?.projectGroup,
            subject = invoice.betreff,
            netSum = share.netSum,
            status = status,
            orderId = orderPos?.auftragId,
            order = orderPos?.let { "${it.auftragNummer}.${it.number}" },
            bookedDate = bookedDate,
            preliminary = preliminary,
          )
        }
      }
    }.sortedWith(compareBy({ it.date }, { it.number }, { it.positionNumber }))
  }

  /**
   * The contribution margin amounts of the given projects (all projects if null) in [from]..[until]: the
   * accounting records, completed after the last imported month by the unbooked invoices and the time
   * sheets × the configured hourly rate. Without access checks: the caller is responsible (e.g. scripts,
   * running as superuser, or [calculate]).
   *
   * Ends today at the latest, whatever [until] is ([ContributionMarginSource.until] is the actual end): the
   * current month counts up to today, preliminary as it isn't booked yet. (The order book's tab, [calculate],
   * ends with the previous month instead.)
   */
  open fun load(projectIds: Collection<Long>?, from: LocalDate, until: LocalDate): ContributionMarginSource {
    return load(projectIds, from, minOf(until, PFDay.now().localDate), bookingImportEnd(), config())
  }

  /** @param until The end of the calculation, already capped by the caller (today or the previous month). */
  private fun load(
    projectIds: Collection<Long>?,
    from: LocalDate,
    until: LocalDate,
    bookingImportEnd: LocalDate?,
    config: ContributionMarginConfig,
    allKost2ToProject: Map<Long, Long> = kost2ToProject(null, config),
  ): ContributionMarginSource {
    val isRevenueAccount = config.isRevenueAccount
    val hourlyRate = config.effectiveHourlyRate
    val preliminaryBegin = ContributionMarginCalculator.preliminaryBegin(bookingImportEnd, from, until)
    val kost2ToProject = if (projectIds == null) allKost2ToProject else allKost2ToProject.filterValues { it in projectIds }
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
      val invoices = selectInvoices(projectIds, allKost2ToProject, preliminaryBegin, until)
      entries += ContributionMarginCalculator.invoiceEntries(
        invoices, preliminaryBegin, until, bookedInvoices, allKost2ToProject, projectIds?.toSet(), ::positionsOf,
      )
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
   * without one the project assigned by [ContributionMarginConfig.kost2Assignments]. The assignments are
   * resolved for all projects first, so a kost2 assigned twice goes to the same project whichever projects
   * are asked for (the contribution margins of single projects are combined, see [ContributionMarginCache]).
   */
  private fun kost2ToProject(projectIds: Collection<Long>?, config: ContributionMarginConfig): Map<Long, Long> {
    val projectIdSet = projectIds?.toSet()
    val result = mutableMapOf<Long, Long>()
    kostCache.getAllKost2(includeDeleted = true).forEach { kost2 ->
      val kost2Id = kost2.id ?: return@forEach
      val projectId = kost2.projekt?.id ?: return@forEach
      result[kost2Id] = projectId
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
      val (kost2Nummernkreis, kost2Bereich, kost2Teilbereich) = kost2
      val kost2List = kostCache.getKost2List(kost2Nummernkreis, kost2Bereich, kost2Teilbereich, includeDeleted = true)
        .filter { it.projekt == null }
      if (kost2List.isEmpty()) {
        log.warn { "Contribution margin: no kost2 ${assignment.kost2}.* without a project to assign." }
      }
      kost2List.forEach { kost2 -> kost2.id?.let { result[it] = projectId } }
    }
    projectIdSet ?: return result
    return result.filterValues { it in projectIdSet }
  }

  /** The project of the given kost (three parts), found by its kost2 (also deleted ones). */
  private fun projectIdOfKost(kost: List<Int>): Long? {
    val (nummernkreis, bereich, teilbereich) = kost
    return kostCache.getKost2List(nummernkreis, bereich, teilbereich, includeDeleted = true)
      .firstNotNullOfOrNull { it.projekt?.id }
  }

  /** The ids of the (not deleted) invoices in [from]..[until] with a cost assignment on one of the given kost2. */
  internal fun invoiceIdsOfKost2(kost2Ids: Collection<Long>, from: LocalDate, until: LocalDate): List<Long> {
    return kost2Ids.chunked(CHUNK_SIZE).flatMap { chunk ->
      persistenceService.executeQuery(
        "select distinct r.id from RechnungDO r join r.positionen p join p.kostZuweisungen k" +
            " where k.kost2.id in :kost2Ids and r.datum between :from and :until and r.deleted = false",
        Long::class.javaObjectType,
        "kost2Ids" to chunk,
        "from" to from,
        "until" to until,
      )
    }.distinct()
  }

  /**
   * The positions of the invoice (not deleted ones). Without cached infos (e.g. while the cache is refreshing),
   * the invoice counts as a single position of its net sum, without cost assignments.
   */
  private fun positionsOf(invoice: RechnungDO): List<InvoicePosition> {
    val positions = rechnungCache.getRechnungInfo(invoice.id)?.positions
      ?: return listOf(InvoicePosition(null, 1, invoice.info.netSum, BigDecimal.ZERO, emptyList()))
    return positions.filter { !it.deleted }.map { InvoicePosition(it) }
  }

  /**
   * The invoices in [from]..[until] of the given projects (all if null): those of the project itself or with
   * a cost assignment on one of its kost2 ([kost2ToProject]). Which part belongs to which project is up to
   * [ContributionMarginCalculator.invoiceShares].
   */
  private fun selectInvoices(
    projectIds: Collection<Long>?,
    kost2ToProject: Map<Long, Long>,
    from: LocalDate,
    until: LocalDate,
  ): List<RechnungDO> {
    if (projectIds == null) {
      val queryFilter = queryFilter()
      queryFilter.add(between("datum", from, until))
      return rechnungDao.select(queryFilter, checkAccess = false)
    }
    val projectIdSet = projectIds.toSet()
    val invoices = projectIds.chunked(CHUNK_SIZE).flatMap { chunk ->
      val queryFilter = queryFilter()
      queryFilter.add(isIn("projekt.id", chunk))
      queryFilter.add(between("datum", from, until))
      rechnungDao.select(queryFilter, checkAccess = false)
    }.associateBy { it.id }
    val assignedIds = invoiceIdsOfKost2(kost2ToProject.filterValues { it in projectIdSet }.keys, from, until)
        .filter { it !in invoices }
    if (assignedIds.isEmpty()) {
      return invoices.values.toList()
    }
    return invoices.values + assignedIds.chunked(CHUNK_SIZE).flatMap { rechnungDao.select(it, checkAccess = false).orEmpty() }
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
          taskId = timesheet.task?.id,
        )
      }
    }
  }

  /**
   * The accounting records of the given kost2 (of all if null) whose booking period (year and month of the
   * record number, not the record's date) lies in [from]..[until]; the records of the months of [from] and
   * [until] are restricted to these days by [ContributionMarginCalculator.bookingDate]. Empty if [until] is
   * before [from] (e.g. a period in the future), as the period query would reject that range.
   */
  private fun selectRecords(kost2Ids: Collection<Long>?, from: LocalDate, until: LocalDate): List<BuchungssatzDO> {
    if (until < from) {
      return emptyList()
    }
    fun inRange(records: List<BuchungssatzDO>) = records.filter { record ->
      ContributionMarginCalculator.bookingDate(record)?.let { it in from..until } == true
    }
    if (kost2Ids == null) {
      return inRange(shareService.expand(buchungssatzDao.select(recordsQueryFilter(from, until), checkAccess = false)))
    }
    val own = kost2Ids.chunked(CHUNK_SIZE).flatMap { chunk ->
      val queryFilter = recordsQueryFilter(from, until)
      queryFilter.add(isIn("kost2.id", chunk))
      buchungssatzDao.select(queryFilter, checkAccess = false)
    }
    // Reallocated records count with their shares (effective view): also the records of other kost2 with a share on
    // one of the given ones, and only the shares on the given kost2.
    val ownIds = own.mapNotNull { it.id }.toSet()
    val sharedIds = shareDao.selectActiveRecordIdsByKost2(
      kost2Ids, from.year * 100 + from.monthValue, until.year * 100 + until.monthValue,
    ) - ownIds
    // Loaded with the same period predicates as the own records (the period of the share query only narrows the ids).
    val shared = sharedIds.chunked(CHUNK_SIZE).flatMap { chunk ->
      val queryFilter = recordsQueryFilter(from, until)
      queryFilter.add(isIn("id", chunk))
      buchungssatzDao.select(queryFilter, checkAccess = false)
    }
    val kost2IdSet = kost2Ids.toSet()
    return inRange(shareService.expand(own + shared).filter { it.kost2?.id in kost2IdSet })
  }

  private fun recordsQueryFilter(from: LocalDate, until: LocalDate): QueryFilter {
    val queryFilter = queryFilter()
    buchungssatzDao.addTimePeriodPredicates(queryFilter, from.year, from.monthValue, until.year, until.monthValue)
    return queryFilter
  }

  private fun projectInfo(projectId: Long): ContributionMarginProjectInfo? {
    val project = caches.getProjekt(projectId) ?: return null
    return ContributionMarginProjectInfo(
      kost = project.kost,
      customer = caches.getKundeIfNotInitialized(project.kunde)?.name,
      project = project.name,
      projectGroup = CustomerGroupService.instanceOrNull?.projectGroupNameOf(projectId),
      identifier = project.identifier?.takeIf { it.isNotBlank() },
    )
  }

  private fun isFibu(user: PFUserDO): Boolean {
    return userGroupCache.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)
  }

  /** As the project manager access of the order book (`AuftragRight`): the manager group or a project contact. */
  private fun isOwnProject(user: PFUserDO, project: ProjektDO): Boolean {
    return userGroupCache.isUserMemberOfGroup(user.id, project.projektManagerGroupId)
        || project.isContact(user.id)
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

    /** Order positions of these (or of orders with these) get time sheets only if a task has no others. */
    private val INACTIVE_STATUS = setOf(AuftragsStatus.ABGELEHNT, AuftragsStatus.ERSETZT)
  }
}
