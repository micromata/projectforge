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
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOPagesRest
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
    auftrag.copyFrom(obj)
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
   * Presets the three dates and the contact person of a new order, as `AuftragEditPage.onPreEdit` does.
   */
  override fun newBaseDTO(request: HttpServletRequest?): Auftrag {
    val auftrag = super.newBaseDTO(request)
    val today = LocalDate.now()
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
   * Validates the nested collections, which the generic field validation doesn't reach: it walks the
   * properties of one class ([ValidationUtils.validateFields]) and doesn't descend into collections.
   *
   * The row index is the one of the posted list, so the client can show the error at the row that
   * caused it. [ValidationError.fieldId] is free-form, so a nested path needs no framework change.
   */
  override fun validate(validationErrors: MutableList<ValidationError>, dto: Auftrag) {
    super.validate(validationErrors, dto)
    validateRows(validationErrors, dto.positionen, "positionen")
    validateRows(validationErrors, dto.paymentSchedules, "paymentSchedules")
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

  private fun validateRows(
    validationErrors: MutableList<ValidationError>,
    rows: List<org.projectforge.rest.dto.BaseDTO<*>>?,
    property: String,
  ) {
    rows?.forEachIndexed { index, row ->
      if (row.deleted) {
        return@forEachIndexed // A deleted row is only sent so it isn't removed physically.
      }
      ValidationUtils.validateFields(row).forEach { error ->
        validationErrors.add(error.copy(fieldId = "$property[$index].${error.fieldId}"))
      }
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
    return filters
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
}
