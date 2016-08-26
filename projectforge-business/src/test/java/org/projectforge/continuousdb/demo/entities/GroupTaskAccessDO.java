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

package org.projectforge.continuousdb.demo.entities;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Represents an access entry with the permissions of one group to one task. The persistent data object of GroupTaskAccess.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(name = "T_GROUP_TASK_ACCESS", uniqueConstraints = { @UniqueConstraint(columnNames = { "group_id", "task_id"})})
public class GroupTaskAccessDO extends DefaultBaseDO
{
  private GroupDO group;

  private TaskDO task;

  private boolean recursive = true;

  private String description;

  private Set<AccessEntryDO> accessEntries = null;

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

  public GroupTaskAccessDO setAccessEntries(final Set<AccessEntryDO> col)
  {
    this.accessEntries = col;
    return this;
  }

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE})
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

  @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE}, targetEntity = TaskDO.class)
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

  /**
   * If true then the group rights are also valid for all sub tasks. If false, then each sub task needs its own definition.
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
}
