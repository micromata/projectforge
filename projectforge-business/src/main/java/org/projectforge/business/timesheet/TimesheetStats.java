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

package org.projectforge.business.timesheet;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.projectforge.framework.time.TimePeriod;

/**
 * Stores some statistics of time sheets.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TimesheetStats
{
  private final TimePeriod period;

  private Date earliestStartDate;

  private Date latestStopDate;

  private Long totalBreakMillis, totalMillis;

  private Set<TimesheetDO> timesheets;

  public TimesheetStats(final Date fromDate, final Date toDate)
  {
    this.period = new TimePeriod(fromDate, toDate);
  }

  /**
   * @return the time period of this stats.
   */
  public TimePeriod getPeriod()
  {
    return period;
  }

  /**
   * @return the earliestStartDate if not before fromDate, otherwise fromDate itself is returned. If no matching time sheet found, null is
   *         returned.
   */
  public Date getEarliestStartDate()
  {
    if (earliestStartDate == null) {
      return null;
    }
    if (earliestStartDate.before(this.period.getFromDate()) == true) {
      return this.period.getFromDate();
    }
    return earliestStartDate;
  }

  /**
   * @return the earliestStartDate.
   */
  public Date getEarliestStartDateIgnoringPeriod()
  {
    return earliestStartDate;
  }

  /**
   * @param earliestStartDate the earliestStartDate to set
   * @return this for chaining.
   */
  public TimesheetStats setEarliestStartDate(final Date earliestStartDate)
  {
    this.earliestStartDate = earliestStartDate;
    return this;
  }

  /**
   * @return the latestStopDate
   */
  public Date getLatestStopDate()
  {
    if (latestStopDate == null) {
      return null;
    }
    if (latestStopDate.after(this.period.getToDate()) == true) {
      return this.period.getToDate();
    }
    return latestStopDate;
  }

  /**
   * @return the latestStopDate
   */
  public Date getLatestStopDateIgnoringPeriod()
  {
    return latestStopDate;
  }

  /**
   * @param latestStopDate the latestStopDate to set
   * @return this for chaining.
   */
  public TimesheetStats setLatestStopDate(final Date latestStopDate)
  {
    this.latestStopDate = latestStopDate;
    return this;
  }

  /**
   * @return the totalBreakHours
   */
  public long getTotalBreakMillis()
  {
    if (totalBreakMillis == null) {
      calculateMillis();
    }
    return totalBreakMillis;
  }

  /**
   * @return the totalHours of time sheets inside given period.
   */
  public long getTotalMillis()
  {
    if (totalMillis == null) {
      calculateMillis();
    }
    return totalMillis;
  }

  /**
   * @return the timesheets
   */
  public Collection<TimesheetDO> getTimesheets()
  {
    return timesheets;
  }

  /**
   * Adds the given time sheet only if the time sheet fits the period (full or partly).
   * @param timesheet the timesheet to add
   * @return this for chaining.
   */
  public TimesheetStats add(final TimesheetDO timesheet)
  {
    final Date startTime = timesheet.getStartTime();
    final Date stopTime = timesheet.getStopTime();
    if (startTime == null || stopTime == null) {
      return this;
    }
    if (period.getFromDate().before(stopTime) == false || period.getToDate().after(startTime) == false) {
      return this;
    }
    if (earliestStartDate == null || earliestStartDate.after(startTime)) {
      this.earliestStartDate = startTime;
    }
    if (latestStopDate == null || latestStopDate.before(stopTime)) {
      this.latestStopDate = stopTime;
    }
    if (this.timesheets == null) {
      this.timesheets = new TreeSet<TimesheetDO>();
    }
    this.timesheets.add(timesheet);
    totalBreakMillis = totalMillis = null; // Set as dirty.
    return this;
  }

  private void calculateMillis()
  {
    this.totalBreakMillis = 0L;
    this.totalMillis = 0L;
    if (timesheets == null || timesheets.size() == 0) {
      return;
    }
    Date lastStopTime = null;
    for (final TimesheetDO timesheet : timesheets) {
      if (lastStopTime != null) {
        if (lastStopTime.before(timesheet.getStartTime()) == true) {
          this.totalBreakMillis += timesheet.getStartTime().getTime() - lastStopTime.getTime();
        }
      }
      lastStopTime = timesheet.getStopTime();
      Date startTime = timesheet.getStartTime();
      if (startTime.before(period.getFromDate()) == true) {
        startTime = period.getFromDate();
      }
      Date stopTime = timesheet.getStopTime();
      if (stopTime.after(period.getToDate()) == true) {
        stopTime = period.getToDate();
      }
      totalMillis += stopTime.getTime() - startTime.getTime();
    }
  }
}
