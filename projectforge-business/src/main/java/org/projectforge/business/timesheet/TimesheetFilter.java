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

import java.io.Serializable;
import java.util.Date;

import org.projectforge.business.task.TaskDependentFilter;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.time.TimePeriod;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TimesheetFilter extends BaseSearchFilter implements Serializable, TaskDependentFilter
{
  private static final long serialVersionUID = -1449906331186225597L;

  private TimePeriod timePeriod;

  private Integer userId;

  private Integer taskId;

  private boolean marked;

  @PropertyInfo(i18nKey = "longFormat")
  private boolean longFormat = false;

  @PropertyInfo(i18nKey = "recursive")
  private boolean recursive = true;

  private boolean onlyBillable = false;

  private OrderDirection orderType = OrderDirection.DESC;

  public TimesheetFilter()
  {
  }

  public TimesheetFilter(final BaseSearchFilter filter)
  {
    super(filter);

    if (filter instanceof TimesheetFilter) {
      TimesheetFilter tf = (TimesheetFilter) filter;
      this.timePeriod = tf.timePeriod;
      this.userId = tf.userId;
      this.taskId = tf.taskId;
      this.marked = tf.marked;
      this.longFormat = tf.longFormat;
      this.recursive = tf.recursive;
      this.onlyBillable = tf.onlyBillable;
      this.orderType = tf.orderType;
    }
  }

  @Override
  public Integer getTaskId()
  {
    return taskId;
  }

  @Override
  public void setTaskId(final Integer taskId)
  {
    this.taskId = taskId;
  }

  public Integer getUserId()
  {
    return userId;
  }

  public void setUserId(final Integer userId)
  {
    this.userId = userId;
  }

  /**
   * @return the startTime
   */
  public Date getStartTime()
  {
    return getTimePeriod().getFromDate();
  }

  /**
   * @param startTime the startTime to set
   */
  public void setStartTime(final Date startTime)
  {
    getTimePeriod().setFromDate(startTime);
  }

  /**
   * @return the stopTime
   */
  public Date getStopTime()
  {
    return getTimePeriod().getToDate();
  }

  /**
   * @param stopTime the stopTime to set
   */
  public void setStopTime(final Date stopTime)
  {
    getTimePeriod().setToDate(stopTime);
  }

  /**
   * Gets start and stop time from timePeriod.
   *
   * @param timePeriod
   */
  public void setTimePeriod(final TimePeriod timePeriod)
  {
    setStartTime(timePeriod.getFromDate());
    setStopTime(timePeriod.getToDate());
  }

  /**
   * Is this time sheet marked? (Currently marked time sheets are time sheets with time period overlap (collision) with another time sheet
   * of the same user.)
   *
   * @return
   */
  public boolean isMarked()
  {
    return marked;
  }

  public void setMarked(final boolean marked)
  {
    this.marked = marked;
  }

  /**
   * Show description abbreviated or in long format.
   *
   * @return
   */
  public boolean isLongFormat()
  {
    return longFormat;
  }

  public void setLongFormat(final boolean longFormat)
  {
    this.longFormat = longFormat;
  }

  /**
   * If recursive flag is false then only the time sheets of the chosen task are selected, otherwise the time sheets of the chosen task
   * including all sub task are selected.
   *
   * @return
   */
  public boolean isRecursive()
  {
    return recursive;
  }

  public void setRecursive(final boolean recursive)
  {
    this.recursive = recursive;
  }

  /**
   * If only billable flag is true then only the time sheets with billable kost2 elements are selected.
   *
   * @return
   */
  public boolean isOnlyBillable()
  {
    return onlyBillable;
  }

  public void setOnlyBillable(final boolean onlyBillable)
  {
    this.onlyBillable = onlyBillable;
  }

  /**
   * Should the result set ordered descendant (default)?
   *
   * @return
   */
  public OrderDirection getOrderType()
  {
    return orderType;
  }

  public void setOrderType(final OrderDirection orderType)
  {
    this.orderType = orderType;
  }

  private TimePeriod getTimePeriod()
  {
    if (timePeriod == null) {
      timePeriod = new TimePeriod();
    }
    return timePeriod;
  }
}
