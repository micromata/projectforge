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

package org.projectforge.rest.objects;

import java.lang.reflect.Field;
import java.sql.Timestamp;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.projectforge.rest.AbstractBaseObject;

/**
 * For documentation please refer the ProjectForge-API: TimesheetDO object.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TimesheetObject extends AbstractBaseObject
{
  private TaskObject task;

  private UserObject user;

  private String timeZone;

  private Timestamp startTime;

  private Timestamp stopTime;

  private String location;

  private String description;

  private Cost2Object cost2;

  public TimesheetObject()
  {
  }

  public TaskObject getTask()
  {
    return task;
  }

  public void setTask(final TaskObject task)
  {
    this.task = task;
  }

  public UserObject getUser()
  {
    return user;
  }

  public void setUser(final UserObject user)
  {
    this.user = user;
  }

  public String getTimeZone()
  {
    return timeZone;
  }

  public void setTimeZone(final String timeZone)
  {
    this.timeZone = timeZone;
  }

  public Timestamp getStartTime()
  {
    return startTime;
  }

  public void setStartTime(final Timestamp startTime)
  {
    this.startTime = startTime;
  }

  public Timestamp getStopTime()
  {
    return stopTime;
  }

  public void setStopTime(final Timestamp stopTime)
  {
    this.stopTime = stopTime;
  }

  public String getLocation()
  {
    return location;
  }

  public void setLocation(final String location)
  {
    this.location = location;
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription(final String description)
  {
    this.description = description;
  }

  public Cost2Object getCost2()
  {
    return cost2;
  }

  public void setCost2(final Cost2Object cost2)
  {
    this.cost2 = cost2;
  }

  @Override
  public String toString()
  {
    return new ReflectionToStringBuilder(this) {
      @Override
      protected boolean accept(final Field f)
      {
        return super.accept(f) && !f.getName().equals("authenticationToken");
      }
    }.toString();
  }
}
