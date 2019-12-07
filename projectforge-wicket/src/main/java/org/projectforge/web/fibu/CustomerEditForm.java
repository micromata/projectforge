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

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.fibu.KontoCache;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.KundeStatus;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.converter.IntegerConverter;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.slf4j.Logger;

public class CustomerEditForm extends AbstractEditForm<KundeDO, CustomerEditPage>
{
  private static final long serialVersionUID = -6018131069720611834L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomerEditForm.class);

  @SpringBean
  KontoCache kontoCache;

  public CustomerEditForm(final CustomerEditPage parentPage, final KundeDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    /* GRID16 - BLOCK */
    gridBuilder.newGridPanel();
    {
      // Number
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde.nummer"));
      final MinMaxNumberField<Integer> number = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID,
          new PropertyModel<Integer>(data, "id"), 0, KundeDO.MAX_ID)
      {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public IConverter getConverter(final Class type)
        {
          return new IntegerConverter(3);
        }
      };
      number.setRequired(true);
      WicketUtils.setSize(number, 7);
      fs.add(number);
      if (isNew() == true) {
        WicketUtils.setFocus(number);
      }
    }
    {
      // Name
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde.name"));
      final RequiredMaxLengthTextField name = new RequiredMaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "name"));
      fs.add(name);
      if (isNew() == false) {
        WicketUtils.setFocus(name);
      }
    }
    if (kontoCache.isEmpty() == false) {
      // Show this field only if DATEV accounts does exist.
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.konto"));
      final KontoSelectPanel kontoSelectPanel = new KontoSelectPanel(fs.newChildId(),
          new PropertyModel<KontoDO>(data, "konto"), null,
          "kontoId");
      kontoSelectPanel.setKontoNumberRanges(AccountingConfig.getInstance().getDebitorsAccountNumberRanges()).init();
      fs.addHelpIcon(getString("fibu.kunde.konto.tooltip"));
      fs.add(kontoSelectPanel);
    }
    {
      // Identifier
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde.identifier"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "identifier")));
    }
    {
      // Identifier
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.kunde.division"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<String>(data, "division")));
    }
    {
      // Identifier
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("description"));
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "description")));
    }
    {
      // Status drop down box:
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("status"));
      final LabelValueChoiceRenderer<KundeStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<KundeStatus>(fs,
          KundeStatus.values());
      final DropDownChoice<KundeStatus> statusChoice = new DropDownChoice<KundeStatus>(fs.getDropDownChoiceId(),
          new PropertyModel<KundeStatus>(data, "status"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false).setRequired(true);
      fs.add(statusChoice);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  public boolean isNew()
  {
    return parentPage.isNew();
  }
}
