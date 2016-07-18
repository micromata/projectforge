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
 * This panel show the buttons for selecting current, previous and following week inside an existing form. Calls caller.select("selectWeek",
 * offset): offset -1 for previous week, 0 for current week and +1 for following week.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class QuickSelectWeekPanel extends AbstractSelectPanel<Date>
{
  private static final long serialVersionUID = -3173096216643497466L;

  private Date beginOfWeek;

  private final String selectProperty;

  /**
   * @param id
   * @param model Should contain begin of week as object.
   * @param caller
   * @param selectProperty
   */
  public QuickSelectWeekPanel(final String id, final IModel<Date> model, final ISelectCallerPage caller, final String selectProperty)
  {
    super(id, model, caller, selectProperty);
    this.beginOfWeek = model.getObject();
    this.selectProperty = selectProperty;
  }

  @Override
  @SuppressWarnings("serial")
  public QuickSelectWeekPanel init()
  {
    super.init();
    {
      final SubmitLink previousButton = new SubmitLink("previousWeek") {
        @Override
        public void onSubmit()
        {
          final DateHolder dateHolder = new DateHolder(getModelObject());
          dateHolder.add(Calendar.WEEK_OF_YEAR, -1);
          dateHolder.setBeginOfWeek();
          beginOfWeek = dateHolder.getDate();
          caller.select(selectProperty, beginOfWeek);
        };
      };
      previousButton.setDefaultFormProcessing(false);
      add(previousButton);
      previousButton.add(new TooltipImage("previousWeekImage", WebConstants.IMAGE_QUICKSELECT_PREVIOUS_WEEK,
          getString("calendar.quickselect.tooltip.selectPreviousWeek")));
    }
    {
      final SubmitLink currentWeekButton = new SubmitLink("currentWeek") {
        @Override
        public void onSubmit()
        {
          final DateHolder dateHolder = new DateHolder();
          dateHolder.setBeginOfWeek();
          beginOfWeek = dateHolder.getDate();
          caller.select(selectProperty, beginOfWeek);
        };
      };
      currentWeekButton.setDefaultFormProcessing(false);
      add(currentWeekButton);
      currentWeekButton.add(new TooltipImage("currentWeekImage", WebConstants.IMAGE_QUICKSELECT_CURRENT_WEEK,
          getString("calendar.quickselect.tooltip.selectCurrentWeek")));
    }
    {
      final SubmitLink followingWeekButton = new SubmitLink("followingWeek") {
        @Override
        public void onSubmit()
        {
          final DateHolder dateHolder = new DateHolder(getModelObject());
          dateHolder.add(Calendar.WEEK_OF_YEAR, +1);
          beginOfWeek = dateHolder.getDate();
          caller.select(selectProperty, beginOfWeek);
        };
      };
      followingWeekButton.setDefaultFormProcessing(false);
      add(followingWeekButton);
      followingWeekButton.add(new TooltipImage("followingWeekImage", WebConstants.IMAGE_QUICKSELECT_FOLLOWING_WEEK,
          getString("calendar.quickselect.tooltip.selectNextWeek")));
    }
    return this;
  }
}
