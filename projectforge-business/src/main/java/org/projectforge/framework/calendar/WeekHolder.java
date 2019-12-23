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

package org.projectforge.framework.calendar;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.projectforge.framework.time.PFDate;

import java.io.Serializable;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class WeekHolder implements Serializable {
  private static final long serialVersionUID = -3895513078248004222L;

  private PFDate[] days;

  private int weekOfYear;

  private Map<String, Object> objects;

  public WeekHolder(PFDate date) {
    final Month month = date.getMonth();
    weekOfYear = date.getWeekOfYear();
    days = new PFDate[7];
    PFDate day = date.getBeginOfWeek();
    // Process week
    for (int i = 0; i < 7; i++) {
      if (day.getMonth() != month) {
        // TODO: Mark this day as day from the previous or next month:
        //day.setMarker(true);
      }
      days[i] = day;
      day.plusDays(1);
    }
  }


  public int getWeekOfYear() {
    return weekOfYear;
  }

  public PFDate[] getDays() {
    return days;
  }

  public PFDate getFirstDayDate() {
    return days[0];
  }

  public PFDate getLastDayDate() {
    return days[days.length - 1];
  }

  /**
   * For storing additional objects to a week. This is used by the date selector for showing the user's total working time.
   */
  public void addObject(String key, Object value) {
    if (this.objects == null) {
      this.objects = new HashMap<>();
    }
    this.objects.put(key, value);
  }

  /**
   * Used for getting e. g. the user total working time at this week.
   *
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

  public String toString() {
    ToStringBuilder tos = new ToStringBuilder(this);
    tos.append("days", getDays());
    return tos.toString();
  }
}
