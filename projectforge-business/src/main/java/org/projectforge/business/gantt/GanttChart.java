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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.export.SVGColor;
import org.projectforge.export.SVGHelper;
import org.projectforge.export.SVGHelper.ArrowDirection;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.xstream.XmlObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@XmlObject(alias = "ganttChart")
public class GanttChart
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GanttChart.class);

  @SuppressWarnings("unused")
  private String name;

  private GanttChartStyle style;

  private GanttChartSettings settings;

  private Date fromDate;

  private Date toDate;

  private transient Date calculatedStartDate;

  private transient Date calculatedEndDate;

  private transient int fromToDays = -1;

  private transient double height;

  private GanttTask rootNode;

  private transient String fontFamily = "Helvetica";

  private transient Map<GanttTask, ObjectInfo> objectMap = new HashMap<GanttTask, ObjectInfo>();

  private class ObjectInfo
  {
    final Date fromDate;

    final Date toDate;

    final double x1;

    final double x2;

    final int row;

    final double y;

    ObjectInfo(final GanttTask node, final int row)
    {
      this.fromDate = GanttUtils.getCalculatedStartDate(node);
      this.toDate = GanttUtils.getCalculatedEndDate(node);
      if (fromDate != null) {
        this.x1 = getXValue(fromDate);
      } else {
        x1 = 0;
      }
      if (toDate != null) {
        this.x2 = getXValue(toDate);
      } else {
        x2 = 0;
      }
      this.row = row;
      this.y = style.getYScale() * row;
    }

    boolean isNaN()
    {
      return fromDate == null || toDate == null;
    }

    boolean isVisible()
    {
      return this.row >= 0;
    }
  }

  public GanttChart()
  {
    this.style = new GanttChartStyle();
    this.settings = new GanttChartSettings();
  }

  public GanttChart(final GanttTask rootNode, final GanttChartStyle style, final GanttChartSettings settings, final String name)
  {
    this.rootNode = rootNode;
    this.style = style;
    this.settings = settings;
    this.name = name;
  }

  public GanttChart setFontFamily(String fontFamily)
  {
    this.fontFamily = fontFamily;
    return this;
  }

  private ObjectInfo getObjectInfo(final GanttTask node)
  {
    ObjectInfo taskInfo = objectMap.get(node);
    if (taskInfo != null) {
      return taskInfo;
    }
    taskInfo = new ObjectInfo(node, -1);
    objectMap.put(node, taskInfo);
    return taskInfo;
  }

  public int getWidth()
  {
    return style.getWidth();
  }

  /**
   * The earliest date of all contained tasks.
   */
  public Date getCalculatedStartDate()
  {
    return calculatedStartDate;
  }

  /**
   * The latest date of all contained tasks.
   */
  public Date getCalculatedEndDate()
  {
    return calculatedEndDate;
  }

  public GanttTask getRootNode()
  {
    return rootNode;
  }

  /**
   * Usage:
   * 
   * <pre>
   * final BatikImage ganttImage = new BatikImage(&quot;ganttTest&quot;, ganttDiagram.create(), 800);
   * body.add(ganttImage);
   * </pre>
   * @return The SVG DOM model for this Gantt diagram.
   */
  public Document create()
  {
    if (rootNode == null || rootNode.getChildren() == null) {
      return null;
    }
    int row = 0;
    final Collection<GanttTask> allVisibleGanttObjects = recalculate();
    if (settings.getFromDate() != null) {
      fromDate = settings.getFromDate();
    }
    if (settings.getToDate() != null) {
      toDate = settings.getToDate();
    }
    if (fromDate == null) {
      fromDate = new DateHolder().setBeginOfDay().setHourOfDay(8).getDate();
    }
    if (toDate == null) {
      toDate = new DateHolder().setBeginOfDay().setHourOfDay(8).add(Calendar.DAY_OF_MONTH, 30).getDate();
    }
    for (final GanttTask node : allVisibleGanttObjects) {
      final ObjectInfo taskInfo = new ObjectInfo(node, row++);
      objectMap.put(node, taskInfo);
    }
    height = style.getYScale() * row + GanttChartStyle.HEAD_HEIGHT;
    final Document doc = SVGHelper.createDocument(style.getWidth(), height);
    final Element root = doc.getDocumentElement();

    Element e, g1, g2, g3;
    if (getDiagramWidth() < 0) {
      g1 = SVGHelper.createElement(doc, "g", "font-size", "9pt");
      root.appendChild(g1);
      g1.appendChild(SVGHelper.createText(doc, 0, 0, "TO SMALL"));
      return doc;
    }
    // Defs
    e = SVGHelper.createElement(doc, "defs");
    root.appendChild(e);
    e.appendChild(SVGHelper.createElement(doc, "path", SVGColor.DARK_RED, "d", "M 0 0 L "
        + GanttChartStyle.SUMMARY_ARROW_SIZE
        + " 0 L 0 "
        + GanttChartStyle.SUMMARY_ARROW_SIZE
        + " z", "id", "redLeftArrow"));
    e.appendChild(SVGHelper.createElement(doc, "path", SVGColor.DARK_RED, "d", "M 0 0 L "
        + GanttChartStyle.SUMMARY_ARROW_SIZE
        + " 0 L "
        + GanttChartStyle.SUMMARY_ARROW_SIZE
        + " "
        + GanttChartStyle.SUMMARY_ARROW_SIZE
        + " z", "id", "redRightArrow"));
    e.appendChild(SVGHelper.createElement(doc, "path", SVGColor.BLACK, "d", "M -5 0 L 0 5 L 5 0 L 0 -5 z", "id", "diamond"));
    e = SVGHelper.createElement(doc, "defs");
    root.appendChild(e);

    g1 = SVGHelper.createElement(doc, "g", "transform", "translate(5,20)");
    root.appendChild(g1);
    if (fontFamily != null) {
      g2 = SVGHelper.createElement(doc, "g", "font-family", fontFamily, "font-size", "9pt");
    } else {
      g2 = SVGHelper.createElement(doc, "g", "font-size", "9pt");
    }
    g1.appendChild(g2);
    if (style.getWorkPackageLabelWidth() > 0) {
      g2.appendChild(SVGHelper.createText(doc, 0, 0, "WP"));
      g2.appendChild(SVGHelper.createText(doc, 0, 20, "Code"));
      g2.appendChild(SVGHelper.createText(doc, style.getWorkPackageLabelWidth(), 10, settings.getTitle()));
    } else {
      g2.appendChild(SVGHelper.createText(doc, 0, 10, settings.getTitle()));
    }

    // labelbar
    if (fontFamily != null) {
      g1 = SVGHelper.createElement(doc, "g", "transform", "translate(" + style.getTotalLabelWidth() + ",20)", "text-anchor", "middle",
          "font-family", fontFamily, "font-size", "9pt");
    } else {
      g1 = SVGHelper.createElement(doc, "g", "transform", "translate(" + style.getTotalLabelWidth() + ",20)", "text-anchor", "middle",
          "font-size", "9pt");
    }
    root.appendChild(g1);
    final Element diagram = SVGHelper.createElement(doc, "g", "transform", "translate("
        + style.getTotalLabelWidth()
        + ","
        + GanttChartStyle.HEAD_HEIGHT
        + ")");
    root.appendChild(diagram);
    final Element grid = SVGHelper.createElement(doc, "g", "stroke", "gray", "stroke-width", "1");// , "stroke-dasharray", "5,5");
    diagram.appendChild(grid);
    final GanttChartXLabelBarRenderer xLabelBarRenderer = new GanttChartXLabelBarRenderer(fromDate, toDate, getDiagramWidth(), style);
    xLabelBarRenderer.draw(doc, g1, grid, getDiagramHeight());

    // Show today line, if configured.
    if (style.isShowToday() == true) {
      final DateHolder today = new DateHolder();
      if (today.isBetween(fromDate, toDate) == true) {
        diagram.appendChild(SVGHelper.createLine(doc, getXValue(today.getDate()), 0, getXValue(today.getDate()), getDiagramHeight(),
            SVGColor.RED, "stroke-width", "2"));
      }
    }

    // Task descriptions:
    if (fontFamily != null) {
      g1 = SVGHelper.createElement(doc, "g", "transform", "translate(5,65)", "font-family", fontFamily, "font-size", "9pt");
    } else {
      g1 = SVGHelper.createElement(doc, "g", "transform", "translate(5,65)", "font-size", "9pt");
    }
    root.appendChild(g1);
    drawGanttObjects(doc, g1, diagram, grid, allVisibleGanttObjects);

    g2 = SVGHelper.createElement(doc, "g", "transform", "translate(5,0)");
    g1.appendChild(g2);
    if (fontFamily != null) {
      g3 = SVGHelper.createElement(doc, "g", "font-family", fontFamily, "font-size", "9pt");
    } else {
      g3 = SVGHelper.createElement(doc, "g", "font-size", "9pt");
    }
    g2.appendChild(g3);
    // diagram.appendChild(SVGHelper.createUse(doc, "#diamond", 100, 15.5 * style.getYScale()));
    // diagram.appendChild(SVGHelper.createRect(doc, 110, 15 * style.getYScale() + 2, 140, 16, "white"));
    // diagram.appendChild(SVGHelper.createText(doc, 110, 15.5 * style.getYScale() + 5, "This is a nonsens milestone.", "fill", "gray",
    // "font-size", "8pt"));

    g1 = SVGHelper.createElement(doc, "g", "transform", "translate(265,65)");
    root.appendChild(g1);

    g1 = SVGHelper.createElement(doc, "g", "stroke", SVGColor.BLACK.getName());
    root.appendChild(g1);
    // Show outline of canvas using 'rect' element. -->
    g1.appendChild(SVGHelper.createRect(doc, 0, 0, style.getWidth(), height, "none", "stroke-width", "2"));
    // Horizontal line after head row.
    g1.appendChild(SVGHelper.createLine(doc, 0, 50, style.getWidth(), 50, "stroke-width", "2"));
    // Vertical line between Description and bar charts.
    g1.appendChild(SVGHelper.createLine(doc, style.getTotalLabelWidth(), 50, style.getTotalLabelWidth(), height, "stroke-width", "2"));

    return doc;
  }

  /**
   * Recalculates all start and end dates of all nodes and the earliest calculated start date and latest calculated end date.
   * @return All visible nodes.
   */
  public Collection<GanttTask> recalculate()
  {
    rootNode.recalculate();
    fromDate = toDate = null;
    final Collection<GanttTask> allVisibleGanttObjects = getAllVisibleGanttObjects(new ArrayList<GanttTask>(), rootNode);
    for (final GanttTask node : allVisibleGanttObjects) {
      Date periodStart = GanttUtils.getCalculatedStartDate(node);
      Date periodEnd = GanttUtils.getCalculatedEndDate(node);
      if (periodEnd == null) {
        periodEnd = periodStart;
      } else if (periodStart == null) {
        periodStart = periodEnd;
      }
      if (fromDate == null) {
        fromDate = periodStart;
      } else if (periodStart != null && fromDate.after(periodStart) == true) {
        fromDate = periodStart;
      }
      if (toDate == null) {
        toDate = periodEnd;
      } else if (periodEnd != null && toDate.before(periodEnd) == true) {
        toDate = periodEnd;
      }
    }
    this.calculatedStartDate = fromDate;
    this.calculatedEndDate = toDate;
    return allVisibleGanttObjects;
  }

  private void drawGanttObjects(final Document doc, final Element g, final Element diagram, final Element grid,
      final Collection<GanttTask> allVisibleGanttObjects)
  {
    if (CollectionUtils.isEmpty(rootNode.getChildren()) == true) {
      return;
    }
    boolean first = true;
    for (final GanttTask node : allVisibleGanttObjects) {
      if (node.isVisible() == false) {
        continue;
      }
      final ObjectInfo taskInfo = getObjectInfo(node);
      drawLabel(node, doc, g);
      GanttObjectType type = node.getType();
      if (type == null) {
        if (node.hasDuration() == false) {
          type = GanttObjectType.MILESTONE;
        } else {
          type = GanttObjectType.ACTIVITY;
          if (node.getChildren() != null) {
            for (final GanttTask child : node.getChildren()) {
              if (child.isVisible() == true) {
                type = GanttObjectType.SUMMARY;
                break;
              }
            }
          }
        }
      } else {
        if (type == GanttObjectType.MILESTONE == true && node.hasDuration() == true) {
          // Milestones can't have durations. Change it to a normal activity.
          type = GanttObjectType.ACTIVITY;
        }
      }
      if (type == GanttObjectType.MILESTONE) {
        // Type milestone and node has no duration.
        drawMilestone(node, doc, diagram);
      } else if (type == GanttObjectType.ACTIVITY) {
        drawActivity(node, doc, diagram);
      } else if (type == GanttObjectType.SUMMARY) {
        drawSummary(node, doc, diagram);
      } else {
        log.error("Unsupported type: " + node.getType());
      }
      if (first == true) {
        first = false;
      } else {
        grid.appendChild(SVGHelper.createLine(doc, 0, taskInfo.y, getDiagramWidth(), taskInfo.y));
      }
    }
  }

  private Collection<GanttTask> getAllVisibleGanttObjects(final Collection<GanttTask> col, final GanttTask node)
  {
    if (node != rootNode) {
      if (node.isVisible() == true) {
        col.add(node);
      }
    }
    if (CollectionUtils.isEmpty(node.getChildren()) == true) {
      return col;
    }
    for (final GanttTask child : node.getChildren()) {
      getAllVisibleGanttObjects(col, child);
    }
    return col;
  }

  private void drawLabel(final GanttTask node, final Document doc, final Element labels)
  {
    int indent = 0;
    GanttTask n = node;
    while (true) {
      n = rootNode.findParent(n.getId());
      if (n == rootNode || n == null) {
        break;
      }
      if (n.isVisible() == true) {
        ++indent;
      }
    }
    final ObjectInfo taskInfo = getObjectInfo(node);
    if (StringUtils.isNotBlank(node.getWorkpackageCode()) == true && style.getWorkPackageLabelWidth() > 0) {
      labels.appendChild(SVGHelper.createText(doc, 0 + indent * 5, taskInfo.y, node.getWorkpackageCode()));
    }
    if (StringUtils.isNotBlank(node.getTitle()) == true) {
      labels.appendChild(SVGHelper.createText(doc, style.getWorkPackageLabelWidth() + indent * 10, taskInfo.y, node.getTitle()));
    }
  }

  private void drawSummary(final GanttTask node, final Document doc, final Element diagram)
  {
    final ObjectInfo taskInfo = getObjectInfo(node);
    if (log.isDebugEnabled() == true) {
      log.debug("Task added: fromDate=" + taskInfo.fromDate + " (x=" + taskInfo.x1 + "), toDate=" + taskInfo.toDate + " (x=" + taskInfo.x2);
    }
    double x1 = taskInfo.x1;
    double x2 = taskInfo.x2;
    double diagramWidth = getDiagramWidth();
    if (x2 - GanttChartStyle.SUMMARY_ARROW_SIZE < 0 || x1 > diagramWidth) {
      return;
    }
    boolean drawLeftArrow = true;
    boolean drawRightArrow = true;
    if (x1 < 0) {
      x1 = 0;
      drawLeftArrow = false;
    }
    if (x2 > diagramWidth) {
      x2 = diagramWidth;
      drawRightArrow = false;
    }
    final double width = (x2 - x1);
    if (width <= 0) {
      return;
    }
    diagram.appendChild(SVGHelper.createRect(doc, x1, taskInfo.y + 0.2 * style.getActivityHeight(), width, 0.8 * style.getActivityHeight(),
        SVGColor.DARK_RED, "stroke", "none"));
    if (drawLeftArrow == true) {
      diagram.appendChild(SVGHelper.createUse(doc, "#redLeftArrow", taskInfo.x1, taskInfo.y + style.getActivityHeight()));
    }
    if (drawRightArrow == true) {
      diagram.appendChild(SVGHelper.createUse(doc, "#redRightArrow", (taskInfo.x2 - GanttChartStyle.SUMMARY_ARROW_SIZE), taskInfo.y
          + style.getActivityHeight()));
    }
    drawDependency(node, GanttObjectType.SUMMARY, doc, diagram);
  }

  private void drawActivity(final GanttTask node, final Document doc, final Element diagram)
  {
    final ObjectInfo taskInfo = getObjectInfo(node);
    if (taskInfo.isNaN() == true) {
      // No start and end date given, do nothing:
      return;
    }
    if (log.isDebugEnabled() == true) {
      log.debug("Activity added: fromDate="
          + taskInfo.fromDate
          + " (x="
          + taskInfo.x1
          + "), toDate="
          + taskInfo.toDate
          + " (x="
          + taskInfo.x2
          + ")");
    }
    if (taskInfo.x2 < taskInfo.x1) {
      log.error("Oups, x2 < x1?: " + node);
      return;
    }
    double x1 = taskInfo.x1;
    double x2 = taskInfo.x2;
    double diagramWidth = getDiagramWidth();
    if (x2 < 0 || x1 > diagramWidth) {
      return;
    }
    if (x1 < 0) {
      x1 = 0;
    }
    if (x2 > diagramWidth) {
      x2 = diagramWidth;
    }
    final double width = (x2 - x1);
    if (width <= 0) {
      return;
    }
    final double y = taskInfo.y + style.getActivityHeight() / 2;
    final double height = style.getActivityHeight();
    if (style.isShowCompletion() == true) {
      Integer completion = node.getProgress();
      if (completion == null || completion < 0) {
        completion = 0;
      } else if (completion > 100) {
        completion = 100;
      }
      final double width1 = width * completion / 100;
      final double width2 = width - width1;
      if (width1 > 0) {
        diagram.appendChild(SVGHelper.createRect(doc, x1, y, width1, height, SVGColor.DARK_BLUE, SVGColor.DARK_BLUE));
      }
      if (width2 > 0) {
        diagram.appendChild(SVGHelper.createRect(doc, x1 + width1, y, width2, height, SVGColor.LIGHT_BLUE, SVGColor.DARK_BLUE));
      }
    } else {
      diagram.appendChild(SVGHelper.createRect(doc, x1, y, width, height, SVGColor.DARK_BLUE, SVGColor.NONE));
    }
    drawDependency(node, GanttObjectType.ACTIVITY, doc, diagram);
  }

  private void drawMilestone(final GanttTask node, final Document doc, final Element diagram)
  {
    final ObjectInfo taskInfo = getObjectInfo(node);
    final Date date = taskInfo.fromDate != null ? taskInfo.fromDate : taskInfo.toDate;
    if (date == null) {
      // Neither start nor end date given, do nothing:
      return;
    }
    final double x = getXValue(date);
    if (x < 0 || x > getDiagramWidth()) {
      return;
    }
    if (log.isDebugEnabled() == true) {
      log.debug("Milestone added: date=" + date + " (x=" + x + ")");
    }
    diagram.appendChild(SVGHelper.createUse(doc, "#diamond", x, taskInfo.y + style.getYScale() / 2));
    drawDependency(node, GanttObjectType.MILESTONE, doc, diagram);
  }

  private void drawDependency(final GanttTask node, final GanttObjectType objectType, final Document doc, final Element diagram)
  {
    final ObjectInfo taskInfo = getObjectInfo(node);
    if (node.getPredecessor() != null) {
      final double dist;
      if (objectType == GanttObjectType.MILESTONE) {
        dist = 5;
      } else {
        dist = 1;
      }
      final ObjectInfo depObjectInfo = getObjectInfo(node.getPredecessor());
      if (depObjectInfo.isVisible() == true) {
        final GanttRelationType type = node.getRelationType() != null ? node.getRelationType() : GanttRelationType.FINISH_START;
        final double depX1;
        final double depX2;
        final double depY1 = depObjectInfo.y + style.getActivityHeight();
        final double depY2 = taskInfo.y + style.getActivityHeight();
        if (type == GanttRelationType.START_START) {
          depX1 = depObjectInfo.x1;
          depX2 = taskInfo.x1;
        } else if (type == GanttRelationType.START_FINISH) {
          depX1 = depObjectInfo.x1;
          depX2 = taskInfo.x2;
        } else if (type == GanttRelationType.FINISH_START) {
          depX1 = depObjectInfo.x2;
          depX2 = taskInfo.x1;
        } else {
          depX1 = depObjectInfo.x2;
          depX2 = taskInfo.x2;
        }
        double diagramWidth = getDiagramWidth();
        if (depX1 > 0 && depX1 < diagramWidth && depX2 > 0 && depX2 < diagramWidth) {
          diagram.appendChild(SVGHelper.createPath(doc, SVGColor.NONE, 1, SVGColor.BLACK, SVGHelper.drawHorizontalConnectionLine(type,
              depX1, depY1, depX2, depY2, style.getArrowMinXDist() + dist)));
          if (type.isIn(GanttRelationType.FINISH_START, GanttRelationType.START_START) == true) {
            diagram.appendChild(SVGHelper.createPath(doc, SVGColor.BLACK, 1, SVGColor.BLACK, SVGHelper.drawArrow(ArrowDirection.RIGHT,
                depX2 - dist, depY2, style.getArrowSize())));
          } else {
            diagram.appendChild(SVGHelper.createPath(doc, SVGColor.BLACK, 1, SVGColor.BLACK, SVGHelper.drawArrow(ArrowDirection.LEFT, depX2
                + dist
                / 2, depY2, style.getArrowSize())));
          }
        }
      } else if (log.isDebugEnabled() == true) {
        log.debug("Depend on task is invisible, so cannot draw dependency.");
      }
    }

  }

  private double getDiagramWidth()
  {
    return style.getWidth() - style.getTotalLabelWidth();
  }

  private double getDiagramHeight()
  {
    return height - GanttChartStyle.HEAD_HEIGHT;
  }

  private double getXValue(final Date date)
  {
    if (date == null) {
      return 0.0;
    }
    final DateHolder dh = new DateHolder(fromDate);
    final int days = dh.daysBetween(date);
    final int fromToDays = getFromToDays();
    if (fromToDays == 0) {
      return 0;
    }
    final int hourOfDay = new DateHolder(date).getHourOfDay();
    return this.getDiagramWidth() * (days * 24 + hourOfDay) / (fromToDays * 24);
  }

  private int getFromToDays()
  {
    if (fromToDays < 0) {
      final DateHolder dh = new DateHolder(fromDate);
      fromToDays = dh.daysBetween(toDate);
    }
    return fromToDays;
  }
}
