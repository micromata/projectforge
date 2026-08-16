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

package org.projectforge.rest.fibu

import mu.KotlinLogging
import org.projectforge.NextMigration
import org.projectforge.SystemStatus
import org.projectforge.business.PfCaches
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.fibu.*
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.SortPropertyComparator
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.core.ValidationUtils
import org.projectforge.rest.dto.Auftrag
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.inGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/order")
open class OrderEntityRest : // open needed by Wicket's SpringBean for proxying.
  AbstractDTOEntityRest<AuftragDO, Auftrag, AuftragDao>(AuftragDao::class.java, "fibu.auftrag.title") {

  @Autowired
  private lateinit var orderAccessChecker: AccessChecker

  @Autowired
  private lateinit var domainService: DomainService

  @Autowired
  private lateinit var forecastOrderAnalysis: ForecastOrderAnalysis

  @Autowired
  private lateinit var orderExport: OrderExport

  @Autowired
  private lateinit var forecastExport: ForecastExport

  /**
   * Builds a fresh [AuftragDO] instead of mutating the persisted one on purpose: the persistence layer
   * merges the posted object over the database row (`BaseDOPersistenceService.privateUpdate` ->
   * `CandHMaster.copyValues`) and `AuftragRight.hasAccess(obj, oldObj)` compares the posted object
   * against the persisted one — mutating the latter would compare it against itself and defeat the
   * `vollstaendigFakturiert` protection.
   *
   * Because of that merge, every field the DTO doesn't carry ends up as null in the database. The
   * attachment fields are such fields (they are written by the attachment endpoints, not by this form),
   * so they are copied back from the database row, exactly as `AuftragEditPage.update` does.
   */
  override fun transformForDB(dto: Auftrag): AuftragDO {
    val auftragDO = AuftragDO()
    dto.copyTo(auftragDO)
    if (auftragDO.kunde != null) {
      // A customer chosen from the list wins over the free text one, see AuftragEditPage.onSaveOrUpdate.
      auftragDO.kundeText = null
    }
    if (auftragDO.nummer == null) {
      auftragDO.nummer = baseDao.getNextNumber(auftragDO)
    }
    assignNumbersToNewRows(auftragDO)
    dto.id?.let { id ->
      baseDao.find(id, checkAccess = false)?.let { dbObj ->
        auftragDO.attachmentsCounter = dbObj.attachmentsCounter
        auftragDO.attachmentsNames = dbObj.attachmentsNames
        auftragDO.attachmentsIds = dbObj.attachmentsIds
        auftragDO.attachmentsSize = dbObj.attachmentsSize
        auftragDO.attachmentsLastUserAction = dbObj.attachmentsLastUserAction
      }
    }
    return auftragDO
  }

  override fun transformFromDB(obj: AuftragDO, editMode: Boolean): Auftrag {
    val auftrag = Auftrag()
    // Only the edit page needs the positions and the payment schedules, and only it can afford them:
    // both are lazy, so mapping them is a query per order (see Auftrag.copyFrom).
    if (editMode) {
      auftrag.copyFromWithCollections(obj)
    } else {
      auftrag.copyFrom(obj)
    }
    auftrag.deleteAccess = baseDao.hasLoggedInUserDeleteAccess(obj, obj, false)
    auftrag.writeAccess = if (obj.id == null) {
      baseDao.hasLoggedInUserInsertAccess(obj, false)
    } else {
      baseDao.hasLoggedInUserUpdateAccess(obj, obj, false)
    }
    // Mirrors AuftragEditForm, which shows the checkboxes for FIBU_AUSGANGSRECHNUNGEN = READWRITE while
    // AuftragRight enforces FINANCE group membership on write. Kept asymmetric on purpose: hiding what
    // Wicket hides, and leaving the DAO the authority on what may be written.
    auftrag.vollstaendigFakturiertWriteAccess = orderAccessChecker.hasLoggedInUserRight(
      RechnungDao.USER_RIGHT_ID, false, UserRightValue.READWRITE
    )
    if (obj.id == null) {
      return auftrag
    }
    if (auftrag.erfassungsDatum == null) {
      // Old orders may have no date of entry, see AuftragEditPage.onPreEdit.
      auftrag.erfassungsDatum = obj.created?.let { PFDay.from(it).localDate }
        ?: auftrag.angebotsDatum
            ?: LocalDate.now()
    }
    auftrag.sendEMailNotification = !orderAccessChecker.userEqualsToContextUser(obj.contactPerson)
    return auftrag
  }

  /**
   * Presets the status, the date of entry, the date of offer and the contact person of a new order, as
   * `AuftragEditPage.onPreEdit` does. The date of decision stays empty: it is only known once the
   * customer has decided.
   */
  override fun newBaseDTO(request: HttpServletRequest?): Auftrag {
    val auftrag = super.newBaseDTO(request)
    val today = LocalDate.now()
    // `AuftragDO.status` has no default, and `AuftragDao.onInsertOrModify` refuses null (as well as
    // OPTIONAL). Wicket gets away without a preset because its drop down cannot be empty
    // (`setNullValid(false)`, AuftragEditForm), which silently makes the first constant the answer —
    // so that constant is named here instead, for both frontends.
    auftrag.status = AuftragsStatus.IN_ERSTELLUNG
    auftrag.angebotsDatum = today
    auftrag.erfassungsDatum = today
    if (orderAccessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.PROJECT_MANAGER)) {
      // The project manager is the contact person of their own order, so there is nobody to notify.
      auftrag.contactPerson = ThreadLocalUserContext.loggedInUser?.let { org.projectforge.rest.dto.User(it) }
      auftrag.sendEMailNotification = false
    }
    return auftrag
  }

  /**
   * Sends the notification mail to the contact person, if the form asked for it.
   *
   * Deliberate deviation from Wicket, which notifies on an update only for
   * `EntityCopyStatus.MAJOR`: that status doesn't reach this hook, so every save notifies here. The
   * checkbox is unchecked by default whenever the contact person is the logged-in user, so this affects
   * the case of an explicit request only.
   *
   * A failing notification must never fail the save: this hook runs after the order has been committed
   * (`AbstractPagesRestUtils.saveOrUpdate`), so an exception escaping here would answer a written order
   * with HTTP 406 and the client would show an error for a change that did happen. Mail delivery is out
   * of the user's hands anyway (no reachable SMTP server, no address for the contact person), hence it is
   * logged rather than reported.
   */
  override fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: AuftragDO, postData: PostData<Auftrag>) {
    super.onAfterSaveOrUpdate(request, obj, postData)
    if (!postData.data.sendEMailNotification) {
      return
    }
    val operationType = if (postData.data.id == null) OperationType.INSERT else OperationType.UPDATE
    val url = domainService.getDomain(
      NextMigration.standardEditPage(category).replace(NextMigration.ID_PLACEHOLDER, "${obj.id}")
    )
    try {
      baseDao.sendNotificationIfRequired(obj, operationType, url)
    } catch (ex: Exception) {
      log.error(ex) { "Order #${obj.nummer} (id=${obj.id}) was saved, but sending the notification mail failed: ${ex.message}" }
    }
  }

  /**
   * The rules the period of performance adds on top of the field rules, which are validated generically
   * for the order and its nested rows alike ([ValidationUtils.validateFields]).
   *
   * These cannot be expressed as field annotations: they compare two dates, and two of them make a field
   * mandatory only depending on another row's `periodOfPerformanceType`.
   */
  override fun validate(validationErrors: MutableList<ValidationError>, dto: Auftrag) {
    super.validate(validationErrors, dto)
    PeriodOfPerformanceValidator.validate(
      periodOfPerformanceBegin = dto.periodOfPerformanceBegin,
      periodOfPerformanceEnd = dto.periodOfPerformanceEnd,
      positions = dto.positionen?.filter { !it.deleted }?.map { position ->
        PeriodOfPerformanceValidator.Position(
          type = position.periodOfPerformanceType,
          begin = position.periodOfPerformanceBegin,
          end = position.periodOfPerformanceEnd,
        )
      },
    ).forEach { error ->
      val message = error.labelKey?.let { translateMsg(error.messageKey, translate(it)) } ?: translate(error.messageKey)
      validationErrors.add(ValidationError(message, fieldId = error.fieldId, messageId = error.messageKey))
    }
  }

  @PostConstruct
  private fun postConstruct() {
    /**
     * Enable attachments for this entity.
     */
    enableJcr()
  }

  /**
   * Adds the statistics of the whole result set, the ones the Wicket list shows above its table
   * (`AuftragListForm.addStatistics`).
   *
   * Computed here rather than in the browser: two of the six sums — the acquisition sum and the not yet
   * invoiced sum — are no property of the [Auftrag] DTO at all, and which statuses count as commissioned
   * and how a probability of occurrence weighs into the acquisition sum is [OrderInfo]'s business. A
   * second implementation on the client would be a second answer. It costs nothing beyond the rows that
   * were loaded anyway: [AuftragsStatistik.add] is a map lookup in [AuftragsCache] per order.
   *
   * Sent as data, not as the markdown [ResultSet.resultInfo] carries for the legacy React app: the hand
   * built next page formats currency in the user's locale and takes its colours from css tokens (see
   * OrderStatisticsLine there).
   */
  override fun postProcessResultSet(
    resultSet: ResultSet<AuftragDO>,
    request: HttpServletRequest,
    magicFilter: MagicFilter,
  ): ResultSet<*> {
    val orders = resultSet.resultSet
    return super.postProcessResultSet(resultSet, request, magicFilter).also {
      it.statistics = OrderStatistics(baseDao.buildStatistik(orders))
    }
  }

  /**
   * Opts the order list into the lean row: [Auftrag.copyFrom4ListRow] fills only the columns of
   * `order.page.tsx`, which is what the hand built next page renders.
   *
   * Measured on the order book of a real installation (7132 rows): 1755 B/row become 741 B/row, 12.5 MB
   * become 5.3 MB, and 1.5 MB become 549 KB on the wire. Most of it is the four manager DTOs (~524 B/row
   * for a column showing nothing but the derived `assignedPersons` string), the sums travelling twice (raw
   * and pre-formatted), and a dozen fields no column reads.
   */
  override fun newDTO(): Auftrag {
    return Auftrag()
  }

  /**
   * The sums and counters of a list of orders, each counter being the number of orders that contributed
   * to the sum beside it — a sum of 0.00 over 0 orders is a line the client leaves out, exactly as the
   * Wicket page hides it.
   */
  class OrderStatistics(statistics: AuftragsStatistik) {
    val netSum: BigDecimal = statistics.nettoSum
    val counter: Int = statistics.counter
    val akquiseSum: BigDecimal = statistics.akquiseSum
    val counterAkquise: Int = statistics.counterAkquise
    val commissionedSum: BigDecimal = statistics.beauftragtSum
    val counterCommissioned: Int = statistics.counterBeauftragt
    val invoicedSum: BigDecimal = statistics.invoicedSum
    val counterInvoiced: Int = statistics.counterInvoiced
    val notYetInvoicedSum: BigDecimal = statistics.notYetInvoicedSum
    val counterNotYetInvoiced: Int = statistics.counterNotYetInvoiced
    val toBeInvoicedSum: BigDecimal = statistics.toBeInvoiced
    val counterToBeInvoiced: Int = statistics.counterToBeInvoiced
  }

  override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
    // The group the position filters belong to, hand-set: the three below are not derived from a property,
    // and the ones that are get "Pos." from AuftragDO.positionen (label.position.short), which is the
    // abbreviation of a table column, not a heading.
    val positions = translate("fibu.auftrag.positions")
    elements.filterIsInstance<UIFilterElement>()
      .filter { it.id.startsWith("positionen.") }
      .forEach { it.group = positions }
    elements.add(
      UIFilterListElement("positionsArt", label = translate("fibu.auftrag.position.art"))
        .buildValues(AuftragsPositionsArt::class.java)
        .inGroup(positions, translate("fibu.auftrag.position.art"))
    )
    elements.add(
      UIFilterListElement("positionsStatus", label = translate("fibu.auftrag.positions"), defaultFilter = true)
        .buildValues(AuftragsStatus::class.java)
        .inGroup(positions, translate("status"))
    )
    elements.add(
      UIFilterListElement("positionsPaymentType", label = translate("fibu.auftrag.position.paymenttype"))
        .buildValues(AuftragsPositionsPaymentType::class.java)
        .inGroup(positions, translate("fibu.auftrag.position.paymenttype"))
    )
    elements.add(
      UIFilterListElement("fakturiert", label = translate("fibu.auftrag.status.fakturiert"), defaultFilter = true)
        .buildValues(AuftragFakturiertFilterStatus::class.java)
    )
    val statusFilter = elements.find { it is UIFilterElement && it.id == "status" } as UIFilterElement
    statusFilter.defaultFilter = true
    // The two ends of the period of performance are one question, as the edit form asks it: one label,
    // two dates. `searchFields` yields them as two independent date range filters ("Leistungszeitraum
    // von" with its own from/to, the same for "bis"), which are four dates for what a user reads as a
    // single time window — and each of them alone matches orders whose *other* end lies outside it.
    elements.removeIf { it is UIFilterElement && it.id in PERIOD_OF_PERFORMANCE_FIELDS }
    elements.add(
      UIFilterElement(
        PERIOD_OF_PERFORMANCE_FILTER,
        UIFilterElement.FilterType.DATE,
        label = translate("fibu.periodOfPerformance"),
      )
    )
  }

  override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<AuftragDO>>? {
    val filters = mutableListOf<CustomResultFilter<AuftragDO>>()

    val positionTypeFilter = source.entries.find { it.field == "positionsArt" }
    positionTypeFilter?.synthetic = true // Don't process this filter by data base.
    positionTypeFilter?.value?.values?.let {
      if (it.isNotEmpty()) {
        filters.add(AuftragsPositionsArtFilter.create(it))
      }
    }

    val positionsStatusFilter = source.entries.find { it.field == "positionsStatus" }
    positionsStatusFilter?.synthetic = true // Don't process this filter by data base.
    positionsStatusFilter?.value?.values?.let {
      if (it.isNotEmpty()) {
        filters.add(AuftragsPositionsStatusFilter.create(it))
      }
    }

    val paymentTypeFilter = source.entries.find { it.field == "positionsPaymentType" }
    paymentTypeFilter?.synthetic = true // Don't process this filter by data base.
    paymentTypeFilter?.value?.values?.let {
      if (it.isNotEmpty()) {
        filters.add(AuftragsPositionsPaymentTypeFilter.create(it))
      }
    }

    val fakturiertFilter = source.entries.find { it.field == "fakturiert" }
    fakturiertFilter?.synthetic = true // Don't process this filter by data base.
    fakturiertFilter?.value?.values?.let {
      if (it.isNotEmpty()) {
        filters.add(AuftragFakturiertFilter.create(it))
      }
    }
    addPeriodOfPerformanceCriterion(target, source)
    return filters
  }

  /**
   * Turns the single [PERIOD_OF_PERFORMANCE_FILTER] entry into the overlap criterion Wicket's
   * "Leistungszeitraum" fieldset uses (see [AuftragAndRechnungDaoHelper]): an order matches if its own
   * period reaches into the window asked for — end not before the window's start, begin not after its
   * end. Filtering `periodOfPerformanceBegin` by the window instead would hide a two-year order from a
   * one-month window it runs right through.
   *
   * Synthetic, because the entry's field is no property of [AuftragDO]: the predicate is over two of
   * them, so [MagicFilterProcessor] cannot derive it.
   */
  private fun addPeriodOfPerformanceCriterion(target: QueryFilter, source: MagicFilter) {
    val entry = source.entries.find { it.field == PERIOD_OF_PERFORMANCE_FILTER } ?: return
    entry.synthetic = true
    val filter = object : SearchFilterWithPeriodOfPerformance {
      override val periodOfPerformanceStartDate = PFDayUtils.parseDate(entry.value.fromValue)
      override val periodOfPerformanceEndDate = PFDayUtils.parseDate(entry.value.toValue)
    }
    AuftragAndRechnungDaoHelper.createCriterionForPeriodOfPerformance(filter).ifPresent { target.add(it) }
  }

  /**
   * Takes the columns no `ORDER BY` can express out of the query — [filterList] sorts by those.
   *
   * `addOrder` swallows the exception such a property causes, so the query went out with no `ORDER BY`
   * at all and the list came back in whatever order the database produced — the sort a user asked for
   * silently did nothing.
   */
  override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
    target.sortProperties.removeIf { COMPUTED_SORT_PROPERTIES.containsKey(it.property) }
  }

  /**
   * Sorts the loaded orders by the columns that are no database column, the way the Wicket list page has
   * always done it (`MyListPageSortableDataProvider` with `MyBeanComparator`).
   *
   * Eight of the list's columns are computed. The four sums and the person days are `@get:Transient`
   * getters of [AuftragDO] over [OrderInfo], and the position count is a string ("#3"); all six are
   * readable here for free, because [AuftragsCache] answered them for every row that was loaded anyway —
   * a map lookup per comparison, not a query. The count sorts numerically, so `#2` comes before `#10`.
   *
   * The customer and the project are the other two, and they sort by the very string the cell shows —
   * `displayName`, formatted by `KostFormatter` out of number and name ("473 - Air Liquide",
   * "5.100.06: Xmlgate"), which is also what the Wicket list sorts its two columns by. Ordering by
   * `kunde.name`/`projekt.name` instead, as this did before, produced a list that looks unsorted to
   * whoever reads it: the numbers lead every cell, so the names they hide sort in no visible order. The
   * numbers are left padded, so ordering by the string is ordering by the number.
   *
   * The place for it: the whole result set is loaded (there is no server side paging yet, see
   * `MIGRATION-list-paging.md`), so sorting it here is sorting all of it. Once paging lands, this moves
   * onto the materialized id list — the ordering itself stays as it is.
   *
   * Sorts only by what [postProcessMagicFilter] took out of the query, and leaves the order the database
   * produced alone otherwise: a stable sort by nothing is not the same as no sort.
   */
  override fun filterList(resultSet: MutableList<AuftragDO>, filter: MagicFilter): List<AuftragDO> {
    val computed = filter.sortProperties.filter { COMPUTED_SORT_PROPERTIES.containsKey(it.property) }
    if (computed.isEmpty()) {
      return resultSet
    }
    // The number as the last criterion, so equal values (0.00 is the most common one of all four sums,
    // and a customer has many orders) keep a deterministic order instead of shifting between two
    // requests over the same data.
    val sortProperties = computed + SortProperty.desc(AuftragDO::nummer.name)
    return SortPropertyComparator.sort(resultSet, sortProperties) { order, property ->
      COMPUTED_SORT_PROPERTIES[property]?.invoke(order)
    }
  }

  /**
   * The sums of an order as they are right now in the form, i.e. computed on the posted state, not on
   * the stored one.
   *
   * Needed because a hand built form has to show the same sums the list and the Wicket page show, and
   * those are calculated server side by [OrderInfo] from the whole order - a client re-implementing that
   * would drift. The cache is no help either: it answers empty sums for an order without an id, and the
   * stored ones for an order whose form the user has changed.
   *
   * Not a `saveOrUpdate` in disguise: nothing is written, so the read access has to be checked here.
   */
  @PostMapping("recalculate")
  fun recalculate(@RequestBody postData: PostData<Auftrag>): OrderSums {
    baseDao.hasLoggedInUserSelectAccess(throwException = true)
    val order = AuftragDO()
    postData.data.copyTo(order)
    val info = Auftrag.calculateOrderInfo(order)
    val period = effectivePeriodOfPerformance(info)
    return OrderSums(
      netSum = info.netSum,
      commissionedNetSum = info.commissionedNetSum,
      akquiseSum = info.akquiseSum,
      invoicedSum = info.invoicedSum,
      notYetInvoicedSum = info.notYetInvoicedSum,
      toBeInvoicedSum = info.toBeInvoicedSum,
      personDays = info.personDays,
      weightedProbabilityOfOccurrence = ForecastUtils.getWeightedProbabilityOfAccurence(info),
      vollstaendigFakturiert = info.isVollstaendigFakturiert,
      toBeInvoiced = info.toBeInvoiced,
      periodOfPerformanceBegin = period.first,
      periodOfPerformanceEnd = period.second,
      positions = info.infoPositions?.map { position ->
        PositionSums(
          number = position.number,
          netSum = position.netSum,
          invoicedSum = position.invoicedSum,
          notYetInvoicedSum = position.notYetInvoiced,
          toBeInvoiced = position.toBeInvoiced,
          probabilityOfOccurrence = ForecastUtils.getProbabilityOfAccurence(info, position),
        )
      },
    )
  }

  /**
   * The period of performance the order actually spans, over all of its positions: the earliest begin
   * and the latest end each position effectively has.
   *
   * A position of type [PeriodOfPerformanceType.OWN] carries its own two dates, every other one refers
   * to the order's - the same rule [ForecastUtils.getStartLeistungszeitraum] applies, spelled out here
   * because that one substitutes *today* for a date that is not set. Today is the right answer for a
   * forecast, which has to distribute the net sum over some months; it is the wrong one for a form,
   * where it would show a period the user never entered.
   *
   * Deleted positions don't count, and an order without any position is its own period.
   */
  private fun effectivePeriodOfPerformance(info: OrderInfo): Pair<LocalDate?, LocalDate?> {
    val positions = info.infoPositions?.filter { !it.deleted }
    if (positions.isNullOrEmpty()) {
      return info.periodOfPerformanceBegin to info.periodOfPerformanceEnd
    }
    val own = { pos: OrderPositionInfo -> pos.periodOfPerformanceType == PeriodOfPerformanceType.OWN }
    return positions.mapNotNull { if (own(it)) it.periodOfPerformanceBegin else info.periodOfPerformanceBegin }
      .minOrNull() to
        positions.mapNotNull { if (own(it)) it.periodOfPerformanceEnd else info.periodOfPerformanceEnd }
          .maxOrNull()
  }

  /**
   * The sums of one order.
   *
   * Positions are identified by `number`, not by id: a position the user just added has no id, and
   * number is the key of a position inside its order anyway.
   */
  class OrderSums(
    val netSum: BigDecimal,
    val commissionedNetSum: BigDecimal,
    val akquiseSum: BigDecimal,
    val invoicedSum: BigDecimal,
    val notYetInvoicedSum: BigDecimal,
    val toBeInvoicedSum: BigDecimal,
    val personDays: BigDecimal,
    /**
     * The probability the forecast effectively works with, weighted over the positions' net sums - see
     * [ForecastUtils.getWeightedProbabilityOfAccurence]. Null for an order whose positions have no net sum.
     */
    val weightedProbabilityOfOccurrence: BigDecimal?,
    val vollstaendigFakturiert: Boolean,
    val toBeInvoiced: Boolean,
    /**
     * Begin of the period of performance over all positions, i.e. the earliest one any of them
     * effectively has - see [effectivePeriodOfPerformance]. Null where neither the order nor a position
     * of its own states one.
     */
    val periodOfPerformanceBegin: LocalDate?,
    /** End of that same period: the latest one any position effectively has. */
    val periodOfPerformanceEnd: LocalDate?,
    val positions: List<PositionSums>?,
  )

  class PositionSums(
    val number: Short?,
    val netSum: BigDecimal,
    val invoicedSum: BigDecimal,
    val notYetInvoicedSum: BigDecimal,
    /**
     * Whether this position is due to be invoiced - see [OrderPositionInfo.recalculateAll]. Unlike
     * [notYetInvoicedSum], which is positive for every commissioned position that is not fully invoiced,
     * this is what marks a position as overdue: the position or its order is closed, or a payment schedule
     * entry of this position has been reached.
     */
    val toBeInvoiced: Boolean,
    /**
     * The probability the forecast applies to this position, following from the status of the order and of
     * the position - see [ForecastUtils.getProbabilityOfAccurence]. A factor between 0 and 1.
     */
    val probabilityOfOccurrence: BigDecimal,
  )

  /**
   * The forecast analysis of a stored order as an html fragment, the same one Wicket shows in its modal
   * dialog (`HtmlPreviewModalDialog`).
   *
   * Only the id based overload of [ForecastOrderAnalysis.htmlExport] is exposed: it loads the order
   * through [AuftragDao] and thereby checks the read access, while the `htmlExport(orderInfo)` overload
   * takes an already loaded order and checks nothing.
   */
  @GetMapping("forecastAnalysis/{id}")
  fun forecastAnalysis(@PathVariable("id") id: Long): String {
    return forecastOrderAnalysis.htmlExport(orderId = id)
  }

  /**
   * The same analysis as raw json, for comparing the forecast against the numbers of an export.
   * Development only, as in Wicket (`AuftragEditPage`, "Export as json (dev)").
   */
  @GetMapping("forecastAnalysisJson/{id}")
  fun forecastAnalysisJson(@PathVariable("id") id: Long): ResponseEntity<*> {
    if (!SystemStatus.isDevelopmentMode()) {
      return ResponseEntity.notFound().build<Any>()
    }
    val order = baseDao.find(id) // Throws an AccessException if the user may not read the order.
    val analysis = forecastOrderAnalysis.exportOrderAnalysis(id)
      ?: return ResponseEntity.notFound().build<Any>()
    return RestUtils.downloadFile("orderAnalysis-${order?.nummer}.json", JsonUtils.toJson(analysis))
  }

  /**
   * The whole filtered list as the three sheet Excel file Wicket's "Excel export" produces
   * (`AuftragListPage`, [OrderExport]).
   *
   * The rows come from [getResultList], i.e. through the same pipeline the list itself uses
   * (`preProcessMagicFilter` with its synthetic position filters, then `filterList`) - calling
   * `getObjectList` directly would export orders the list doesn't show.
   *
   * An empty result answers 404 rather than a file: Wicket reports it as a form error
   * (`datatable.no-records-found`), and a downloaded file saying "nothing to export" looks like a
   * successful export in the download folder.
   */
  @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
  fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
    log.info("Exporting orders as Excel file.")
    val list = getResultList(filter)
    val xls = orderExport.export(list)
    if (xls == null || xls.isEmpty()) {
      return ResponseEntity.notFound().build<Any>()
    }
    val filename = "ProjectForge-OrderExport_${DateHelper.getDateAsFilenameSuffix(Date())}.xls"
    return RestUtils.downloadFile(filename, xls)
  }

  /**
   * What the forecast export dialog of the next frontend is preset with, remembered per user.
   *
   * Unlike Wicket, which derives the start month silently from the period of performance filter, the
   * next frontend asks for it: the start date is what the whole sheet is laid out around, so it is a
   * question of the export and not of the list. Storing the answer means it only has to be given once.
   */
  @GetMapping("forecastExportSettings")
  fun getForecastExportSettings(): ForecastExportSettings {
    baseDao.hasLoggedInUserSelectAccess(throwException = true)
    val stored = userPrefService.getEntry(category, USER_PREF_PARAM_FORECAST_EXPORT, ForecastExportSettings::class.java)
    return ForecastExportSettings(
      // The same fallback ForecastExport itself uses for a filter without a period of performance.
      startDate = stored?.startDate ?: PFDay.now().beginOfYear.localDate,
      distributeUnusedBudget = stored?.distributeUnusedBudget
        ?: ForecastOrderPosInfo.defaultDistributeUnusedBudget,
    )
  }

  /**
   * The forecast of the filtered orders as the xlsx file Wicket's "Forecast" button produces
   * ([ForecastExport]), with the start date and the budget variant taken from the dialog instead of from
   * the filter, and remembered for the next time.
   *
   * The settings are stored before the export runs: it is the answer the user gave, and it should be
   * preset next time whether or not there was anything to export.
   */
  @PostMapping("exportForecast")
  fun exportForecast(@RequestBody request: ForecastExportRequest): ResponseEntity<*> {
    log.info("Exporting forecast of orders as Excel file.")
    val settings = request.settings ?: ForecastExportSettings()
    userPrefService.putEntry(category, USER_PREF_PARAM_FORECAST_EXPORT, settings, true)
    val filter = toAuftragFilter(request.filter ?: MagicFilter())
    // The dialog's answer replaces the filter's, which is what Wicket goes by.
    filter.periodOfPerformanceStartDate = settings.startDate
    filter.periodOfPerformanceEndDate = null
    val xls = forecastExport.xlsExport(
      filter,
      distributeUnusedBudget = settings.distributeUnusedBudget,
      copyAllFilterCriteria = true,
    )
    if (xls == null || xls.isEmpty()) {
      return ResponseEntity.notFound().build<Any>()
    }
    return RestUtils.downloadFile(
      forecastExport.getExcelFilenmame(filter, settings.distributeUnusedBudget),
      xls,
    )
  }

  /** What the forecast export dialog asks for, and what is remembered of it per user. */
  class ForecastExportSettings(
    /**
     * The month the forecast starts with. [ForecastExport] takes the begin of its month, so any day of
     * it means the same thing.
     */
    var startDate: LocalDate? = null,
    /** See [ForecastOrderPosInfo.distributeUnusedBudget] - the optimistic variant of the forecast. */
    var distributeUnusedBudget: Boolean? = null,
  )

  /**
   * The two halves of a forecast export: what to export and how. Separate objects, so the filter never
   * ends up in what is stored as the user's settings.
   */
  class ForecastExportRequest(
    var filter: MagicFilter? = null,
    var settings: ForecastExportSettings? = null,
  )

  companion object {
    /**
     * The list's [MagicFilter] as the [AuftragFilter] the two exports of `projectforge-business` take.
     *
     * A translation rather than a shared filter object: the exports predate the magic filter and are used
     * by Wicket and by the forecast scripts as well, so they keep their own filter type. The field names
     * are the ones [addMagicFilterElements] declares and [preProcessMagicFilter] reads.
     *
     * `user` and `projectList` stay empty - the order list of the next frontend offers no filter for
     * either, and guessing one from the search string would filter by something nobody asked for.
     *
     * `internal` and in the companion object rather than a private method, as [assignNumbersToNewRows]:
     * it needs nothing of the instance, which is what makes it testable without a Spring context
     * (`OrderFilterTest`).
     */
    internal fun toAuftragFilter(magicFilter: MagicFilter): AuftragFilter {
      val filter = AuftragFilter()
      filter.searchString = magicFilter.searchString
      magicFilter.entries.forEach { entry ->
        val values = entry.value.values?.filter { it.isNotBlank() } ?: emptyList()
        when (entry.field) {
          "status" -> filter.auftragsStatuses.addAll(values.mapNotNull { AuftragsStatus.safeValueOf(it) })
          "positionsArt" -> filter.auftragsPositionsArten.addAll(
            values.mapNotNull { AuftragsPositionsArt.safeValueOf(it) })
          // Single valued in AuftragFilter, multi valued in the filter panel: the export can only be told
          // one, so the first is used and the rest is lost - the same choice the legacy filter form offers.
          "positionsPaymentType" -> filter.auftragsPositionsPaymentType =
            values.firstNotNullOfOrNull { AuftragsPositionsPaymentType.safeValueOf(it) }

          // A java enum, so it has no safeValueOf of its own.
          "fakturiert" -> filter.auftragFakturiertFilterStatus = values.firstNotNullOfOrNull { value ->
            AuftragFakturiertFilterStatus.values().firstOrNull { it.name == value }
          }

          PERIOD_OF_PERFORMANCE_FILTER -> {
            filter.periodOfPerformanceStartDate = PFDayUtils.parseDate(entry.value.fromValue)
            filter.periodOfPerformanceEndDate = PFDayUtils.parseDate(entry.value.toValue)
          }
          // The date the order was entered, which AuftragFilter calls startDate/endDate.
          AuftragDO::erfassungsDatum.name -> {
            filter.startDate = PFDayUtils.parseDate(entry.value.fromValue)
            filter.endDate = PFDayUtils.parseDate(entry.value.toValue)
          }
        }
      }
      return filter
    }

    /**
     * Numbers the rows the client added, leaving every stored row untouched: `number` is what the
     * collection handler matches a posted row against its database row by, and `AuftragRight` looks a
     * position up by number to protect `vollstaendigFakturiert` — renumbering an existing position would
     * make both compare the wrong pairs.
     *
     * A payment schedule points at a position by number as well, so a schedule referring to a position
     * that just got its number has to follow. The old number of a new position is the placeholder the
     * client gave it — the next free one as far as the form can tell (see `nextPositionNumber` of the
     * next frontend), so that an instalment can refer to a position before it is saved — which is why
     * the mapping is built before anything is renumbered.
     *
     * The next free number is taken from the **stored** positions only: the placeholders are the
     * client's guess and are about to be replaced, so counting them in would leave a gap for every new
     * row (and, for a brand new order, start its positions above 1). The payment schedules follow the
     * same rule, because the client numbers a new instalment as well now — it shows that number in the
     * row's header, and a preview differing from what is stored would be worse than none.
     *
     * A stored row that the client marked deleted still counts: its number stays taken (the row remains
     * in the database, `UNIQUE(auftrag_id, number)`, and `payment#<number>` is its history key), so a
     * number is never reused — a gap is the record of what was deleted, and the next frontend offers to
     * bring such a row back (see `RepeatableList`).
     *
     * `internal` and in the companion object rather than a private method: it needs nothing of the
     * instance, and this is what the numbering of a whole posted order can be tested through
     * (`AuftragDtoTest`) without a Spring context.
     */
    internal fun assignNumbersToNewRows(order: AuftragDO) {
      val positions = order.positionen
      if (positions != null) {
        val storedNumbers = positions.filter { it.id != null }.map { it.number }.toSet()
        var nextNumber = (storedNumbers.maxOrNull() ?: 0).toInt()
        val renumbered = mutableMapOf<Short, Short>()
        positions.filter { it.id == null }.forEach { position ->
          val oldNumber = position.number
          position.number = (++nextNumber).toShort()
          // A placeholder colliding with a stored position's number is not mapped: the schedules
          // pointing at that number mean the stored position, which keeps its number.
          if (oldNumber != position.number && oldNumber !in storedNumbers) {
            renumbered[oldNumber] = position.number
          }
        }
        order.paymentSchedules?.forEach { schedule ->
          schedule.positionNumber?.let { positionNumber ->
            renumbered[positionNumber]?.let { schedule.positionNumber = it }
          }
        }
      }
      // Outside the positions branch: an order may carry a payment schedule and no position of its own
      // (the instalments then refer to nothing), and its new rows still need a number.
      val schedules = order.paymentSchedules ?: return
      var nextScheduleNumber = (schedules.filter { it.id != null }.maxOfOrNull { it.number } ?: 0).toInt()
      schedules.filter { it.id == null }.forEach { schedule ->
        schedule.number = (++nextScheduleNumber).toShort()
      }
    }

    /**
     * The sort ids no database column can answer, and the value each one sorts by (see [filterList]).
     *
     * The ids are the DTO's property names, which is what the list's columns are declared by
     * (`order.page.tsx`) — and what the Wicket list declares as its sort properties too, since
     * [AuftragDO] carries the same names as deprecated getters over `info`.
     *
     * `pos` is the one that is not the value it shows: the cell reads "#3", and comparing that as a string
     * would put #10 before #2. It sorts by the count, and by the same count the cell shows — the deleted
     * positions left out.
     *
     * The customer and the project sort by their `displayName`, which is the string the cell shows: the
     * row carries nothing else of them ([Auftrag.copyFrom4ListRow]). Both are read through [PfCaches] —
     * [filterList] runs before the rows are mapped, so the two relations are still lazy proxies here, and
     * a `displayName` off a proxy would be a select per row. The free text customer stands in for an
     * order naming a customer that is not in the list, as the cell does.
     *
     * `assignedPersons` is deliberately absent: it is a `@Transient` getter of [AuftragDO], so reflection
     * reads it, and it costs a `UserGroupCache` lookup per user rather than a map lookup — the default
     * path handles it. It resolves against the entity, so the sort reaches the query and only its own
     * `addOrder` fails.
     */
    private val COMPUTED_SORT_PROPERTIES = mapOf<String, (AuftragDO) -> Comparable<*>?>(
      Auftrag::nettoSumme.name to { Auftrag.orderInfo(it).netSum },
      Auftrag::beauftragtNettoSumme.name to { Auftrag.orderInfo(it).commissionedNetSum },
      Auftrag::fakturiertSum.name to { Auftrag.orderInfo(it).invoicedSum },
      Auftrag::zuFakturierenSum.name to { Auftrag.orderInfo(it).notYetInvoicedSum },
      Auftrag::personDays.name to { Auftrag.orderInfo(it).personDays },
      Auftrag::pos.name to { order ->
        Auftrag.orderInfo(order).infoPositions?.count { !it.deleted } ?: 0
      },
      CUSTOMER_SORT_PROPERTY to { order ->
        PfCaches.instance.getKundeIfNotInitialized(order.kunde)?.displayName ?: order.kundeText
      },
      PROJECT_SORT_PROPERTY to { order ->
        PfCaches.instance.getProjektIfNotInitialized(order.projekt)?.displayName
      },
    )

    /** Sort ids of the customer and the project column, as `order.page.tsx` declares them. */
    private const val CUSTOMER_SORT_PROPERTY = "kunde.displayName"
    private const val PROJECT_SORT_PROPERTY = "projekt.displayName"

    /**
     * Id of the combined period-of-performance filter — a pseudo field, standing for a criterion over
     * [PERIOD_OF_PERFORMANCE_FIELDS] (see [addPeriodOfPerformanceCriterion]).
     */
    internal const val PERIOD_OF_PERFORMANCE_FILTER = "periodOfPerformance"

    /** The two date properties the combined filter replaces in the filter field list. */
    private val PERIOD_OF_PERFORMANCE_FIELDS = setOf("periodOfPerformanceBegin", "periodOfPerformanceEnd")

    /** User pref name of the forecast export dialog's settings, stored in the `order` area. */
    private const val USER_PREF_PARAM_FORECAST_EXPORT = "forecastExport"
  }
}
