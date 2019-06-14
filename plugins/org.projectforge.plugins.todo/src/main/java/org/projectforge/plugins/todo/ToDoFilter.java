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

package org.projectforge.plugins.todo;

import java.io.Serializable;

import org.projectforge.framework.persistence.api.BaseSearchFilter;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ToDoFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = -2799448444375717414L;

  private boolean opened, reopened, inprogress, closed, postponed, onlyRecent;

  private Integer reporterId, assigneeId;

  private Integer taskId;
  
  public ToDoFilter()
  {
  }

  public ToDoFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  public Integer getReporterId()
  {
    return reporterId;
  }

  public void setReporterId(Integer reporterId)
  {
    this.reporterId = reporterId;
  }

  public Integer getAssigneeId()
  {
    return assigneeId;
  }

  public void setAssigneeId(Integer assigneeId)
  {
    this.assigneeId = assigneeId;
  }

  public Integer getTaskId()
  {
    return taskId;
  }

  public void setTaskId(Integer taskId)
  {
    this.taskId = taskId;
  }

  public boolean isOpened()
  {
    return opened;
  }

  public void setOpened(boolean opened)
  {
    this.opened = opened;
  }

  public boolean isReopened()
  {
    return reopened;
  }

  public void setReopened(boolean reopened)
  {
    this.reopened = reopened;
  }

  public boolean isInprogress()
  {
    return inprogress;
  }

  public void setInprogress(boolean inprogress)
  {
    this.inprogress = inprogress;
  }

  public boolean isClosed()
  {
    return closed;
  }

  public void setClosed(boolean closed)
  {
    this.closed = closed;
  }

  public boolean isPostponed()
  {
    return postponed;
  }

  public void setPostponed(boolean postponed)
  {
    this.postponed = postponed;
  }
  
  public boolean isOnlyRecent()
  {
    return onlyRecent;
  }
  
  public void setOnlyRecent(boolean onlyRecent)
  {
    this.onlyRecent = onlyRecent;
  }
}
