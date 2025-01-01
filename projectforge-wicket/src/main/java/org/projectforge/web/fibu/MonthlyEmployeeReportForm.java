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

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDayUtils;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.calendar.QuickSelectMonthPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import java.time.LocalDate;
import java.util.ArrayList;

public class MonthlyEmployeeReportForm
    extends AbstractStandardForm<MonthlyEmployeeReportFilter, MonthlyEmployeeReportPage>
{
  private static final long serialVersionUID = 8746545908106124484L;

  protected MonthlyEmployeeReportFilter filter;

  private DropDownChoice<Integer> yearChoice, monthChoice;

  public MonthlyEmployeeReportForm(final MonthlyEmployeeReportPage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("timesheet.user"));
      if (WicketSupport.getAccessChecker().hasLoggedInUserAccessToTimesheetsOfOtherUsers()) {
        final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(),
            new PropertyModel<PFUserDO>(filter, "user"),
            parentPage, "user");
        userSelectPanel.setRequired(true);
        fs.add(userSelectPanel);
        userSelectPanel.init();
      } else {
        filter.setUser(ThreadLocalUserContext.getLoggedInUser());
        fs.add(new DivTextPanel(fs.newChildId(), filter.getUser().getFullname()));
      }
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("calendar.month"));
      yearChoice = new DropDownChoice<>(fs.getDropDownChoiceId(), new PropertyModel<>(filter, "year"),
          new ArrayList<Integer>());
      yearChoice.add(new FormComponentUpdatingBehavior());
      yearChoice.setNullValid(false).setRequired(true);
      fs.add(yearChoice);
      // DropDownChoice months
      final LabelValueChoiceRenderer<Integer> monthChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
      for (int month = 1; month <= 12; month++) {
        monthChoiceRenderer.addValue(month, StringHelper.format2DigitNumber(month));
      }
      monthChoice = new DropDownChoice<>(fs.getDropDownChoiceId(), new PropertyModel<>(filter, "month"),
          monthChoiceRenderer.getValues(), monthChoiceRenderer);
      monthChoice.add(new FormComponentUpdatingBehavior());
      monthChoice.setNullValid(false).setRequired(true);
      fs.add(monthChoice);
      final QuickSelectMonthPanel quickSelectPanel = new QuickSelectMonthPanel(fs.newChildId(), new Model<LocalDate>()
      {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public LocalDate getObject()
        {
          Integer year = filter.getYear();
          Integer month = filter.getMonth();
          PFDateTime date;
          if (year == null || month == null) {
            date = PFDateTime.now().getBeginOfMonth();
          } else {
            date = PFDateTime.withDate(filter.getYear(), PFDayUtils.validateMonthValue(filter.getMonth()), 1);
          }
          return date.getLocalDate();
        }

        /**
         * @see org.apache.wicket.model.Model#setObject(java.io.Serializable)
         */
        @Override
        public void setObject(final LocalDate object)
        {
          if (object != null) {
            setDate(object);
          }
        }
      }, parentPage, "quickSelect");
      fs.add(quickSelectPanel);
      quickSelectPanel.init();
    }
    {
      final Button showButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("show"));
      final SingleButtonPanel showButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), showButton,
          getString("show"),
          SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(showButtonPanel);
      setDefaultButton(showButton);
    }
  }

  void setDate(final LocalDate date)
  {
    PFDateTime dt = PFDateTime.fromOrNow(date); // not null
    filter.setYear(dt.getYear());
    filter.setMonth(dt.getMonthValue());
    yearChoice.modelChanged();
    monthChoice.modelChanged();
  }

  @Override
  public void onBeforeRender()
  {
    refreshYearList();
    super.onBeforeRender();
  }

  private void refreshYearList()
  {
    final int[] years;
    if (filter.getUser() == null) {
      years = new int[] { new DateHolder().getYear() };
    } else {
      years = WicketSupport.get(TimesheetDao.class).getYears(filter.getUser().getId());
    }
    final LabelValueChoiceRenderer<Integer> yearChoiceRenderer = new LabelValueChoiceRenderer<Integer>();
    for (final int year : years) {
      yearChoiceRenderer.addValue(year, String.valueOf(year));
    }
    yearChoice.setChoiceRenderer(yearChoiceRenderer);
    yearChoice.setChoices(yearChoiceRenderer.getValues());
  }
}
