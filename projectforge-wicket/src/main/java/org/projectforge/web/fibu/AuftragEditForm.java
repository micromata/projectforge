/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.fibu;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.fibu.AuftragsPositionDO;
import org.projectforge.business.fibu.AuftragsPositionsArt;
import org.projectforge.business.fibu.AuftragsPositionsPaymentType;
import org.projectforge.business.fibu.AuftragsPositionsStatus;
import org.projectforge.business.fibu.AuftragsStatus;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ModeOfPaymentType;
import org.projectforge.business.fibu.PaymentScheduleDO;
import org.projectforge.business.fibu.PeriodOfPerformanceType;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.RechnungCache;
import org.projectforge.business.fibu.RechnungDao;
import org.projectforge.business.fibu.RechnungsPositionVO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.AbstractUnsecureBasePage;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.converter.CurrencyConverter;
import org.projectforge.web.wicket.flowlayout.ButtonType;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.projectforge.web.wicket.flowlayout.TextStyle;
import org.projectforge.web.wicket.flowlayout.ToggleContainerPanel;
import org.projectforge.web.wicket.flowlayout.ToggleContainerPanel.ToggleStatus;

public class AuftragEditForm extends AbstractEditForm<AuftragDO, AuftragEditPage>
{
  private static final long serialVersionUID = 3150725003240437752L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AuftragEditForm.class);

  private static final BigDecimal MAX_PERSON_DAYS = new BigDecimal(10000);

  private boolean sendEMailNotification = true;

  protected CheckBox sendEMailNotficationCheckBox;

  protected RepeatingView positionsRepeater, paymentSchedulesRepeater;

  protected NewCustomerSelectPanel kundeSelectPanel;

  private final List<DropDownChoice<PeriodOfPerformanceType>> performanceChoices = new ArrayList<>();

  private final List<Component> ajaxPosTargets = new ArrayList<Component>();

  private FormComponent<?>[] positionsDependentFormComponents = new FormComponent[0];

  private DatePanel fromDatePanel, endDatePanel;

  private PaymentSchedulePanel paymentSchedulePanel;

  protected NewProjektSelectPanel projektSelectPanel;

  private UserSelectPanel projectManagerSelectPanel, headOfBusinessManagerSelectPanel, salesManagerSelectPanel;

  @SpringBean
  AccessChecker accessChecker;

  @SpringBean
  RechnungCache rechnungCache;

  @SpringBean
  private AuftragDao auftragDao;

  public AuftragEditForm(final AuftragEditPage parentPage, final AuftragDO data)
  {
    super(parentPage, data);
  }

  public boolean isSendEMailNotification()
  {
    return sendEMailNotification;
  }

  public void setSendEMailNotification(final boolean sendEMailNotification)
  {
    this.sendEMailNotification = sendEMailNotification;
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();

    auftragDao.calculateInvoicedSum(data);

    /* GRID8 - BLOCK */
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Number
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.auftrag.nummer"));
      final MinMaxNumberField<Integer> number = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID,
          new PropertyModel<Integer>(data,
              "nummer"),
          0, 99999999);
      number.setMaxLength(8).add(AttributeModifier.append("style", "width: 6em !important;"));
      fs.add(number);
      if (NumberHelper.greaterZero(getData().getNummer()) == false) {
        fs.addHelpIcon(getString("fibu.tooltip.nummerWirdAutomatischVergeben"));
      }
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Net sum
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.auftrag.nettoSumme")).suppressLabelForWarning();
      final DivTextPanel netPanel = new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return CurrencyFormatter.format(data.getNettoSumme());
        }
      }, TextStyle.FORM_TEXT);
      fs.add(netPanel);
      fs.add(new DivTextPanel(fs.newChildId(), ", " + getString("fibu.auftrag.commissioned") + ": "));
      final DivTextPanel orderedPanel = new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return CurrencyFormatter.format(data.getBeauftragtNettoSumme());
        }
      }, TextStyle.FORM_TEXT);
      fs.add(orderedPanel);

      String orderInvoiceInfo = I18nHelper.getLocalizedMessage("fibu.auftrag.invoice.info", CurrencyFormatter.format(data.getFakturiertSum()),
          CurrencyFormatter.format(data.getZuFakturierenSum()));
      fs.add(new DivTextPanel(fs.newChildId(), orderInvoiceInfo));
      ;
    }
    gridBuilder.newGridPanel();
    {
      // Title
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.auftrag.titel"));
      final MaxLengthTextField subject = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "titel"));
      subject.add(WicketUtils.setFocus());
      fs.add(subject);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // reference
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.common.customer.reference"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "referenz")));
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // DropDownChoice status
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("status"));
      final LabelValueChoiceRenderer<AuftragsStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<AuftragsStatus>(
          this,
          AuftragsStatus.values());
      final DropDownChoice<AuftragsStatus> statusChoice = new DropDownChoice<AuftragsStatus>(fs.getDropDownChoiceId(),
          new PropertyModel<AuftragsStatus>(data, "auftragsStatus"), statusChoiceRenderer.getValues(),
          statusChoiceRenderer);
      statusChoice.setNullValid(false).setRequired(true);
      fs.add(statusChoice);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // project
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.projekt")).suppressLabelForWarning();
      projektSelectPanel = new NewProjektSelectPanel(fs.newChildId(), new PropertyModel<>(data, "projekt"), parentPage, "projektId");
      projektSelectPanel.getTextField().add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          setKundePmHobmAndSmIfEmpty(projektSelectPanel.getModelObject(), target);
        }
      });
      // ajaxUpdateComponents.add(projektSelectPanel.getTextField());
      fs.add(projektSelectPanel);
      projektSelectPanel.init();
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // customer
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde")).suppressLabelForWarning();
      kundeSelectPanel = new NewCustomerSelectPanel(fs.newChildId(), new PropertyModel<KundeDO>(data, "kunde"),
          new PropertyModel<String>(
              data, "kundeText"),
          parentPage, "kundeId");
      kundeSelectPanel.getTextField().setOutputMarkupId(true);
      fs.add(kundeSelectPanel);
      kundeSelectPanel.init();
      fs.addHelpIcon(getString("fibu.auftrag.hint.kannVonProjektKundenAbweichen"));
    }
    gridBuilder.newSplitPanel(GridSize.COL33);
    {
      // project manager
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.projectManager"));
      projectManagerSelectPanel = new UserSelectPanel(fs.newChildId(),
          new PropertyModel<>(data, "projectManager"),
          parentPage, "projectManagerId");
      projectManagerSelectPanel.getFormComponent().setOutputMarkupId(true);
      fs.add(projectManagerSelectPanel);
      projectManagerSelectPanel.init();
    }
    gridBuilder.newSplitPanel(GridSize.COL33);
    {
      // head of business manager
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.headOfBusinessManager"));
      headOfBusinessManagerSelectPanel = new UserSelectPanel(fs.newChildId(),
          new PropertyModel<>(data, "headOfBusinessManager"),
          parentPage, "headOfBusinessManagerId");
      headOfBusinessManagerSelectPanel.getFormComponent().setOutputMarkupId(true);
      fs.add(headOfBusinessManagerSelectPanel);
      headOfBusinessManagerSelectPanel.init();
    }
    gridBuilder.newSplitPanel(GridSize.COL33);
    {
      //sales manager
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.salesManager"));
      salesManagerSelectPanel = new UserSelectPanel(fs.newChildId(),
          new PropertyModel<>(data, "salesManager"),
          parentPage, "salesManagerId");
      salesManagerSelectPanel.getFormComponent().setOutputMarkupId(true);
      fs.add(salesManagerSelectPanel);
      salesManagerSelectPanel.init();
    }
    gridBuilder.newSplitPanel(GridSize.SPAN2);
    {
      // order date
      final FieldsetPanel fsEntryDate = gridBuilder.newFieldset(getString("fibu.auftrag.erfassung.datum"));
      final DatePanel erfassungsDatumPanel = new DatePanel(fsEntryDate.newChildId(),
          new PropertyModel<Date>(data, "erfassungsDatum"), DatePanelSettings
          .get().withTargetType(java.sql.Date.class));
      erfassungsDatumPanel.setRequired(true);
      erfassungsDatumPanel.setEnabled(false);
      fsEntryDate.add(erfassungsDatumPanel);
    }
    gridBuilder.newSplitPanel(GridSize.SPAN2);
    {
      // entry date
      final FieldsetPanel fsOrderDate = gridBuilder.newFieldset(getString("fibu.auftrag.angebot.datum"));
      final DatePanel angebotsDatumPanel = new DatePanel(fsOrderDate.newChildId(),
          new PropertyModel<Date>(data, "angebotsDatum"), DatePanelSettings
          .get().withTargetType(java.sql.Date.class));
      angebotsDatumPanel.setRequired(true);
      fsOrderDate.add(angebotsDatumPanel);
    }
    gridBuilder.newSplitPanel(GridSize.SPAN2);
    {
      // entry date
      final FieldsetPanel fsOrderDate = gridBuilder.newFieldset(getString("fibu.auftrag.entscheidung.datum"));
      final DatePanel angebotsDatumPanel = new DatePanel(fsOrderDate.newChildId(),
          new PropertyModel<Date>(data, "entscheidungsDatum"), DatePanelSettings
          .get().withTargetType(java.sql.Date.class));
      fsOrderDate.add(angebotsDatumPanel);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Bindungsfrist
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.auftrag.bindungsFrist"));
      final DatePanel bindungsFristPanel = new DatePanel(fs.newChildId(),
          new PropertyModel<Date>(data, "bindungsFrist"), DatePanelSettings
          .get().withTargetType(java.sql.Date.class));
      fs.add(bindungsFristPanel);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // contact person
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("contactPerson"));
      final UserSelectPanel contactPersonSelectPanel = new UserSelectPanel(fs.newChildId(),
          new PropertyModel<PFUserDO>(data,
              "contactPerson"),
          parentPage, "contactPersonId");
      contactPersonSelectPanel.setRequired(true);
      fs.add(contactPersonSelectPanel);
      contactPersonSelectPanel.init();
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Beauftragungsdatum
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.auftrag.beauftragungsdatum"));
      final DatePanel beauftragungsDatumPanel = new DatePanel(fs.newChildId(),
          new PropertyModel<Date>(data, "beauftragungsDatum"),
          DatePanelSettings.get().withTargetType(java.sql.Date.class));
      fs.add(beauftragungsDatumPanel);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Period of performance
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.periodOfPerformance"));
      final BooleanSupplier isAnyPerformanceTypeSeeAboveSelected = () -> performanceChoices
          .stream()
          .map(FormComponent::getRawInput) // had to use getRawInput here instead of getModelObject, because it did not work well
          .anyMatch(PeriodOfPerformanceType.SEEABOVE.name()::equals);

      fromDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(data, "periodOfPerformanceBegin"),
          DatePanelSettings.get().withTargetType(java.sql.Date.class), isAnyPerformanceTypeSeeAboveSelected);
      fs.add(fromDatePanel);

      fs.add(new DivTextPanel(fs.newChildId(), "-"));

      endDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(data, "periodOfPerformanceEnd"),
          DatePanelSettings.get().withTargetType(java.sql.Date.class), isAnyPerformanceTypeSeeAboveSelected);
      fs.add(endDatePanel);
    }
    {
      // Probability of occurrence
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.probabilityOfOccurrence"));
      final MinMaxNumberField<Integer> probabilityOfOccurrence = new MinMaxNumberField<>(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "probabilityOfOccurrence"), 0, 100);
      probabilityOfOccurrence.add(AttributeModifier.append("style", "width: 6em;"));
      fs.add(probabilityOfOccurrence);
    }

    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Payment schedule
      final ToggleContainerPanel schedulesPanel = new ToggleContainerPanel(gridBuilder.getPanel().newChildId())
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel#wantsOnStatusChangedNotification()
         */
        @Override
        protected boolean wantsOnStatusChangedNotification()
        {
          return true;
        }

        /**
         * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel#onToggleStatusChanged(AjaxRequestTarget, ToggleStatus)
         *
         */
        @Override
        protected void onToggleStatusChanged(final AjaxRequestTarget target, final ToggleStatus toggleStatus)
        {
          setHeading(getPaymentScheduleHeading(data.getPaymentSchedules(), this));
        }
      };
      schedulesPanel.setHeading(getPaymentScheduleHeading(data.getPaymentSchedules(), schedulesPanel));
      gridBuilder.getPanel().add(schedulesPanel);
      final GridBuilder innerGridBuilder = schedulesPanel.createGridBuilder();
      final DivPanel dp = innerGridBuilder.getPanel();
      dp.add(paymentSchedulePanel = new PaymentSchedulePanel(dp.newChildId(),
          new CompoundPropertyModel<AuftragDO>(data), getUser()));
      paymentSchedulePanel
          .setVisible(data.getPaymentSchedules() != null && data.getPaymentSchedules().isEmpty() == false);
      final Button addPositionButton = new Button(SingleButtonPanel.WICKET_ID)
      {
        @Override
        public final void onSubmit()
        {
          data.addPaymentSchedule(new PaymentScheduleDO());
          paymentSchedulePanel.rebuildEntries();
          paymentSchedulePanel.setVisible(true);
        }
      };
      final SingleButtonPanel addPositionButtonPanel = new SingleButtonPanel(dp.newChildId(), addPositionButton,
          getString("add"));
      addPositionButtonPanel.setTooltip(getString("fibu.auftrag.tooltip.addPosition"));
      dp.add(addPositionButtonPanel);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // comment
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("comment"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "bemerkung")), true);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // status comment
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.auftrag.statusBeschreibung"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "statusBeschreibung")),
          true);
    }
    // positions
    gridBuilder.newGridPanel();
    positionsRepeater = gridBuilder.newRepeatingView();
    refresh();
    if (getBaseDao().hasInsertAccess(getUser()) == true) {
      final DivPanel panel = gridBuilder.newGridPanel().getPanel();
      final Button addPositionButton = new Button(SingleButtonPanel.WICKET_ID)
      {
        @Override
        public final void onSubmit()
        {
          getData().addPosition(new AuftragsPositionDO());
          refresh();
        }
      };
      final SingleButtonPanel addPositionButtonPanel = new SingleButtonPanel(panel.newChildId(), addPositionButton,
          getString("add"));
      addPositionButtonPanel.setTooltip(getString("fibu.auftrag.tooltip.addPosition"));
      panel.add(addPositionButtonPanel);
    }
    {
      // email
      gridBuilder.newFieldset(getString("email"))
          .addCheckBox(new PropertyModel<Boolean>(this, "sendEMailNotification"), null)
          .setTooltip(getString("label.sendEMailNotification"));
    }
    add(new IFormValidator()
    {
      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return positionsDependentFormComponents;
      }

      @Override
      public void validate(final Form<?> form)
      {
        final Date performanceFromDate = fromDatePanel.getDateField().getConvertedInput();
        final Date performanceEndDate = endDatePanel.getDateField().getConvertedInput();
        if (performanceFromDate == null || performanceEndDate == null) {
          return;
        } else if (performanceEndDate.before(performanceFromDate) == true) {
          endDatePanel.error(getString("error.endDateBeforeBeginDate"));
        }
        for (int i = 0; i < positionsDependentFormComponents.length - 1; i += 2) {
          final Date posPerformanceFromDate = ((DatePanel) positionsDependentFormComponents[i]).getDateField()
              .getConvertedInput();
          final Date posPerformanceEndDate = ((DatePanel) positionsDependentFormComponents[i + 1]).getDateField()
              .getConvertedInput();
          if (posPerformanceFromDate == null || posPerformanceEndDate == null) {
            continue;
          }
          if (posPerformanceEndDate.before(posPerformanceFromDate) == true) {
            positionsDependentFormComponents[i + 1].error(getString("error.endDateBeforeBeginDate"));
          }
          if (posPerformanceFromDate.before(performanceFromDate) == true) {
            positionsDependentFormComponents[i + 1].error(getString("error.posFromDateBeforeFromDate"));
          }
        }
      }
    });

    setKundePmHobmAndSmIfEmpty(getData().getProjekt(), null);
  }

  void setKundePmHobmAndSmIfEmpty(final ProjektDO project, final AjaxRequestTarget target)
  {
    if (project == null) {
      return;
    }

    if (getData().getKundeId() == null && StringUtils.isBlank(getData().getKundeText()) == true) {
      getData().setKunde(project.getKunde());
      kundeSelectPanel.getTextField().modelChanged();
      if (target != null) {
        target.add(kundeSelectPanel.getTextField());
      }
    }

    if (getData().getProjectManager() == null) {
      getData().setProjectManager(project.getProjectManager());
      projectManagerSelectPanel.getFormComponent().modelChanged();
      if (target != null) {
        target.add(projectManagerSelectPanel.getFormComponent());
      }
    }

    if (getData().getHeadOfBusinessManager() == null) {
      getData().setHeadOfBusinessManager(project.getHeadOfBusinessManager());
      headOfBusinessManagerSelectPanel.getFormComponent().modelChanged();
      if (target != null) {
        target.add(headOfBusinessManagerSelectPanel.getFormComponent());
      }
    }

    if (getData().getSalesManager() == null) {
      getData().setSalesManager(project.getSalesManager());
      salesManagerSelectPanel.getFormComponent().modelChanged();
      if (target != null) {
        target.add(salesManagerSelectPanel.getFormComponent());
      }
    }
  }

  @SuppressWarnings("serial")
  void refresh()
  {
    positionsRepeater.removeAll();
    performanceChoices.clear();
    this.ajaxPosTargets.clear();

    final Collection<FormComponent<?>> dependentComponents = new ArrayList<FormComponent<?>>();
    if (CollectionUtils.isEmpty(data.getPositionen()) == true) {
      // Ensure that at least one position is available:
      data.addPosition(new AuftragsPositionDO());
    }

    for (final AuftragsPositionDO position : data.getPositionen()) {
      final boolean abgeschlossenUndNichtFakturiert = position.isAbgeschlossenUndNichtVollstaendigFakturiert();
      final ToggleContainerPanel positionsPanel = new ToggleContainerPanel(positionsRepeater.newChildId())
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel#wantsOnStatusChangedNotification()
         */
        @Override
        protected boolean wantsOnStatusChangedNotification()
        {
          return true;
        }

        /**
         * @see org.projectforge.web.wicket.flowlayout.ToggleContainerPanel#onToggleStatusChanged(AjaxRequestTarget, ToggleStatus)
         */
        @Override
        protected void onToggleStatusChanged(final AjaxRequestTarget target, final ToggleStatus toggleStatus)
        {
          if (toggleStatus == ToggleStatus.OPENED) {
            data.getUiStatus().openPosition(position.getNumber());
          } else {
            data.getUiStatus().closePosition(position.getNumber());
          }
          setHeading(getPositionHeading(position, this));
        }
      };
      if (abgeschlossenUndNichtFakturiert == true) {
        positionsPanel.setHighlightedHeader();
      }
      positionsRepeater.add(positionsPanel);
      if (data.getUiStatus().isClosed(position.getNumber()) == true) {
        positionsPanel.setClosed();
      } else {
        positionsPanel.setOpen();
      }
      positionsPanel.setHeading(getPositionHeading(position, positionsPanel));

      final GridBuilder posGridBuilder = positionsPanel.createGridBuilder();
      posGridBuilder.newGridPanel();
      {
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("fibu.auftrag.titel"));
        fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(position, "titel")));
      }
      posGridBuilder.newSplitPanel(GridSize.COL33);
      {
        // DropDownChoice type
        final FieldsetPanel fsType = posGridBuilder.newFieldset(getString("fibu.auftrag.position.art"));
        final LabelValueChoiceRenderer<AuftragsPositionsArt> artChoiceRenderer = new LabelValueChoiceRenderer<>(
            fsType,
            AuftragsPositionsArt.values());
        final DropDownChoice<AuftragsPositionsArt> artChoice = new DropDownChoice<>(
            fsType.getDropDownChoiceId(),
            new PropertyModel<AuftragsPositionsArt>(position, "art"), artChoiceRenderer.getValues(), artChoiceRenderer);
        //artChoice.setNullValid(false);
        //artChoice.setRequired(true);
        fsType.add(artChoice);

        // DropDownChoice payment type
        final FieldsetPanel fsPaymentType = posGridBuilder.newFieldset(getString("fibu.auftrag.position.paymenttype"));
        final LabelValueChoiceRenderer<AuftragsPositionsPaymentType> paymentTypeChoiceRenderer = new LabelValueChoiceRenderer<>(
            fsPaymentType,
            AuftragsPositionsPaymentType.values());
        final DropDownChoice<AuftragsPositionsPaymentType> paymentTypeChoice = new DropDownChoice<>(
            fsPaymentType.getDropDownChoiceId(),
            new PropertyModel<AuftragsPositionsPaymentType>(position, "paymentType"), paymentTypeChoiceRenderer.getValues(), paymentTypeChoiceRenderer);
        //paymentTypeChoice.setNullValid(false);
        //paymentTypeChoice.setRequired(true);
        fsPaymentType.add(paymentTypeChoice);
      }
      posGridBuilder.newSplitPanel(GridSize.COL33);
      {
        // Person days
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("projectmanagement.personDays"));
        fs.add(new MinMaxNumberField<BigDecimal>(InputPanel.WICKET_ID,
            new PropertyModel<BigDecimal>(position, "personDays"),
            BigDecimal.ZERO, MAX_PERSON_DAYS));
      }
      posGridBuilder.newSplitPanel(GridSize.COL33);
      {
        // Net sum
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("fibu.auftrag.nettoSumme"));
        final TextField<String> nettoSumme = new TextField<String>(InputPanel.WICKET_ID, new PropertyModel<>(position, "nettoSumme"))
        {
          @SuppressWarnings({ "rawtypes", "unchecked" })
          @Override
          public IConverter getConverter(final Class type)
          {
            return new CurrencyConverter();
          }
        };
        nettoSumme.setRequired(true);
        fs.add(nettoSumme);
        if (abgeschlossenUndNichtFakturiert == true) {
          fs.setWarningBackground();
        }
      }
      posGridBuilder.newSplitPanel(GridSize.COL25);
      final Set<RechnungsPositionVO> invoicePositionsByOrderPositionId = rechnungCache
          .getRechnungsPositionVOSetByAuftragsPositionId(position.getId());
      final boolean showInvoices = CollectionUtils.isNotEmpty(invoicePositionsByOrderPositionId);
      {
        // Invoices
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("fibu.rechnungen")).suppressLabelForWarning();
        if (showInvoices == true) {
          final InvoicePositionsPanel panel = new InvoicePositionsPanel(fs.newChildId());
          fs.add(panel);
          panel.init(invoicePositionsByOrderPositionId);
        } else {
          fs.add(AbstractUnsecureBasePage.createInvisibleDummyComponent(fs.newChildId()));
        }
      }
      posGridBuilder.newSplitPanel(GridSize.COL25);
      {
        // invoiced
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("fibu.title.fakturiert")).suppressLabelForWarning();
        if (showInvoices == true) {
          fs.add(new DivTextPanel(fs.newChildId(),
              CurrencyFormatter.format(RechnungDao.getNettoSumme(invoicePositionsByOrderPositionId))));
        } else {
          fs.add(AbstractUnsecureBasePage.createInvisibleDummyComponent(fs.newChildId()));
        }
        if (accessChecker.hasRight(getUser(), RechnungDao.USER_RIGHT_ID, UserRightValue.READWRITE) == true) {
          final DivPanel checkBoxDiv = fs.addNewCheckBoxButtonDiv();
          checkBoxDiv.add(new CheckBoxButton(checkBoxDiv.newChildId(),
              new PropertyModel<Boolean>(position, "vollstaendigFakturiert"),
              getString("fibu.auftrag.vollstaendigFakturiert")));
        }
      }
      posGridBuilder.newSplitPanel(GridSize.COL25);
      {
        // not invoiced
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("fibu.title.fakturiert.not")).suppressLabelForWarning();
        if (position.getNettoSumme() != null) {
          BigDecimal invoiced = BigDecimal.ZERO;

          if (showInvoices == true) {
            BigDecimal invoicedSumForPosition = RechnungDao.getNettoSumme(invoicePositionsByOrderPositionId);
            BigDecimal notInvoicedSumForPosition = position.getNettoSumme().subtract(invoicedSumForPosition);
            invoiced = notInvoicedSumForPosition;
          } else {
            invoiced = position.getNettoSumme();
          }
          if (position.getStatus() != null) {
            if (position.getStatus().equals(AuftragsPositionsStatus.ABGELEHNT) || position.getStatus().equals(AuftragsPositionsStatus.ERSETZT) || position
                .getStatus()
                .equals(AuftragsPositionsStatus.OPTIONAL)) {
              invoiced = BigDecimal.ZERO;
            }
          }
          fs.add(new DivTextPanel(fs.newChildId(),
              CurrencyFormatter.format(invoiced)));
        }
      }
      posGridBuilder.newSplitPanel(GridSize.COL25);
      {
        // DropDownChoice status
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("status"));
        final LabelValueChoiceRenderer<AuftragsPositionsStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<AuftragsPositionsStatus>(
            fs, AuftragsPositionsStatus.values());
        final DropDownChoice<AuftragsPositionsStatus> statusChoice = new DropDownChoice<AuftragsPositionsStatus>(
            fs.getDropDownChoiceId(),
            new PropertyModel<AuftragsPositionsStatus>(position, "status"), statusChoiceRenderer.getValues(),
            statusChoiceRenderer);
        statusChoice.setNullValid(true);
        statusChoice.setRequired(false);
        fs.add(statusChoice);
        if (abgeschlossenUndNichtFakturiert == true) {
          fs.setWarningBackground();
        }
      }
      posGridBuilder.newSplitPanel(GridSize.COL100);
      {
        // Task
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("task"));
        final TaskSelectPanel taskSelectPanel = new TaskSelectPanel(fs, new PropertyModel<TaskDO>(position, "task"),
            parentPage, "taskId:"
            + position.getNumber())
        {
          @Override
          protected void selectTask(final TaskDO task)
          {
            super.selectTask(task);
            parentPage.getBaseDao().setTask(position, task.getId());
          }
        };
        fs.add(taskSelectPanel);
        taskSelectPanel.init();
      }

      posGridBuilder.newSplitPanel(GridSize.COL100);
      {
        // Period of performance
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("fibu.periodOfPerformance"));
        final LabelValueChoiceRenderer<PeriodOfPerformanceType> performanceChoiceRenderer = new LabelValueChoiceRenderer<PeriodOfPerformanceType>(
            fs, PeriodOfPerformanceType.values());
        final DropDownChoice<PeriodOfPerformanceType> performanceChoice = new DropDownChoice<PeriodOfPerformanceType>(
            fs.getDropDownChoiceId(), new PropertyModel<PeriodOfPerformanceType>(position, "periodOfPerformanceType"),
            performanceChoiceRenderer.getValues(), performanceChoiceRenderer)
        {
          /**
           * @see org.apache.wicket.markup.html.form.AbstractSingleSelectChoice#getDefaultChoice(java.lang.String)
           */
          @Override
          protected CharSequence getDefaultChoice(final String selectedValue)
          {
            if (posHasOwnPeriodOfPerformance(position.getNumber()) == true) {
              return super.getDefaultChoice(PeriodOfPerformanceType.OWN.toString());
            } else {
              return super.getDefaultChoice(PeriodOfPerformanceType.SEEABOVE.toString());
            }
          }
        };

        performanceChoice.add(new AjaxFormComponentUpdatingBehavior("onchange")
        {
          @Override
          protected void onUpdate(final AjaxRequestTarget target)
          {
            final short pos = position.getNumber();
            final PeriodOfPerformanceType s = performanceChoice.getModelObject();
            final boolean visible = s.equals(PeriodOfPerformanceType.OWN);
            setPosPeriodOfPerformanceVisible(pos, visible);
            if (ajaxPosTargets != null) {
              for (final Component ajaxPosTarget : ajaxPosTargets)
                target.add(ajaxPosTarget);
            }
          }
        });
        performanceChoice.setOutputMarkupPlaceholderTag(true);
        fs.add(performanceChoice);
        performanceChoices.add(performanceChoice);

        final BooleanSupplier isPerformanceTypeOwnSelected = () -> PeriodOfPerformanceType.OWN.equals(performanceChoice.getModelObject());

        final DatePanel fromDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(position, "periodOfPerformanceBegin"),
            DatePanelSettings.get().withTargetType(java.sql.Date.class), isPerformanceTypeOwnSelected);
        fromDatePanel.getDateField().setOutputMarkupPlaceholderTag(true);
        fs.add(fromDatePanel);
        ajaxPosTargets.add(fromDatePanel.getDateField());
        dependentComponents.add(fromDatePanel);

        final DivTextPanel divPanel = new DivTextPanel(fs.newChildId(), "-");
        divPanel.getLabel4Ajax().setOutputMarkupPlaceholderTag(true);
        fs.add(divPanel);
        ajaxPosTargets.add(divPanel.getLabel4Ajax());

        final DatePanel endDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(position, "periodOfPerformanceEnd"),
            DatePanelSettings.get().withTargetType(java.sql.Date.class), isPerformanceTypeOwnSelected);
        endDatePanel.getDateField().setOutputMarkupPlaceholderTag(true);
        fs.add(endDatePanel);
        ajaxPosTargets.add(endDatePanel.getDateField());
        dependentComponents.add(endDatePanel);

        final LabelValueChoiceRenderer<ModeOfPaymentType> paymentChoiceRenderer = new LabelValueChoiceRenderer<ModeOfPaymentType>(
            fs,
            ModeOfPaymentType.values());
        final DropDownChoice<ModeOfPaymentType> paymentChoice = new DropDownChoice<ModeOfPaymentType>(
            fs.getDropDownChoiceId(),
            new PropertyModel<ModeOfPaymentType>(position, "modeOfPaymentType"), paymentChoiceRenderer.getValues(),
            paymentChoiceRenderer);
        paymentChoice.setOutputMarkupPlaceholderTag(true);
        fs.add(paymentChoice);
        ajaxPosTargets.add(paymentChoice);
      }
      posGridBuilder.newGridPanel();
      {
        // Comment
        final FieldsetPanel fs = posGridBuilder.newFieldset(getString("comment"));
        fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(position, "bemerkung")));
      }
      GridBuilder removeButtonGridBuilder = posGridBuilder.newGridPanel();
      {
        // Remove Position
        DivPanel divPanel = removeButtonGridBuilder.getPanel();
        final Button removePositionButton = new Button(SingleButtonPanel.WICKET_ID)
        {
          @Override
          public final void onSubmit()
          {
            position.setDeleted(true);
            refresh();
          }
        };
        removePositionButton.add(AttributeModifier.append("class", ButtonType.DELETE.getClassAttrValue()));
        final SingleButtonPanel removePositionButtonPanel = new SingleButtonPanel(divPanel.newChildId(), removePositionButton,
            getString("delete"));
        divPanel.add(removePositionButtonPanel);
      }
      if (position.isDeleted()) {
        positionsPanel.setVisible(false);
      }
      setPosPeriodOfPerformanceVisible(position.getNumber(), posHasOwnPeriodOfPerformance(position.getNumber()));
    }
    positionsDependentFormComponents = dependentComponents.toArray(new FormComponent[0]);

  }

  protected String getPositionHeading(final AuftragsPositionDO position, final ToggleContainerPanel positionsPanel)
  {
    if (positionsPanel.getToggleStatus() == ToggleStatus.OPENED) {
      return getString("label.position.short") + " #" + position.getNumber();
    }
    final StringBuffer heading = new StringBuffer();
    heading.append(escapeHtml(getString("label.position.short"))).append(" #").append(position.getNumber());
    heading.append(": ").append(CurrencyFormatter.format(position.getNettoSumme()));
    if (position.getStatus() != null) {
      heading.append(", ").append(getString(position.getStatus().getI18nKey()));
    }
    if (position.isVollstaendigFakturiert() == false) {
      heading.append(" (").append(getString("fibu.fakturiert.not")).append(")");
    }
    if (StringHelper.isNotBlank(position.getTitel()) == true) {
      heading.append(": ").append(StringUtils.abbreviate(position.getTitel(), 80));
    }
    return heading.toString();
  }

  protected String getPaymentScheduleHeading(final List<PaymentScheduleDO> paymentSchedules,
      final ToggleContainerPanel schedulesPanel)
  {
    BigDecimal ges = BigDecimal.ZERO;
    BigDecimal invoiced = BigDecimal.ZERO;
    if (paymentSchedules != null) {
      for (final PaymentScheduleDO schedule : paymentSchedules) {
        if (schedule.getAmount() != null) {
          ges = ges.add(schedule.getAmount());
          if (schedule.isVollstaendigFakturiert() == true) {
            invoiced = invoiced.add(schedule.getAmount());
          }
        }
        if (schedule.isReached() == true && schedule.isVollstaendigFakturiert() == false) {
          schedulesPanel.setHighlightedHeader();
        }
      }
    }
    if (schedulesPanel.getToggleStatus() == ToggleStatus.OPENED) {
      return getString("fibu.auftrag.paymentschedule") + " ("
          + (paymentSchedules != null ? paymentSchedules.size() : "0") + ")";
    }
    final StringBuffer heading = new StringBuffer();
    heading.append(escapeHtml(getString("fibu.auftrag.paymentschedule"))).append(" (")
        .append(paymentSchedules != null ? paymentSchedules.size() : "0").append(")");
    heading.append(": ").append(CurrencyFormatter.format(ges)).append(" ").append(getString("fibu.fakturiert"))
        .append(" ")
        .append(CurrencyFormatter.format(invoiced));
    return heading.toString();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  private boolean posHasOwnPeriodOfPerformance(final short number)
  {
    return ((getData().getPosition(number).getPeriodOfPerformanceBegin() != null
        && StringUtils.isBlank(getData().getPosition(number)
        .getPeriodOfPerformanceBegin().toString()) == false)
        || (getData().getPosition(number).getPeriodOfPerformanceEnd() != null
        && StringUtils.isBlank(getData().getPosition(number)
        .getPeriodOfPerformanceEnd().toString()) == false)
        || getData().getPosition(number).getPeriodOfPerformanceType() == PeriodOfPerformanceType.OWN);
  }

  private void setPosPeriodOfPerformanceVisible(final short pos, final boolean visible)
  {
    ajaxPosTargets.get(pos * 4 - 4).setVisible(visible);
    ajaxPosTargets.get(pos * 4 - 3).setVisible(visible);
    ajaxPosTargets.get(pos * 4 - 2).setVisible(visible);
    ajaxPosTargets.get(pos * 4 - 1).setVisible(visible);
  }
}
