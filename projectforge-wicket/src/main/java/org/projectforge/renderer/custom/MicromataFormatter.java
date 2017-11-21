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

package org.projectforge.renderer.custom;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.KostCache;
import org.projectforge.business.scripting.NullObject;
import org.projectforge.business.task.formatter.WicketTaskFormatter;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.business.utils.HtmlDateTimeFormatter;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.renderer.CellHolder;
import org.projectforge.framework.renderer.RenderType;
import org.projectforge.framework.renderer.RowHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Sebastian Hardt (s.hardt@micromata.de)
 */
@Service
public class MicromataFormatter extends Formatter
{

  @Autowired
  private UserFormatter userFormatter;

  @Autowired
  private HtmlDateTimeFormatter htmlDateTimeFormatter;

  @Autowired
  private KostCache kostCache;

  @Override
  public Map<String, Object> getData(final List<TimesheetDO> timeSheets, final Integer taskId,
      final HttpServletRequest request,
      final HttpServletResponse response, final TimesheetFilter actionFilter)
  {

    final Map<String, Object> data = new HashMap<String, Object>();

    long durationSum = 0;

    for (final TimesheetDO timesheet : timeSheets) {
      durationSum += timesheet.getDuration();
    }

    final List<RowHolder> list = new ArrayList<RowHolder>();
    for (final TimesheetDO timesheet : timeSheets) {
      final RowHolder row = new RowHolder();
      if (actionFilter.getUserId() != null) {
        final Kost2DO kost2 = kostCache.getKost2(timesheet.getKost2Id());
        if (kost2 != null) {
          row.addCell(new CellHolder(KostFormatter.format(kost2)));
        } else {
          row.addCell(new CellHolder(""));
        }
      } else {
        row.addCell(new CellHolder(userFormatter.getFormattedUser(timesheet.getUser())));
      }
      final String taskPath = WicketTaskFormatter.getTaskPath(timesheet.getTaskId(), taskId, true, OutputType.PLAIN);
      row.addCell(new CellHolder(HtmlHelper.formatXSLFOText(taskPath, true)));
      row.addCell(
          new CellHolder(
              htmlDateTimeFormatter.getFormattedTimePeriod(timesheet.getTimePeriod(), RenderType.FOP, true)));
      row.addCell(new CellHolder(htmlDateTimeFormatter.getFormattedDuration(timesheet.getTimePeriod())));
      row.addCell(new CellHolder(HtmlHelper.formatXSLFOText(timesheet.getDescription(), true)));
      if (StringUtils.isNotBlank(timesheet.getLocation()) == true) {
        row.addCell(new CellHolder(HtmlHelper.formatXSLFOText(timesheet.getLocation(), true)));
      } else {
        row.addCell(new CellHolder(""));
      }
      list.add(row);
    }

    data.put("list", list);
    data.put("title", getLocalizedString("timesheet.title.list"));
    data.put("systemDate", htmlDateTimeFormatter.getFormattedDateTime(new Date()));
    data.put("searchStringLabel", getLocalizedString("searchString"));

    if (StringUtils.isNotEmpty(actionFilter.getSearchString()) == true) {
      data.put("searchString", HtmlHelper.formatXSLFOText(actionFilter.getSearchString(), true));
    } else {
      data.put("searchString", NullObject.instance);
    }

    data.put("timePeriodLabel", getLocalizedString("timePeriod"));

    data.put("startTime", htmlDateTimeFormatter.getFormattedDate(actionFilter.getStartTime()));
    data.put("stopTime", htmlDateTimeFormatter.getFormattedDate(actionFilter.getStopTime()));
    data.put("taskLabel", getLocalizedString("task"));

    if (taskId != null) {
      data.put("task", WicketTaskFormatter.getTaskPath(taskId, true, OutputType.PLAIN));
    } else {
      data.put("task", NullObject.instance);
    }
    data.put("userLabel", getLocalizedString("timesheet.user"));

    if (actionFilter.getUserId() != null) {
      data.put("user", userFormatter.getFormattedUser(actionFilter.getUserId()));
    } else {
      data.put("user", NullObject.instance);
    }
    data.put("totalDurationLabel", getLocalizedString("timesheet.totalDuration"));

    final String str1 = htmlDateTimeFormatter.getFormattedDuration(durationSum);
    final String str2 = htmlDateTimeFormatter.getFormattedDuration(durationSum,
        htmlDateTimeFormatter.getDurationOfWorkingDay(),
        -1);
    data.put("totalDuration", str1);
    if (str1.equals(str2) == false) {
      data.put("totalHours", str2);
    } else {
      data.put("totalHours", NullObject.instance);
    }

    data.put("optionsLabel", getLocalizedString("label.options"));
    data.put("deletedLabel", getLocalizedString("deleted"));

    data.put("deleted", actionFilter.isDeleted());

    data.put("durationLabel", getLocalizedString("timesheet.duration"));
    data.put("descriptionLabel", getLocalizedString("description"));
    data.put("locationLabel", getLocalizedString("timesheet.location"));

    return data;
  }

  public HtmlDateTimeFormatter getHtmlDateTimeFormatter()
  {
    return htmlDateTimeFormatter;
  }

  public void setHtmlDateTimeFormatter(HtmlDateTimeFormatter htmlDateTimeFormatter)
  {
    this.htmlDateTimeFormatter = htmlDateTimeFormatter;
  }

  public UserFormatter getUserFormatter()
  {
    return userFormatter;
  }

  public void setUserFormatter(UserFormatter userFormatter)
  {
    this.userFormatter = userFormatter;
  }

}
