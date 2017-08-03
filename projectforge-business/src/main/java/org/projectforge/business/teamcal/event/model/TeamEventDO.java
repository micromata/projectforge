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
import java.util.Calendar;
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
import javax.persistence.UniqueConstraint;

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
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.Constants;
import org.projectforge.framework.persistence.api.PFPersistancyBehavior;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.RecurrenceFrequency;
import org.projectforge.framework.time.TimePeriod;

import de.micromata.genome.db.jpa.history.api.NoHistory;
import de.micromata.genome.db.jpa.history.api.WithHistory;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
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
    uniqueConstraints = { @UniqueConstraint(name = "unique_t_plugin_calendar_event_uid_calendar_fk", columnNames = { "uid", "calendar_fk" }) },
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

  private Timestamp dtStamp;

  @IndexedEmbedded(depth = 1)
  private TeamCalDO calendar;

  private transient RRule recurrenceRuleObject;

  private String recurrenceRule, recurrenceExDate, recurrenceReferenceDate, recurrenceReferenceId;

  private java.util.Date recurrenceUntil;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String note;

  private Set<TeamEventAttendeeDO> attendees;

  private Boolean ownership;

  private String organizer;
  private String organizer_additional_params;

  private String uid;

  private Integer reminderDuration;

  private ReminderDurationUnit reminderDurationType;

  private ReminderActionType reminderActionType;

  // See RFC 2445 section 4.8.7.4
  private Integer sequence = 0;

  @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
  private Set<TeamEventAttachmentDO> attachments;

  private PFUserDO creator;

  /**
   * Clear fields for viewers with minimal access. If you add new fields don't forget to clear these fields here.
   */
  public TeamEventDO clearFields()
  {
    ownership = null;
    subject = location = note = null;
    if (attendees != null) {
      attendees.clear();
    }
    organizer = null;
    organizer_additional_params = null;
    reminderDuration = null;
    reminderDurationType = null;
    reminderActionType = null;
    lastEmail = null;
    sequence = null;
    dtStamp = null;
    if (attachments != null) {
      attachments.clear();
    }
    uid = null;

    return this;
  }

  public TeamEventDO()
  {

  }

  @Override
  public void setLastUpdate()
  {
    super.setLastUpdate();
  }

  /**
   * Loads or creates the team event uid. Its very important that the uid is always the same in every ics file, which is
   * created. So only one time creation.
   */
  @Override
  @Column(nullable = false)
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
   * @param subject
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
   * @return the DTSTAMP
   */
  @Column(name = "dt_stamp")
  public Timestamp getDtStamp()
  {
    return dtStamp;
  }

  /**
   * @param dtStamp the DTSTAMP to set
   * @return this for chaining.
   */
  public TeamEventDO setDtStamp(final Timestamp dtStamp)
  {
    this.dtStamp = dtStamp;
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

  @Column
  public Boolean isOwnership()
  {
    return this.ownership;
  }

  public TeamEventDO setOwnership(final Boolean ownership)
  {
    this.ownership = ownership;
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

  @Column(length = 1000, name = "organizer_additional_params")
  public String getOrganizerAdditionalParams()
  {
    return organizer_additional_params;
  }

  public TeamEventDO setOrganizerAdditionalParams(final String organizer_additional_params)
  {
    this.organizer_additional_params = organizer_additional_params;
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
  private TeamEventDO setRecurrenceRule(final String recurrenceRule)
  {
    this.recurrenceRule = recurrenceRule;
    this.recurrenceRuleObject = null;

    return this;
  }

  /**
   * @param rRule
   * @return this for chaining.
   */
  @Transient
  public TeamEventDO setRecurrence(final RRule rRule)
  {
    if (rRule == null || rRule.getRecur() == null) {
      this.recurrenceRuleObject = null;
      this.recurrenceRule = null;
      this.recurrenceUntil = null;

      return this;
    }

    final Recur recur = rRule.getRecur();

    if (recur.getUntil() != null) {
      this.recurrenceUntil = recur.getUntil();
    } else {
      this.recurrenceUntil = null;
    }

    this.recurrenceRuleObject = null; // do not use rRule param here!
    this.recurrenceRule = rRule.getValue();

    return this;
  }

  /**
   * @param recurData
   * @return this for chaining.
   */
  @Transient
  public TeamEventDO setRecurrence(final TeamEventRecurrenceData recurData)
  {
    if (recurData == null || recurData.getFrequency() == null || recurData.getFrequency() == RecurrenceFrequency.NONE) {
      this.recurrenceRuleObject = null;
      this.recurrenceRule = null;
      this.recurrenceUntil = null;

      return this;
    }

    if (recurData.isCustomized() == false) {
      recurData.setInterval(1);
    }

    final Recur recur = new Recur();
    recur.setInterval(recurData.getInterval());
    recur.setFrequency(ICal4JUtils.getCal4JFrequencyString(recurData.getFrequency()));

    // Set until
    if (recurData.getUntil() != null) {
      if (this.allDay) {
        // just use date, no time
        final net.fortuna.ical4j.model.Date untilICal4J = new net.fortuna.ical4j.model.Date(recurData.getUntil());
        recur.setUntil(untilICal4J);
        this.recurrenceUntil = recurData.getUntil();
      } else {
        this.recurrenceUntil = this.fixUntilInRecur(recur, recurData.getUntil(), recurData.getTimeZone());
      }
    } else {
      this.recurrenceUntil = null;
    }

    final RRule rrule = new RRule(recur);

    this.recurrenceRuleObject = rrule;
    this.recurrenceRule = rrule.getValue();

    return this;
  }

  private Date fixUntilInRecur(final Recur recur, final Date until, TimeZone timezone)
  {
    // until in RecurrenceData is always in UTC!
    Calendar calUntil = Calendar.getInstance(DateHelper.UTC);
    Calendar calStart = Calendar.getInstance(timezone);

    calUntil.setTime(until);
    //    calStart.setTime(this.startDate);

    // update date of start date to until date
    calStart.set(Calendar.YEAR, calUntil.get(Calendar.YEAR));
    calStart.set(Calendar.DAY_OF_YEAR, calUntil.get(Calendar.DAY_OF_YEAR));

    // set until to last limit of day in user time
    calStart.set(Calendar.HOUR_OF_DAY, 23);
    calStart.set(Calendar.MINUTE, 59);
    calStart.set(Calendar.SECOND, 59);
    calStart.set(Calendar.MILLISECOND, 0);

    // update recur until
    DateTime untilICal4J = new DateTime(calStart.getTime());
    untilICal4J.setUtc(true);
    recur.setUntil(untilICal4J);

    // return new until date for DB usage
    return calStart.getTime();
  }

  /**
   * @return true if any recurrenceRule is given, otherwise false.
   */
  @Transient
  public boolean hasRecurrence()
  {
    return StringUtils.isNotBlank(this.recurrenceRule);
  }

  @Transient
  public TeamEventDO clearAllRecurrenceFields()
  {
    this.recurrenceRule = null;
    this.recurrenceRuleObject = null;
    this.recurrenceExDate = null;
    this.recurrenceUntil = null;

    return this;
  }

  @Transient
  public TeamEventRecurrenceData getRecurrenceData(TimeZone timezone)
  {
    final TeamEventRecurrenceData recurrenceData = new TeamEventRecurrenceData(timezone);
    final Recur recur = this.getRecurrenceObject();

    if (recur == null) {
      return recurrenceData;
    }

    recurrenceData.setInterval(recur.getInterval() < 1 ? 1 : recur.getInterval());

    if (this.recurrenceUntil != null) {
      // transform until to timezone
      if (this.isAllDay()) {
        recurrenceData.setUntil(this.recurrenceUntil);
      } else {
        // determine last possible event in event time zone (owner time zone)
        Calendar calUntil = Calendar.getInstance(this.getTimeZone());
        Calendar calStart = Calendar.getInstance(this.getTimeZone());

        calUntil.setTime(this.recurrenceUntil);
        calStart.setTime(this.startDate);

        calStart.set(Calendar.YEAR, calUntil.get(Calendar.YEAR));
        calStart.set(Calendar.DAY_OF_YEAR, calUntil.get(Calendar.DAY_OF_YEAR));

        if (calStart.after(calUntil)) {
          calStart.add(Calendar.DAY_OF_YEAR, -1);
        }

        // move to target time zone and transform to UTC
        Calendar calTimeZone = Calendar.getInstance(timezone);
        Calendar calUTC = Calendar.getInstance(DateHelper.UTC);

        calTimeZone.setTime(calStart.getTime());

        calUTC.set(Calendar.YEAR, calTimeZone.get(Calendar.YEAR));
        calUTC.set(Calendar.DAY_OF_YEAR, calTimeZone.get(Calendar.DAY_OF_YEAR));
        calUTC.set(Calendar.HOUR_OF_DAY, 0);
        calUTC.set(Calendar.MINUTE, 0);
        calUTC.set(Calendar.SECOND, 0);
        calUTC.set(Calendar.MILLISECOND, 0);

        recurrenceData.setUntil(calUTC.getTime());
      }
    }
    recurrenceData.setFrequency(ICal4JUtils.getFrequency(recur));

    if (recurrenceData.getFrequency() == RecurrenceFrequency.WEEKLY) {
      boolean[] weekdays = recurrenceData.getWeekdays();
      for (WeekDay wd : recur.getDayList()) {
        recurrenceData.setCustomized(true);
        if (wd.getDay() == WeekDay.MO.getDay()) {
          weekdays[0] = true;
        } else if (wd.getDay() == WeekDay.TU.getDay()) {
          weekdays[1] = true;
        } else if (wd.getDay() == WeekDay.WE.getDay()) {
          weekdays[2] = true;
        } else if (wd.getDay() == WeekDay.TH.getDay()) {
          weekdays[3] = true;
        } else if (wd.getDay() == WeekDay.FR.getDay()) {
          weekdays[4] = true;
        } else if (wd.getDay() == WeekDay.SA.getDay()) {
          weekdays[5] = true;
        } else if (wd.getDay() == WeekDay.SU.getDay()) {
          weekdays[6] = true;
        }
      }
      recurrenceData.setWeekdays(weekdays);
    }
    if (recurrenceData.getFrequency() == RecurrenceFrequency.MONTHLY) {
      boolean[] monthdays = recurrenceData.getMonthdays();
      for (Integer day : recur.getMonthDayList()) {
        recurrenceData.setCustomized(true);
        recurrenceData.setMonthDays(true);
        monthdays[day] = true;
      }
      recurrenceData.setMonthdays(monthdays);
    }
    if (recurrenceData.getFrequency() == RecurrenceFrequency.YEARLY) {
      boolean[] months = recurrenceData.getMonths();
      for (Integer day : recur.getMonthList()) {
        recurrenceData.setCustomized(true);
        months[day - 1] = true;
      }
      recurrenceData.setMonths(months);
    }
    return recurrenceData;
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
      recurrenceRuleObject = ICal4JUtils.calculateRRule(recurrenceRule);
    }
    return recurrenceRuleObject;
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
   * Sets the ExDates for recurring event. Expected format is CSV (date,date,date).
   * Supported date formats are <b>yyyyMMdd</b> for all day events and <b>yyyyMMdd'T'HHmmss</b> otherwise.
   * <p>
   * <b>Caution:</b> all timestamps must be represented in UTC!
   *
   * @param recurrenceExDate the recurrenceExDate to set
   * @return this for chaining.
   */
  public TeamEventDO setRecurrenceExDate(final String recurrenceExDate)
  {
    this.recurrenceExDate = recurrenceExDate;
    return this;
  }

  /**
   * Adds a new ExDate to this event.
   *
   * @param date
   * @return this for chaining.
   */
  public TeamEventDO addRecurrenceExDate(final Date date)
  {
    if (date == null) {
      return this;
    }
    final String exDate;
    exDate = ICal4JUtils.asICalDateString(date, DateHelper.UTC, isAllDay());
    if (recurrenceExDate == null || recurrenceExDate.isEmpty()) {
      recurrenceExDate = exDate;
    } else if (recurrenceExDate.contains(exDate) == false) {
      // Add this ExDate only if not yet added:
      recurrenceExDate = recurrenceExDate + "," + exDate;
    }
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
   * @param recurrenceDate the recurrenceDate to set
   * @return this for chaining.
   */
  @Transient
  public TeamEventDO setRecurrenceDate(final Date recurrenceDate, TimeZone timezone)
  {
    final DateFormat df = new SimpleDateFormat(DateFormats.ICAL_DATETIME_FORMAT);
    // Need the user's time-zone for getting midnight of desired date.
    df.setTimeZone(timezone);
    // But print it as UTC date:
    final String recurrenceDateString = df.format(recurrenceDate);
    setRecurrenceDate(recurrenceDateString);
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
   * If not given the recurrence will never ends. Identifies the last possible event occurrence.
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
  private TeamEventDO setRecurrenceUntil(final java.util.Date recurrenceUntil)
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
    result = prime * result + ((organizer == null) ? 0 : organizer.hashCode());
    result = prime * result + ((organizer_additional_params == null) ? 0 : organizer_additional_params.hashCode());
    result = prime * result + ((dtStamp == null) ? 0 : dtStamp.hashCode());
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

    if (ownership == null && other.ownership != null) {
      return false;
    } else if (ownership != other.ownership) {
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
    if (organizer == null) {
      if (other.organizer != null) {
        return false;
      }
    } else if (organizer.equals(other.organizer) == false) {
      return false;
    }
    if (organizer_additional_params == null) {
      if (other.organizer_additional_params != null) {
        return false;
      }
    } else if (organizer_additional_params.equals(other.organizer_additional_params) == false) {
      return false;
    }
    if (startDate == null) {
      if (other.startDate != null) {
        return false;
      }
    } else if (startDate.equals(other.startDate) == false) {
      return false;
    }
    if (dtStamp == null) {
      if (other.dtStamp != null) {
        return false;
      }
    } else if (dtStamp.equals(other.dtStamp) == false) {
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

  @Transient
  public boolean mustIncSequence(final TeamEventDO other)
  {
    if (allDay != other.allDay) {
      return true;
    }

    if (endDate == null) {
      if (other.endDate != null) {
        return true;
      }
    } else if (endDate.equals(other.endDate) == false) {
      return true;
    }
    if (location == null) {
      if (other.location != null) {
        return true;
      }
    } else if (location.equals(other.location) == false) {
      return true;
    }
    if (note == null) {
      if (other.note != null) {
        return true;
      }
    } else if (note.equals(other.note) == false) {
      return true;
    }
    if (recurrenceExDate == null) {
      if (other.recurrenceExDate != null) {
        return true;
      }
    } else if (recurrenceExDate.equals(other.recurrenceExDate) == false) {
      return true;
    }
    if (recurrenceRule == null) {
      if (other.recurrenceRule != null) {
        return true;
      }
    } else if (recurrenceRule.equals(other.recurrenceRule) == false) {
      return true;
    }
    if (recurrenceUntil == null) {
      if (other.recurrenceUntil != null) {
        return true;
      }
    } else if (recurrenceUntil.equals(other.recurrenceUntil) == false) {
      return true;
    }
    if (organizer == null) {
      if (other.organizer != null) {
        return true;
      }
    } else if (organizer.equals(other.organizer) == false) {
      return true;
    }
    if (organizer_additional_params == null) {
      if (other.organizer_additional_params != null) {
        return true;
      }
    } else if (organizer_additional_params.equals(other.organizer_additional_params) == false) {
      return true;
    }
    if (startDate == null) {
      if (other.startDate != null) {
        return true;
      }
    } else if (startDate.equals(other.startDate) == false) {
      return true;
    }
    if (subject == null) {
      if (other.subject != null) {
        return true;
      }
    } else if (subject.equals(other.subject) == false) {
      return true;
    }
    if (attendees == null || attendees.isEmpty()) {
      if (other.attendees != null && other.attendees.isEmpty() == false) {
        return true;
      }
    } else if (attendees.equals(other.attendees) == false) {
      return true;
    }
    if (attachments == null || attachments.isEmpty()) {
      if (other.attachments != null && other.attachments.isEmpty() == false) {
        return true;
      }
    } else if (attachments.equals(other.attachments) == false) {
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
    clone.ownership = this.ownership;
    clone.organizer = this.organizer;
    clone.organizer_additional_params = this.organizer_additional_params;
    clone.note = this.note;
    clone.lastEmail = this.lastEmail;
    clone.dtStamp = this.dtStamp;
    clone.sequence = this.sequence;
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

  @Transient
  public TeamEventDO createMinimalCopy()
  {
    final TeamEventDO result = new TeamEventDO();
    result.setId(this.getId());
    result.setCalendar(this.getCalendar());
    result.startDate = this.startDate;
    result.endDate = this.endDate;
    result.dtStamp = this.dtStamp;
    result.allDay = this.allDay;
    result.recurrenceExDate = this.recurrenceExDate;
    result.recurrenceRule = this.recurrenceRule;
    result.recurrenceReferenceDate = this.recurrenceReferenceDate;
    result.recurrenceReferenceId = this.recurrenceReferenceId;
    result.recurrenceUntil = this.recurrenceUntil;
    result.sequence = this.sequence;
    result.ownership = this.ownership;
    result.organizer = this.organizer;
    result.organizer_additional_params = this.organizer_additional_params;
    return result;
  }

  /**
   * Returns time zone of event owner.
   *
   * @return Returns time zone of event owner.
   */
  @Transient
  public TimeZone getTimeZone()
  {
    final PFUserDO user = this.getCreator();

    if (user == null) {
      return DateHelper.UTC;
    }

    return user.getTimeZoneObject();
  }
}
