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

package org.projectforge.business.task;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.projectforge.business.gantt.GanttObjectType;
import org.projectforge.business.gantt.GanttRelationType;
import org.projectforge.common.StringHelper;
import org.projectforge.common.i18n.Priority;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.common.task.TimesheetBookingStatus;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Indexed
@ClassBridge(name = "taskpath", index = Index.YES /* TOKENIZED */, store = Store.NO,
    impl = HibernateSearchTaskPathBridge.class)
@Table(name = "T_TASK",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "parent_task_id", "title" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_task_gantt_predecessor_fk", columnList = "gantt_predecessor_fk"),
        @javax.persistence.Index(name = "idx_fk_t_task_parent_task_id", columnList = "parent_task_id"),
        @javax.persistence.Index(name = "idx_fk_t_task_responsible_user_id", columnList = "responsible_user_id"),
        @javax.persistence.Index(name = "idx_fk_t_task_tenant_id", columnList = "tenant_id")
    })
@JpaXmlPersist(beforePersistListener = TaskXmlBeforePersistListener.class)
public class TaskDO extends DefaultBaseDO implements ShortDisplayNameCapable, Cloneable// , GanttObject
{
  public static final String KOST2_SEPARATOR_CHARS = ",; ";

  public static final int TITLE_LENGTH = 40;

  public static final int DESCRIPTION_LENGTH = 4000;

  public static final int SHORT_DESCRIPTION_LENGTH = 255;

  public static final int REFERENCE_LENGTH = 1000;

  public static final int PRIORITY_LENGTH = 7;

  public static final int STATUS_LENGTH = 1;

  private static final long serialVersionUID = -9167354530511386533L;

  private TaskDO parentTask = null;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String title;

  private TaskStatus status = TaskStatus.N;

  private Priority priority;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String shortDescription;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  /** -&gt; Gantt */
  @Deprecated
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO,
  bridge = @FieldBridge(impl = IntegerBridge.class))
  private Integer progress;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */, store = Store.NO,
          bridge = @FieldBridge(impl = IntegerBridge.class))
  private Integer maxHours;

  /** -&gt; Gantt */
  @Deprecated
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date startDate;

  /** -&gt; Gantt */
  @Deprecated
  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date endDate;

  /** -&gt; Gantt */
  @Deprecated
  private BigDecimal duration;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date protectTimesheetsUntil;

  @IndexedEmbedded(depth = 1)
  private PFUserDO responsibleUser;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String reference;

  private TimesheetBookingStatus timesheetBookingStatus = TimesheetBookingStatus.DEFAULT;

  private String kost2BlackWhiteList;

  private boolean kost2IsBlackList;

  private boolean protectionOfPrivacy;

  @Deprecated
  private Integer oldKost2Id;

  /** -&gt; Gantt */
  @Deprecated
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String workpackageCode;

  /** -&gt; Gantt */
  @Deprecated
  private Integer ganttPredecessorOffset;

  /** -&gt; Gantt */
  @Deprecated
  private GanttRelationType ganttRelationType;

  /** -&gt; Gantt */
  @Deprecated
  private GanttObjectType ganttObjectType;

  /** -&gt; Gantt */
  @Deprecated
  private TaskDO ganttPredecessor;

  @Column(name = "description", length = DESCRIPTION_LENGTH)
  public String getDescription()
  {
    return description;
  }

  public TaskDO setDescription(final String longDescription)
  {
    this.description = longDescription;
    return this;
  }

  @Column(name = "max_hours")
  public Integer getMaxHours()
  {
    return maxHours;
  }

  public TaskDO setMaxHours(final Integer maxHours)
  {
    this.maxHours = maxHours;
    return this;
  }

  /** -&gt; Gantt */
  @Deprecated
  @Column
  public Integer getProgress()
  {
    return progress;
  }

  /** -&gt; Gantt */
  @Deprecated
  public TaskDO setProgress(final Integer progress)
  {
    this.progress = progress;
    return this;
  }

  @Column(length = TITLE_LENGTH, nullable = false)
  public String getTitle()
  {
    return title;
  }

  public TaskDO setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  /**
   * Please use getTitle() instead. getName() should only be used in Groovy scripts.
   * 
   * @return The title.
   */
  @Deprecated
  @Transient
  public String getName()
  {
    return title;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_task_id")
  public TaskDO getParentTask()
  {
    return parentTask;
  }

  @Transient
  public Integer getParentTaskId()
  {
    if (this.parentTask == null) {
      return null;
    }
    return this.parentTask.getId();
  }

  public TaskDO setParentTask(final TaskDO parentTask)
  {
    this.parentTask = parentTask;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = PRIORITY_LENGTH)
  public Priority getPriority()
  {
    return priority;
  }

  public TaskDO setPriority(final Priority priority)
  {
    this.priority = priority;
    return this;
  }

  /**
   * Zu diesem Task können keine Zeitberichte mehr eingegeben werden, die vor diesem Datum liegen (z. B. weil bis zu
   * diesem Datum die Zeitberichte bereits berechnet wurden. Nur die Buchhaltung (PF_Finance) kann noch Änderungen
   * vornehmen. Auch können diese Zeitberichte nicht mehr in der Dauer geändert oder gelöscht bzw. außerhalb des Tasks
   * verschoben werden.
   * 
   * @return
   */
  @Column(name = "protect_timesheets_until")
  public Date getProtectTimesheetsUntil()
  {
    return protectTimesheetsUntil;
  }

  public TaskDO setProtectTimesheetsUntil(final Date protectTimesheetsUntil)
  {
    this.protectTimesheetsUntil = protectTimesheetsUntil;
    return this;
  }

  @Column(name = "short_description", length = SHORT_DESCRIPTION_LENGTH)
  public String getShortDescription()
  {
    return shortDescription;
  }

  public TaskDO setShortDescription(final String shortDescription)
  {
    this.shortDescription = shortDescription;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(length = STATUS_LENGTH)
  public TaskStatus getStatus()
  {
    return status;
  }

  public TaskDO setStatus(final TaskStatus status)
  {
    this.status = status;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "responsible_user_id")
  public PFUserDO getResponsibleUser()
  {
    return responsibleUser;
  }

  public TaskDO setResponsibleUser(final PFUserDO responsibleUser)
  {
    this.responsibleUser = responsibleUser;
    return this;
  }

  @Transient
  public Integer getResponsibleUserId()
  {
    if (this.responsibleUser == null) {
      return null;
    }
    return responsibleUser.getId();
  }

  /**
   * Reference is a free use-able field, which will be inherited to all sibling tasks. The reference is exported e. g.
   * in the time sheet MS Excel export.
   */
  @Column(length = REFERENCE_LENGTH)
  public String getReference()
  {
    return reference;
  }

  public TaskDO setReference(final String reference)
  {
    this.reference = reference;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "timesheet_booking_status", length = 20, nullable = false)
  public TimesheetBookingStatus getTimesheetBookingStatus()
  {
    if (timesheetBookingStatus == null) {
      return TimesheetBookingStatus.DEFAULT;
    } else {
      return timesheetBookingStatus;
    }
  }

  public TaskDO setTimesheetBookingStatus(final TimesheetBookingStatus timesheetBookingStatus)
  {
    if (timesheetBookingStatus != null) {
      this.timesheetBookingStatus = timesheetBookingStatus;
    } else {
      this.timesheetBookingStatus = TimesheetBookingStatus.DEFAULT;
    }
    return this;
  }

  /**
   * Whether this list is a black or a white list depends on isBlackList() value. Rules:
   * <ul>
   * <li>General
   * <ul>
   * <li>Multiple entries should be separated by comma, semicolon and/or spaces.</li>
   * <li>A Kost2 matches if it ends with at least one entry of the list.
   * </ul>
   * </li>
   * <li>Examples
   * <ul>
   * <li>"02" matches 5.123.76.02</li>
   * <li>"76.02" matches 5.123.76.02</li>
   * <li>"7602" does not match 5.123.76.02</li>
   * <li>"123.76.02" does not match 5.123.76.02</li>
   * <li>"5.123.76.02" does not match 5.123.76.02</li>
   * </ul>
   * </li>
   * <li>Black list
   * <ul>
   * <li>Has only an effect if a project is assigned to the task.</li>
   * <li>"*" means, that no cost entry matches.</li>
   * <li>Every kost2 entry which matches at list one entry will be removed from the kost2 list.</li>
   * </ul>
   * </li>
   * <li>White list
   * <ul>
   * <li>If Kost2 entries are assigned (project is given), the only such entries will match, which ends with at least
   * one entry of the list.</li>
   * <li>"*" or empty string means, that all cost entry matches.</li>
   * </ul>
   * </li>
   * </ul>
   */
  @Column(name = "kost2_black_white_list", length = 1024)
  public String getKost2BlackWhiteList()
  {
    return kost2BlackWhiteList;
  }

  public TaskDO setKost2BlackWhiteList(final String kost2BlackWhiteList)
  {
    this.kost2BlackWhiteList = kost2BlackWhiteList;
    return this;
  }

  /**
   * Get the items of the kost2 black white list as string array.
   * 
   * @return The items as string array or null, if black white list is null.
   * @see StringHelper#splitAndTrim(String, String)
   */
  @Transient
  public String[] getKost2BlackWhiteItems()
  {
    return getKost2BlackWhiteItems(getKost2BlackWhiteList());
  }

  /**
   * Get the items of the kost2 black white list as string array.
   * 
   * @return The items as string array or null, if black white list is null.
   * @see StringHelper#splitAndTrim(String, String)
   */
  @Transient
  public static String[] getKost2BlackWhiteItems(final String kost2BlackWhiteList)
  {
    return StringHelper.splitAndTrim(kost2BlackWhiteList, KOST2_SEPARATOR_CHARS);
  }

  /**
   * @return True if the black-white-list should be interpreted as black list, otherwise false (default).
   */
  @Column(name = "kost2_is_black_list", nullable = false)
  public boolean isKost2IsBlackList()
  {
    return kost2IsBlackList;
  }

  public TaskDO setKost2IsBlackList(final boolean kost2IsBlackList)
  {
    this.kost2IsBlackList = kost2IsBlackList;
    return this;
  }

  @Deprecated
  @Column(name = "old_kost2_id")
  public Integer getOldKost2Id()
  {
    return oldKost2Id;
  }

  @Deprecated
  public void setOldKost2Id(final Integer oldKost2Id)
  {
    this.oldKost2Id = oldKost2Id;
  }

  /**
   * If set then normal user are not allowed to select (read) the time sheets of other users of this task and all sub
   * tasks. This is important e. g. for hiding the days of illness of an employee.
   * 
   * @return True if the flag is set.
   */
  @Column(name = "protectionOfPrivacy", nullable = false, columnDefinition = "BOOLEAN DEFAULT 'false'")
  public boolean isProtectionOfPrivacy()
  {
    return protectionOfPrivacy;
  }

  /**
   * @param protectionOfPrivacy
   * @return This for chaining.
   */
  public TaskDO setProtectionOfPrivacy(final boolean protectionOfPrivacy)
  {
    this.protectionOfPrivacy = protectionOfPrivacy;
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getStartDate()
   */
  /** -&gt; Gantt */
  @Deprecated
  @Column(name = "start_date")
  public Date getStartDate()
  {
    return startDate;
  }

  public TaskDO setStartDate(final Date startDate)
  {
    this.startDate = startDate;
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getEndDate()
   */
  /** -&gt; Gantt */
  @Deprecated
  @Column(name = "end_date")
  public Date getEndDate()
  {
    return endDate;
  }

  /** -&gt; Gantt */
  @Deprecated
  public TaskDO setEndDate(final Date endDate)
  {
    this.endDate = endDate;
    return this;
  }

  /**
   * Duration in days.
   * 
   * @see org.projectforge.business.gantt.GanttTask#getDuration()
   */
  /** -&gt; Gantt */
  @Deprecated
  @Column(name = "duration", scale = 2, precision = 10)
  public BigDecimal getDuration()
  {
    return duration;
  }

  /** -&gt; Gantt */
  @Deprecated
  public TaskDO setDuration(final BigDecimal duration)
  {
    this.duration = duration;
    return this;
  }

  /** -&gt; Gantt */
  @Column(name = "workpackage_code", length = 100)
  @Deprecated
  public String getWorkpackageCode()
  {
    return workpackageCode;
  }

  @Deprecated
  public TaskDO setWorkpackageCode(final String workpackageCode)
  {
    this.workpackageCode = workpackageCode;
    return this;
  }

  /** -&gt; Gantt */
  @Deprecated
  @Enumerated(EnumType.STRING)
  @Column(name = "gantt_type", length = 10)
  public GanttObjectType getGanttObjectType()
  {
    return ganttObjectType;
  }

  /** -&gt; Gantt */
  @Deprecated
  public TaskDO setGanttObjectType(final GanttObjectType ganttObjectType)
  {
    this.ganttObjectType = ganttObjectType;
    return this;
  }

  /** -&gt; Gantt */
  @Deprecated
  @Enumerated(EnumType.STRING)
  @Column(name = "gantt_rel_type", length = 15)
  public GanttRelationType getGanttRelationType()
  {
    return ganttRelationType;
  }

  /** -&gt; Gantt */
  @Deprecated
  public TaskDO setGanttRelationType(final GanttRelationType ganttRelationType)
  {
    this.ganttRelationType = ganttRelationType;
    return this;
  }

  /**
   * In days.
   */
  /** -&gt; Gantt */
  @Deprecated
  @Column(name = "gantt_predecessor_offset")
  public Integer getGanttPredecessorOffset()
  {
    return ganttPredecessorOffset;
  }

  /** -&gt; Gantt */
  @Deprecated
  public TaskDO setGanttPredecessorOffset(final Integer ganttPredecessorOffset)
  {
    this.ganttPredecessorOffset = ganttPredecessorOffset;
    return this;
  }

  /**
   * Please note: if you use TaskTree as cache then note, that the depend-on-task can be out-dated! Get the id of the
   * depend-on-task and get the every-time up-to-date task from the task tree by this id.
   */
  /** -&gt; Gantt */
  @Deprecated
  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, targetEntity = TaskDO.class)
  @JoinColumn(name = "gantt_predecessor_fk")
  public TaskDO getGanttPredecessor()
  {
    return ganttPredecessor;
  }

  /** -&gt; Gantt */
  @Deprecated
  public TaskDO setGanttPredecessor(final TaskDO ganttPredecessor)
  {
    this.ganttPredecessor = ganttPredecessor;
    return this;
  }

  /** -&gt; Gantt */
  @Deprecated
  @Transient
  public Integer getGanttPredecessorId()
  {
    if (this.ganttPredecessor == null) {
      return null;
    } else {
      return this.ganttPredecessor.getId();
    }
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof TaskDO) {
      final TaskDO other = (TaskDO) o;
      return ObjectUtils.equals(this.getParentTaskId(), other.getParentTaskId()) == true
          && ObjectUtils.equals(this.getTitle(), other.getTitle()) == true;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(this.getParentTaskId()).append(this.getTitle());
    return hcb.toHashCode();
  }

  @Override
  public String toString()
  {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("id", getId());
    builder.append("parentTaskId", getParentTaskId());
    builder.append("title", getTitle());
    builder.append("status", getStatus());
    builder.append("priority", getPriority());
    builder.append("progress", getProgress());
    builder.append("shortDescription", getShortDescription());
    builder.append("description", getDescription());
    builder.append("maxHours", getMaxHours());
    builder.append("startDate", getStartDate());
    builder.append("endDate", getEndDate());
    builder.append("responsibleUserId", getResponsibleUserId());
    if (this.kost2IsBlackList == true) {
      builder.append("kost2BlackWhiteList", isKost2IsBlackList());
    }
    if (StringUtils.isNotBlank(kost2BlackWhiteList) == true) {
      builder.append("kost2BlackWhiteList", getKost2BlackWhiteList());
    }
    builder.append("timesheetBookingStatus", getTimesheetBookingStatus());
    return builder.toString();
  }

  @Override
  @Transient
  public String getShortDisplayName()
  {
    return this.getTitle() + " (#" + this.getId() + ")";
  }

  /**
   * Used for building read-only clone in ScriptingTaskNode.
   * 
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException
  {
    final TaskDO clone = (TaskDO) super.clone();
    if (this.startDate != null) {
      clone.startDate = (Date) this.startDate.clone();
    }
    if (this.endDate != null) {
      clone.endDate = (Date) this.endDate.clone();
    }
    if (this.ganttPredecessor != null) {
      clone.ganttPredecessor = new TaskDO();
      clone.ganttPredecessor.setId(this.getGanttPredecessorId());
    }
    if (this.parentTask != null) {
      clone.parentTask = new TaskDO();
      clone.parentTask.setId(this.getParentTaskId());
    }
    if (this.protectTimesheetsUntil != null) {
      clone.protectTimesheetsUntil = (Date) this.protectTimesheetsUntil.clone();
    }
    if (this.responsibleUser != null) {
      clone.responsibleUser = new PFUserDO();
      clone.responsibleUser.setId(getResponsibleUserId());
    }
    return clone;
  }
}
