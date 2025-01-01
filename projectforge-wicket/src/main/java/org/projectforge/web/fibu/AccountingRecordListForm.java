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

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.kost.BuchungssatzDao;
import org.projectforge.business.fibu.kost.BusinessAssessment;
import org.projectforge.common.StringHelper;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.YearListCoiceRenderer;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;

public class AccountingRecordListForm extends AbstractListForm<AccountingRecordListFilter, AccountingRecordListPage> {
  private static final long serialVersionUID = -1669760774183582053L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccountingRecordListForm.class);

  @SuppressWarnings("serial")
  @Override
  protected void init() {
    searchFilter.setMaxRows(1000);
    super.init();
    if (isFilterVisible() == false) {
      return;
    }
    {
      // Statistics
      gridBuilder.newGridPanel();
      new BusinessAssessment4Fieldset(gridBuilder) {
        /**
         * @see org.projectforge.web.fibu.BusinessAssessment4Fieldset#getBusinessAssessment()
         */
        @Override
        protected BusinessAssessment getBusinessAssessment() {
          return parentPage.getBusinessAssessment();
        }
      };
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   * org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel) {
    // DropDownChoices from
    final YearListCoiceRenderer yearListChoiceRenderer = new YearListCoiceRenderer(WicketSupport.get(BuchungssatzDao.class).getYears(), false);
    final DropDownChoice<Integer> fromYearChoice = new DropDownChoice<Integer>(
            optionsFieldsetPanel.getDropDownChoiceId(),
            new PropertyModel<Integer>(this, "fromYear"), yearListChoiceRenderer.getYears(), yearListChoiceRenderer);
    fromYearChoice.setNullValid(false).setRequired(true);
    optionsFieldsetPanel.add(fromYearChoice);
    final LabelValueChoiceRenderer<Integer> monthChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
    for (int i = 1; i <= 12; i++) {
      monthChoiceRenderer.addValue(i, StringHelper.format2DigitNumber(i));
    }
    final DropDownChoice<Integer> fromMonthChoice = new DropDownChoice<Integer>(
            optionsFieldsetPanel.getDropDownChoiceId(),
            new PropertyModel<Integer>(this, "fromMonth"), monthChoiceRenderer.getValues(), monthChoiceRenderer);
    fromMonthChoice.setNullValid(true);
    optionsFieldsetPanel.add(fromMonthChoice);

    optionsFieldsetPanel.add(new DivTextPanel(optionsFieldsetPanel.newChildId(), " - "));

    // DropDownChoices to
    final DropDownChoice<Integer> toYearChoice = new DropDownChoice<Integer>(optionsFieldsetPanel.getDropDownChoiceId(),
            new PropertyModel<Integer>(this, "toYear"), yearListChoiceRenderer.getYears(), yearListChoiceRenderer);
    toYearChoice.setNullValid(false).setRequired(true);
    optionsFieldsetPanel.add(toYearChoice);

    final DropDownChoice<Integer> toMonthChoice = new DropDownChoice<Integer>(
            optionsFieldsetPanel.getDropDownChoiceId(),
            new PropertyModel<Integer>(this, "toMonth"), monthChoiceRenderer.getValues(), monthChoiceRenderer);
    toMonthChoice.setNullValid(true);
    optionsFieldsetPanel.add(toMonthChoice);
  }

  /**
   * The filter is not visible if only a fixed list of accounting records of a record is displayed.
   *
   * @see org.projectforge.web.wicket.AbstractListForm#isFilterVisible()
   */
  @Override
  protected boolean isFilterVisible() {
    return (parentPage.reportId == null);
  }

  protected void refresh() {
  }

  public Integer getFromYear() {
    return getSearchFilter().getFromYear();
  }

  public void setFromYear(final Integer year) {
    getSearchFilter().setFromYear(year);
  }

  public Integer getToYear() {
    return getSearchFilter().getToYear();
  }

  public void setToYear(final Integer year) {
    getSearchFilter().setToYear(year);
  }

  public Integer getFromMonth() {
    return getSearchFilter().getFromMonth();
  }

  public void setFromMonth(final Integer month) {
    getSearchFilter().setFromMonth(month);
  }

  public Integer getToMonth() {
    return getSearchFilter().getToMonth();
  }

  public void setToMonth(final Integer month) {
    getSearchFilter().setToMonth(month);
  }

  public AccountingRecordListForm(final AccountingRecordListPage parentPage) {
    super(parentPage);
  }

  @Override
  protected AccountingRecordListFilter newSearchFilterInstance() {
    return new AccountingRecordListFilter();
  }

  @Override
  protected Logger getLogger() {
    return log;
  }
}
