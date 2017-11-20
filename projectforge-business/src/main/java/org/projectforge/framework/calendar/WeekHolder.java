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

package org.projectforge.framework.calendar;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.DayHolder;


/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class WeekHolder implements Serializable
{
  private static final long serialVersionUID = -3895513078248004222L;

  private DayHolder[] days;

  private int weekOfYear = -1;

  private Map<String, Object> objects;

  /** Initializes month containing all days of actual month. */
  public WeekHolder(Locale locale)
  {
    this(Calendar.getInstance(locale));
  }

  /**
   * 
   * @param cal
   */
  public WeekHolder(Calendar cal)
  {
    this(cal, -1);
  }

  /**
   * Builds the week for the given date. Every day will be marked, if it is not part of the given month.
   * @param cal
   * @param month
   */
  public WeekHolder(Calendar cal, int month)
  {
    DateHolder dateHolder = new DateHolder(cal, DatePrecision.DAY);
    weekOfYear = DateHelper.getWeekOfYear(cal);
    dateHolder.setBeginOfWeek();
    dateHolder.computeTime();
    days = new DayHolder[7];
    // Process week
    for (int i = 0; i < 7; i++) {
      DayHolder day = new DayHolder(dateHolder);
      if (day.getMonth() != month) {
        // Mark this day as day from the previous or next month:
        day.setMarker(true);
      }
      days[i] = day;
      dateHolder.add(Calendar.DAY_OF_YEAR, 1);
    }
  }

  public int getWeekOfYear()
  {
    return weekOfYear;
  }

  public DayHolder[] getDays()
  {
    return days;
  }
  
  public DayHolder getFirstDay()
  {
    return days[0];
  }

  public DayHolder getLastDay()
  {
    return days[days.length - 1];
  }

  /**
   * For storing additional objects to a week. This is used by the date selector for showing the user's total working time.
   * @param obj
   */
  public void addObject(String key, Object value) {
    if (this.objects == null) {
      this.objects = new HashMap<String, Object>();
    }
    this.objects.put(key, value);
  }
  
  /**
   * Used for getting e. g. the user total working time at this week.
   * @return the stored objects to this day or null, if not exist.
   */
  public Object getObject(String key) {
    if (this.objects == null) {
      return null;
    }
    return this.objects.get(key);
  }
  
  public Map<String, Object> getObjects() {
    return this.objects;
  }
  
  public String toString()
  {
    ToStringBuilder tos = new ToStringBuilder(this);
    tos.append("days", getDays());
    return tos.toString();
  }
}
