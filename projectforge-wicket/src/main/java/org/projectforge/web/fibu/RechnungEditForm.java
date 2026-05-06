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

package org.projectforge.web.fibu;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.business.fibu.kost.KundeCache;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.*;
import org.projectforge.web.wicket.flowlayout.FieldProperties;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.slf4j.Logger;

import java.time.LocalDate;

public class RechnungEditForm extends AbstractRechnungEditForm<RechnungDO, RechnungsPositionDO, RechnungEditPage>
{
  private static final long serialVersionUID = -6018131069720611834L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RechnungEditForm.class);

  private PeriodOfPerformanceHelper periodOfPerformanceHelper = new PeriodOfPerformanceHelper();

  NewCustomerSelectPanel customerSelectPanel;

  NewProjektSelectPanel projektSelectPanel;

  private org.apache.wicket.markup.html.basic.Label eInvoiceSummaryLabel;

  EInvoiceModalDialog eInvoiceDialog;

  public RechnungEditForm(final RechnungEditPage parentPage, final RechnungDO data)
  {
    super(parentPage, data);
    // no submit on clone
    this.ignoreErrorOnClone = true;
  }

  @SuppressWarnings("serial")
  @Override
  protected void onInit()
  {
    gridBuilder.newGridPanel();
    {
      // Subject
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "betreff");
      final MaxLengthTextField subject = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "betreff"));
      subject.add(WicketUtils.setFocus());
      fs.add(subject);
    }
    // GRID 50% - BLOCK
    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL50);
    {
      // Number
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "nummer");
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
    {
      // Type
      gridBuilder.newSubSplitPanel(GridSize.COL50);
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "typ");
      final LabelValueChoiceRenderer<RechnungTyp> typeChoiceRenderer = new LabelValueChoiceRenderer<RechnungTyp>(this,
              RechnungTyp.values());
      final DropDownChoice<RechnungTyp> typeChoice = new DropDownChoice<RechnungTyp>(fs.getDropDownChoiceId(),
              new PropertyModel<RechnungTyp>(data, "typ"), typeChoiceRenderer.getValues(), typeChoiceRenderer);
      typeChoice.setNullValid(false);
      typeChoice.setRequired(true);
      fs.add(typeChoice);
    }
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    {
      // Date
      final FieldProperties<LocalDate> props = getDatumProperties();
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "datum");
      LocalDatePanel components = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      components.setRequired(true);
      fs.add(components);
    }
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    {
      // Status
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "status");
      final LabelValueChoiceRenderer<RechnungStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<RechnungStatus>(
          this,
          RechnungStatus.values());
      final DropDownChoice<RechnungStatus> statusChoice = new DropDownChoice<>(fs.getDropDownChoiceId(), new PropertyModel<>(data, "status"),
          statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false);
      statusChoice.setRequired(true);
      fs.add(statusChoice);
    }
    gridBuilder.newSubSplitPanel(GridSize.COL100);
    if (WicketSupport.get(KontoCache.class).isEmpty() == false) {
      // Show this field only if DATEV accounts does exist.
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "konto");
      final KontoSelectPanel kontoSelectPanel = new KontoSelectPanel(fs.newChildId(),
              new PropertyModel<KontoDO>(data, "konto"), null,
              "kontoId");
      kontoSelectPanel.setKontoNumberRanges(AccountingConfig.getInstance().getDebitorsAccountNumberRanges());
      fs.addHelpIcon(getString("fibu.rechnung.konto.tooltip"));
      fs.add(kontoSelectPanel);
      kontoSelectPanel.init();
    }
    {
      // Projekt
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "projekt").suppressLabelForWarning();
      projektSelectPanel = new NewProjektSelectPanel(fs.newChildId(), new PropertyModel<ProjektDO>(data,
          "projekt"), parentPage, "projektId");
      projektSelectPanel.getTextField().add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          KundeDO kunde = WicketSupport.get(KundeCache.class).getKundeIfNotInitialized(getData().getKunde());
          if (kunde == null && StringUtils.isBlank(getData().getKundeText()) == true && projektSelectPanel.getModelObject() != null) {
            getData().setKunde(projektSelectPanel.getModelObject().getKunde());
          }
          target.add(customerSelectPanel.getTextField());
        }
      });
      fs.add(projektSelectPanel);
      projektSelectPanel.init();
    }
    {
      // Customer
      final FieldsetPanel fs = gridBuilder.newFieldset(RechnungDO.class, "kunde");
      customerSelectPanel = new NewCustomerSelectPanel(fs.newChildId(), new PropertyModel<KundeDO>(data, "kunde"),
          new PropertyModel<String>(data, "kundeText"), parentPage, "kundeId");
      customerSelectPanel.getTextField().setOutputMarkupId(true);
      fs.add(customerSelectPanel);
      customerSelectPanel.init();
      fs.setLabelFor(customerSelectPanel.getKundeTextField());
      fs.addHelpIcon(getString("fibu.rechnung.hint.kannVonProjektKundenAbweichen"));
    }
    {
      // E-Invoice summary (read-only display of filled e-invoice fields)
      final FieldsetPanel fs1 = gridBuilder.newFieldset(getString("fibu.kunde.eInvoice"));
      eInvoiceSummaryLabel = new org.apache.wicket.markup.html.basic.Label(fs1.newChildId(),
          new org.apache.wicket.model.LoadableDetachableModel<String>() {
            @Override
            protected String load() {
              return buildEInvoiceSummary();
            }
          }) {
        @Override
        public boolean isVisible() {
          return hasEInvoiceData();
        }
      };
      eInvoiceSummaryLabel.setEscapeModelStrings(false);
      eInvoiceSummaryLabel.setOutputMarkupPlaceholderTag(true);
      fs1.add(eInvoiceSummaryLabel);
    }
    {
      // Customer reference
      final FieldsetPanel fs1 = gridBuilder.newFieldset(RechnungDO.class, "customerref1");
      final MaxLengthTextArea customerref1 = new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<>(data, "customerref1"));
      fs1.add(customerref1);
    }
    {
      // Attachment
      final FieldsetPanel fs1 = gridBuilder.newFieldset(RechnungDO.class, "attachment");
      final MaxLengthTextArea customerref1 = new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<>(data, "attachment"));
      fs1.add(customerref1);
    }
    {
      // Period of performance
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.periodOfPerformance"));
      periodOfPerformanceHelper.createPeriodOfPerformanceFields(fs,
          new PropertyModel<>(data, "periodOfPerformanceBegin"),
          new PropertyModel<>(data, "periodOfPerformanceEnd"));
    }
    add(periodOfPerformanceHelper.createValidator());
  }

  @Override
  protected void onRenderPosition(final GridBuilder posGridBuilder, final AbstractRechnungsPositionDO position)
  {
    // Period of performance
    posGridBuilder.newSplitPanel(GridSize.COL100);
    final FieldsetPanel fs = posGridBuilder.newFieldset(getString("fibu.periodOfPerformance"));
    periodOfPerformanceHelper.createPositionsPeriodOfPerformanceFields(fs,
        new PropertyModel<>(position, "periodOfPerformanceType"),
        new PropertyModel<>(position, "periodOfPerformanceBegin"),
        new PropertyModel<>(position, "periodOfPerformanceEnd"));
  }

  /**
   * Highlights the cost2 element if it differs from the cost2 of the given project (if any).
   *
   * @param position
   */
  @Override
  protected void onRenderCostRow(final AbstractRechnungsPositionDO position, final KostZuweisungDO costAssignment,
      final Component cost1Component, final Component cost2Component)
  {
    final RechnungDO invoice = ((RechnungsPositionDO) position).getRechnung();
    if (invoice == null) {
      log.warn("Oups, no invoice given. Shouldn't occur!");
      return;
    }
    final Kost2DO cost2 = costAssignment.getKost2();
    final ProjektDO projekt = invoice.getProjekt();
    int numberRange; // First number of cost.
    int area = -1; // Number 2-4
    long number; // Number 5-6.
    if (projekt != null) {
      numberRange = projekt.getNummernkreis();
      area = projekt.getBereich();
      number = projekt.getNummer();
    } else {
      final KundeDO customer = invoice.getKunde();
      if (customer == null) {
        return;
      }
      numberRange = customer.getNummernkreis();
      number = customer.getNummer();
    }
    boolean differs = false;
    if (numberRange >= 0 && cost2.getNummernkreis() != numberRange) {
      differs = true;
    } else if (area >= 0 && cost2.getBereich() != area) {
      differs = true;
    } else if (number >= 0 && cost2.getTeilbereich() != number) {
      differs = true;
    }
    if (differs == true) {
      WicketUtils.setWarningTooltip(cost2Component);
    }
  }

  @Override
  protected void refreshPositions()
  {
    periodOfPerformanceHelper.onRefreshPositions();
    super.refreshPositions();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected RechnungsPositionDO newPositionInstance()
  {
    return new RechnungsPositionDO();
  }

  void addEInvoiceModalDialog() {
    eInvoiceDialog = new EInvoiceModalDialog();
    eInvoiceDialog.setOutputMarkupId(true);
    parentPage.add(eInvoiceDialog);
    eInvoiceDialog.init();
  }

  private boolean hasEInvoiceData() {
    return StringUtils.isNotBlank(data.getCustomerAddress())
        || StringUtils.isNotBlank(data.getCustomerZipCode())
        || StringUtils.isNotBlank(data.getCustomerCity())
        || StringUtils.isNotBlank(data.getCustomerVatId())
        || StringUtils.isNotBlank(data.getCustomerLeitwegId())
        || StringUtils.isNotBlank(data.getCustomerEInvoiceEmail())
        || StringUtils.isNotBlank(data.getSellerBankAccount());
  }

  private String buildEInvoiceSummary() {
    final java.util.List<String> parts = new java.util.ArrayList<>();
    final StringBuilder addr = new StringBuilder();
    if (StringUtils.isNotBlank(data.getCustomerAddress())) {
      addr.append(data.getCustomerAddress().trim().replace("\n", ", ").replaceAll("\\s*,\\s*", ", "));
    }
    if (StringUtils.isNotBlank(data.getCustomerZipCode()) || StringUtils.isNotBlank(data.getCustomerCity())) {
      if (addr.length() > 0) addr.append(", ");
      if (StringUtils.isNotBlank(data.getCustomerZipCode())) addr.append(data.getCustomerZipCode()).append(" ");
      if (StringUtils.isNotBlank(data.getCustomerCity())) addr.append(data.getCustomerCity());
    }
    if (StringUtils.isNotBlank(data.getCustomerCountry())) {
      if (addr.length() > 0) addr.append(", ");
      addr.append(data.getCustomerCountry());
    }
    if (addr.length() > 0) parts.add(addr.toString());
    if (StringUtils.isNotBlank(data.getCustomerVatId())) parts.add(getString("fibu.kunde.vatId") + ": " + data.getCustomerVatId());
    if (StringUtils.isNotBlank(data.getCustomerLeitwegId())) parts.add(getString("fibu.kunde.leitwegId") + ": " + data.getCustomerLeitwegId());
    if (StringUtils.isNotBlank(data.getCustomerEInvoiceEmail())) parts.add(getString("fibu.kunde.eInvoiceEmail") + ": " + data.getCustomerEInvoiceEmail());
    if (StringUtils.isNotBlank(data.getSellerBankAccount())) {
      final EInvoiceSellerConfig cfg = WicketSupport.get(EInvoiceSellerConfig.class);
      final BankAccountConfig ba = cfg.findBankAccount(data.getSellerBankAccount());
      parts.add(getString("fibu.rechnung.sellerBankAccount") + ": " + (ba != null ? ba.getDisplayName() : data.getSellerBankAccount()));
    }
    return String.join(" | ", parts);
  }

  @SuppressWarnings("serial")
  class EInvoiceModalDialog extends ModalDialog {
    private static final long serialVersionUID = 1L;

    EInvoiceModalDialog() {
      super(parentPage.newModalDialogId());
      setBigWindow();
      setShowCancelButton();
      setCloseButtonLabel(getString("save"));
    }

    @Override
    public void init() {
      setTitle(getString("fibu.kunde.eInvoice"));
      super.init(new Form<String>(getFormId()));
      // Download behavior for XRechnung export
      final org.apache.wicket.behavior.AbstractAjaxBehavior downloadBehavior = new org.apache.wicket.behavior.AbstractAjaxBehavior() {
        @Override
        public void onRequest() {
          final EInvoiceExportService service = WicketSupport.get(EInvoiceExportService.class);
          byte[] xml = service.exportAsXRechnung(data);
          String filename = service.getExportFilename(data);
          org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler handler =
              new org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler(
                  new org.apache.wicket.util.resource.AbstractResourceStream() {
                    @Override
                    public java.io.InputStream getInputStream() {
                      return new java.io.ByteArrayInputStream(xml);
                    }
                    @Override
                    public void close() {}
                  }, filename);
          handler.setContentDisposition(org.apache.wicket.request.resource.ContentDisposition.ATTACHMENT);
          getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
        }
      };
      add(downloadBehavior);
      // Export button: validates via Ajax, then triggers download
      appendNewAjaxActionButton(new de.micromata.wicket.ajax.AjaxFormSubmitCallback() {
        @Override
        public void callback(final AjaxRequestTarget target) {
          parentPage.getBaseDao().update(data);
          final EInvoiceExportService service = WicketSupport.get(EInvoiceExportService.class);
          final java.util.List<String> errors = service.validate(data);
          if (!errors.isEmpty()) {
            error(getString("fibu.rechnung.eInvoice.validationErrors") + ": " + String.join("; ", errors));
            target.add(formFeedback);
            return;
          }
          target.appendJavaScript("window.location.href='" + downloadBehavior.getCallbackUrl() + "';");
          close(target);
        }
        @Override
        public void onError(final AjaxRequestTarget target, final org.apache.wicket.markup.html.form.Form<?> form) {
          target.add(formFeedback);
        }
      }, getString("fibu.rechnung.exportEInvoice"), SingleButtonPanel.NORMAL);
      gridBuilder.newSplitPanel(GridSize.COL100);
      // Street (multi-line)
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde.street"));
        final MaxLengthTextArea streetArea = new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<>(data, "customerAddress"));
        streetArea.add(AttributeModifier.replace("style", "width: 100%; box-sizing: border-box;"));
        fs.add(streetArea, false);
      }
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde.zipCode"));
        final MaxLengthTextField zipField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "customerZipCode"));
        WicketUtils.setSize(zipField, 10);
        fs.add(zipField);
      }
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde.city"));
        fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "customerCity")));
      }
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde.country"));
        final MaxLengthTextField countryField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "customerCountry"));
        WicketUtils.setSize(countryField, 2);
        fs.add(countryField);
      }
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde.vatId"));
        fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "customerVatId")));
      }
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde.leitwegId"));
        fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "customerLeitwegId")));
      }
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde.eInvoiceEmail"));
        fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "customerEInvoiceEmail")));
      }
      {
        // Seller bank account selection
        final EInvoiceSellerConfig sellerConfig = WicketSupport.get(EInvoiceSellerConfig.class);
        if (!sellerConfig.getBankAccounts().isEmpty()) {
          final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.rechnung.sellerBankAccount"));
          final java.util.List<String> ibanChoices = new java.util.ArrayList<>();
          for (BankAccountConfig account : sellerConfig.getBankAccounts()) {
            ibanChoices.add(account.getIban());
          }
          final DropDownChoice<String> bankAccountChoice = new DropDownChoice<>(fs.getDropDownChoiceId(),
              new PropertyModel<>(data, "sellerBankAccount"), ibanChoices,
              new org.apache.wicket.markup.html.form.IChoiceRenderer<String>() {
                @Override
                public Object getDisplayValue(String iban) {
                  BankAccountConfig account = WicketSupport.get(EInvoiceSellerConfig.class).findBankAccount(iban);
                  return account != null ? account.getDisplayName() : iban;
                }
                @Override
                public String getIdValue(String iban, int index) {
                  return iban;
                }
                @Override
                public String getObject(String id, org.apache.wicket.model.IModel<? extends java.util.List<? extends String>> choices) {
                  return id;
                }
              });
          bankAccountChoice.setNullValid(true);
          fs.add(bankAccountChoice);
        }
      }
    }

    @Override
    protected boolean onCloseButtonSubmit(final AjaxRequestTarget target) {
      parentPage.getBaseDao().update(data);
      target.add(eInvoiceSummaryLabel);
      return true;
    }
  }
}
