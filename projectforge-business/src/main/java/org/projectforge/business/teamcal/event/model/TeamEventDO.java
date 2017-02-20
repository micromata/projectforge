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

package org.projectforge.business.teamcal.event.model;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventRecurrenceData;
import org.projectforge.business.teamcal.service.TeamCalServiceImpl;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.Constants;
import org.projectforge.framework.persistence.api.PFPersistancyBehavior;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.TimePeriod;

import de.micromata.genome.db.jpa.history.api.NoHistory;
import de.micromata.genome.db.jpa.history.api.WithHistory;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.property.RRule;

/**
 * Overview of used (and may-be planned) fields:
 * <ul>
 * <li><b>ATTENDEE</b>: ATTENDEE;MEMBER="mailto:DEV-GROUP@example.com": mailto:joecool@example.com</li>
 * <li><b>CONTACT</b>: CONTACT:Jim Dolittle\, ABC Industries\, +1-919-555-1234</li>
 * <li><b>DTEND</b> - End date (DATE-TIME)</li>
 * <li><b>DTSTAMP</b> - Time of creation (DATE-TIME)</li>
 * <li><b>DTSTART</b> - Start date (DATE-TIME)</li>
 * <li><b>EXDATE</b> - exception dates of recurrence (list of DATE-TIME)</li>
 * <li><b>LAST-MODIFIED</b> - (DATE-TIME)</li>
 * <li><b>PARTSTAT</b>=DECLINED:mailto:jsmith@example.com</li>
 * <li><b>ORGANIZER</b>: ORGANIZER;CN=John Smith:mailto:jsmith@example.com</li>
 * <li><b>RDATE</b> - Dates of recurrence</li>
 * <li><b>RRULE</b> - Rule of recurrence: RRULE:FREQ=DAILY;UNTIL=19971224T000000Z</li>
 * <li><b>UID</b>: UID:19960401T080045Z-4000F192713-0052@example.com
 * </ul>
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_CALENDAR_EVENT",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_calendar_fk", columnList = "calendar_fk"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_tenant_id", columnList = "tenant_id"),
        @javax.persistence.Index(name = "idx_plugin_team_cal_end_date", columnList = "calendar_fk, end_date"),
        @javax.persistence.Index(name = "idx_plugin_team_cal_start_date", columnList = "calendar_fk, start_date"),
        @javax.persistence.Index(name = "idx_plugin_team_cal_time", columnList = "calendar_fk, start_date, end_date")
    })
@WithHistory(noHistoryProperties = { "lastUpdate", "created" }, nestedEntities = { TeamEventAttendeeDO.class })
@AUserRightId(value = "PLUGIN_CALENDAR_EVENT")
public class TeamEventDO extends DefaultBaseDO implements TeamEvent, Cloneable
{
  private static final long serialVersionUID = -9205582135590380919L;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String subject;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String location;

  private boolean allDay;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
  private Timestamp startDate;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
  private Timestamp endDate;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.SECOND, encoding = EncodingType.STRING)
  @NoHistory
  private Timestamp lastEmail;

  @IndexedEmbedded(depth = 1)
  private TeamCalDO calendar;

  private transient RRule recurrenceRuleObject;

  private String recurrenceRule, recurrenceExDate, recurrenceReferenceDate, recurrenceReferenceId;

  private java.util.Date recurrenceUntil;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String note;

  private Set<TeamEventAttendeeDO> attendees;

  private String organizer;

  private String uid;

  private Integer reminderDuration;

  private ReminderDurationUnit reminderDurationType;

  private ReminderActionType reminderActionType;

  // See RFC 2445 section 4.8.7.4
  private Integer sequence = 0;

  // See RFC 2445 section 4.8.1.11
  // private TeamEventStatus status = TeamEventStatus.UNKNOWN;

  @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
  private Set<TeamEventAttachmentDO> attachments;

  private TimeZone timeZone;

  private PFUserDO creator;

  /**
   * Clear fields for viewers with minimal access. If you add new fields don't forget to clear these fields here.
   */
  public TeamEventDO clearFields()
  {
    subject = location = note = null;
    if (attendees != null) {
      attendees.clear();
    }
    organizer = null;
    reminderDuration = null;
    reminderDurationType = null;
    reminderActionType = null;
    lastEmail = null;
    sequence = null;
    if (attachments != null) {
      attachments.clear();
    }
    uid = null;
    // status = null;
    return this;
  }

  public TeamEventDO()
  {

  }

  /**
   * Loads or creates the team event uid. Its very important that the uid is always the same in every ics file, which is
   * created. So only one time creation.
   */
  @Override
  @Column
  public String getUid()
  {
    return uid;
  }

  /**
   * @param uid
   */
  public void setUid(final String uid)
  {
    this.uid = uid;
  }

  @Override
  @Column(length = Constants.LENGTH_SUBJECT)
  public String getSubject()
  {
    return subject;
  }

  /**
   * @param title
   * @return this for chaining.
   */
  public TeamEventDO setSubject(final String subject)
  {
    this.subject = subject;
    return this;
  }

  @Override
  @Column(length = Constants.LENGTH_SUBJECT)
  /**
   * @return the location
   */
  public String getLocation()
  {
    return location;
  }

  /**
   * @param location the location to set
   * @return this for chaining.
   */
  public TeamEventDO setLocation(final String location)
  {
    this.location = location;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "calendar_fk", nullable = false)
  /**
   * @return the calendar
   */
  public TeamCalDO getCalendar()
  {
    return calendar;
  }

  @Transient
  public Integer getCalendarId()
  {
    return calendar != null ? calendar.getId() : null;
  }

  /**
   * @param calendar the calendar to set
   * @return this for chaining.
   */
  public TeamEventDO setCalendar(final TeamCalDO calendar)
  {
    this.calendar = calendar;
    return this;
  }

  /**
   * @return the allDay
   */
  @Override
  @Column(name = "all_day")
  public boolean isAllDay()
  {
    return allDay;
  }

  /**
   * @param allDay the allDay to set
   * @return this for chaining.
   */
  public TeamEventDO setAllDay(final boolean allDay)
  {
    this.allDay = allDay;
    return this;
  }

  /**
   * @return the startDate
   */
  @Override
  @Column(name = "start_date")
  public Timestamp getStartDate()
  {
    return startDate;
  }

  /**
   * @param startDate the startDate to set
   * @return this for chaining.
   */
  public TeamEventDO setStartDate(final Timestamp startDate)
  {
    this.startDate = startDate;
    return this;
  }

  /**
   * @return the endDate
   */
  @Override
  @Column(name = "end_date")
  public Timestamp getEndDate()
  {
    return endDate;
  }

  /**
   * @param endDate the endDate to set
   * @return this for chaining.
   */
  public TeamEventDO setEndDate(final Timestamp endDate)
  {
    this.endDate = endDate;
    return this;
  }

  /**
   * @return the lastEmail
   */
  @Column(name = "last_email")
  public Timestamp getLastEmail()
  {
    return lastEmail;
  }

  /**
   * @param lastEmail the lastEmail to set
   * @return this for chaining.
   */
  public TeamEventDO setLastEmail(final Timestamp lastEmail)
  {
    this.lastEmail = lastEmail;
    return this;
  }

  /**
   * @return the note
   */
  @Override
  @Column(length = 4000)
  public String getNote()
  {
    return note;
  }

  /**
   * @param note the note to set
   * @return this for chaining.
   */
  public TeamEventDO setNote(final String note)
  {
    this.note = note;
    return this;
  }

  /**
   * @return the attendees
   */
  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "team_event_fk")
  public Set<TeamEventAttendeeDO> getAttendees()
  {
    ensureAttendees();
    return attendees;
  }

  /**
   * @param attendees the attendees to set
   * @return this for chaining.
   */
  public TeamEventDO setAttendees(final Set<TeamEventAttendeeDO> attendees)
  {
    this.attendees = attendees;
    return this;
  }

  /**
   * Creates a {@link TreeSet}.
   *
   * @return this for chaining.
   */
  public Set<TeamEventAttendeeDO> ensureAttendees()
  {
    if (this.attendees == null) {
      this.attendees = new HashSet<TeamEventAttendeeDO>();
    }
    return this.attendees;
  }

  public TeamEventDO addAttendee(final TeamEventAttendeeDO attendee)
  {
    ensureAttendees();
    short number = 1;
    for (final TeamEventAttendeeDO pos : attendees) {
      if (pos.getNumber() >= number) {
        number = pos.getNumber();
        number++;
      }
    }
    attendee.setNumber(number);
    this.attendees.add(attendee);
    return this;
  }

  /**
   * @return the organizer
   */
  @Column(length = 1000)
  public String getOrganizer()
  {
    return organizer;
  }

  /**
   * @param organizer the organizer to set
   * @return this for chaining.
   */
  public TeamEventDO setOrganizer(final String organizer)
  {
    this.organizer = organizer;
    return this;
  }

  /**
   * RRULE (rfc5545)
   *
   * @return the recurrence
   */
  @Column(name = "recurrence_rule", length = 4000)
  public String getRecurrenceRule()
  {
    return recurrenceRule;
  }

  /**
   * @param recurrenceRule the recurrence to set
   * @return this for chaining.
   */
  public TeamEventDO setRecurrenceRule(final String recurrenceRule)
  {
    this.recurrenceRule = recurrenceRule;
    this.recurrenceRuleObject = null;
    recalculate();
    return this;
  }

  /**
   * @param recurData
   * @return this for chaining.
   */
  @Transient
  public TeamEventDO setRecurrence(final TeamEventRecurrenceData recurData)
  {
    final String rruleString = TeamCalServiceImpl.calculateRRule(recurData);
    setRecurrenceRule(rruleString);
    return this;
  }

  /**
   * Will be renewed if {@link #setRecurrenceRule(String)} is called.
   *
   * @return the recurrenceRuleObject
   */
  @Transient
  public RRule getRecurrenceRuleObject()
  {
    if (recurrenceRuleObject == null) {
      recalculate();
    }
    return recurrenceRuleObject;
  }

  /**
   * @return true if any recurrenceRule is given, otherwise false.
   */
  @Transient
  public boolean hasRecurrence()
  {
    return StringUtils.isNotBlank(this.recurrenceRule);
  }

  public TeamEventDO clearAllRecurrenceFields()
  {
    setRecurrence(null).setRecurrenceExDate(null).setRecurrenceUntil(null);
    return this;
  }

  /**
   * The recurrenceUntil date is calculated by the recurrenceRule string if given, otherwise the date is set to null.
   * get
   *
   * @see org.projectforge.framework.persistence.entities.AbstractBaseDO#recalculate()
   */
  @Override
  public void recalculate()
  {
    super.recalculate();
    recurrenceRuleObject = ICal4JUtils.calculateRecurrenceRule(recurrenceRule, getTimeZone());
    if (recurrenceRuleObject == null || recurrenceRuleObject.getRecur() == null) {
      this.recurrenceUntil = null;
      return;
    }
    this.recurrenceUntil = recurrenceRuleObject.getRecur().getUntil();
  }

  /**
   * Will be renewed if {@link #setRecurrenceRule(String)} is called.
   *
   * @return the recurrenceRuleObject
   */
  @Transient
  public Recur getRecurrenceObject()
  {
    final RRule rrule = getRecurrenceRuleObject();
    return rrule != null ? rrule.getRecur() : null;
  }

  /**
   * EXDATE (rfc5545) Ex dates are time stamps of deleted events out of the recurrence events.
   *
   * @return the recurrenceExDate
   */
  @Column(name = "recurrence_ex_date", length = 4000)
  public String getRecurrenceExDate()
  {
    return recurrenceExDate;
  }

  /**
   * @param date
   * @param timeZone Only used for all day events.
   * @return
   */
  public TeamEventDO addRecurrenceExDate(final Date date, final TimeZone timeZone)
  {
    if (date == null) {
      return this;
    }
    final String exDate;
    if (isAllDay() == true) {
      exDate = ICal4JUtils.asISODateString(date, timeZone);
    } else {
      exDate = ICal4JUtils.asISODateTimeString(date);
    }
    if (recurrenceExDate == null) {
      recurrenceExDate = exDate;
    } else if (recurrenceExDate.contains(exDate) == false) {
      // Add this ex date only if not yet added:
      recurrenceExDate = recurrenceExDate + "," + exDate;
    }
    return this;
  }

  /**
   * @param recurrenceExDate the recurrenceExDate to set
   * @return this for chaining.
   */
  public TeamEventDO setRecurrenceExDate(final String recurrenceExDate)
  {
    this.recurrenceExDate = recurrenceExDate;
    return this;
  }

  /**
   * @param recurrenceExDate the recurrenceExDate to set
   * @return this for chaining.
   */
  @Transient
  public TeamEventDO setRecurrenceDate(final Date recurrenceDate)
  {
    final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MILLIS);
    // Need the user's time-zone for getting midnight of desired date.
    df.setTimeZone(ThreadLocalUserContext.getTimeZone());
    // But print it as UTC date:
    final String recurrenceDateString = df.format(recurrenceDate);
    setRecurrenceDate(recurrenceDateString);
    return this;
  }

  /**
   * This field is RECURRENCE_ID. Isn't yet used (ex-date is always used instead in master event).
   *
   * @return the recurrenceId
   */
  @Column(name = "recurrence_date")
  public String getRecurrenceDate()
  {
    return recurrenceReferenceDate;
  }

  /**
   * @param recurrenceDateString the recurrenceId to set
   * @return this for chaining.
   */
  public TeamEventDO setRecurrenceDate(final String recurrenceDateString)
  {
    this.recurrenceReferenceDate = recurrenceDateString;
    return this;
  }

  /**
   * Isn't yet used (ex-date is always used instead in master event).
   *
   * @return the recurrenceReferenceId
   */
  @Column(name = "recurrence_reference_id", length = 255)
  public String getRecurrenceReferenceId()
  {
    return recurrenceReferenceId;
  }

  /**
   * @param recurrenceReferenceId the recurrenceReferenceId to set
   * @return this for chaining.
   */
  public TeamEventDO setRecurrenceReferenceId(final String recurrenceReferenceId)
  {
    this.recurrenceReferenceId = recurrenceReferenceId;
    return this;
  }

  /**
   * @param recurrenceReferenceId the recurrenceReferenceId to set
   * @return this for chaining.
   */
  public TeamEventDO setRecurrenceReferenceId(final Integer recurrenceReferenceId)
  {
    this.recurrenceReferenceId = recurrenceReferenceId != null ? String.valueOf(recurrenceReferenceId) : null;
    return this;
  }

  /**
   * If not given the recurrence will never ends.
   *
   * @return the recurrenceEndDate
   */
  @Column(name = "recurrence_until")
  public java.util.Date getRecurrenceUntil()
  {
    return recurrenceUntil;
  }

  /**
   * Please note: Do not set this property manually! It's set automatically by the recurrence rule! Otherwise the
   * display of calendar events may be incorrect. <br/>
   * This field exist only for data-base query purposes.
   *
   * @param recurrenceUntil the recurrenceEndDate to set
   * @return this for chaining.
   */
  public TeamEventDO setRecurrenceUntil(final java.util.Date recurrenceUntil)
  {
    this.recurrenceUntil = recurrenceUntil;
    return this;
  }

  @Transient
  public TimePeriod getTimePeriod()
  {
    return new TimePeriod(startDate, endDate, true);
  }

  /**
   * @return Duration in millis if startTime and stopTime is given and stopTime is after startTime, otherwise 0.
   */
  @Transient
  public long getDuration()
  {
    return getTimePeriod().getDuration();
  }

  /**
   * Get reminder duration.
   *
   * @return
   */
  @Column(name = "reminder_duration")
  public Integer getReminderDuration()
  {
    return reminderDuration;
  }

  /**
   * Get type of reminder duration minute, hour, day
   *
   * @return the reminderDurationType
   */
  @Column(name = "reminder_duration_unit")
  @Enumerated(EnumType.STRING)
  public ReminderDurationUnit getReminderDurationUnit()
  {
    return reminderDurationType;
  }

  /**
   * @param reminderDurationUnit the alarmReminderType to set
   * @return this for chaining.
   */
  public void setReminderDurationUnit(final ReminderDurationUnit reminderDurationUnit)
  {
    this.reminderDurationType = reminderDurationUnit;
  }

  /**
   * @param trigger the trigger to set
   * @return this for chaining.
   */
  public void setReminderDuration(final Integer trigger)
  {
    this.reminderDuration = trigger;
  }

  /**
   * Gets type of event action. AUDIO or DISPLAY
   *
   * @return the reminderType
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "reminder_action_type")
  public ReminderActionType getReminderActionType()
  {
    return reminderActionType;
  }

  /**
   * Set type of event action.
   *
   * @param reminderActionType the reminderType to set
   * @return this for chaining.
   */
  public void setReminderActionType(final ReminderActionType reminderActionType)
  {
    this.reminderActionType = reminderActionType;
  }

  /**
   * @return the sequence
   */
  @Column
  public Integer getSequence()
  {
    return sequence;
  }

  /**
   * @param sequence the sequence to set
   * @return this for chaining.
   */
  public TeamEventDO setSequence(final Integer sequence)
  {
    this.sequence = sequence;
    return this;
  }

  public void incSequence()
  {
    sequence++;
  }

  /**
   * @return the attachments
   */
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "team_event_fk2")
  public Set<TeamEventAttachmentDO> getAttachments()
  {
    return attachments;
  }

  /**
   * @param attachments the attachments to set
   * @return this for chaining.
   */
  public TeamEventDO setAttachments(final Set<TeamEventAttachmentDO> attachments)
  {
    this.attachments = attachments;
    return this;
  }

  /**
   * Creates a {@link TreeSet}.
   *
   * @return this for chaining.
   */
  public Set<TeamEventAttachmentDO> ensureAttachments()
  {
    if (this.attachments == null) {
      this.attachments = new TreeSet<TeamEventAttachmentDO>();
    }
    return this.attachments;
  }

  public TeamEventDO addAttachment(final TeamEventAttachmentDO attachment)
  {
    ensureAttachments();
    this.attachments.add(attachment);
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_event_fk_creator")
  public PFUserDO getCreator()
  {
    if (this.getPk() != null && this.creator == null) {
      this.creator = this.calendar.getOwner();
    }
    return this.creator;
  }

  public void setCreator(PFUserDO creator)
  {
    this.creator = creator;
  }

  // /**
  // * @return the status
  // */
  // @Column
  // public TeamEventStatus getStatus()
  // {
  // return status;
  // }
  //
  // /**
  // * @param status the status to set
  // * @return this for chaining.
  // */
  // public TeamEventDO setStatus(final TeamEventStatus status)
  // {
  // this.status = status;
  // return this;
  // }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (allDay ? 1231 : 1237);
    result = prime * result + ((attachments == null) ? 0 : attachments.hashCode());
    result = prime * result + ((attendees == null) ? 0 : attendees.hashCode());
    result = prime * result + ((calendar == null) ? 0 : calendar.hashCode());
    result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
    result = prime * result + ((location == null) ? 0 : location.hashCode());
    result = prime * result + ((note == null) ? 0 : note.hashCode());
    result = prime * result + ((recurrenceExDate == null) ? 0 : recurrenceExDate.hashCode());
    result = prime * result + ((recurrenceRule == null) ? 0 : recurrenceRule.hashCode());
    result = prime * result + ((recurrenceUntil == null) ? 0 : recurrenceUntil.hashCode());
    result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
    result = prime * result + ((subject == null) ? 0 : subject.hashCode());
    return result;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final TeamEventDO other = (TeamEventDO) obj;
    if (allDay != other.allDay) {
      return false;
    }
    if (attendees == null) {
      if (other.attendees != null) {
        return false;
      }
    } else if (attendees.equals(other.attendees) == false) {
      return false;
    }
    if (calendar == null) {
      if (other.calendar != null) {
        return false;
      }
    } else if (calendar.equals(other.calendar) == false) {
      return false;
    }
    if (endDate == null) {
      if (other.endDate != null) {
        return false;
      }
    } else if (endDate.equals(other.endDate) == false) {
      return false;
    }
    if (location == null) {
      if (other.location != null) {
        return false;
      }
    } else if (location.equals(other.location) == false) {
      return false;
    }
    if (note == null) {
      if (other.note != null) {
        return false;
      }
    } else if (note.equals(other.note) == false) {
      return false;
    }
    if (recurrenceExDate == null) {
      if (other.recurrenceExDate != null) {
        return false;
      }
    } else if (recurrenceExDate.equals(other.recurrenceExDate) == false) {
      return false;
    }
    if (recurrenceRule == null) {
      if (other.recurrenceRule != null) {
        return false;
      }
    } else if (recurrenceRule.equals(other.recurrenceRule) == false) {
      return false;
    }
    if (recurrenceUntil == null) {
      if (other.recurrenceUntil != null) {
        return false;
      }
    } else if (recurrenceUntil.equals(other.recurrenceUntil) == false) {
      return false;
    }
    if (startDate == null) {
      if (other.startDate != null) {
        return false;
      }
    } else if (startDate.equals(other.startDate) == false) {
      return false;
    }
    if (subject == null) {
      if (other.subject != null) {
        return false;
      }
    } else if (subject.equals(other.subject) == false) {
      return false;
    }
    if (attachments == null) {
      if (other.attachments != null) {
        return false;
      }
    } else if (attachments.equals(other.attachments) == false) {
      return false;
    }
    return true;
  }

  public boolean mustIncSequence(final TeamEventDO other)
  {
    if (location == null) {
      if (other.location != null) {
        return true;
      }
    } else if (!location.equals(other.location)) {
      return true;
    }
    if (startDate == null) {
      if (other.startDate != null) {
        return true;
      }
    } else if (!startDate.equals(other.startDate)) {
      return true;
    }
    if (endDate == null) {
      if (other.endDate != null) {
        return true;
      }
    } else if (!endDate.equals(other.endDate)) {
      return true;
    }
    if (recurrenceExDate == null) {
      if (other.recurrenceExDate != null) {
        return true;
      }
    } else if (!recurrenceExDate.equals(other.recurrenceExDate)) {
      return true;
    }
    if (recurrenceRule == null) {
      if (other.recurrenceRule != null) {
        return true;
      }
    } else if (!recurrenceRule.equals(other.recurrenceRule)) {
      return true;
    }
    if (recurrenceUntil == null) {
      if (other.recurrenceUntil != null) {
        return true;
      }
    } else if (!recurrenceUntil.equals(other.recurrenceUntil)) {
      return true;
    }
    return false;
  }

  /**
   * @see java.lang.Object#clone()
   */
  @Override
  public TeamEventDO clone()
  {
    final TeamEventDO clone = new TeamEventDO();
    clone.setCalendar(this.getCalendar());
    clone.setCreator(this.getCreator());
    clone.startDate = this.startDate;
    clone.endDate = this.endDate;
    clone.allDay = this.allDay;
    clone.subject = this.subject;
    clone.location = this.location;
    clone.recurrenceExDate = this.recurrenceExDate;
    clone.recurrenceRule = this.recurrenceRule;
    clone.recurrenceReferenceDate = this.recurrenceReferenceDate;
    clone.recurrenceReferenceId = this.recurrenceReferenceId;
    clone.recurrenceUntil = this.recurrenceUntil;
    clone.organizer = this.organizer;
    clone.note = this.note;
    clone.lastEmail = this.lastEmail;
    clone.sequence = this.sequence;
    // clone.status = this.status;
    clone.reminderDuration = this.reminderDuration;
    clone.reminderDurationType = this.reminderDurationType;
    clone.reminderActionType = this.reminderActionType;
    if (this.attendees != null && this.attendees.isEmpty() == false) {
      clone.attendees = clone.ensureAttendees();
      for (final TeamEventAttendeeDO attendee : this.getAttendees()) {
        TeamEventAttendeeDO cloneAttendee = new TeamEventAttendeeDO();
        cloneAttendee.setAddress(attendee.getAddress());
        cloneAttendee.setComment(attendee.getComment());
        cloneAttendee.setCommentOfAttendee(attendee.getCommentOfAttendee());
        cloneAttendee.setLoginToken(attendee.getLoginToken());
        cloneAttendee.setNumber(attendee.getNumber());
        cloneAttendee.setStatus(attendee.getStatus());
        cloneAttendee.setUrl(attendee.getUrl());
        cloneAttendee.setUser(attendee.getUser());
        clone.addAttendee(cloneAttendee);
      }
    }
    if (this.attachments != null && this.attachments.isEmpty() == false) {
      clone.attachments = clone.ensureAttachments();
      for (final TeamEventAttachmentDO attachment : this.getAttachments()) {
        TeamEventAttachmentDO cloneAttachment = new TeamEventAttachmentDO();
        cloneAttachment.setFilename(attachment.getFilename());
        cloneAttachment.setContent(attachment.getContent());
        clone.addAttachment(cloneAttachment);
      }
    }
    return clone;
  }

  public TeamEventDO createMinimalCopy()
  {
    final TeamEventDO result = new TeamEventDO();
    result.setId(this.getId());
    result.setCalendar(this.getCalendar());
    result.startDate = this.startDate;
    result.endDate = this.endDate;
    result.allDay = this.allDay;
    result.recurrenceExDate = this.recurrenceExDate;
    result.recurrenceRule = this.recurrenceRule;
    result.recurrenceReferenceDate = this.recurrenceReferenceDate;
    result.recurrenceReferenceId = this.recurrenceReferenceId;
    result.recurrenceUntil = this.recurrenceUntil;
    result.sequence = this.sequence;
    return result;
  }

  @Transient
  public TimeZone getTimeZone()
  {
    if (timeZone == null) {
      timeZone = ThreadLocalUserContext.getTimeZone();
    }
    return timeZone;
  }

  @Transient
  public void setTimeZone(TimeZone timeZone)
  {
    this.timeZone = timeZone;
  }
}
