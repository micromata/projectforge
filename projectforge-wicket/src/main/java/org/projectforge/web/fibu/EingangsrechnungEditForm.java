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

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.AbstractRechnungDO;
import org.projectforge.business.fibu.EingangsrechnungDO;
import org.projectforge.business.fibu.EingangsrechnungsPositionDO;
import org.projectforge.business.fibu.KontoCache;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.PaymentType;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;

public class EingangsrechnungEditForm extends
    AbstractRechnungEditForm<EingangsrechnungDO, EingangsrechnungsPositionDO, EingangsrechnungEditPage>
{
  private static final long serialVersionUID = 5286417118638335693L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EingangsrechnungEditForm.class);

  @SpringBean
  KontoCache kontoCache;

  public EingangsrechnungEditForm(final EingangsrechnungEditPage parentPage, final EingangsrechnungDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void onInit()
  {
    {
      // Subject
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "betreff");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "betreff")));
    }
    /* GRID50 - BLOCK */
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Creditor
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "kreditor");
      final PFAutoCompleteTextField<String> kreditorField = new PFAutoCompleteTextField<String>(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "kreditor"))
      {
        @Override
        protected List<String> getChoices(final String input)
        {
          return parentPage.getBaseDao().getAutocompletion("kreditor", input);
        }
      };
      kreditorField.withMatchContains(true).withMinChars(2).withFocus(true).add(WicketUtils.setFocus());
      fs.add(kreditorField);
    }
    {
      // Customernr
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "customernr");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "customernr")));
    }
    {
      // Reference
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "referenz");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "referenz")));
    }
    if (kontoCache.isEmpty() == false) {
      // Account
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "konto");
      final KontoSelectPanel kontoSelectPanel = new KontoSelectPanel(fs.newChildId(),
          new PropertyModel<KontoDO>(data, "konto"),
          parentPage, "kontoId");
      kontoSelectPanel.setKontoNumberRanges(AccountingConfig.getInstance().getCreditorsAccountNumberRanges()).init();
      fs.add(kontoSelectPanel);
      fs.setLabelFor(kontoSelectPanel);
    }
  }

  @Override
  protected void addCellAfterFaelligkeit()
  {
    {
      // Skonto
      gridBuilder.newSubSplitPanel(GridSize.COL50);
      final FieldsetPanel fs = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage("fibu.rechnung.discount"));
      DatePanel discountMaturity = new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "discountMaturity"), DatePanelSettings.get()
          .withTargetType(java.sql.Date.class), true);
      MaxLengthTextField discountPercent = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "discountPercent"));
      fs.add(discountMaturity);
      fs.add(discountPercent);
    }
    {
      // DropDownChoice payment type
      gridBuilder.newSplitPanel(GridSize.COL50);
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "paymentType");
      final LabelValueChoiceRenderer<PaymentType> paymentTypeChoiceRenderer = new LabelValueChoiceRenderer<PaymentType>(
          this,
          PaymentType.values());
      final DropDownChoice<PaymentType> paymentTypeChoice = new DropDownChoice<PaymentType>(fs.getDropDownChoiceId(),
          new PropertyModel<PaymentType>(data, "paymentType"), paymentTypeChoiceRenderer.getValues(),
          paymentTypeChoiceRenderer);
      paymentTypeChoice.setNullValid(true);
      fs.add(paymentTypeChoice);
    }
    {
      // Reciever
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "receiver");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "receiver")));
    }
    {
      // IBAN
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "iban");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "iban")));
    }
    {
      // BIC
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "bic");
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "bic")));
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected EingangsrechnungsPositionDO newPositionInstance()
  {
    return new EingangsrechnungsPositionDO();
  }
}
