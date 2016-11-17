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

package org.projectforge.business.orga;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.TimePeriod;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("VisitorbookFilter")
public class VisitorbookFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = 8567780910637887786L;

  private TimePeriod timePeriod;

  private boolean showOnlyActiveEntries;

  public VisitorbookFilter()
  {
  }

  public VisitorbookFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  public boolean isShowOnlyActiveEntries()
  {
    return showOnlyActiveEntries;
  }

  public void setShowOnlyActiveEntries(boolean showOnlyActiveEntries)
  {
    this.showOnlyActiveEntries = showOnlyActiveEntries;
  }

  /**
   * @return the startTime
   */
  public Date getStartTime()
  {
    return getTimePeriod().getFromDate();
  }

  /**
   * @param startTime the startTime to set
   */
  public void setStartTime(final Date startTime)
  {
    Date recalculatedDate = null;
    if (startTime != null) {
      SimpleDateFormat sdfParser = new SimpleDateFormat("yyyy-MM-dd");
      SimpleDateFormat sdfUserTimeZone = new SimpleDateFormat("yyyy-MM-dd");
      sdfUserTimeZone.setTimeZone(ThreadLocalUserContext.getTimeZone());
      String startDateUserTimeZone = sdfUserTimeZone.format(startTime);
      try {
        recalculatedDate = sdfParser.parse(startDateUserTimeZone);
      } catch (ParseException e) {
        recalculatedDate = startTime;
      }
    }
    getTimePeriod().setFromDate(recalculatedDate);
  }

  /**
   * @return the stopTime
   */
  public Date getStopTime()
  {
    return getTimePeriod().getToDate();
  }

  /**
   * @param stopTime the stopTime to set
   */
  public void setStopTime(final Date stopTime)
  {
    Date recalculatedDate = null;
    if (stopTime != null) {
      SimpleDateFormat sdfParser = new SimpleDateFormat("yyyy-MM-dd");
      SimpleDateFormat sdfUserTimeZone = new SimpleDateFormat("yyyy-MM-dd");
      sdfUserTimeZone.setTimeZone(ThreadLocalUserContext.getTimeZone());
      String stopDateUserTimeZone = sdfUserTimeZone.format(stopTime);
      try {
        recalculatedDate = sdfParser.parse(stopDateUserTimeZone);
      } catch (ParseException e) {
        recalculatedDate = stopTime;
      }
    }
    getTimePeriod().setToDate(recalculatedDate);
  }

  /**
   * Gets start and stop time from timePeriod.
   *
   * @param timePeriod
   */
  public void setTimePeriod(final TimePeriod timePeriod)
  {
    setStartTime(timePeriod.getFromDate());
    setStopTime(timePeriod.getToDate());
  }

  private TimePeriod getTimePeriod()
  {
    if (timePeriod == null) {
      timePeriod = new TimePeriod();
    }
    return timePeriod;
  }
}
