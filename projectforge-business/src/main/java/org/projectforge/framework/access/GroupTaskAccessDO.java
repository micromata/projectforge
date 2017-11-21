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

package org.projectforge.framework.access;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.Hibernate;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.entities.GroupDO;

import de.micromata.genome.db.jpa.history.api.NoHistory;

/**
 * Represents an access entry with the permissions of one group to one task. The persistent data object of
 * GroupTaskAccess.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_GROUP_TASK_ACCESS",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "group_id", "task_id" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_group_task_access_group_id", columnList = "group_id"),
        @javax.persistence.Index(name = "idx_fk_t_group_task_access_task_id", columnList = "task_id"),
        @javax.persistence.Index(name = "idx_fk_t_group_task_access_tenant_id", columnList = "tenant_id")
    })
public class GroupTaskAccessDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -8819516962428533352L;

  @IndexedEmbedded(depth = 1)
  private GroupDO group;

  @IndexedEmbedded(depth = 1)
  private TaskDO task;

  private boolean recursive = true;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String description;

  @NoHistory
  private Set<AccessEntryDO> accessEntries = null;

  /**
   * Returns the specified access.
   * 
   * @param accessType TASKS_ACCESS, ...
   * @return The specified access or null if not found.
   */
  @Transient
  public AccessEntryDO getAccessEntry(final AccessType accessType)
  {
    if (this.accessEntries == null) {
      return null;
    }
    for (final AccessEntryDO entry : this.accessEntries) {
      if (entry.getAccessType() == accessType) {
        return entry;
      }
    }
    return null;
  }

  @Transient
  public boolean hasPermission(final AccessType accessType, final OperationType opType)
  {
    final AccessEntryDO entry = getAccessEntry(accessType);
    if (entry == null) {
      return false;
    } else {
      return entry.hasPermission(opType);
    }
  }

  /**
   * Get the history entries for this object.
   * 
   */
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
  @JoinColumn(name = "group_task_access_fk", insertable = true, updatable = true)
  public Set<AccessEntryDO> getAccessEntries()
  {
    return this.accessEntries;
  }

  @Transient
  public List<AccessEntryDO> getOrderedEntries()
  {
    final List<AccessEntryDO> list = new ArrayList<AccessEntryDO>();
    AccessEntryDO entry = getAccessEntry(AccessType.TASK_ACCESS_MANAGEMENT);
    if (entry != null) {
      list.add(entry);
    }
    entry = getAccessEntry(AccessType.TASKS);
    if (entry != null) {
      list.add(entry);
    }
    entry = getAccessEntry(AccessType.TIMESHEETS);
    if (entry != null) {
      list.add(entry);
    }
    entry = getAccessEntry(AccessType.OWN_TIMESHEETS);
    if (entry != null) {
      list.add(entry);
    }
    return list;
  }

  public GroupTaskAccessDO setAccessEntries(final Set<AccessEntryDO> col)
  {
    this.accessEntries = col;
    return this;
  }

  public GroupTaskAccessDO addAccessEntry(final AccessEntryDO entry)
  {
    if (this.accessEntries == null) {
      setAccessEntries(new HashSet<AccessEntryDO>());
    }
    this.accessEntries.add(entry);
    return this;
  }

  // @Column(name = "group_id")
  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
  @JoinColumn(name = "group_id")
  public GroupDO getGroup()
  {
    return group;
  }

  public GroupTaskAccessDO setGroup(final GroupDO group)
  {
    this.group = group;
    return this;
  }

  // @Column(name = "task_id")
  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, targetEntity = TaskDO.class)
  @JoinColumn(name = "task_id")
  public TaskDO getTask()
  {
    return task;
  }

  public GroupTaskAccessDO setTask(final TaskDO task)
  {
    this.task = task;
    return this;
  }

  public AccessEntryDO ensureAndGetAccessEntry(final AccessType accessType)
  {
    if (this.accessEntries == null) {
      setAccessEntries(new HashSet<AccessEntryDO>());
    }
    AccessEntryDO entry = getAccessEntry(accessType);
    if (entry == null) {
      entry = new AccessEntryDO(accessType);
      this.addAccessEntry(entry);
    }
    return entry;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof GroupTaskAccessDO) {
      final GroupTaskAccessDO other = (GroupTaskAccessDO) o;
      if (ObjectUtils.equals(this.getGroupId(), other.getGroupId()) == false) {
        return false;
      }
      if (ObjectUtils.equals(this.getTaskId(), other.getTaskId()) == false) {
        return false;
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(getTaskId());
    hcb.append(getGroupId());
    return hcb.toHashCode();
  }

  @Transient
  public Integer getGroupId()
  {
    if (this.group == null) {
      return null;
    }
    return this.group.getId();
  }

  @Transient
  public Integer getTaskId()
  {
    if (this.task == null) {
      return null;
    }
    return this.task.getId();
  }

  /**
   * If true then the group rights are also valid for all sub tasks. If false, then each sub task needs its own
   * definition.
   */
  @Column
  public boolean isRecursive()
  {
    return recursive;
  }

  public void setRecursive(final boolean recursive)
  {
    this.recursive = recursive;
  }

  @Column(name = "description", length = 4000)
  public String getDescription()
  {
    return description;
  }

  public GroupTaskAccessDO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  /**
   * Copies all values from the given src object excluding the values created and modified. Null values will be
   * excluded.
   * 
   * @param src
   */
  @Override
  public ModificationStatus copyValuesFrom(final BaseDO<? extends Serializable> source, final String... ignoreFields)
  {
    ModificationStatus modificationStatus = super.copyValuesFrom(source, ignoreFields);
    final GroupTaskAccessDO src = (GroupTaskAccessDO) source;
    if (src.getAccessEntries() != null) {
      for (final AccessEntryDO srcEntry : src.getAccessEntries()) {
        final AccessEntryDO destEntry = ensureAndGetAccessEntry(srcEntry.getAccessType());
        final ModificationStatus st = destEntry.copyValuesFrom(srcEntry);
        modificationStatus = getModificationStatus(modificationStatus, st);
      }
      final Iterator<AccessEntryDO> iterator = getAccessEntries().iterator();
      while (iterator.hasNext()) {
        final AccessEntryDO destEntry = iterator.next();
        if (src.getAccessEntry(destEntry.getAccessType()) == null) {
          iterator.remove();
        }
      }
    }
    return modificationStatus;
  }

  @Override
  public String toString()
  {
    final ToStringBuilder tos = new ToStringBuilder(this);
    tos.append("id", getId());
    tos.append("task", getTaskId());
    tos.append("group", getGroupId());
    if (Hibernate.isInitialized(this.accessEntries) == true) {
      tos.append("entries", this.accessEntries);
    } else {
      tos.append("entries", "LazyCollection");
    }
    return tos.toString();
  }

  public AccessEntryDO ensureAndGetTasksEntry()
  {
    return ensureAndGetAccessEntry(AccessType.TASKS);
  }

  public AccessEntryDO ensureAndGetAccessManagementEntry()
  {
    return ensureAndGetAccessEntry(AccessType.TASK_ACCESS_MANAGEMENT);
  }

  public AccessEntryDO ensureAndGetTimesheetsEntry()
  {
    return ensureAndGetAccessEntry(AccessType.TIMESHEETS);
  }

  public AccessEntryDO ensureAndGetOwnTimesheetsEntry()
  {
    return ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS);
  }

  /**
   * This template clears all access entries.
   */
  public void clear()
  {
    ensureAndGetAccessManagementEntry().setAccess(false, false, false, false);
    ensureAndGetTasksEntry().setAccess(false, false, false, false);
    ensureAndGetOwnTimesheetsEntry().setAccess(false, false, false, false);
    ensureAndGetTimesheetsEntry().setAccess(false, false, false, false);
  }

  /**
   * This template is used as default for guests (they have only read access to tasks).
   */
  public void guest()
  {
    ensureAndGetAccessManagementEntry().setAccess(false, false, false, false);
    ensureAndGetTasksEntry().setAccess(true, false, false, false);
    ensureAndGetOwnTimesheetsEntry().setAccess(false, false, false, false);
    ensureAndGetTimesheetsEntry().setAccess(false, false, false, false);
  }

  /**
   * This template is used as default for employees. The have read access to the access management, full access to tasks
   * and own time sheets and only read-access to foreign time sheets.
   */
  public void employee()
  {
    ensureAndGetAccessManagementEntry().setAccess(true, false, false, false);
    ensureAndGetTasksEntry().setAccess(true, true, true, true);
    ensureAndGetOwnTimesheetsEntry().setAccess(true, true, true, true);
    ensureAndGetTimesheetsEntry().setAccess(true, false, false, false);
  }

  /**
   * This template is used as default for project managers. Same as employee but with full read-write-access to foreign
   * time-sheets.
   */
  public void leader()
  {
    ensureAndGetAccessManagementEntry().setAccess(true, false, false, false);
    ensureAndGetTasksEntry().setAccess(true, true, true, true);
    ensureAndGetOwnTimesheetsEntry().setAccess(true, true, true, true);
    ensureAndGetTimesheetsEntry().setAccess(true, true, true, true);
  }

  /**
   * This template is used as default for admins. Full access to access management, task, own and foreign time-sheets.
   */
  public void administrator()
  {
    ensureAndGetAccessManagementEntry().setAccess(true, true, true, true);
    ensureAndGetTasksEntry().setAccess(true, true, true, true);
    ensureAndGetOwnTimesheetsEntry().setAccess(true, true, true, true);
    ensureAndGetTimesheetsEntry().setAccess(true, true, true, true);
  }
}
