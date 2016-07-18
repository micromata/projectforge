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

package org.projectforge.web.calendar;

import java.util.Calendar;
import java.util.Date;

import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.components.TooltipImage;

/**
 * This panel show the buttons for selecting current, previous and following month inside an existing form. Calls
 * caller.select("selectMonth", offset): offset -1 for previous month, 0 for current month and +1 for following month.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class QuickSelectMonthPanel extends AbstractSelectPanel<Date>
{
  private static final long serialVersionUID = -3173096216643497466L;

  private Date beginOfMonth;

  private final String selectProperty;

  /**
   * @param id
   * @param model Should contain begin of month as object.
   * @param caller
   * @param selectProperty
   */
  public QuickSelectMonthPanel(final String id, final IModel<Date> model, final ISelectCallerPage caller, final String selectProperty)
  {
    super(id, model, caller, selectProperty);
    this.beginOfMonth = model.getObject();
    this.selectProperty = selectProperty;
  }

  @Override
  @SuppressWarnings("serial")
  public QuickSelectMonthPanel init()
  {
    super.init();
    {
      final SubmitLink previousButton = new SubmitLink("previousMonth") {
        @Override
        public void onSubmit()
        {
          final DateHolder dateHolder = new DateHolder(getModelObject());
          dateHolder.add(Calendar.MONTH, -1);
          dateHolder.setBeginOfMonth();
          beginOfMonth = dateHolder.getDate();
          caller.select(selectProperty, beginOfMonth);
        };
      };
      previousButton.setDefaultFormProcessing(false);
      add(previousButton);
      previousButton.add(new TooltipImage("previousMonthImage", WebConstants.IMAGE_QUICKSELECT_PREVIOUS_MONTH,
          getString("calendar.quickselect.tooltip.selectPreviousMonth")));
    }
    {
      final SubmitLink currentMonthButton = new SubmitLink("currentMonth") {
        @Override
        public void onSubmit()
        {
          final DateHolder dateHolder = new DateHolder();
          dateHolder.setBeginOfMonth();
          beginOfMonth = dateHolder.getDate();
          caller.select(selectProperty, beginOfMonth);
        };
      };
      currentMonthButton.setDefaultFormProcessing(false);
      add(currentMonthButton);
      currentMonthButton.add(new TooltipImage("currentMonthImage", WebConstants.IMAGE_QUICKSELECT_CURRENT_MONTH,
          getString("calendar.quickselect.tooltip.selectCurrentMonth")));
    }
    {
      final SubmitLink followingMonthButton = new SubmitLink("followingMonth") {
        @Override
        public void onSubmit()
        {
          final DateHolder dateHolder = new DateHolder(getModelObject());
          dateHolder.add(Calendar.MONTH, +1);
          beginOfMonth = dateHolder.getDate();
          caller.select(selectProperty, beginOfMonth);
        };
      };
      followingMonthButton.setDefaultFormProcessing(false);
      add(followingMonthButton);
      followingMonthButton.add(new TooltipImage("followingMonthImage", WebConstants.IMAGE_QUICKSELECT_FOLLOWING_MONTH,
          getString("calendar.quickselect.tooltip.selectNextMonth")));
    }
    return this;
  }
}
