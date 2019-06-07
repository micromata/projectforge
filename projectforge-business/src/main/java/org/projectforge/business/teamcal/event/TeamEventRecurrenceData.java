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

package org.projectforge.business.teamcal.event;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;

import org.projectforge.framework.time.RecurrenceFrequency;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TeamEventRecurrenceData implements Serializable
{
  private static final long serialVersionUID = -6258614682123676951L;

  private RecurrenceFrequency frequency = RecurrenceFrequency.NONE;

  private RecurrenceFrequencyModeOne modeOneMonth = RecurrenceFrequencyModeOne.FIRST;
  private RecurrenceFrequencyModeOne modeOneYear = RecurrenceFrequencyModeOne.FIRST;

  private RecurrenceFrequencyModeTwo modeTwoMonth = RecurrenceFrequencyModeTwo.MONDAY;
  private RecurrenceFrequencyModeTwo modeTwoYear = RecurrenceFrequencyModeTwo.MONDAY;

  private boolean customized = false;

  private boolean yearMode = false;
  private RecurrenceMonthMode monthMode = RecurrenceMonthMode.NONE;

  private boolean weekdays[] = new boolean[7];
  private boolean monthdays[] = new boolean[31];
  private boolean months[] = new boolean[12];

  private Date until;
  private int untilDays;

  private int interval = 1;

  private TimeZone timeZone;

  public int getUntilDays()
  {
    return untilDays;
  }

  public void setUntilDays(final int untilDays)
  {
    this.untilDays = untilDays;
  }

  public void setCustomized(final boolean customized)
  {
    this.customized = customized;
  }

  public boolean[] getMonthdays()
  {
    return monthdays;
  }

  public void setMonthdays(final boolean[] monthdays)
  {
    this.monthdays = monthdays;
  }

  public boolean[] getMonths()
  {
    return months;
  }

  public void setMonths(final boolean[] months)
  {
    this.months = months;
  }

  public boolean isYearMode()
  {
    return yearMode;
  }

  public void setYearMode(final boolean yearMode)
  {
    this.yearMode = yearMode;
  }

  public RecurrenceMonthMode getMonthMode()
  {
    return monthMode;
  }

  public void setMonthMode(final RecurrenceMonthMode monthMode)
  {
    this.monthMode = monthMode;
  }

  public boolean[] getWeekdays()
  {
    return weekdays;
  }

  public void setWeekdays(final boolean[] weekdays)
  {
    this.weekdays = weekdays;
  }

  public RecurrenceFrequencyModeOne getModeOneMonth()
  {
    return modeOneMonth;
  }

  public void setModeOneMonth(final RecurrenceFrequencyModeOne modeOneMonth)
  {
    this.modeOneMonth = modeOneMonth;
  }

  public RecurrenceFrequencyModeOne getModeOneYear()
  {
    return modeOneYear;
  }

  public void setModeOneYear(final RecurrenceFrequencyModeOne modeOneYear)
  {
    this.modeOneYear = modeOneYear;
  }

  public RecurrenceFrequencyModeTwo getModeTwoMonth()
  {
    return modeTwoMonth;
  }

  public void setModeTwoMonth(final RecurrenceFrequencyModeTwo modeTwoMonth)
  {
    this.modeTwoMonth = modeTwoMonth;
  }

  public RecurrenceFrequencyModeTwo getModeTwoYear()
  {
    return modeTwoYear;
  }

  public void setModeTwoYear(final RecurrenceFrequencyModeTwo modeTwoYear)
  {
    this.modeTwoYear = modeTwoYear;
  }

  public TeamEventRecurrenceData(final TimeZone timeZone)
  {
    this.timeZone = timeZone;
  }

  /**
   * @return the frequency
   */
  public RecurrenceFrequency getFrequency()
  {
    return frequency;
  }

  /**
   * @param frequency the interval to set
   * @return this for chaining.
   */
  public TeamEventRecurrenceData setFrequency(final RecurrenceFrequency frequency)
  {
    this.frequency = frequency;
    return this;
  }

  /**
   * @return the until, contains the possible last occurrence of an event
   */
  public Date getUntil()
  {
    return until;
  }

  /**
   * @param until the until to set
   * @return this for chaining.
   */
  public TeamEventRecurrenceData setUntil(final Date until)
  {
    this.until = until;
    return this;
  }

  /**
   * @return the interval
   */
  public int getInterval()
  {
    return interval;
  }

  /**
   * @param interval the interval to set
   * @return this for chaining.
   */
  public TeamEventRecurrenceData setInterval(final int interval)
  {
    this.interval = interval;
    return this;
  }

  public boolean isCustomized()
  {
    if (customized || interval > 1)
      return true;
    else
      return false;
  }

  /**
   * @return the timeZone
   */
  public TimeZone getTimeZone()
  {
    return timeZone;
  }
}
