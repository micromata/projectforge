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

package org.projectforge.business.gantt;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Represents a gantt object such as a task and a milestone.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public interface GanttTask
{
  /**
   * Identifier, if needed by the implementation (such as task id).
   */
  public Serializable getId();
  
  public GanttTask setId(final Serializable id);

  public GanttTask findBy(final Matcher<GanttTask> matcher, final Object expression);

  /**
   * Helper methods using findBy with the matcher testing the id.
   * @param id The id of the Gantt object to find.
   */
  public GanttTask findById(final Serializable id);

  /**
   * Helper methods using findBy with the matcher testing the work package code.
   * @param workpackageId The code of the work package of the Gantt object to find.
   */
  public GanttTask findByWorkpackageCode(final String workpackageCode);

  /**
   * Helper methods using findBy with the matcher testing the title.
   * @param title The title of the Gantt object to find.
   */
  public GanttTask findByTitle(final String title);

  /**
   * Search the given task tree (root is given by this) for the task which is the parent task of the task with the given id.
   * @param id
   * @return
   */
  public GanttTask findParent(final Serializable id);

  /**
   * The list of child objects if exist.
   */
  public List<GanttTask> getChildren();

  /**
   * Milestone or task?
   */
  public GanttObjectType getType();

  /**
   * Fluent.
   * @param startDate
   * @return this
   */
  public GanttTask setType(GanttObjectType ganttObjectType);

  /**
   * Required if this object doesn't depend on another Gantt object.
   */
  public Date getStartDate();

  /**
   * Fluent.
   * @param startDate
   * @return this
   */
  public GanttTask setStartDate(Date startDate);

  /**
   * @return True, if the calculated start and end dates differ or if both not given if a duration not equal 0 is set.
   */
  public boolean hasDuration();

  /**
   * @return The given start date or if not exist the calculated start date. If no calculation is possible the now is assumed.
   */
  public Date getCalculatedStartDate();
  
  public GanttTask setCalculatedStartDate(Date calculatedStartDate);

  /**
   * Required if this object is a task and has no duration.
   */
  public Date getEndDate();

  public GanttTask setCalculatedEndDate(Date calculatedEndDate);
  
  public boolean isStartDateCalculated();

  public GanttTask setStartDateCalculated(boolean isStartDateCalculated);
  
  public boolean isEndDateCalculated();

  public GanttTask setEndDateCalculated(boolean isEndDateCalculated);
  
  /**
   * Fluent.
   * @param startDate
   * @return this
   */
  public GanttTask setEndDate(Date endDate);

  /**
   * @return The given end date or if not exist the calculated end date.
   */
  public Date getCalculatedEndDate();

  /**
   * Required if this task has no end date.
   */
  public BigDecimal getDuration();

  /**
   * Fluent.
   * @param duration
   * @return this
   */
  public GanttTask setDuration(BigDecimal duration);

  /**
   * Completion of the task in percentage.
   */
  public Integer getProgress();

  /**
   * Fluent.
   * @param progress
   * @return this
   */
  public GanttTask setProgress(Integer progress);

  /**
   * Optional, short code to display.
   */
  public String getWorkpackageCode();

  /**
   * The title to display.
   */
  public String getTitle();

  public GanttTask setTitle(final String title);

  /**
   * Optional.
   */
  public String getDescription();

  /**
   * Default is FINISH-START.
   */
  public GanttRelationType getRelationType();

  /**
   * Fluent.
   * @param relationType
   * @return this.
   */
  public GanttTask setRelationType(GanttRelationType relationType);

  /**
   * Optional offset in days after predecessor.
   */
  public Integer getPredecessorOffset();

  /**
   * Fluent.
   * @param ganttPredecessorOffset
   * @return this
   */
  public GanttTask setPredecessorOffset(Integer ganttPredecessorOffset);

  /**
   * Sorts all children (recursive) e. g. by calculated start date.
   */
  public void sortChildren();

  /**
   * Optional, this object depends on the beginning or ending of another Gantt object.
   */
  public GanttTask getPredecessor();

  /**
   * Fluent.
   * @param predecessor
   * @return this
   */
  public GanttTask setPredecessor(GanttTask predecessor);

  /**
   * NPE safe method for getting id of the predecessor if exist, otherwise null.
   */
  public Serializable getPredecessorId();

  /**
   * @return true if this object should be visible. False if this object should be suppressed from output.
   */
  public boolean isVisible();

  /**
   * Sets the visibility of this Gantt object.
   */
  public GanttTask setVisible(boolean b);

  /**
   * Recalculate all dates.
   */
  public GanttTask recalculate();

  /**
   * Adds a child object.
   */
  public GanttTask addChild(GanttTask ganttObject);
  
  /**
   * Removes a child object.
   * @param ganttObject Object to remove.
   */
  public void removeChild(GanttTask ganttObject);
}
