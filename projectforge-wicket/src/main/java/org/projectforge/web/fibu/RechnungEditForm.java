/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.business.fibu.kost.KundeCache;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.WicketSupport;
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
      // Customer address
      final FieldsetPanel fs1 = gridBuilder.newFieldset(RechnungDO.class, "customerAddress");
      final MaxLengthTextArea customerAddress = new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<>(data, "customerAddress"));
      fs1.add(customerAddress);
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
}
