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

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.api.UserPrefParameter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.TimePeriod;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Indexed
@Table(name = "T_TIMESHEET",
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_timesheet_kost2_id", columnList = "kost2_id"),
        @javax.persistence.Index(name = "idx_fk_t_timesheet_task_id", columnList = "task_id"),
        @javax.persistence.Index(name = "idx_fk_t_timesheet_user_id", columnList = "user_id"),
        @javax.persistence.Index(name = "idx_fk_t_timesheet_tenant_id", columnList = "tenant_id"),
        @javax.persistence.Index(name = "idx_timesheet_user_time", columnList = "user_id, start_time")
    })
public class TimesheetDO extends DefaultBaseDO implements Comparable<TimesheetDO>
{
  private static final long serialVersionUID = 4239370656510694224L;

  @UserPrefParameter(i18nKey = "task", orderString = "2")
  @IndexedEmbedded(depth = 1)
  private TaskDO task;

  @UserPrefParameter(i18nKey = "user", orderString = "1")
  @IndexedEmbedded(depth = 1)
  private PFUserDO user;

  private String timeZone;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
  private Timestamp startTime;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
  private Timestamp stopTime;

  @UserPrefParameter(i18nKey = "timesheet.location")
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String location;

  @UserPrefParameter(i18nKey = "description", multiline = true)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  @UserPrefParameter(i18nKey = "fibu.kost2", orderString = "3", dependsOn = "task")
  @IndexedEmbedded(depth = 2)
  private Kost2DO kost2;

  private transient boolean marked;

  /**
   * Marker is used to mark this time sheet e. g. as a time sheet with an time period collision.
   * 
   * @return
   */
  @Transient
  public boolean isMarked()
  {
    return marked;
  }

  public TimesheetDO setMarked(final boolean marked)
  {
    this.marked = marked;
    return this;
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
   * If this entry has a kost2 with a working time fraction set or a kost2art with a working time fraction set then the
   * fraction of millis will be returned.
   */
  @Transient
  public long getWorkFractionDuration()
  {
    if (kost2 != null) {
      if (kost2.getWorkFraction() != null) {
        return (long) (kost2.getWorkFraction().doubleValue() * getTimePeriod().getDuration());
      }
      final Kost2ArtDO kost2Art = kost2.getKost2Art();
      if (kost2Art.getWorkFraction() != null) {
        return (long) (kost2Art.getWorkFraction().doubleValue() * getTimePeriod().getDuration());
      }
    }
    return getDuration();
  }

  /**
   * @return the description
   */
  @Column(length = 4000)
  public String getDescription()
  {
    return description;
  }

  /**
   * @return The abbreviated description (maximum length is 50 characters).
   */
  @Transient
  public String getShortDescription()
  {
    if (this.description == null) {
      return "";
    }
    return StringUtils.abbreviate(getDescription(), 50);
  }

  /**
   * @param description the description to set
   */
  public TimesheetDO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  /**
   * @return the location
   */
  @Column(length = 100)
  public String getLocation()
  {
    return location;
  }

  /**
   * @param location the location to set
   */
  public TimesheetDO setLocation(final String location)
  {
    this.location = location;
    return this;
  }

  /**
   * The time zone of this time sheet in format "Europe/Berlin" etc. Not yet used!
   * 
   * @see java.util.TimeZone#getAvailableIDs()
   */
  @Column(name = "time_zone", length = 100)
  public String getTimeZone()
  {
    return timeZone;
  }

  public TimesheetDO setTimeZone(final String timeZone)
  {
    this.timeZone = timeZone;
    return this;
  }

  /**
   * @return the startTime
   */
  @Column(name = "start_time", nullable = false)
  public Timestamp getStartTime()
  {
    return startTime;
  }

  /**
   * Rounds the timestamp to DatePrecision.MINUTE_15 before.
   * 
   * @param startTime the startTime to set
   * @see DateHolder#DateHolder(java.util.Calendar, DatePrecision)
   */
  public TimesheetDO setStartTime(final Timestamp startTime)
  {
    setStartDate(startTime);
    return this;
  }

  /**
   * Rounds the timestamp to DatePrecision.MINUTE_15 before.
   * 
   * @param startTime the startTime to set
   * @see DateHolder#DateHolder(java.util.Calendar, DatePrecision)
   */
  @Transient
  public TimesheetDO setStartDate(final Date startDate)
  {
    if (startDate != null) {
      final DateHolder date = new DateHolder(startDate, DatePrecision.MINUTE_15);
      this.startTime = date.getTimestamp();
    } else {
      this.stopTime = null;
    }
    return this;
  }

  @Transient
  public TimesheetDO setStartDate(final long millis)
  {
    setStartDate(new Date(millis));
    return this;
  }

  /**
   * @return
   * @see DateTimeFormatter#formatWeekOfYear(Date)
   */
  @Transient
  public String getFormattedWeekOfYear()
  {
    return DateTimeFormatter.formatWeekOfYear(startTime);
  }

  /**
   * @return the stopTime
   */
  @Column(name = "stop_time", nullable = false)
  public Timestamp getStopTime()
  {
    return stopTime;
  }

  /**
   * @param stopTime
   * @return this for chaining.
   * @see #setStopDate(Date)
   */
  public TimesheetDO setStopTime(final Timestamp stopTime)
  {
    return setStopDate(stopTime);
  }

  /**
   * Rounds the timestamp to DatePrecision.MINUTE_15 before.
   * 
   * @param stopDate the stopTime to set
   * @return this for chaining.
   * @see DateHolder#DateHolder(java.util.Calendar, DatePrecision)
   */
  @Transient
  public TimesheetDO setStopDate(final Date stopDate)
  {
    if (stopDate != null) {
      final DateHolder date = new DateHolder(stopDate, DatePrecision.MINUTE_15);
      this.stopTime = date.getTimestamp();
    } else {
      this.stopTime = null;
    }
    return this;
  }

  @Transient
  public TimesheetDO setStopTime(final long millis)
  {
    setStopTime(new Timestamp(millis));
    return this;
  }

  @Transient
  public TimePeriod getTimePeriod()
  {
    return new TimePeriod(startTime, stopTime, marked);
  }

  /**
   * The employee assigned to this timesheet.
   * 
   * @return the user
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  public PFUserDO getUser()
  {
    return user;
  }

  /**
   * @param user the user to set
   */
  public TimesheetDO setUser(final PFUserDO user)
  {
    this.user = user;
    return this;
  }

  @Transient
  public Integer getUserId()
  {
    if (this.user == null) {
      return null;
    }
    return user.getId();
  }

  /**
   * Not used as object due to performance reasons.
   * 
   * @return
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id", nullable = false)
  public TaskDO getTask()
  {
    return task;
  }

  public TimesheetDO setTask(final TaskDO task)
  {
    this.task = task;
    return this;
  }

  @Transient
  public Integer getTaskId()
  {
    if (this.task == null) {
      return null;
    }
    return task.getId();
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "kost2_id", nullable = true)
  public Kost2DO getKost2()
  {
    return kost2;
  }

  public TimesheetDO setKost2(final Kost2DO kost2)
  {
    this.kost2 = kost2;
    return this;
  }

  @Transient
  public Integer getKost2Id()
  {
    if (this.kost2 == null) {
      return null;
    }
    return kost2.getId();
  }

  @Override
  public int compareTo(final TimesheetDO o)
  {
    return (getStartTime().compareTo(o.getStartTime()));
  }
}
