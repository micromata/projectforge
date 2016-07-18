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

import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;

@XmlObject(alias = "ganttChartStyle")
public class GanttChartStyle implements Serializable
{
  private static final long serialVersionUID = 314444138347629986L;

  static final int SUMMARY_ARROW_SIZE = 6;

  static final int HEAD_HEIGHT = 52;

  public static final double DEFAULT_WORK_PACKAGE_LABEL_WIDTH = 0.0;

  public static final double DEFAULT_TOTAL_LABEL_WIDTH = 265.0;

  public static final double DEFAULT_ARROW_MIN_X_DIST = 5.0;

  public static final double DEFAULT_ARROW_SIZE = 3.0;

  public static final double DEFAULT_ACTIVITY_HEIGHT = 10.0;

  public static final double DEFAULT_Y_SCALE = 20.0;

  public static final int DEFAULT_X_LABELS_SCALE = -1;

  public static final int DEFAULT_X_TICKS_SCALE = -1;

  public static final int DEFAULT_X_TICKS_SCALE_2 = -1;

  public static final int DEFAULT_X_GRID_SCALE = -1;

  public static final int DEFAULT_WIDTH = 800;

  @XmlField(defaultDoubleValue = DEFAULT_WORK_PACKAGE_LABEL_WIDTH)
  private double workPackageLabelWidth = DEFAULT_WORK_PACKAGE_LABEL_WIDTH;

  @XmlField(defaultDoubleValue = DEFAULT_TOTAL_LABEL_WIDTH)
  private double totalLabelWidth = DEFAULT_TOTAL_LABEL_WIDTH;

  @XmlField(defaultDoubleValue = DEFAULT_ARROW_MIN_X_DIST)
  private double arrowMinXDist = DEFAULT_ARROW_MIN_X_DIST;

  @XmlField(defaultDoubleValue = DEFAULT_ARROW_SIZE)
  private double arrowSize = DEFAULT_ARROW_SIZE;

  @XmlField(defaultDoubleValue = DEFAULT_ACTIVITY_HEIGHT)
  private double activityHeight = DEFAULT_ACTIVITY_HEIGHT;

  @XmlField(defaultDoubleValue = DEFAULT_Y_SCALE)
  private double yScale = DEFAULT_Y_SCALE;

  private String xLabel;

  @XmlField(defaultStringValue = "AUTO")
  private GanttXUnit xUnit = GanttXUnit.AUTO;

  private boolean relativeTimeValues;

  @XmlField(defaultIntValue = DEFAULT_X_LABELS_SCALE)
  private int xLabelsScale = DEFAULT_X_LABELS_SCALE;

  private GanttXUnit xTicks = GanttXUnit.AUTO;

  @XmlField(defaultIntValue = DEFAULT_X_TICKS_SCALE)
  private int xTicksScale = DEFAULT_X_TICKS_SCALE;

  @XmlField(defaultIntValue = DEFAULT_X_TICKS_SCALE_2)
  private int xTicksScale2 = DEFAULT_X_TICKS_SCALE_2;

  @XmlField(defaultIntValue = DEFAULT_X_GRID_SCALE)
  private int xGridScale = DEFAULT_X_GRID_SCALE;

  @XmlField(defaultBooleanValue = true)
  private boolean showToday = true;

  @XmlField(defaultBooleanValue = true)
  private boolean showNonWorkingDays = true;

  @XmlField(defaultBooleanValue = true)
  private boolean showCompletion = true;

  @XmlField(defaultIntValue = DEFAULT_WIDTH)
  private int width = DEFAULT_WIDTH;

  public double getWorkPackageLabelWidth()
  {
    return workPackageLabelWidth;
  }

  public GanttChartStyle setWorkPackageLabelWidth(double workPackageLabelWidth)
  {
    this.workPackageLabelWidth = workPackageLabelWidth;
    return this;
  }

  public double getTotalLabelWidth()
  {
    return totalLabelWidth;
  }

  public GanttChartStyle setTotalLabelWidth(double totalLabelWidth)
  {
    this.totalLabelWidth = totalLabelWidth;
    return this;
  }

  public double getArrowMinXDist()
  {
    return arrowMinXDist;
  }

  public GanttChartStyle setArrowMinXDist(double arrowMinXDist)
  {
    this.arrowMinXDist = arrowMinXDist;
    return this;
  }

  public double getArrowSize()
  {
    return arrowSize;
  }

  public GanttChartStyle setArrowSize(double arrowSize)
  {
    this.arrowSize = arrowSize;
    return this;
  }

  public double getActivityHeight()
  {
    return activityHeight;
  }

  public GanttChartStyle setActivityHeight(double activityHeight)
  {
    this.activityHeight = activityHeight;
    return this;
  }

  public double getYScale()
  {
    return yScale;
  }

  public GanttChartStyle setYScale(double yScale)
  {
    this.yScale = yScale;
    return this;
  }

  public String getXLabel()
  {
    return xLabel;
  }

  public GanttChartStyle setXLabel(String xLabel)
  {
    this.xLabel = xLabel;
    return this;
  }

  public GanttXUnit getXUnit()
  {
    return xUnit;
  }

  public GanttChartStyle setXUnit(GanttXUnit xUnit)
  {
    this.xUnit = xUnit;
    return this;
  }

  /**
   * If true then only the relative number of weeks, months etc. will be used. If false (default) then the dates will be displayed in the x
   * label bar.
   */
  public boolean isRelativeTimeValues()
  {
    return relativeTimeValues;
  }

  public GanttChartStyle setRelativeTimeValues(boolean relativeTimeValues)
  {
    this.relativeTimeValues = relativeTimeValues;
    return this;
  }

  public int getXLabelsScale()
  {
    return xLabelsScale;
  }

  public GanttChartStyle setXLabelsScale(int xLabelsScale)
  {
    this.xLabelsScale = xLabelsScale;
    return this;
  }

  public GanttXUnit getXTicks()
  {
    return xTicks;
  }

  public GanttChartStyle setXTicks(GanttXUnit xTicks)
  {
    this.xTicks = xTicks;
    return this;
  }

  public int getXTicksScale()
  {
    return xTicksScale;
  }

  public GanttChartStyle setXTicksScale(int xTicksScale)
  {
    this.xTicksScale = xTicksScale;
    return this;
  }

  public int getXTicksScale2()
  {
    return xTicksScale2;
  }

  public GanttChartStyle setXTicksScale2(int xTicksScale2)
  {
    this.xTicksScale2 = xTicksScale2;
    return this;
  }

  public int getXGridScale()
  {
    return xGridScale;
  }

  public GanttChartStyle setXGridScale(int xGridScale)
  {
    this.xGridScale = xGridScale;
    return this;
  }

  public boolean isShowToday()
  {
    return showToday;
  }

  public GanttChartStyle setShowToday(boolean showToday)
  {
    this.showToday = showToday;
    return this;
  }

  public boolean isShowNonWorkingDays()
  {
    return showNonWorkingDays;
  }

  public GanttChartStyle setShowNonWorkingDays(boolean showNonWorkingDays)
  {
    this.showNonWorkingDays = showNonWorkingDays;
    return this;
  }

  /**
   * If true then the completion is visualized in the activity bars.
   */
  public boolean isShowCompletion()
  {
    return showCompletion;
  }

  public GanttChartStyle setShowCompletion(boolean showCompletion)
  {
    this.showCompletion = showCompletion;
    return this;
  }

  public int getWidth()
  {
    return width;
  }

  public GanttChartStyle setWidth(int width)
  {
    this.width = width;
    return this;
  }
}
