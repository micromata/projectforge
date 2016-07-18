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

import org.apache.log4j.Logger;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeSalaryDao;
import org.projectforge.business.fibu.EmployeeSalaryFilter;
import org.projectforge.common.StringHelper;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.YearListCoiceRenderer;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class EmployeeSalaryListForm extends AbstractListForm<EmployeeSalaryFilter, EmployeeSalaryListPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EmployeeSalaryListForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  @SpringBean
  private EmployeeSalaryDao employeeSalaryDao;

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   *      org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    final YearListCoiceRenderer yearListChoiceRenderer = new YearListCoiceRenderer(employeeSalaryDao.getYears(), true);
    optionsFieldsetPanel.addDropDownChoice(new PropertyModel<Integer>(this, "year"), yearListChoiceRenderer.getYears(),
        yearListChoiceRenderer, true).setNullValid(false);
    // DropDownChoice months
    final LabelValueChoiceRenderer<Integer> monthChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
    for (int i = 0; i <= 11; i++) {
      monthChoiceRenderer.addValue(i, StringHelper.format2DigitNumber(i + 1));
    }
    optionsFieldsetPanel.addDropDownChoice(new PropertyModel<Integer>(this, "month"), monthChoiceRenderer.getValues(), monthChoiceRenderer,
        true).setNullValid(true);
  }

  public EmployeeSalaryListForm(final EmployeeSalaryListPage parentPage)
  {
    super(parentPage);
  }

  public Integer getYear()
  {
    return getSearchFilter().getYear();
  }

  public void setYear(final Integer year)
  {
    if (year == null) {
      getSearchFilter().setYear(-1);
    } else {
      getSearchFilter().setYear(year);
    }
  }

  public Integer getMonth()
  {
    return getSearchFilter().getMonth();
  }

  public void setMonth(final Integer month)
  {
    if (month == null) {
      getSearchFilter().setMonth(-1);
    } else {
      getSearchFilter().setMonth(month);
    }
  }

  @Override
  protected EmployeeSalaryFilter newSearchFilterInstance()
  {
    return new EmployeeSalaryFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
