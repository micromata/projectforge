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
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.SHType;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.autocompletion.I18nEnumAutoCompleteTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.RequiredMinMaxNumberField;
import org.projectforge.web.wicket.converter.CurrencyConverter;
import org.projectforge.web.wicket.converter.IntegerConverter;
import org.projectforge.web.wicket.converter.MonthConverter;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

public class AccountingRecordEditForm extends AbstractEditForm<BuchungssatzDO, AccountingRecordEditPage>
{
  private static final long serialVersionUID = 8746545908106124484L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AccountingRecordEditForm.class);

  public AccountingRecordEditForm(final AccountingRecordEditPage parentPage, final BuchungssatzDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Date
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("date"));
      final DatePanel datePanel = new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "datum"),
          new DatePanelSettings().withTargetType(java.sql.Date.class));
      WicketUtils.setReadonly(datePanel.getDateField());
      fs.add(datePanel);
    }
    {
      // Year / month
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("calendar.year") + "/" + getString("calendar.month"));
      final MinMaxNumberField<Integer> yearField = new RequiredMinMaxNumberField<Integer>(InputPanel.WICKET_ID, new PropertyModel<Integer>(
          data, "year"), 1900, 2100).setConverter(new IntegerConverter(4));
      fs.add(yearField);
      WicketUtils.setReadonly(yearField);
      final MinMaxNumberField<Integer> monthField = new RequiredMinMaxNumberField<Integer>(InputPanel.WICKET_ID,
          new PropertyModel<Integer>(data, "month"), 0, 11).setConverter(new MonthConverter());
      fs.add(monthField);
      WicketUtils.setReadonly(monthField);
    }
    {
      // Satznr.
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.buchungssatz.satznr"));
      final MinMaxNumberField<Integer> satzNrField = new RequiredMinMaxNumberField<Integer>(InputPanel.WICKET_ID,
          new PropertyModel<Integer>(data, "satznr"), 1, 99999).setConverter(new IntegerConverter(5));
      WicketUtils.setReadonly(satzNrField);
      fs.add(satzNrField);
    }
    {
      // Amount / debit/credit
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.common.betrag") + "/" + getString("finance.accountingRecord.dc"));
      final MinMaxNumberField<BigDecimal> betragField = new MinMaxNumberField<BigDecimal>(InputPanel.WICKET_ID,
          new PropertyModel<BigDecimal>(data, "betrag"), new BigDecimal("-99999999"), new BigDecimal("99999999"));
      fs.add(betragField);
      WicketUtils.setReadonly(betragField.setConverter(new CurrencyConverter()));
      final I18nEnumAutoCompleteTextField<SHType> dcField = new I18nEnumAutoCompleteTextField<SHType>(InputPanel.WICKET_ID,
          new PropertyModel<SHType>(data, "sh"), SHType.values());
      WicketUtils.setReadonly(dcField);
      dcField.setEnabled(false);
      fs.add(dcField);
    }
    {
      // DropDownChoice debitor/creditor
      // final LabelValueChoiceRenderer<SHType> dcChoiceRenderer = new LabelValueChoiceRenderer<SHType>(container, SHType.values());
      // final DropDownChoice<SHType> dcTypeChoice = new DropDownChoice<SHType>(SELECT_ID, new PropertyModel<SHType>(data, "sh"),
      // dcChoiceRenderer.getValues(), dcChoiceRenderer);
      // WicketUtils.setReadonly(dcTypeChoice.setNullValid(false).setRequired(true));
      // dcTypeChoice.setEnabled(false);
      // doPanel.addDropDownChoice(data, "sh", getString("finance.accountingRecord.dc"), HALF, dcTypeChoice, HALF);

    }
    {
      // Beleg
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.buchungssatz.beleg"));
      final MaxLengthTextField belegField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "beleg"));
      WicketUtils.setReadonly(belegField);
      fs.add(belegField);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Cost 1 / cost2
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kost1") + "/" + getString("fibu.kost2"));
      final Kost1FormComponent kost1Component = new Kost1FormComponent(InputPanel.WICKET_ID, new PropertyModel<Kost1DO>(data, "kost1"),
          true);
      fs.add(kost1Component);
      WicketUtils.setReadonly(kost1Component);
      final Kost2FormComponent kost2Component = new Kost2FormComponent(InputPanel.WICKET_ID, new PropertyModel<Kost2DO>(data, "kost2"),
          true);
      fs.add(kost2Component);
      WicketUtils.setReadonly(kost2Component);
    }
    {
      // Cost 1 / cost2
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.buchungssatz.konto")
          + "/"
          + getString("fibu.buchungssatz.gegenKonto"));
      final KontoFormComponent kontoComponent = new KontoFormComponent(InputPanel.WICKET_ID, new PropertyModel<KontoDO>(data, "konto"),
          true);
      fs.add(kontoComponent);
      WicketUtils.setReadonly(kontoComponent);
      final KontoFormComponent gegenKontoComponent = new KontoFormComponent(InputPanel.WICKET_ID, new PropertyModel<KontoDO>(data,
          "gegenKonto"), true);
      fs.add(gegenKontoComponent);
      WicketUtils.setReadonly(gegenKontoComponent);
    }
    {
      // Text
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.buchungssatz.text"));
      final MaxLengthTextField textField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "text"));
      WicketUtils.setReadonly(textField);
      fs.add(textField);
    }
    {
      // Menge
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.buchungssatz.menge"));
      final MaxLengthTextField mengeField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "menge"));
      WicketUtils.setReadonly(mengeField);
      fs.add(mengeField);
    }
    gridBuilder.newGridPanel();
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("comment"));
      final MaxLengthTextArea commentField = new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "comment"));
      fs.add(commentField);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
