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

import java.util.List;

import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
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
  private transient KontoCache kontoCache;

  @SpringBean
  private transient PfEmgrFactory pfEmgrFactory;

  private MaxLengthTextField recieverField;
  private MaxLengthTextField ibanField;
  private MaxLengthTextField bicField;
  private MaxLengthTextField customernrField;

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
      kreditorField.add(new AjaxFormComponentUpdatingBehavior("change")
      {
        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          autofillLatestKreditorInformations(kreditorField.getModelObject(), target);
        }
      });
      fs.add(kreditorField);
    }
    {
      // Customernr
      final FieldsetPanel fs = gridBuilder.newFieldset(EingangsrechnungDO.class, "customernr");
      customernrField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "customernr"));
      customernrField.setOutputMarkupId(true);
      fs.add(customernrField);
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

  private void autofillLatestKreditorInformations(final String kreditorName, final AjaxRequestTarget target)
  {
    if (StringUtils.isEmpty(kreditorName)) {
      return;
    }

    final EingangsrechnungDO latestRe = pfEmgrFactory.runRoTrans(emgr -> {
      final String sql = "SELECT er FROM EingangsrechnungDO er WHERE er.kreditor = :kreditor AND er.deleted = false ORDER BY er.created DESC";
      final TypedQuery<EingangsrechnungDO> query = emgr.createQueryDetached(EingangsrechnungDO.class, sql, "kreditor", kreditorName);
      final List<EingangsrechnungDO> resultList = query.setMaxResults(1).getResultList();
      return (resultList != null && resultList.size() > 0) ? resultList.get(0) : null;
    });

    if (latestRe == null) {
      return;
    }

    // Update Customer No.
    getData().setCustomernr(latestRe.getCustomernr());
    target.add(customernrField);

    // Update Konto
    getData().setReceiver(latestRe.getReceiver());
    target.add(recieverField);

    getData().setIban(latestRe.getIban());
    target.add(ibanField);

    getData().setBic(latestRe.getBic());
    target.add(bicField);
  }

  @Override
  protected void addCellAfterDiscount()
  {
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
      recieverField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "receiver"));
      recieverField.setOutputMarkupId(true);
      fs.add(recieverField);
    }
    {
      // IBAN
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "iban");
      ibanField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "iban"));
      ibanField.setOutputMarkupId(true);
      fs.add(ibanField);
    }
    {
      // BIC
      final FieldsetPanel fs = gridBuilder.newFieldset(AbstractRechnungDO.class, "bic");
      bicField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "bic"));
      bicField.setOutputMarkupId(true);
      fs.add(bicField);
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
