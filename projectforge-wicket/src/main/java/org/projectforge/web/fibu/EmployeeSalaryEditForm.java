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

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.fibu.EmployeeSalaryType;
import org.projectforge.common.StringHelper;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.converter.CurrencyConverter;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;
import org.slf4j.Logger;

public class EmployeeSalaryEditForm extends AbstractEditForm<EmployeeSalaryDO, EmployeeSalaryEditPage>
{
  private static final long serialVersionUID = 8746545908106124484L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeSalaryEditForm.class);

  public EmployeeSalaryEditForm(final EmployeeSalaryEditPage parentPage, final EmployeeSalaryDO data)
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
      // Employee
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeSalaryDO.class, "employee");
      final EmployeeSelectPanel employeeSelectPanel = new EmployeeSelectPanel(fs.newChildId(), new PropertyModel<EmployeeDO>(data,
          "employee"), parentPage, "employee");
      fs.add(employeeSelectPanel);
      employeeSelectPanel.setFocus().setRequired(true);
      employeeSelectPanel.init();
    }
    {
      // DropDownChoice months
      final FieldsetPanel fs = gridBuilder.newFieldset(WicketUtils.createMultipleFieldsetLabel(getString("calendar.month"),
          getString("calendar.year")));
      final LabelValueChoiceRenderer<Integer> monthChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
      for (int i = 0; i <= 11; i++) {
        monthChoiceRenderer.addValue(i, StringHelper.format2DigitNumber(i + 1));
      }
      final DropDownChoice<Integer> monthChoice = new DropDownChoice<Integer>(fs.getDropDownChoiceId(), new PropertyModel<Integer>(data,
          "month"), monthChoiceRenderer.getValues(), monthChoiceRenderer);
      monthChoice.setNullValid(false).setRequired(true);
      WicketUtils.setSize(monthChoice, 2);
      fs.add(monthChoice);
      final MinMaxNumberField<Integer> year = new MinMaxNumberField<Integer>(InputPanel.WICKET_ID,
          new PropertyModel<Integer>(data, "year"), 1900, 2999);
      WicketUtils.setSize(year, 4);
      fs.add(year);
    }
    {
      // DropDownChoice salary type
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeSalaryDO.class, "type");
      final LabelValueChoiceRenderer<EmployeeSalaryType> typeStatusChoiceRenderer = new LabelValueChoiceRenderer<EmployeeSalaryType>(fs,
          EmployeeSalaryType.values());
      final DropDownChoice<EmployeeSalaryType> typeChoice = new DropDownChoice<EmployeeSalaryType>(fs.getDropDownChoiceId(),
          new PropertyModel<EmployeeSalaryType>(data, "type"), typeStatusChoiceRenderer.getValues(), typeStatusChoiceRenderer);
      typeChoice.setNullValid(false).setRequired(true);
      fs.add(typeChoice);
    }
    {
      // DropDownChoice salary gross sum with employee part
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeSalaryDO.class, "bruttoMitAgAnteil");
      fs.add(new TextField<BigDecimal>(InputPanel.WICKET_ID, new PropertyModel<BigDecimal>(data, "bruttoMitAgAnteil")) {
        @SuppressWarnings({ "unchecked", "rawtypes"})
        @Override
        public IConverter getConverter(final Class type)
        {
          return new CurrencyConverter();
        }
      });
    }
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeSalaryDO.class, "comment");
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<String>(data, "comment")), true);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
