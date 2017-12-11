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

package org.projectforge.business.gantt;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.utils.NumberHelper;

public class GanttTaskImpl implements GanttTask, Serializable
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GanttTaskImpl.class);

  private static final long serialVersionUID = -2948691380516113581L;

  /**
   * Maximum supported depth (independent from any display algorithm). This value is only used for the detection of cyclic references!
   */
  public static int MAX_DEPTH = 50;

  private Integer id;

  private Integer predecessorOffset;

  private GanttRelationType relationType;

  private GanttTask predecessor;

  private String description;

  private BigDecimal duration;

  private Date endDate;

  private Date startDate;

  private Integer progress;

  private String title;

  private GanttObjectType type;

  private String workpackageCode;

  private List<GanttTask> children;

  private boolean visible = false;

  private transient Date calculatedStartDate;

  private transient boolean startDateCalculated;

  private transient Date calculatedEndDate;

  private transient boolean endDateCalculated;

  public GanttTaskImpl()
  {
  }

  public GanttTaskImpl(final Integer id)
  {
    this.id = id;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getId()
   */
  @Override
  public Serializable getId()
  {
    return this.id;
  }

  @Override
  public GanttTaskImpl setId(final Serializable id)
  {
    this.id = (Integer) id;
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getPredecessorOffset()
   */
  @Override
  public Integer getPredecessorOffset()
  {
    return predecessorOffset;
  }

  @Override
  public GanttTaskImpl setPredecessorOffset(final Integer predecessorOffset)
  {
    this.predecessorOffset = predecessorOffset;
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getRelationType()
   */
  @Override
  public GanttRelationType getRelationType()
  {
    return relationType;
  }

  @Override
  public GanttTaskImpl setRelationType(GanttRelationType relationType)
  {
    this.relationType = relationType;
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getPredecessor()
   */
  @Override
  public GanttTask getPredecessor()
  {
    return predecessor;
  }

  @Override
  public GanttTaskImpl setPredecessor(GanttTask predecessor)
  {
    this.predecessor = predecessor;
    return this;
  }

  @Override
  public Serializable getPredecessorId()
  {
    if (this.predecessor == null) {
      return null;
    }
    return this.predecessor.getId();
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getDescription()
   */
  @Override
  public String getDescription()
  {
    return description;
  }

  public GanttTaskImpl setDescription(String description)
  {
    this.description = description;
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getDuration()
   */
  @Override
  public BigDecimal getDuration()
  {
    return duration;
  }

  @Override
  public GanttTaskImpl setDuration(final BigDecimal duration)
  {
    this.duration = duration;
    this.startDateCalculated = this.endDateCalculated = false; // Force recalculation (also taking the children into account)
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getEndDate()
   */
  @Override
  public Date getEndDate()
  {
    return endDate;
  }

  @Override
  public GanttTaskImpl setEndDate(Date endDate)
  {
    this.endDate = endDate;
    this.startDateCalculated = this.endDateCalculated = false; // Force recalculation (also taking the children into account)
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getStartDate()
   */
  @Override
  public Date getStartDate()
  {
    return startDate;
  }

  @Override
  public GanttTaskImpl setStartDate(Date startDate)
  {
    this.startDate = startDate;
    this.startDateCalculated = this.endDateCalculated = false; // Force recalculation (also taking the children into account)
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getTitle()
   */
  @Override
  public String getTitle()
  {
    return title;
  }

  @Override
  public GanttTaskImpl setTitle(String title)
  {
    this.title = title;
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getType()
   */
  @Override
  public GanttObjectType getType()
  {
    return type;
  }

  @Override
  public GanttTaskImpl setType(GanttObjectType type)
  {
    this.type = type;
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getWorkpackageCode()
   */
  @Override
  public String getWorkpackageCode()
  {
    return workpackageCode;
  }

  public GanttTaskImpl setWorkpackageCode(String workpackageCode)
  {
    this.workpackageCode = workpackageCode;
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getChildren()
   */
  @Override
  public List<GanttTask> getChildren()
  {
    return this.children;
  }

  /**
   * Sort all children by calculated start date.
   * @see org.projectforge.business.gantt.GanttTask#sortChildren()
   */
  @Override
  public void sortChildren()
  {
    if (this.children == null) {
      return;
    }
    Collections.sort(this.children, GanttUtils.GANTT_OBJECT_COMPARATOR);
    for (final GanttTask child : this.children) {
      child.sortChildren();
    }
  }

  /**
   * 
   * @see org.projectforge.business.gantt.GanttTask#addChild(org.projectforge.business.gantt.GanttTask)
   */
  @Override
  public GanttTaskImpl addChild(final GanttTask child)
  {
    if (this.children == null) {
      this.children = new ArrayList<GanttTask>();
    }
    this.children.add(child);
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#removeChild(org.projectforge.business.gantt.GanttTask)
   */
  @Override
  public void removeChild(final GanttTask ganttObject)
  {
    if (this.children == null) {
      log.error("Can't remove child object because current Gantt activity has no children: " + this);
    } else if (this.children.remove(ganttObject) == false) {
      log.error("Can't remove child object: " + ganttObject + " because it's not a child of the given activity: " + this);
    }
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#hasDuration()
   */
  @Override
  public boolean hasDuration()
  {
    if (getCalculatedStartDate() != null && getCalculatedEndDate() != null) {
      final DateHolder dh = new DateHolder(this.calculatedStartDate);
      return dh.isSameDay(getCalculatedEndDate()) == false;
    }
    return !NumberHelper.isZeroOrNull(this.duration);
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getCalculatedStartDate()
   */
  @Override
  public Date getCalculatedStartDate()
  {
    if (startDateCalculated == false) {
      calculatedStartDate = GanttUtils.getCalculatedStartDate(this);
      startDateCalculated = true;
    }
    return this.calculatedStartDate;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#setCalculatedStartDate(java.util.Date)
   */
  @Override
  public GanttTaskImpl setCalculatedStartDate(Date calculatedStartDate)
  {
    this.calculatedStartDate = calculatedStartDate;
    return this;
  }

  /**
   * 
   * @see org.projectforge.business.gantt.GanttTask#getCalculatedEndDate()
   */
  @Override
  public Date getCalculatedEndDate()
  {
    if (endDateCalculated == false) {
      calculatedEndDate = GanttUtils.getCalculatedEndDate(this);
      endDateCalculated = true;
    }
    return this.calculatedEndDate;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#setCalculatedEndDate(java.util.Date)
   */
  @Override
  public GanttTaskImpl setCalculatedEndDate(Date calculatedEndDate)
  {
    this.calculatedEndDate = calculatedEndDate;
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#isStartDateCalculated()
   */
  @Override
  public boolean isStartDateCalculated()
  {
    return this.startDateCalculated == true;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#setStartDateCalculated(boolean)
   */
  @Override
  public GanttTaskImpl setStartDateCalculated(boolean startDateCalculated)
  {
    this.startDateCalculated = startDateCalculated;
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#isEndDateCalculated()
   */
  @Override
  public boolean isEndDateCalculated()
  {
    return this.endDateCalculated;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#setEndDateCalculated(boolean)
   */
  @Override
  public GanttTaskImpl setEndDateCalculated(boolean isEndDateCalculated)
  {
    this.endDateCalculated = isEndDateCalculated;
    return this;
  }

  /**
   * Default: true.
   * @see org.projectforge.business.gantt.GanttTask#isVisible()
   */
  @Override
  public boolean isVisible()
  {
    return this.visible;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#setVisible(boolean)
   */
  @Override
  public GanttTask setVisible(boolean visible)
  {
    this.visible = visible;
    return this;
  }

  /**
   * Sets task visibility and the visibility of all descendants to false.
   */
  public GanttTaskImpl setInvisible()
  {
    this.setVisible(false);
    if (this.children != null) {
      for (GanttTask child : children) {
        ((GanttTaskImpl) child).setInvisible();
      }
    }
    return this;
  }

  /**
   * @see org.projectforge.business.gantt.GanttTask#getProgress()
   */
  @Override
  public Integer getProgress()
  {
    return progress;
  }

  @Override
  public GanttTaskImpl setProgress(Integer progress)
  {
    this.progress = progress;
    return this;
  }

  /**
   * Sets the calculated start and end dates (including the children) to null (recalculation is forced when calculated time will be needed). <br/>
   * Please note: The calculation is only done at the first usage.
   */
  @Override
  public GanttTaskImpl recalculate()
  {
    this.calculatedStartDate = this.calculatedEndDate = null;
    this.startDateCalculated = this.endDateCalculated = false;
    if (this.children != null) {
      for (final GanttTask child : this.children) {
        ((GanttTaskImpl) child).recalculate();
      }
    }
    return this;
  }

  /**
   * Traverses through the child trees.
   * @return true, if no cyclic references are found (everything seems to be OK), otherwise false.
   */
  public boolean checkCyclicReferences()
  {
    return checkCyclicReferences(0);
  }

  private boolean checkCyclicReferences(final int depth)
  {
    if (depth > MAX_DEPTH) {
      // Maximum of allowed depth exceeded.
      return false;
    }
    if (this.children == null) {
      return true;
    }
    for (final GanttTask child : this.children) {
      if (((GanttTaskImpl) child).checkCyclicReferences(depth + 1) == false) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString()
  {
    final ToStringBuilder tos = new ToStringBuilder(this);
    tos.append("id", getId());
    tos.append("title", getTitle());
    if (getChildren() != null) {
      tos.append("children", getChildren());
    }
    return tos.toString();
  }

  @Override
  public GanttTask findBy(final Matcher<GanttTask> matcher, final Object expression)
  {
    if (matcher.match(this, expression) == true) {
      return this;
    }
    if (this.children != null) {
      for (final GanttTask child : this.children) {
        final GanttTask found = child.findBy(matcher, expression);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  @Override
  public GanttTask findById(final Serializable id)
  {
    return findBy(new Matcher<GanttTask>() {
      @Override
      public boolean match(GanttTask object, Object expression)
      {
        return (object.getId() != null && object.getId().equals(expression) == true);
      }
    }, id);
  }

  @Override
  public GanttTask findByTitle(final String title)
  {
    return findBy(new Matcher<GanttTask>() {
      @Override
      public boolean match(GanttTask object, Object expression)
      {
        return (StringUtils.equals(object.getTitle(), (String) expression) == true);
      }
    }, title);
  }

  @Override
  public GanttTask findByWorkpackageCode(String workpackageCode)
  {
    return findBy(new Matcher<GanttTask>() {
      @Override
      public boolean match(GanttTask object, Object expression)
      {
        return (StringUtils.equals(object.getWorkpackageCode(), (String) expression) == true);
      }
    }, workpackageCode);
  }

  @Override
  public GanttTask findParent(final Serializable id)
  {
    return findBy(new Matcher<GanttTask>() {
      @Override
      public boolean match(GanttTask object, Object expression)
      {
        if (CollectionUtils.isEmpty(object.getChildren()) == true) {
          return false;
        }
        for (final GanttTask child : object.getChildren()) {
          if (child.getId() != null && child.getId().equals(expression) == true) {
            return true;
          }
        }
        return false;
      }
    }, id);
  }

  /**
   * Get the next free id for new Gantt tasks to insert (starting with -1). Positive values are reserved for Gantt tasks related to tasks
   * (of ProjectForge's TaskTree) (the id is equals to the task id).
   * @return
   */
  public int getNextId()
  {
    final Integer id = getNextId(this, -1);
    return id != null ? id : -1;
  }

  private Integer getNextId(final GanttTask node, final int id)
  {
    Integer result = null;
    if (node == null) {
      return null;
    }
    if (node.getId() != null && ((Integer) node.getId()) <= id) {
      result = ((Integer) node.getId()) - 1;
    }
    final List<GanttTask> children = node.getChildren();
    if (children == null) {
      return result;
    }
    for (final GanttTask child : children) {
      final Integer i = getNextId(child, id);
      if (i == null) {
        continue;
      }
      if (result != null && i <= result || result == null && i <= id) {
        result = i;
      }
    }
    return result;
  }
}
