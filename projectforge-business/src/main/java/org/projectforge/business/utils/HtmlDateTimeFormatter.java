/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.utils;

import java.util.Date;

import org.projectforge.framework.renderer.RenderType;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.time.TimePeriod;
import org.springframework.stereotype.Service;

@Service
public class HtmlDateTimeFormatter extends DateTimeFormatter
{
  /**
   * 
   * @param timePeriod
   * @param renderType Default is HTML
   * @param multiLines If true, &gt;br/&lt; tags will be used for a multi line output.
   * @return
   */
  public String getFormattedTimePeriod(final TimePeriod timePeriod, final RenderType renderType,
      final boolean multiLines)
  {
    if (timePeriod == null) {
      return "";
    }
    final StringBuffer buf = new StringBuffer();
    if (timePeriod.getMarker() == true) {
      // Time collision is marked!
      buf.append("<span");
      if (renderType == RenderType.FOP) {
        HtmlHelper.attribute(buf, "use-font", "bold");
      } else {
        HtmlHelper.attribute(buf, "class", "tp_collision");
      }
      buf.append(">").append("***");
    }
    if (timePeriod.getFromDate() != null) {
      appendCSSDate(buf, timePeriod.getFromDate(), renderType);
      if (timePeriod.getMarker() == true) {
        buf.append("***</span>");
      }
      buf.append(" ");
      appendCSSTime(buf, timePeriod.getFromDate(), renderType);
      if (timePeriod.getToDate() != null) {
        buf.append("-");
        appendCSSTime(buf, timePeriod.getToDate(), renderType);
        final DayHolder day = new DayHolder(timePeriod.getFromDate());
        if (day.isSameDay(timePeriod.getToDate()) == false) {
          if (multiLines == true) {
            buf.append("<br/>(");
          } else {
            buf.append(" (");
          }
          buf.append(getFormattedDate(timePeriod.getToDate())).append(")");
        }
      }
    } else {
      if (timePeriod.getToDate() != null) {
        buf.append(getFormattedDateTime(timePeriod.getFromDate()));
      }
      if (timePeriod.getMarker() == true) {
        buf.append("***</span>");
      }
    }
    return buf.toString();
  }

  private void appendCSSDate(final StringBuffer buf, final Date date, final RenderType renderType)
  {
    buf.append("<span");
    if (renderType == RenderType.FOP) {
      HtmlHelper.attribute(buf, "use-font", "bold");
    } else {
      HtmlHelper.attribute(buf, "class", "tp_date");
    }
    buf.append(">");
    buf.append(getFormattedDate(date)).append("</span>");
  }

  private void appendCSSTime(final StringBuffer buf, final Date date, final RenderType renderType)
  {
    if (renderType == RenderType.FOP) {
      buf.append(getFormattedTime(date));
    } else {
      buf.append("<span");
      HtmlHelper.attribute(buf, "class", "tp_time");
      buf.append(">").append(getFormattedTime(date)).append("</span>");
    }
  }

  public String getFormattedTimePeriod(final TimePeriod timePeriod)
  {
    return getFormattedTimePeriod(timePeriod, RenderType.HTML, true);
  }

}
