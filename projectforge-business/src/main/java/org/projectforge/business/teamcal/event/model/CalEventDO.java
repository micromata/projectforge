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

package org.projectforge.business.teamcal.event.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.micromata.genome.db.jpa.history.api.WithHistory;
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
import org.projectforge.framework.persistence.api.AUserRightId;
import org.projectforge.framework.persistence.api.Constants;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.model.rest.CustomerDateAndTimeDeserialize;
import org.projectforge.model.rest.CustomerDateAndTimeSerialize;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Indexed
@Table(name = "T_CALENDAR_EVENT",
  uniqueConstraints = { @UniqueConstraint(name = "unique_t_plugin_calendar_event_uid_calendar_fk", columnNames = { "uid", "calendar_fk" }) })
@WithHistory(noHistoryProperties = { "lastUpdate", "created" }, nestedEntities = { TeamEventAttendeeDO.class })
@AUserRightId(value = "PLUGIN_CALENDAR_EVENT")
public class CalEventDO extends DefaultBaseDO implements TeamEvent
{

  @IndexedEmbedded(depth = 1)
  private TeamCalDO calendar;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
  private Timestamp startDate;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
  private Timestamp endDate;

  private String uid;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String subject;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String location;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String note;

  private String icsData;

  private boolean allDay;

  @Override
  public boolean isAllDay()
  {
    return false;
  }

  public void setAllDay(boolean allDay)
  {
    this.allDay = allDay;
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
   */
  public void setSubject(final String subject)
  {
    this.subject = subject;
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
   */
  public void setLocation(final String location)
  {
    this.location = location;
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

  /**
   * @param calendar the calendar to set
   * @return this for chaining.
   */
  public void setCalendar(final TeamCalDO calendar)
  {
    this.calendar = calendar;
  }

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
  public void setNote(final String note)
  {
    this.note = note;
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
   */
  public void setStartDate(final Timestamp startDate)
  {
    this.startDate = startDate;
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
   */
  public void setEndDate(final Timestamp endDate)
  {
    this.endDate = endDate;
  }

  /**
   * @return the ics
   */
  @Column(name = "ics")
  public String getIcsData()
  {
    return icsData;
  }

  /**
   * @param icsData the icsData to set
   */
  public void setIcsData(final String icsData)
  {
    this.icsData = icsData;
  }
}
