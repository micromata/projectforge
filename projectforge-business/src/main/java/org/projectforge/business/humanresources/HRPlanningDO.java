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

package org.projectforge.business.humanresources;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.framework.persistence.api.PFPersistancyBehavior;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.DayHolder;

import de.micromata.genome.db.jpa.history.api.WithHistory;

/**
 * 
 * @author Mario Groß (m.gross@micromata.de)
 * 
 */
@Entity
@Indexed
@Table(name = "T_HR_PLANNING",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_fk", "week", "tenant_id" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_hr_planning_user_fk", columnList = "user_fk"),
        @javax.persistence.Index(name = "idx_fk_t_hr_planning_tenant_id", columnList = "tenant_id")
    })
@WithHistory(noHistoryProperties = { "lastUpdate", "created" }, nestedEntities = { HRPlanningEntryDO.class })
public class HRPlanningDO extends DefaultBaseDO
{
  private static final long serialVersionUID = 6413788186422319573L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRPlanningDO.class);

  @IndexedEmbedded(depth = 1)
  private PFUserDO user;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date week;

  @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
  @ContainedIn
  private List<HRPlanningEntryDO> entries;

  /**
   * @param date
   * @return The first day of week (UTC). The first day of the week is monday (use Locale.GERMAN) because monday is the
   *         first working day of the week.
   * @see DayHolder#setBeginOfWeek()
   */
  public static final Date getFirstDayOfWeek(final Date date)
  {
    final DayHolder day = new DayHolder(date, DateHelper.UTC, Locale.GERMAN);
    day.setBeginOfWeek();
    return day.getSQLDate();
  }

  /**
   * @param date
   * @return The first day of week (UTC). The first day of the week is monday (use Locale.GERMAN) because monday is the
   *         first working day of the week.
   * @see DayHolder#setBeginOfWeek()
   */
  public static final Date getFirstDayOfWeek(final java.util.Date date)
  {
    final DayHolder day = new DayHolder(date, DateHelper.UTC, Locale.GERMAN);
    day.setBeginOfWeek();
    return day.getSQLDate();
  }

  /**
   * @return The first day of the week.
   */
  @Column(name = "week", nullable = false)
  public Date getWeek()
  {
    return week;
  }

  /**
   * @param week
   */
  public void setWeek(final Date week)
  {
    this.week = week;
  }

  /**
   * @param week
   * @see #getFirstDayOfWeek(Date)
   */
  public void setFirstDayOfWeek(final Date week)
  {
    this.week = getFirstDayOfWeek(week);
  }

  @Transient
  public String getFormattedWeekOfYear()
  {
    return DateTimeFormatter.formatWeekOfYear(week);
  }

  /**
   * The employee assigned to this planned week.
   * 
   * @return the user
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_fk", nullable = false)
  public PFUserDO getUser()
  {
    return user;
  }

  /**
   * @param user the user to set
   */
  public HRPlanningDO setUser(final PFUserDO user)
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
   * Get the entries for this planned week.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "planning", fetch = FetchType.EAGER, orphanRemoval = true,
      targetEntity = HRPlanningEntryDO.class)
  //  @JoinColumn(name = "planning_fk")
  //  widerspruch zu jpa @Cascade(value = org.hibernate.annotations.CascadeType.SAVE_UPDATE)
  public List<HRPlanningEntryDO> getEntries()
  {
    return this.entries;
  }

  public void setEntries(final List<HRPlanningEntryDO> entries)
  {
    this.entries = entries;
  }

  public void addEntry(final HRPlanningEntryDO entry)
  {
    ensureAndGetEntries();
    entry.setPlanning(this);
    this.entries.add(entry);
  }

  /**
   * Deletes the given entry from the list of entries if not already persisted. If the entry is already persisted then
   * the entry will be marked as deleted. Undelete is possible by adding a entry with the same status/project again.
   * 
   * @param entry
   */
  public void deleteEntry(final HRPlanningEntryDO entry)
  {
    if (this.entries == null) {
      log.error("Can't remove entry because the list of entries is null (do nothing): " + entry);
      return;
    }
    if (entry.getId() == null) {
      if (this.entries.remove(entry) == false) {
        log.error("Can't remove entry because the list of entries does not contain such an entry: " + entry);
      }
    } else {
      entry.setDeleted(true);
    }
  }

  public List<HRPlanningEntryDO> ensureAndGetEntries()
  {
    if (this.entries == null) {
      setEntries(new ArrayList<HRPlanningEntryDO>());
    }
    return getEntries();
  }

  /**
   * @param idx
   * @return HRPlanningEntryDO with given index or null, if not exist.
   */
  public HRPlanningEntryDO getEntry(final int idx)
  {
    if (entries == null) {
      return null;
    }
    if (idx >= entries.size()) { // Index out of bounds.
      return null;
    }
    return entries.get(idx);
  }

  public HRPlanningEntryDO getProjectEntry(final ProjektDO project)
  {
    if (entries == null) {
      return null;
    }
    for (final HRPlanningEntryDO entry : entries) {
      if (project.getId().equals(entry.getProjektId()) == true) {
        return entry;
      }
    }
    return null;
  }

  public HRPlanningEntryDO getStatusEntry(final HRPlanningEntryStatus status)
  {
    if (entries == null) {
      return null;
    }
    for (final HRPlanningEntryDO entry : entries) {
      if (entry.getStatus() == status) {
        return entry;
      }
    }
    return null;
  }

  /**
   * @return The total duration of all entries.
   * @see HRPlanningEntryDO#getTotalHours()
   */
  @Transient
  public BigDecimal getTotalHours()
  {
    BigDecimal duration = BigDecimal.ZERO;
    if (entries == null) {
      return duration;
    }
    for (final HRPlanningEntryDO entry : entries) {
      if (entry.isDeleted() == false) {
        duration = duration.add(entry.getTotalHours());
      }
    }
    return duration;
  }

  private BigDecimal add(final BigDecimal sum, final BigDecimal value)
  {
    if (value == null) {
      return sum;
    } else {
      return sum.add(value);
    }
  }

  /**
   * @return The total hours of all unassigned entries.
   * @see HRPlanningEntryDO#getUnassignedHours()
   */
  @Transient
  public BigDecimal getTotalUnassignedHours()
  {
    BigDecimal duration = BigDecimal.ZERO;
    if (entries == null) {
      return duration;
    }
    for (final HRPlanningEntryDO entry : entries) {
      if (entry.isDeleted() == false) {
        duration = add(duration, entry.getUnassignedHours());
      }
    }
    return duration;
  }

  /**
   * @return The total hours of all entries for Monday.
   * @see HRPlanningEntryDO#getMondayHours()
   */
  @Transient
  public BigDecimal getTotalMondayHours()
  {
    BigDecimal duration = BigDecimal.ZERO;
    if (entries == null) {
      return duration;
    }
    for (final HRPlanningEntryDO entry : entries) {
      if (entry.isDeleted() == false) {
        duration = add(duration, entry.getMondayHours());
      }
    }
    return duration;
  }

  /**
   * @return The total hours of all entries for Tuesday.
   * @see HRPlanningEntryDO#getTuesdayHours()
   */
  @Transient
  public BigDecimal getTotalTuesdayHours()
  {
    BigDecimal duration = BigDecimal.ZERO;
    if (entries == null) {
      return duration;
    }
    for (final HRPlanningEntryDO entry : entries) {
      if (entry.isDeleted() == false) {
        duration = add(duration, entry.getTuesdayHours());
      }
    }
    return duration;
  }

  /**
   * @return The total hours of all entries for Wednesday.
   * @see HRPlanningEntryDO#getWednesdayHours()
   */
  @Transient
  public BigDecimal getTotalWednesdayHours()
  {
    BigDecimal duration = BigDecimal.ZERO;
    if (entries == null) {
      return duration;
    }
    for (final HRPlanningEntryDO entry : entries) {
      if (entry.isDeleted() == false) {
        duration = add(duration, entry.getWednesdayHours());
      }
    }
    return duration;
  }

  /**
   * @return The total hours of all entries for Thursday.
   * @see HRPlanningEntryDO#getThursdayHours()
   */
  @Transient
  public BigDecimal getTotalThursdayHours()
  {
    BigDecimal duration = BigDecimal.ZERO;
    if (entries == null) {
      return duration;
    }
    for (final HRPlanningEntryDO entry : entries) {
      if (entry.isDeleted() == false) {
        duration = add(duration, entry.getThursdayHours());
      }
    }
    return duration;
  }

  /**
   * @return The total hours of all entries for Friday.
   * @see HRPlanningEntryDO#getFridayHours()
   */
  @Transient
  public BigDecimal getTotalFridayHours()
  {
    BigDecimal duration = BigDecimal.ZERO;
    if (entries == null) {
      return duration;
    }
    for (final HRPlanningEntryDO entry : entries) {
      if (entry.isDeleted() == false) {
        duration = add(duration, entry.getFridayHours());
      }
    }
    return duration;
  }

  /**
   * @return The total hours of all entries for weekend.
   * @see HRPlanningEntryDO#getWeekendHours()
   */
  @Transient
  public BigDecimal getTotalWeekendHours()
  {
    BigDecimal duration = BigDecimal.ZERO;
    if (entries == null) {
      return duration;
    }
    for (final HRPlanningEntryDO entry : entries) {
      if (entry.isDeleted() == false) {
        duration = add(duration, entry.getWeekendHours());
      }
    }
    return duration;
  }

  public boolean hasDeletedEntries()
  {
    if (this.entries == null) {
      return false;
    }
    for (final HRPlanningEntryDO entry : this.entries) {
      if (entry.isDeleted() == true) {
        return true;
      }
    }
    return false;
  }
}
