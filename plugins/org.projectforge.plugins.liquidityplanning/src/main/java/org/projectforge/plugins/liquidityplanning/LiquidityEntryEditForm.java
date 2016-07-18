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

package org.projectforge.plugins.liquidityplanning;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.framework.utils.Constants;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.components.RequiredMinMaxNumberField;
import org.projectforge.web.wicket.converter.CurrencyConverter;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

/**
 * This is the edit formular page.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LiquidityEntryEditForm extends AbstractEditForm<LiquidityEntryDO, LiquidityEntryEditPage>
{
  private static final long serialVersionUID = -6208809585214296635L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LiquidityEntryEditForm.class);

  public LiquidityEntryEditForm(final LiquidityEntryEditPage parentPage, final LiquidityEntryDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    {
      // Date of payment
      final FieldsetPanel fs = gridBuilder.newFieldset(LiquidityEntryDO.class, "dateOfPayment");
      final DatePanel dateOfPayment = new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "dateOfPayment"),
          DatePanelSettings
              .get().withTargetType(java.sql.Date.class));
      fs.add(dateOfPayment);
      if (isNew() == true) {
        dateOfPayment.setFocus();
      }
    }
    {
      // Amount
      final FieldsetPanel fs = gridBuilder.newFieldset(LiquidityEntryDO.class, "amount");
      final RequiredMinMaxNumberField<BigDecimal> amount = new RequiredMinMaxNumberField<BigDecimal>(
          fs.getTextFieldId(),
          new PropertyModel<BigDecimal>(data, "amount"), Constants.TEN_BILLION_NEGATIVE, Constants.TEN_BILLION)
      {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public IConverter getConverter(final Class type)
        {
          return new CurrencyConverter();
        }
      };
      WicketUtils.setSize(amount, 8);
      fs.add(amount);
      if (isNew() == false) {
        amount.add(WicketUtils.setFocus());
      }
    }
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(LiquidityEntryDO.class, "paid");
      fs.addCheckBox(new PropertyModel<Boolean>(data, "paid"), null);
    }
    {
      // Subject
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.rechnung.betreff"));
      final RequiredMaxLengthTextField subject = new RequiredMaxLengthTextField(fs.getTextFieldId(),
          new PropertyModel<String>(data,
              "subject"));
      fs.add(subject);
    }
    {
      // Text comment
      final FieldsetPanel fs = gridBuilder.newFieldset(LiquidityEntryDO.class, "comment");
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<String>(data, "comment"))).setAutogrow();
    }
    addCloneButton();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
