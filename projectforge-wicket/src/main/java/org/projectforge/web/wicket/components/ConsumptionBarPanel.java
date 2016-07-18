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

package org.projectforge.web.wicket.components;

import java.math.BigDecimal;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.timesheet.TimesheetListPage;


/**
 * Shows a div layer with a colored percentage bar.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ConsumptionBarPanel extends Panel
{
  private static final long serialVersionUID = -4328646802035960450L;

  private final String tooltip;

  /**
   * @param id
   * @param usage
   * @param maxValue
   * @param taskId
   * @param taskNodeFinished Depending on the task node is finished or not, the colors are different: E. g. a 95% used bar is green for
   *          finished tasks, for unfinished ones not.
   * @param unit
   * @param linkEnabled If true then the user can click on this bar for getting all time sheets behind this bar.
   */
  public ConsumptionBarPanel(final String id, final BigDecimal usage, BigDecimal maxValue, final Integer taskId,
      final boolean taskNodeFinished, final String unit, final boolean linkEnabled)
  {
    super(id);
    if (NumberHelper.isNotZero(maxValue) == false) {
      maxValue = null;
    }
    @SuppressWarnings("serial")
    final Link< Void> showTimesheetsLink = new Link<Void>("sheets") {
      @Override
      public void onClick()
      {
        final PageParameters parameters = new PageParameters();
        parameters.add(TimesheetListPage.PARAMETER_KEY_CLEAR_ALL, true);
        parameters.add(TimesheetListPage.PARAMETER_KEY_STORE_FILTER, false);
        parameters.add(TimesheetListPage.PARAMETER_KEY_TASK_ID, taskId);
        final TimesheetListPage timesheetListPage = new TimesheetListPage(parameters);
        setResponsePage(timesheetListPage);
      }
    };
    showTimesheetsLink.setEnabled(linkEnabled);
    add(showTimesheetsLink);
    final WebMarkupContainer bar = new WebMarkupContainer("bar");
    final Label progressLabel = new Label("progress", new Model<String>(" "));
    final int percentage = maxValue != null ? usage.divide(maxValue, 2, BigDecimal.ROUND_HALF_UP).multiply(NumberHelper.HUNDRED).intValue()
        : 0;
    final int width = percentage <= 100 ? percentage : 10000 / percentage;
    bar.add(AttributeModifier.replace("class", "progress"));
    if (percentage <= 80 || (taskNodeFinished == true && percentage <= 100)) {
      if (percentage > 0) {
        bar.add(AttributeModifier.append("class", "progress-done"));
      } else {
        bar.add(AttributeModifier.append("class", "progress-none"));
        progressLabel.setVisible(false);
      }
    } else if (percentage <= 90) {
      bar.add(AttributeModifier.append("class", "progress-80"));
    } else if (percentage <= 100) {
      bar.add(AttributeModifier.append("class", "progress-90"));
    } else if (taskNodeFinished == true && percentage <= 110) {
      bar.add(AttributeModifier.append("class", "progress-overbooked-min"));
    } else {
      bar.add(AttributeModifier.append("class", "progress-overbooked"));
    }
    if (maxValue == null && (usage == null || usage.compareTo(BigDecimal.ZERO) == 0)) {
      bar.setVisible(false);
    }
    progressLabel.add(AttributeModifier.replace("style", "width: " + width + "%;"));
    final StringBuffer buf = new StringBuffer();
    buf.append(NumberHelper.getNumberFractionFormat(getLocale(), usage.scale()).format(usage));
    if (unit != null) {
      buf.append(unit);
    }
    if (maxValue != null) {
      buf.append("/");
      buf.append(NumberHelper.getNumberFractionFormat(getLocale(), maxValue.scale()).format(maxValue));
      buf.append(unit);
      buf.append(" (").append(percentage).append("%)");
    }
    tooltip = buf.toString();
    WicketUtils.addTooltip(bar, tooltip);
    showTimesheetsLink.add(bar);
    bar.add(progressLabel);
  }

  /**
   * @return the tooltip
   */
  public String getTooltip()
  {
    return tooltip;
  }
}
