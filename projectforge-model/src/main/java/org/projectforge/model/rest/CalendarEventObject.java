/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.model.rest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.lang.reflect.Field;
import java.util.Date;

/**
 * For documentation please refer the ProjectForge-API: TeamEventDO object.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XmlRootElement
public class CalendarEventObject extends AbstractBaseObject
{
  private Long calendarId;

  @JsonSerialize(using = CustomerDateAndTimeSerialize.class)
  @JsonDeserialize(using = CustomerDateAndTimeDeserialize.class)
  private Date startDate;

  @JsonSerialize(using = CustomerDateAndTimeSerialize.class)
  @JsonDeserialize(using = CustomerDateAndTimeDeserialize.class)
  private Date endDate;

  private String uid;

  private String subject;

  private String location;

  private String note;

  @JsonSerialize(using = CustomerDateAndTimeSerialize.class)
  @JsonDeserialize(using = CustomerDateAndTimeDeserialize.class)
  private Date reminder;

  private String reminderType, reminderUnit;

  private Long reminderDuration;

  private String recurrenceRule, recurrenceExDate;

  @JsonSerialize(using = CustomerDateAndTimeSerialize.class)
  @JsonDeserialize(using = CustomerDateAndTimeDeserialize.class)
  private Date recurrenceUntil;

  private String icsData;

  public Long getCalendarId()
  {
    return calendarId;
  }

  public CalendarEventObject setCalendarId(final Long calendarId)
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
   *
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
  public Long getReminderDuration()
  {
    return reminderDuration;
  }

  public CalendarEventObject setReminderDuration(final Long reminderDuration)
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

  public String getIcsData()
  {
    return icsData;
  }

  public void setIcsData(String icsData)
  {
    this.icsData = icsData;
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
    return new ReflectionToStringBuilder(this)
    {
      @Override
      protected boolean accept(final Field f)
      {
        return super.accept(f);
      }
    }.toString();
  }
}
