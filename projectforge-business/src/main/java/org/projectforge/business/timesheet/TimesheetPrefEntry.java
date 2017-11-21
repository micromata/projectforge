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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@XStreamAlias("timesheetPrefEntry")
public class TimesheetPrefEntry
{
  @XStreamAsAttribute
  private Integer taskId;

  @XStreamAsAttribute
  private Integer userId;

  @XStreamAsAttribute
  private Integer kost2Id;

  @XStreamAsAttribute
  private String location;

  @XStreamAsAttribute
  private String description;

  public TimesheetPrefEntry()
  {
  }
  
  public TimesheetPrefEntry(TimesheetDO timesheet) {
    this.taskId = timesheet.getTaskId();
    this.userId = timesheet.getUserId();
    this.kost2Id = timesheet.getKost2Id();
    this.location = timesheet.getLocation();
    this.description = timesheet.getDescription();
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  public String getLocation()
  {
    return location;
  }

  public void setLocation(String location)
  {
    this.location = location;
  }

  public Integer getUserId()
  {
    return userId;
  }

  public void setUserId(Integer userId)
  {
    this.userId = userId;
  }

  public Integer getTaskId()
  {
    return taskId;
  }

  public void setTaskId(Integer taskId)
  {
    this.taskId = taskId;
  }
  
  public Integer getKost2Id()
  {
    return kost2Id;
  }
  
  public void setKost2Id(Integer kost2Id)
  {
    this.kost2Id = kost2Id;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof TimesheetPrefEntry) {
      TimesheetPrefEntry other = (TimesheetPrefEntry)obj;
      if (ObjectUtils.equals(this.taskId, other.taskId) == false) {
        return false;
      }
      if (ObjectUtils.equals(this.kost2Id, other.kost2Id) == false) {
        return false;
      }
      if (ObjectUtils.equals(this.location, other.location) == false) {
        return false;
      }
      if (ObjectUtils.equals(this.description, other.description) == false) {
        return false;
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(this.taskId);
    hcb.append(this.kost2Id);
    hcb.append(this.location);
    hcb.append(this.description);
    return hcb.toHashCode();
  }
}
