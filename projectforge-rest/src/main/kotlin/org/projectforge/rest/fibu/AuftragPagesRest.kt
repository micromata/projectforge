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

import org.projectforge.NextMigration
import org.projectforge.SystemStatus
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
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.core.ValidationUtils
import org.projectforge.rest.dto.Auftrag
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
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

@RestController
@RequestMapping("${Rest.URL}/order")
open class AuftragPagesRest : // open needed by Wicket's SpringBean for proxying.
  AbstractDTOPagesRest<AuftragDO, Auftrag, AuftragDao>(AuftragDao::class.java, "fibu.auftrag.title") {

  @Autowired
  private lateinit var orderAccessChecker: AccessChecker

  @Autowired
  private lateinit var domainService: DomainService

  @Autowired
  private lateinit var forecastOrderAnalysis: ForecastOrderAnalysis

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

  /**
   * Numbers the rows the client added, leaving every stored row untouched: `number` is what the
   * collection handler matches a posted row against its database row by, and `AuftragRight` looks a
   * position up by number to protect `vollstaendigFakturiert` — renumbering an existing position would
   * make both compare the wrong pairs.
   *
   * A payment schedule points at a position by number as well, so a schedule referring to a position
   * that just got its number has to follow. The old number of a new position is the placeholder the
   * client sent (0 for the first one, since the form has no numbers to give), which is why the mapping
   * is built before anything is renumbered.
   */
  private fun assignNumbersToNewRows(order: AuftragDO) {
    val positions = order.positionen ?: return
    var nextNumber = (positions.maxOfOrNull { it.number } ?: 0).toInt()
    val renumbered = mutableMapOf<Short, Short>()
    positions.filter { it.id == null }.forEach { position ->
      val oldNumber = position.number
      position.number = (++nextNumber).toShort()
      renumbered[oldNumber] = position.number
    }
    order.paymentSchedules?.forEach { schedule ->
      schedule.positionNumber?.let { positionNumber ->
        renumbered[positionNumber]?.let { schedule.positionNumber = it }
      }
    }
    var nextScheduleNumber = (order.paymentSchedules?.maxOfOrNull { it.number } ?: 0).toInt()
    order.paymentSchedules?.filter { it.id == null }?.forEach { schedule ->
      schedule.number = (++nextScheduleNumber).toShort()
    }
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
   * Presets the status, the three dates and the contact person of a new order, as
   * `AuftragEditPage.onPreEdit` does.
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
    auftrag.entscheidungsDatum = today
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
    baseDao.sendNotificationIfRequired(obj, operationType, url)
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
   * Opts the order list into the lean row: [Auftrag.copyFrom4ListRow] fills only the 19 columns of
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

  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
    layout.add(UITable.createUIResultSetTable()
          .add(lc, "nummer")
          .add(UITableColumn("kunde.displayName", title = "fibu.kunde"))
          .add(UITableColumn("projekt.displayName", title = "fibu.projekt"))
          .add(lc, "titel")
          .add(UITableColumn("pos", title = "label.position.short"))
          .add(UITableColumn("attachmentsSizeFormatted", titleIcon = UIIconType.PAPER_CLIP))
          .add(
            UITableColumn(
              "personDays", title = "projectmanagement.personDays",
              dataType = UIDataType.DECIMAL
            )
          )
          .add(lc, "referenz")
          .add(
            UITableColumn(
              "assignedPersons", title = "fibu.common.assignedPersons",
              dataType = UIDataType.STRING
            )
          )
          .add(lc, "erfassungsDatum", "entscheidungsDatum")
          .add(
            UITableColumn(
              "formattedNettoSumme", title = "fibu.auftrag.nettoSumme",
              dataType = UIDataType.DECIMAL
            )
          )
          .add(
            UITableColumn(
              "formattedBeauftragtNettoSumme", title = "fibu.auftrag.commissioned",
              dataType = UIDataType.DECIMAL
            )
          )
          .add(UITableColumn("formattedFakturiertSum", title = "fibu.fakturiert"))
          .add(UITableColumn("formattedZuFakturierenSum", title = "fibu.toBeInvoiced"))
          .add(lc, "periodOfPerformanceBegin", "periodOfPerformanceEnd", "probabilityOfOccurrence", "status")
      )
    layout.getTableColumnById("erfassungsDatum").formatter = UITableColumn.Formatter.DATE
    layout.getTableColumnById("entscheidungsDatum").formatter = UITableColumn.Formatter.DATE
    layout.getTableColumnById("periodOfPerformanceBegin").formatter = UITableColumn.Formatter.DATE
    layout.getTableColumnById("periodOfPerformanceEnd").formatter = UITableColumn.Formatter.DATE
  }

  override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
    elements.add(
      UIFilterListElement("positionsArt", label = translate("fibu.auftrag.position.art"), defaultFilter = true)
        .buildValues(AuftragsPositionsArt::class.java)
    )
    elements.add(
      UIFilterListElement("positionsStatus", label = translate("fibu.auftrag.positions"), defaultFilter = true)
        .buildValues(AuftragsStatus::class.java)
    )
    elements.add(
      UIFilterListElement(
        "positionsPaymentType",
        label = translate("fibu.auftrag.position.paymenttype"),
        defaultFilter = true
      )
        .buildValues(AuftragsPositionsPaymentType::class.java)
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
   * Orders the customer and project columns by the name they show, not by the computed string.
   *
   * Both columns are `displayName` — a `@Transient` getter formatting number and name
   * (`KostFormatter`), so the database cannot order by it. The name is what a reader sorts by, and it is
   * a column; the leading number is the customer id, which orders the same way for the customer column
   * anyway.
   *
   * `MagicFilterProcessor` keeps these paths whole now, so without this the sort would reach
   * `addOrder`, fail on the missing column and log per request.
   */
  override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
    target.sortProperties.replaceAll { sortProperty ->
      DISPLAY_NAME_SORT_PROPERTIES[sortProperty.property]?.let { SortProperty(it, sortProperty.sortOrder) }
        ?: sortProperty
    }
  }

  /**
   * LAYOUT Edit page: attachments and a read-only summary of the order.
   *
   * The order is edited in projectforge-next (hand built, see `order.page.tsx`) and in Wicket, not
   * through this layout — an order is a tree of two nested collections with server-side sums, which the
   * generic UILayout renderer cannot express. What is left here is the attachment list, which needs an
   * edit layout to be reachable at all, plus the few fields naming the order the attachments belong to.
   *
   * `userAccess` is no longer forced to false: the DTO round trip is complete now (see [transformForDB]
   * / [Auftrag.copyTo]), so saving this layout writes the order back unchanged instead of emptying it.
   */
  override fun createEditLayout(dto: Auftrag, userAccess: UILayout.UserAccess): UILayout {
    val layout = super.createEditLayout(dto, userAccess)
      .add(
        UIRow()
          .add(
            UICol()
              .add(UIReadOnlyField("nummer", lc))
              .add(UIReadOnlyField("customer.displayName", lc, label = "fibu.kunde"))
          )
          .add(
            UICol()
              .add(UIReadOnlyField("formattedNettoSumme", lc, label = "fibu.auftrag.nettoSumme"))
              .add(UIReadOnlyField("project.displayName", lc, label = "fibu.projekt"))
          )
      )
      .add(
        UIRow()
          .add(
            UICol()
              .add(UIReadOnlyField("titel", lc))
          )
      )
      .add(
        UIFieldset(title = "attachment.list")
          .add(
            UIAttachmentList(
              category, dto.id, maxSizeInKB = getMaxFileSizeKB()
            )
          )
      )
    //layout.enableHistoryBackButton()
    return LayoutUtils.processEditPage(layout, dto, this)
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
    return OrderSums(
      netSum = info.netSum,
      commissionedNetSum = info.commissionedNetSum,
      akquiseSum = info.akquiseSum,
      invoicedSum = info.invoicedSum,
      notYetInvoicedSum = info.notYetInvoicedSum,
      toBeInvoicedSum = info.toBeInvoicedSum,
      personDays = info.personDays,
      vollstaendigFakturiert = info.isVollstaendigFakturiert,
      toBeInvoiced = info.toBeInvoiced,
      positions = info.infoPositions?.map { position ->
        PositionSums(
          number = position.number,
          netSum = position.netSum,
          invoicedSum = position.invoicedSum,
          notYetInvoicedSum = position.notYetInvoiced,
        )
      },
    )
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
    val vollstaendigFakturiert: Boolean,
    val toBeInvoiced: Boolean,
    val positions: List<PositionSums>?,
  )

  class PositionSums(
    val number: Short?,
    val netSum: BigDecimal,
    val invoicedSum: BigDecimal,
    val notYetInvoicedSum: BigDecimal,
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

  companion object {
    /** Sort ids of the two `displayName` columns and the column each is ordered by. */
    private val DISPLAY_NAME_SORT_PROPERTIES = mapOf(
      "kunde.displayName" to "kunde.name",
      "projekt.displayName" to "projekt.name",
    )

    /**
     * Id of the combined period-of-performance filter — a pseudo field, standing for a criterion over
     * [PERIOD_OF_PERFORMANCE_FIELDS] (see [addPeriodOfPerformanceCriterion]).
     */
    internal const val PERIOD_OF_PERFORMANCE_FILTER = "periodOfPerformance"

    /** The two date properties the combined filter replaces in the filter field list. */
    private val PERIOD_OF_PERFORMANCE_FIELDS = setOf("periodOfPerformanceBegin", "periodOfPerformanceEnd")
  }
}
