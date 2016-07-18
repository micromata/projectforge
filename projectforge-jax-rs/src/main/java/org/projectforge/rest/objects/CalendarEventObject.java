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

package org.projectforge.rest.objects;

import java.lang.reflect.Field;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.projectforge.rest.AbstractBaseObject;

/**
 * For documentation please refer the ProjectForge-API: TeamEventDO object.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class CalendarEventObject extends AbstractBaseObject
{
  private Integer calendarId;

  private Date startDate;

  private Date endDate;

  private String uid;

  private String subject;

  private String location;

  private String note;

  private Date reminder;

  private String reminderType, reminderUnit;

  private Integer reminderDuration;

  private String recurrenceRule, recurrenceExDate;

  private Date recurrenceUntil;

  public Integer getCalendarId()
  {
    return calendarId;
  }

  public CalendarEventObject setCalendarId(final Integer calendarId)
  {
    this.calendarId = calendarId;
    return this;
  }

  public Date getStartDate()
  {
    return startDate;
  }

  public CalendarEventObject setStartDate(final Date startDate)
  {
    this.startDate = startDate;
    return this;
  }

  public Date getEndDate()
  {
    return endDate;
  }

  public CalendarEventObject setEndDate(final Date endDate)
  {
    this.endDate = endDate;
    return this;
  }

  public String getUid()
  {
    return uid;
  }

  public CalendarEventObject setUid(final String uid)
  {
    this.uid = uid;
    return this;
  }

  public String getSubject()
  {
    return subject;
  }

  public CalendarEventObject setSubject(final String subject)
  {
    this.subject = subject;
    return this;
  }

  public String getLocation()
  {
    return location;
  }

  public CalendarEventObject setLocation(final String location)
  {
    this.location = location;
    return this;
  }

  public String getNote()
  {
    return note;
  }

  public CalendarEventObject setNote(final String note)
  {
    this.note = note;
    return this;
  }

  /**
   * Calculated out of reminderDuration and reminderUnit for the developer's convenience.
   * @return The date (UTC) at which the reminder should be fired.
   */
  public Date getReminder()
  {
    return reminder;
  }

  public CalendarEventObject setReminder(final Date reminder)
  {
    this.reminder = reminder;
    return this;
  }

  public String getReminderType()
  {
    return reminderType;
  }

  public CalendarEventObject setReminderType(final String reminderType)
  {
    this.reminderType = reminderType;
    return this;
  }

  /**
   * @return the reminderDuration
   * @see #getReminder()
   */
  public Integer getReminderDuration()
  {
    return reminderDuration;
  }

  public CalendarEventObject setReminderDuration(final Integer reminderDuration)
  {
    this.reminderDuration = reminderDuration;
    return this;
  }

  /**
   * @return the reminderUnit
   * @see #getReminder()
   */
  public String getReminderUnit()
  {
    return reminderUnit;
  }

  public CalendarEventObject setReminderUnit(final String reminderUnit)
  {
    this.reminderUnit = reminderUnit;
    return this;
  }

  /**
   * RRULE (rfc5545)
   */
  public String getRecurrenceRule()
  {
    return recurrenceRule;
  }

  public CalendarEventObject setRecurrenceRule(final String recurrenceRule)
  {
    this.recurrenceRule = recurrenceRule;
    return this;
  }

  /**
   * @return true if any recurrenceRule is given, otherwise false.
   */
  public boolean hasRecurrence()
  {
    return StringUtils.isNotBlank(this.recurrenceRule);
  }

  /**
   * EXDATE (rfc5545) Ex dates are time stamps of deleted events out of the recurrence events.
   */
  public String getRecurrenceExDate()
  {
    return recurrenceExDate;
  }

  public CalendarEventObject setRecurrenceExDate(final String recurrenceExDate)
  {
    this.recurrenceExDate = recurrenceExDate;
    return this;
  }

  /**
   * If not given the recurrence will never ends.
   */
  public Date getRecurrenceUntil()
  {
    return recurrenceUntil;
  }

  public CalendarEventObject setRecurrenceUntil(final Date recurrenceUntil)
  {
    this.recurrenceUntil = recurrenceUntil;
    return this;
  }

  @Override
  public String toString()
  {
    return new ReflectionToStringBuilder(this) {
      @Override
      protected boolean accept(final Field f)
      {
        return super.accept(f);
      }
    }.toString();
  }
}
