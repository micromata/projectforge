/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.fibu

import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.wicket.AttributeModifier
import org.apache.wicket.ajax.AjaxRequestTarget
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior
import org.apache.wicket.markup.html.form.Button
import org.apache.wicket.markup.html.form.DropDownChoice
import org.apache.wicket.markup.html.form.TextField
import org.apache.wicket.markup.repeater.RepeatingView
import org.apache.wicket.model.CompoundPropertyModel
import org.apache.wicket.model.Model
import org.apache.wicket.model.PropertyModel
import org.apache.wicket.spring.injection.annot.SpringBean
import org.apache.wicket.util.convert.IConverter
import org.projectforge.business.fibu.*
import org.projectforge.business.task.TaskDO
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.utils.CurrencyFormatter
import org.projectforge.common.StringHelper
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.utils.NumberHelper.greaterZero
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.fibu.AuftragPagesRest
import org.projectforge.web.URLHelper
import org.projectforge.web.WicketSupport
import org.projectforge.web.task.TaskSelectPanel
import org.projectforge.web.user.UserSelectPanel
import org.projectforge.web.wicket.AbstractEditForm
import org.projectforge.web.wicket.AbstractUnsecureBasePage
import org.projectforge.web.wicket.WicketUtils
import org.projectforge.web.wicket.bootstrap.GridSize
import org.projectforge.web.wicket.components.*
import org.projectforge.web.wicket.converter.CurrencyConverter
import org.projectforge.web.wicket.flowlayout.*
import org.projectforge.web.wicket.flowlayout.ToggleContainerPanel.ToggleStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate

open class AuftragEditForm(parentPage: AuftragEditPage?, data: AuftragDO?) :
  AbstractEditForm<AuftragDO?, AuftragEditPage?>(parentPage, data) {
  private val periodOfPerformanceHelper = PeriodOfPerformanceHelper()
  var isSendEMailNotification = true
  private var positionsRepeater: RepeatingView? = null

  @JvmField
  var kundeSelectPanel: NewCustomerSelectPanel? = null
  private var paymentSchedulePanel: PaymentSchedulePanel? = null

  @JvmField
  var projektSelectPanel: NewProjektSelectPanel? = null
  private var projectManagerSelectPanel: UserSelectPanel? = null
  private var headOfBusinessManagerSelectPanel: UserSelectPanel? = null
  private var salesManagerSelectPanel: UserSelectPanel? = null

  override fun init() {
    super.init()
    WicketSupport.get(AuftragDao::class.java).calculateInvoicedSum(data)

    /* GRID8 - BLOCK */gridBuilder.newSplitPanel(GridSize.COL50)
    run {
      // Number
      val fs = gridBuilder.newFieldset(getString("fibu.auftrag.nummer"))
      val number = MinMaxNumberField(
        InputPanel.WICKET_ID,
        PropertyModel(
          data,
          "nummer"
        ),
        0, 99999999
      )
      number.setMaxLength(8).add(AttributeModifier.append("style", "width: 6em !important;"))
      fs.add(number)
      if (greaterZero(getData()!!.nummer) == false) {
        fs.addHelpIcon(getString("fibu.tooltip.nummerWirdAutomatischVergeben"))
      }
    }

    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // Net sum
      val fs = gridBuilder.newFieldset(getString("fibu.auftrag.nettoSumme")).suppressLabelForWarning()
      val netPanel = DivTextPanel(fs.newChildId(), object : Model<String>() {
        override fun getObject(): String {
          return CurrencyFormatter.format(data!!.nettoSumme)
        }
      }, TextStyle.FORM_TEXT)
      fs.add(netPanel)
      fs.add(DivTextPanel(fs.newChildId(), ", " + getString("fibu.auftrag.commissioned") + ": "))
      val orderedPanel = DivTextPanel(fs.newChildId(), object : Model<String>() {
        override fun getObject(): String {
          return CurrencyFormatter.format(data!!.beauftragtNettoSumme)
        }
      }, TextStyle.FORM_TEXT)
      fs.add(orderedPanel)
      val orderInvoiceInfo = I18nHelper.getLocalizedMessage(
        "fibu.auftrag.invoice.info", CurrencyFormatter.format(
          data!!.invoicedSum
        ),
        CurrencyFormatter.format(data!!.notYetInvoicedSum)
      )
      fs.add(DivTextPanel(fs.newChildId(), orderInvoiceInfo))
    }
    gridBuilder.newGridPanel()
    run {

      // Title
      val fs = gridBuilder.newFieldset(getString("fibu.auftrag.titel"))
      val subject: MaxLengthTextField = RequiredMaxLengthTextField(
        InputPanel.WICKET_ID,
        PropertyModel(data, "titel")
      )
      subject.add(WicketUtils.setFocus())
      fs.add(subject)
    }
    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // reference
      val fs = gridBuilder.newFieldset(getString("fibu.common.customer.reference"))
      fs.add(MaxLengthTextField(InputPanel.WICKET_ID, PropertyModel(data, "referenz")))
    }
    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // DropDownChoice status
      val fs = gridBuilder.newFieldset(getString("status"))
      val statusChoiceRenderer = LabelValueChoiceRenderer<AuftragsStatus>(
        this,
        AuftragsStatus.values()
      )
      val statusChoice = DropDownChoice(
        fs.dropDownChoiceId,
        PropertyModel(data, "auftragsStatus"), statusChoiceRenderer.values,
        statusChoiceRenderer
      )
      statusChoice.setNullValid(false).isRequired = true
      fs.add(statusChoice)
    }
    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // project
      val fs = gridBuilder.newFieldset(getString("fibu.projekt")).suppressLabelForWarning()
      projektSelectPanel =
        NewProjektSelectPanel(fs.newChildId(), PropertyModel(data, "projekt"), parentPage, "projektId")
      projektSelectPanel!!.textField.add(object : AjaxFormComponentUpdatingBehavior("change") {
        override fun onUpdate(target: AjaxRequestTarget) {
          setKundePmHobmAndSmIfEmpty(projektSelectPanel!!.modelObject, target)
        }
      })
      // ajaxUpdateComponents.add(projektSelectPanel.getTextField());
      fs.add(projektSelectPanel)
      projektSelectPanel!!.init()
    }
    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // customer
      val fs = gridBuilder.newFieldset(getString("fibu.kunde")).suppressLabelForWarning()
      kundeSelectPanel = NewCustomerSelectPanel(
        fs.newChildId(), PropertyModel(data, "kunde"),
        PropertyModel(
          data, "kundeText"
        ),
        parentPage, "kundeId"
      )
      kundeSelectPanel!!.textField.outputMarkupId = true
      fs.add(kundeSelectPanel)
      kundeSelectPanel!!.init()
      fs.addHelpIcon(getString("fibu.auftrag.hint.kannVonProjektKundenAbweichen"))
    }
    gridBuilder.newSplitPanel(GridSize.COL33)
    run {

      // project manager
      val fs = gridBuilder.newFieldset(getString("fibu.projectManager"))
      projectManagerSelectPanel = UserSelectPanel(
        fs.newChildId(),
        PropertyModel(data, "projectManager"),
        parentPage, "projectManagerId"
      )
      projectManagerSelectPanel!!.formComponent.outputMarkupId = true
      fs.add(projectManagerSelectPanel)
      projectManagerSelectPanel!!.init()
    }
    gridBuilder.newSplitPanel(GridSize.COL33)
    run {

      // head of business manager
      val fs = gridBuilder.newFieldset(getString("fibu.headOfBusinessManager"))
      headOfBusinessManagerSelectPanel = UserSelectPanel(
        fs.newChildId(),
        PropertyModel(data, "headOfBusinessManager"),
        parentPage, "headOfBusinessManagerId"
      )
      headOfBusinessManagerSelectPanel!!.formComponent.outputMarkupId = true
      fs.add(headOfBusinessManagerSelectPanel)
      headOfBusinessManagerSelectPanel!!.init()
    }
    gridBuilder.newSplitPanel(GridSize.COL33)
    run {

      //sales manager
      val fs = gridBuilder.newFieldset(getString("fibu.salesManager"))
      salesManagerSelectPanel = UserSelectPanel(
        fs.newChildId(),
        PropertyModel(data, "salesManager"),
        parentPage, "salesManagerId"
      )
      salesManagerSelectPanel!!.formComponent.outputMarkupId = true
      fs.add(salesManagerSelectPanel)
      salesManagerSelectPanel!!.init()
    }
    gridBuilder.newSplitPanel(GridSize.SPAN2)
    run {

      // erfassungsDatum
      val props = erfassungsDatumProperties
      val fsEntryDate = gridBuilder.newFieldset(getString("fibu.auftrag.erfassung.datum"))
      val erfassungsDatumPanel = LocalDatePanel(fsEntryDate.newChildId(), LocalDateModel(props.model))
      erfassungsDatumPanel.isRequired = true
      erfassungsDatumPanel.isEnabled = false
      fsEntryDate.add(erfassungsDatumPanel)
    }
    gridBuilder.newSplitPanel(GridSize.SPAN2)
    run {

      // angebotsDatum
      val props = angebotsDatumProperties
      val fsOrderDate = gridBuilder.newFieldset(getString("fibu.auftrag.angebot.datum"))
      val angebotsDatumPanel = LocalDatePanel(fsOrderDate.newChildId(), LocalDateModel(props.model))
      angebotsDatumPanel.isRequired = true
      fsOrderDate.add(angebotsDatumPanel)
    }
    gridBuilder.newSplitPanel(GridSize.SPAN2)
    run {

      // entscheidungsDatum
      val props = entscheidungsDatumProperties
      val fsOrderDate = gridBuilder.newFieldset(getString("fibu.auftrag.entscheidung.datum"))
      val angebotsDatumPanel = LocalDatePanel(fsOrderDate.newChildId(), LocalDateModel(props.model))
      fsOrderDate.add(angebotsDatumPanel)
    }
    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // Bindungsfrist
      val props = bindungsfristProperties
      val fs = gridBuilder.newFieldset(getString("fibu.auftrag.bindungsFrist"))
      val bindungsFristPanel = LocalDatePanel(fs.newChildId(), LocalDateModel(props.model))
      fs.add(bindungsFristPanel)
    }
    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // contact person
      val fs = gridBuilder.newFieldset(getString("contactPerson"))
      val contactPersonSelectPanel = UserSelectPanel(
        fs.newChildId(),
        PropertyModel(
          data,
          "contactPerson"
        ),
        parentPage, "contactPersonId"
      )
      contactPersonSelectPanel.isRequired = true
      fs.add(contactPersonSelectPanel)
      contactPersonSelectPanel.init()
    }
    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // Beauftragungsdatum
      val props = beauftragungsDatumProperties
      val fs = gridBuilder.newFieldset(getString("fibu.auftrag.beauftragungsdatum"))
      val beauftragungsDatumPanel = LocalDatePanel(fs.newChildId(), LocalDateModel(props.model))
      fs.add(beauftragungsDatumPanel)
    }
    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // Period of performance
      val fs = gridBuilder.newFieldset(getString("fibu.periodOfPerformance"))
      periodOfPerformanceHelper.createPeriodOfPerformanceFields(
        fs,
        PropertyModel(data, "periodOfPerformanceBegin"),
        PropertyModel(data, "periodOfPerformanceEnd")
      )
    }
    run {

      // Probability of occurrence
      val fs = gridBuilder.newFieldset(getString("fibu.probabilityOfOccurrence"))
      val probabilityOfOccurrence = MinMaxNumberField(
        InputPanel.WICKET_ID,
        PropertyModel(data, "probabilityOfOccurrence"), 0, 100
      )
      probabilityOfOccurrence.add(AttributeModifier.append("style", "width: 6em;"))
      fs.add(probabilityOfOccurrence)
    }
    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // Payment schedule
      val schedulesPanel: ToggleContainerPanel = object : ToggleContainerPanel(gridBuilder.panel.newChildId()) {
        /**
         * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel.wantsOnStatusChangedNotification
         */
        override fun wantsOnStatusChangedNotification(): Boolean {
          return true
        }

        /**
         * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel.onToggleStatusChanged
         */
        override fun onToggleStatusChanged(target: AjaxRequestTarget, toggleStatus: ToggleStatus) {
          setHeading(getPaymentScheduleHeading(data!!.paymentSchedules, this))
        }
      }
      schedulesPanel.setHeading(getPaymentScheduleHeading(data!!.paymentSchedules, schedulesPanel))
      gridBuilder.panel.add(schedulesPanel)
      val innerGridBuilder = schedulesPanel.createGridBuilder()
      val dp = innerGridBuilder.panel
      dp.add(PaymentSchedulePanel(dp.newChildId(), CompoundPropertyModel(data), user).also {
        paymentSchedulePanel = it
      })
      paymentSchedulePanel!!.isVisible = data!!.paymentSchedules != null && data!!.paymentSchedules!!.isEmpty() == false
      if (baseDao.hasLoggedInUserUpdateAccess(data, data, false)) {
        val addPositionButton: Button = object : Button(SingleButtonPanel.WICKET_ID) {
          override fun onSubmit() {
            data!!.addPaymentSchedule(PaymentScheduleDO())
            paymentSchedulePanel!!.rebuildEntries()
            paymentSchedulePanel!!.isVisible = true
          }
        }
        val addPositionButtonPanel = SingleButtonPanel(dp.newChildId(), addPositionButton, getString("add"))
        addPositionButtonPanel.setTooltip(getString("fibu.auftrag.tooltip.addPaymentschedule"))
        dp.add(addPositionButtonPanel)
      }
    }
    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // comment
      val fs = gridBuilder.newFieldset(getString("comment"))
      fs.add(MaxLengthTextArea(TextAreaPanel.WICKET_ID, PropertyModel(data, "bemerkung")), true)
    }
    gridBuilder.newSplitPanel(GridSize.COL50)
    run {

      // status comment
      val fs = gridBuilder.newFieldset(getString("fibu.auftrag.statusBeschreibung"))
      fs.add(
        MaxLengthTextArea(TextAreaPanel.WICKET_ID, PropertyModel(data, "statusBeschreibung")),
        true
      )
    }
    // positions
    gridBuilder.newGridPanel()
    positionsRepeater = gridBuilder.newRepeatingView()
    refreshPositions()
    if (baseDao.hasInsertAccess(user) == true) {
      val panel = gridBuilder.newGridPanel().panel
      val addPositionButton: Button = object : Button(SingleButtonPanel.WICKET_ID) {
        override fun onSubmit() {
          getData()!!.addPosition(AuftragsPositionDO())
          refreshPositions()
          paymentSchedulePanel!!.rebuildEntries()
        }
      }
      val addPositionButtonPanel = SingleButtonPanel(
        panel.newChildId(), addPositionButton,
        getString("add")
      )
      addPositionButtonPanel.setTooltip(getString("fibu.auftrag.tooltip.addPosition"))
      panel.add(addPositionButtonPanel)
    }
    run {
      // email
      gridBuilder.newFieldset(getString("email"))
        .addCheckBox(PropertyModel(this, "sendEMailNotification"), null)
        .setTooltip(getString("label.sendEMailNotification"))
    }
    run {
      // attachments
      val fs = gridBuilder.newFieldset(getString("attachments"))
      var attachments = ""
      if ((data?.attachmentsCounter ?: 0) > 0) {
        val auftragPagesRest = WicketSupport.get(AuftragPagesRest::class.java)
        attachments = WicketSupport.get(AttachmentsService::class.java).getAttachments(
          auftragPagesRest.jcrPath!!,
          data!!.id!!,
          auftragPagesRest.attachmentsAccessChecker
        )
          ?.joinToString(
            "<br/>",
            postfix = "<br/><br/>"
          ) {
            "<a href=\"${
              RestResolver.getRestUrl(
                AttachmentsServicesRest::class.java,
                AttachmentsServicesRest.getDownloadUrl(it, category = auftragPagesRest.category, id = data!!.id!!, listId = "attachments")
              )
            }\">${URLHelper.encode(it.name)} (${it.sizeHumanReadable})</a>"
          }
          ?: ""
      }
      val editLink = "<a href=\"/react/order/edit/${data?.id}\" target=\"_blank\" \">${getString("edit")}</a>"
      val divTextPanel = DivTextPanel(fs.newChildId(), "$attachments$editLink")
      divTextPanel.setEscapeModelStringsInLabel(false)
      fs.add(divTextPanel)
    }
    add(periodOfPerformanceHelper.createValidator())
    setKundePmHobmAndSmIfEmpty(getData()!!.projekt, null)
  }

  private val erfassungsDatumProperties: FieldProperties<LocalDate>
    get() = FieldProperties("fibu.auftrag.angebot.datum", PropertyModel(super.data, "angebotsDatum"))
  private val angebotsDatumProperties: FieldProperties<LocalDate>
    get() = FieldProperties("fibu.auftrag.erfassung.datum", PropertyModel(super.data, "erfassungsDatum"))
  private val entscheidungsDatumProperties: FieldProperties<LocalDate>
    get() = FieldProperties("fibu.auftrag.entscheidung.datum", PropertyModel(super.data, "entscheidungsDatum"))
  private val bindungsfristProperties: FieldProperties<LocalDate>
    get() = FieldProperties("fibu.auftrag.bindungsFrist", PropertyModel(super.data, "bindungsFrist"))
  private val beauftragungsDatumProperties: FieldProperties<LocalDate>
    get() = FieldProperties("fibu.auftrag.beauftragungsdatum", PropertyModel(super.data, "beauftragungsDatum"))

  fun setKundePmHobmAndSmIfEmpty(project: ProjektDO?, target: AjaxRequestTarget?) {
    if (project == null) {
      return
    }
    if (getData()!!.kundeId == null && StringUtils.isBlank(getData()!!.kundeText) == true) {
      getData()!!.kunde = project.kunde
      kundeSelectPanel!!.textField.modelChanged()
      target?.add(kundeSelectPanel!!.textField)
    }
    if (getData()!!.projectManager == null) {
      getData()!!.projectManager = project.projectManager
      projectManagerSelectPanel!!.formComponent.modelChanged()
      target?.add(projectManagerSelectPanel!!.formComponent)
    }
    if (getData()!!.headOfBusinessManager == null) {
      getData()!!.headOfBusinessManager = project.headOfBusinessManager
      headOfBusinessManagerSelectPanel!!.formComponent.modelChanged()
      target?.add(headOfBusinessManagerSelectPanel!!.formComponent)
    }
    if (getData()!!.salesManager == null) {
      getData()!!.salesManager = project.salesManager
      salesManagerSelectPanel!!.formComponent.modelChanged()
      target?.add(salesManagerSelectPanel!!.formComponent)
    }
  }

  private fun refreshPositions() {
    positionsRepeater!!.removeAll()
    periodOfPerformanceHelper.onRefreshPositions()
    if (CollectionUtils.isEmpty(data!!.positionenIncludingDeleted) == true) {
      // Ensure that at least one position is available:
      data!!.addPosition(AuftragsPositionDO())
    }
    for (position in data!!.positionenIncludingDeleted!!) {
      val abgeschlossenUndNichtFakturiert = position.toBeInvoiced
      val positionsPanel: ToggleContainerPanel = object : ToggleContainerPanel(positionsRepeater!!.newChildId()) {
        /**
         * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel.wantsOnStatusChangedNotification
         */
        override fun wantsOnStatusChangedNotification(): Boolean {
          return true
        }

        /**
         * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel.onToggleStatusChanged
         */
        override fun onToggleStatusChanged(target: AjaxRequestTarget, toggleStatus: ToggleStatus) {
          if (toggleStatus == ToggleStatus.OPENED) {
            data!!.getUiStatus().openPosition(position.number)
          } else {
            data!!.getUiStatus().closePosition(position.number)
          }
          setHeading(getPositionHeading(position, this))
        }
      }
      if (abgeschlossenUndNichtFakturiert == true) {
        positionsPanel.setHighlightedHeader()
      }
      positionsRepeater!!.add(positionsPanel)
      if (data!!.getUiStatus().isClosed(position.number) == true) {
        positionsPanel.setClosed()
      } else {
        positionsPanel.setOpen()
      }
      positionsPanel.setHeading(getPositionHeading(position, positionsPanel))
      val posGridBuilder = positionsPanel.createGridBuilder()
      posGridBuilder.newGridPanel()
      run {
        val fs = posGridBuilder.newFieldset(getString("fibu.auftrag.titel"))
        fs.add(MaxLengthTextField(InputPanel.WICKET_ID, PropertyModel(position, "titel")))
      }
      posGridBuilder.newSplitPanel(GridSize.COL33)
      run {

        // DropDownChoice type
        val fsType = posGridBuilder.newFieldset(getString("fibu.auftrag.position.art"))
        val artChoiceRenderer = LabelValueChoiceRenderer<AuftragsPositionsArt>(
          fsType,
          AuftragsPositionsArt.values()
        )
        val artChoice = DropDownChoice(
          fsType.dropDownChoiceId,
          PropertyModel(position, "art"), artChoiceRenderer.values, artChoiceRenderer
        )
        //artChoice.setNullValid(false);
        //artChoice.setRequired(true);
        fsType.add(artChoice)

        // DropDownChoice payment type
        val fsPaymentType = posGridBuilder.newFieldset(getString("fibu.auftrag.position.paymenttype"))
        val paymentTypeChoiceRenderer = LabelValueChoiceRenderer<AuftragsPositionsPaymentType>(
          fsPaymentType,
          AuftragsPositionsPaymentType.values()
        )
        val paymentTypeChoice = DropDownChoice(
          fsPaymentType.dropDownChoiceId,
          PropertyModel(position, "paymentType"), paymentTypeChoiceRenderer.values, paymentTypeChoiceRenderer
        )
        //paymentTypeChoice.setNullValid(false);
        paymentTypeChoice.isRequired = true
        fsPaymentType.add(paymentTypeChoice)
      }
      posGridBuilder.newSplitPanel(GridSize.COL33)
      run {

        // Person days
        val fs = posGridBuilder.newFieldset(getString("projectmanagement.personDays"))
        fs.add(
          MinMaxNumberField(
            InputPanel.WICKET_ID,
            PropertyModel(position, "personDays"),
            BigDecimal.ZERO, MAX_PERSON_DAYS
          )
        )
      }
      posGridBuilder.newSplitPanel(GridSize.COL33)
      run {

        // Net sum
        val fs = posGridBuilder.newFieldset(getString("fibu.auftrag.nettoSumme"))
        val nettoSumme: TextField<String> =
          object : TextField<String>(InputPanel.WICKET_ID, PropertyModel(position, "nettoSumme")) {
            override fun <C : Any?> getConverter(type: Class<C>?): IConverter<C> {
              return CurrencyConverter() as IConverter<C>
            }
          }
        nettoSumme.isRequired = true
        fs.add(nettoSumme)
        if (abgeschlossenUndNichtFakturiert == true) {
          fs.setWarningBackground()
        }
      }
      posGridBuilder.newSplitPanel(GridSize.COL25)
      val invoicePositionsByOrderPositionId = WicketSupport.get(RechnungCache::class.java)
        .getRechnungsPositionVOSetByAuftragsPositionId(position.id)
      val showInvoices = CollectionUtils.isNotEmpty(invoicePositionsByOrderPositionId)
      run {

        // Invoices
        val fs = posGridBuilder.newFieldset(getString("fibu.rechnungen")).suppressLabelForWarning()
        if (showInvoices == true) {
          val panel = InvoicePositionsPanel(fs.newChildId())
          fs.add(panel)
          panel.init(invoicePositionsByOrderPositionId)
        } else {
          fs.add(AbstractUnsecureBasePage.createInvisibleDummyComponent(fs.newChildId()))
        }
      }
      posGridBuilder.newSplitPanel(GridSize.COL25)
      run {

        // invoiced
        val fs = posGridBuilder.newFieldset(getString("fibu.title.fakturiert")).suppressLabelForWarning()
        if (showInvoices == true) {
          fs.add(
            DivTextPanel(
              fs.newChildId(),
              CurrencyFormatter.format(RechnungDao.getNettoSumme(invoicePositionsByOrderPositionId))
            )
          )
        } else {
          fs.add(AbstractUnsecureBasePage.createInvisibleDummyComponent(fs.newChildId()))
        }
        if (WicketSupport.getAccessChecker().hasRight(user, RechnungDao.USER_RIGHT_ID, UserRightValue.READWRITE) == true) {
          val checkBoxDiv = fs.addNewCheckBoxButtonDiv()
          checkBoxDiv.add(
            CheckBoxButton(
              checkBoxDiv.newChildId(),
              PropertyModel(position, "vollstaendigFakturiert"),
              getString("fibu.auftrag.vollstaendigFakturiert")
            )
          )
        }
      }
      posGridBuilder.newSplitPanel(GridSize.COL25)
      run {

        // not invoiced
        val fs = posGridBuilder.newFieldset(getString("fibu.title.fakturiert.not")).suppressLabelForWarning()
        if (position.nettoSumme != null) {
          var invoiced = BigDecimal.ZERO
          invoiced = if (showInvoices == true) {
            val invoicedSumForPosition = RechnungDao.getNettoSumme(invoicePositionsByOrderPositionId)
            val notInvoicedSumForPosition = position.nettoSumme!!.subtract(invoicedSumForPosition)
            notInvoicedSumForPosition
          } else {
            position.nettoSumme
          }
          if (position.status != null) {
            if (position.status == AuftragsPositionsStatus.ABGELEHNT || position.status == AuftragsPositionsStatus.ERSETZT || (position
                .status
                  == AuftragsPositionsStatus.OPTIONAL)
            ) {
              invoiced = BigDecimal.ZERO
            }
          }
          fs.add(
            DivTextPanel(
              fs.newChildId(),
              CurrencyFormatter.format(invoiced)
            )
          )
        }
      }
      posGridBuilder.newSplitPanel(GridSize.COL25)
      run {

        // DropDownChoice status
        val fs = posGridBuilder.newFieldset(getString("status"))
        val statusChoiceRenderer = LabelValueChoiceRenderer<AuftragsPositionsStatus>(
          fs, AuftragsPositionsStatus.values()
        )
        val statusChoice = DropDownChoice(
          fs.dropDownChoiceId,
          PropertyModel(position, "status"), statusChoiceRenderer.values,
          statusChoiceRenderer
        )
        statusChoice.isNullValid = true
        statusChoice.isRequired = true
        fs.add(statusChoice)
        if (abgeschlossenUndNichtFakturiert == true) {
          fs.setWarningBackground()
        }
      }
      posGridBuilder.newSplitPanel(GridSize.COL100)
      run {

        // Task
        val fs = posGridBuilder.newFieldset(getString("task"))
        val taskSelectPanel: TaskSelectPanel = object : TaskSelectPanel(
          fs, PropertyModel(position, "task"),
          parentPage, "taskId:"
              + position.number
        ) {
          override fun selectTask(task: TaskDO) {
            super.selectTask(task)
            parentPage!!.baseDao.setTask(position, task.id!!)
          }
        }
        fs.add(taskSelectPanel)
        taskSelectPanel.init()
      }
      posGridBuilder.newSplitPanel(GridSize.COL100)
      run {

        // Period of performance
        val fs = posGridBuilder.newFieldset(getString("fibu.periodOfPerformance"))
        val paymentChoiceRenderer = LabelValueChoiceRenderer<ModeOfPaymentType>(fs, ModeOfPaymentType.values())
        val paymentChoice = DropDownChoice(
          fs.dropDownChoiceId,
          PropertyModel(position, "modeOfPaymentType"), paymentChoiceRenderer.values, paymentChoiceRenderer
        )
        paymentChoice.outputMarkupPlaceholderTag = true
        periodOfPerformanceHelper.createPositionsPeriodOfPerformanceFields(
          fs,
          PropertyModel(position, "periodOfPerformanceType"),
          PropertyModel(position, "periodOfPerformanceBegin"),
          PropertyModel(position, "periodOfPerformanceEnd"),
          paymentChoice
        )
        fs.add(paymentChoice)
      }
      posGridBuilder.newGridPanel()
      run {

        // Comment
        val fs = posGridBuilder.newFieldset(getString("comment"))
        fs.add(MaxLengthTextArea(TextAreaPanel.WICKET_ID, PropertyModel(position, "bemerkung")))
      }
      if (baseDao.hasLoggedInUserUpdateAccess(data, data, false) == true) {
        val removeButtonGridBuilder = posGridBuilder.newGridPanel()
        run {

          // Remove Position
          val divPanel = removeButtonGridBuilder.panel
          val removePositionButton: Button = object : Button(SingleButtonPanel.WICKET_ID) {
            override fun onSubmit() {
              position.deleted = true
              refreshPositions()
              paymentSchedulePanel!!.rebuildEntries()
            }
          }
          removePositionButton.add(AttributeModifier.append("class", ButtonType.DELETE.classAttrValue))
          val removePositionButtonPanel = SingleButtonPanel(
            divPanel.newChildId(), removePositionButton,
            getString("delete")
          )
          removePositionButtonPanel.isVisible = positionInInvoiceExists(position) == false
          divPanel.add(removePositionButtonPanel)
        }
      }
      if (position.deleted) {
        positionsPanel.isVisible = false
      }
    }
  }

  private fun positionInInvoiceExists(position: AuftragsPositionDO): Boolean {
    if (position.id != null) {
      val invoicePositionList = WicketSupport.get(RechnungCache::class.java).getRechnungsPositionVOSetByAuftragsPositionId(position.id)
      return invoicePositionList != null && invoicePositionList.isEmpty() == false
    }
    return false
  }

  protected fun getPositionHeading(position: AuftragsPositionDO, positionsPanel: ToggleContainerPanel): String {
    if (positionsPanel.toggleStatus == ToggleStatus.OPENED) {
      return getString("label.position.short") + " #" + position.number
    }
    val heading = StringBuffer()
    heading.append(escapeHtml(getString("label.position.short"))).append(" #").append(position.number.toInt())
    heading.append(": ").append(CurrencyFormatter.format(position.nettoSumme))
    position.status?.let { status ->
      heading.append(", ").append(getString(status.i18nKey))
    }
    if (position.vollstaendigFakturiert == false) {
      heading.append(" (").append(getString("fibu.fakturiert.not")).append(")")
    }
    if (StringHelper.isNotBlank(position.titel)) {
      heading.append(": ").append(StringUtils.abbreviate(position.titel, 80))
    }
    return heading.toString()
  }

  protected fun getPaymentScheduleHeading(
    paymentSchedules: List<PaymentScheduleDO>?,
    schedulesPanel: ToggleContainerPanel
  ): String {
    var ges = BigDecimal.ZERO
    var invoiced = BigDecimal.ZERO
    if (paymentSchedules != null) {
      for (schedule in paymentSchedules) {
        if (schedule.amount != null) {
          ges = ges.add(schedule.amount)
          if (schedule.vollstaendigFakturiert) {
            invoiced = invoiced.add(schedule.amount)
          }
        }
        if (schedule.reached && schedule.vollstaendigFakturiert == false) {
          schedulesPanel.setHighlightedHeader()
        }
      }
    }
    val size = paymentSchedules?.count { !it.deleted } ?: 0
    val heading = StringBuffer()
    heading.append("${escapeHtml(getString("fibu.auftrag.paymentschedule"))} ($size)")
    if (schedulesPanel.toggleStatus == ToggleStatus.OPENED) {
      return heading.toString()
    }
    heading.append(": ").append(CurrencyFormatter.format(ges)).append(" ").append(getString("fibu.fakturiert"))
      .append(" ")
      .append(CurrencyFormatter.format(invoiced))
    return heading.toString()
  }

  override fun getLogger(): Logger {
    return log
  }

  companion object {
    private const val serialVersionUID = 3150725003240437752L
    private val log = LoggerFactory.getLogger(AuftragEditForm::class.java)
    private val MAX_PERSON_DAYS = BigDecimal(10000)
  }
}
