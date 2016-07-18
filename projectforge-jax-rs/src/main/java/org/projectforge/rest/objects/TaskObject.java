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

import java.util.Collection;
import java.util.TreeSet;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.projectforge.common.i18n.Priority;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.common.task.TimesheetBookingStatus;
import org.projectforge.rest.AbstractBaseObject;

/**
 * For documentation please refer the ProjectForge-API: TaskDO object.
 * TaskDO object for REST,
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TaskObject extends AbstractBaseObject implements Comparable<TaskObject>
{
  private Collection<TaskObject> children;

  private  Integer parentTaskId;

  private  String description, title, shortDescription, reference;

  private  Integer maxHours;

  private  Priority priority;

  private  TaskStatus status;

  private TimesheetBookingStatus timesheetBookingStatus;

  private boolean bookableForTimesheets;

  public TaskObject()
  {
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription(final String description)
  {
    this.description = description;
  }

  public Integer getMaxHours()
  {
    return maxHours;
  }

  public void setMaxHours(final Integer maxHours)
  {
    this.maxHours = maxHours;
  }

  public String getTitle()
  {
    return title;
  }

  public void setTitle(final String title)
  {
    this.title = title;
  }

  public Integer getParentTaskId()
  {
    return parentTaskId;
  }

  public void setParentTaskId(final Integer parentTaskId)
  {
    this.parentTaskId = parentTaskId;
  }

  public String getShortDescription()
  {
    return shortDescription;
  }

  public void setShortDescription(final String shortDescription)
  {
    this.shortDescription = shortDescription;
  }

  /**
   * @return the priority
   */
  public Priority getPriority()
  {
    return priority;
  }

  public void setPriority(final Priority priority)
  {
    this.priority = priority;
  }

  public TaskStatus getStatus()
  {
    return status;
  }

  public void setStatus(final TaskStatus status)
  {
    this.status = status;
  }

  public String getReference()
  {
    return reference;
  }

  public void setReference(final String reference)
  {
    this.reference = reference;
  }

  public TimesheetBookingStatus getTimesheetBookingStatus()
  {
    return timesheetBookingStatus;
  }

  public void setTimesheetBookingStatus(final TimesheetBookingStatus timesheetBookingStatus)
  {
    this.timesheetBookingStatus = timesheetBookingStatus;
  }

  /**
   * @param bookableForTimesheets the bookableForTimesheets to set
   * @return this for chaining.
   */
  public void setBookableForTimesheets(final boolean bookableForTimesheets)
  {
    this.bookableForTimesheets = bookableForTimesheets;
  }

  /**
   * @return the bookableForTimesheets
   */
  public boolean isBookableForTimesheets()
  {
    return bookableForTimesheets;
  }

  /**
   * @return the children
   */
  public Collection<TaskObject> getChildren()
  {
    return children;
  }

  public void add(final TaskObject child)
  {
    if (this.children == null) {
      this.children = new TreeSet<TaskObject>();
    }
    this.children.add(child);
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    return getId().hashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj)
  {
    if (obj instanceof TaskObject == false) {
      return false;
    }
    return this.hashCode() == obj.hashCode();
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return new ReflectionToStringBuilder(this).toString();
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final TaskObject o)
  {
    final String title1 = this.title != null ? this.title.toLowerCase() : "";
    final String title2 = o.title != null ? o.title.toLowerCase() : "";
    return title1.compareTo(title2);
  }
}
