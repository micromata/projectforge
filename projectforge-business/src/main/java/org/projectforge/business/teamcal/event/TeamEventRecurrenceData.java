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

  private Date until;

  private int interval = 1;

  private TimeZone timeZone;

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
   * If given interval is greater than 1 then the interval is set, otherwise the interval is set to -1 (default).
   *
   * @param interval the interval to set
   * @return this for chaining.
   */
  public TeamEventRecurrenceData setInterval(final int interval)
  {
    if (interval > 1) {
      this.interval = interval;
    } else {
      this.interval = -1;
    }
    return this;
  }

  /**
   * @return true if interval > 1, otherwise false.
   */
  public boolean isCustomized()
  {
    return this.interval > 1;
  }

  /**
   * Used by Wicket form field in {@link TeamEventEditForm}.
   *
   * @param value If true than interval will be set as 2 as default otherwise as -1.
   */
  public void setCustomized(final boolean value)
  {
    if (value == true) {
      this.interval = 2;
    } else {
      this.interval = -1;
    }
  }

  /**
   * @return the timeZone
   */
  public TimeZone getTimeZone()
  {
    return timeZone;
  }
}
