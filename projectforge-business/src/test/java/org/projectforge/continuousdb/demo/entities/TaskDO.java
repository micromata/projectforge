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

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Table(name = "T_TASK", uniqueConstraints = { @UniqueConstraint(columnNames = { "parent_task_id", "title"})})
public class TaskDO extends DefaultBaseDO
{
  private TaskDO parentTask = null;

  private String title;

  private String description;
  
  private BigDecimal duration;
  
  private Integer maxHours;

  private UserDO responsibleUser;

  @Column(name = "description", length = 1000)
  public String getDescription()
  {
    return description;
  }

  public TaskDO setDescription(final String longDescription)
  {
    this.description = longDescription;
    return this;
  }

  @Column(length = 100, nullable = false)
  public String getTitle()
  {
    return title;
  }

  public TaskDO setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_task_id")
  public TaskDO getParentTask()
  {
    return parentTask;
  }

  public TaskDO setParentTask(final TaskDO parentTask)
  {
    this.parentTask = parentTask;
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

  @Column(name = "duration", scale = 2, precision = 10)
  public BigDecimal getDuration()
  {
    return duration;
  }

  public TaskDO setDuration(final BigDecimal duration)
  {
    this.duration = duration;
    return this;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "responsible_user_id")
  public UserDO getResponsibleUser()
  {
    return responsibleUser;
  }

  public TaskDO setResponsibleUser(final UserDO responsibleUser)
  {
    this.responsibleUser = responsibleUser;
    return this;
  }
}
