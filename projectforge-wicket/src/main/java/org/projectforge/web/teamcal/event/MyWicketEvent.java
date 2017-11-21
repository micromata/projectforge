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

package org.projectforge.web.teamcal.event;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.WebConfiguration;

import net.ftlines.wicket.fullcalendar.Event;

public class MyWicketEvent extends Event
{
  private static final long serialVersionUID = 4820590313641751522L;

  private String tooltipTitle;

  private String tooltipContent;

  private transient String duration;

  /**
   * @return the tooltipTitle
   */
  public String getTooltipTitle()
  {
    return tooltipTitle;
  }

  /**
   * @param tooltipTitle the tooltipTitle to set
   * @return this for chaining.
   */
  public MyWicketEvent setTooltipTitle(final String tooltipTitle)
  {
    this.tooltipTitle = tooltipTitle;
    return this;
  }

  /**
   * @return the tooltipContent
   */
  public String getTooltipContent()
  {
    return tooltipContent;
  }

  /**
   * @param tooltipContent the tooltipContent to set
   * @return this for chaining.
   */
  public MyWicketEvent setTooltipContent(final String tooltipContent)
  {
    this.tooltipContent = tooltipContent;
    return this;
  }

  /**
   * @param title
   * @param labelValues {{"text without label"}{"value", "label"}, ...}
   * @return
   */
  public MyWicketEvent setTooltip(final String title, final String[][] labelValues)
  {
    this.tooltipTitle = title;
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final String[] lv : labelValues) {
      if (lv == null || lv.length < 1 || lv.length > 2) {
        if (WebConfiguration.isDevelopmentMode() == true) {
          throw new IllegalArgumentException("labelValues must be string arrays of length 1 or 2!");
        }
        continue;
      }
      String value = lv[0];
      if (StringUtils.isBlank(value) == true) {
        continue;
      }
      value = StringUtils.abbreviate(value, 80);
      final String label = lv.length == 2 ? lv[1] : null;
      if (first == true) {
        first = false;
      } else {
        buf.append("\n");
      }
      if (label != null) {
        buf.append(label).append(": ").append(value);
      } else {
        buf.append(value);
      }
    }
    if (first == false) {
      buf.append("\n");
    }
    buf.append(ThreadLocalUserContext.getLocalizedString("timesheet.duration")).append(": ").append(getDuration());
    this.tooltipContent = HtmlHelper.escapeHtml(buf.toString(), true);
    return this;
  }

  public String getDuration()
  {
    if (duration != null) {
      return duration;
    }

    final StringBuilder buffer = new StringBuilder();
    final Duration dur = new Duration(this.getStart(), this.getEnd());
    long days = dur.getStandardDays();

    if (isAllDay()) {
      ++days;
    }

    if (days > 0) { // days
      buffer
          .append(days)
          .append(ThreadLocalUserContext.getLocalizedString("calendar.unit.day"))
          .append(" ");
    }

    if (isAllDay() == false) {
      final Period period = new Period(this.getStart(), this.getEnd());
      final int hours = period.getHours();
      final int minutes = period.getMinutes();
      buffer.append(hours).append(":"); // hours
      if (minutes < 10) {
        buffer.append("0");
      }
      buffer.append(minutes);
    }

    duration = buffer.toString();
    return duration;
  }
}
