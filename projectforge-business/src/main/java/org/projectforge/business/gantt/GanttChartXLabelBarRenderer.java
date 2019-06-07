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

import java.util.Calendar;
import java.util.Date;

import org.projectforge.export.SVGColor;
import org.projectforge.export.SVGHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.DayHolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GanttChartXLabelBarRenderer
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GanttChartXLabelBarRenderer.class);

  private static final double MIN_PIXEL_PER_UNIT = 10.0;

  private static final int[] QUARTER_SCALES = { 1, 2, 4, 8, 12};

  private static final int[] MONTH_SCALES = { 1, 2, 3, 6, 12, 24, 36, 48, 60};

  private static final int[] SCALES = { 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000};

  GanttXUnit labelUnit;

  int labelScale;

  // Ticks with labels
  GanttXUnit ticksUnit;

  int ticksScale;

  // Small ticks
  GanttXUnit ticksUnit2;

  int ticksScale2;

  GanttXUnit gridUnit;

  int gridScale;

  String label;

  private boolean showNonWorkingDays;

  private Date fromDate;

  private Date toDate;

  int fromToDays = -1;

  private double diagramWidth;

  private GanttChartStyle style;

  public GanttChartXLabelBarRenderer(final Date fromDate, final Date toDate, final double diagramWidth, final GanttChartStyle style)
  {
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.diagramWidth = diagramWidth;
    this.style = style;
    this.gridScale = style.getXGridScale();
    init();
  }

  private void init()
  {

    final int numberOfDays = getFromToDays(); // Initialize fromToDays.
    final double numberOfWeeks = 0.25 * numberOfDays;
    final double numberOfMonths = 0.033 * numberOfDays;
    final double numberOfQuarters = numberOfMonths / 3;
    if (this.style.getXUnit() == GanttXUnit.AUTO) {
      if (diagramWidth / numberOfDays > MIN_PIXEL_PER_UNIT) {
        labelUnit = GanttXUnit.DAY;
        ticksUnit = GanttXUnit.DAY;
        ticksUnit2 = GanttXUnit.DAY;
      } else if (diagramWidth / numberOfWeeks > MIN_PIXEL_PER_UNIT) {
        labelUnit = GanttXUnit.WEEK;
        ticksUnit = GanttXUnit.WEEK;
        ticksUnit2 = GanttXUnit.DAY;
      } else if (diagramWidth / numberOfMonths > MIN_PIXEL_PER_UNIT) {
        labelUnit = GanttXUnit.MONTH;
        ticksUnit = GanttXUnit.MONTH;
        ticksUnit2 = GanttXUnit.WEEK;
      } else {
        labelUnit = GanttXUnit.QUARTER;
        ticksUnit = GanttXUnit.MONTH;
        ticksUnit2 = GanttXUnit.WEEK;
      }
    }
    this.gridUnit = ticksUnit;
    double numberOfUnits;
    if (labelUnit == GanttXUnit.DAY) {
      numberOfUnits = (double) numberOfDays;
    } else if (labelUnit == GanttXUnit.WEEK) {
      numberOfUnits = numberOfWeeks;
    } else if (labelUnit == GanttXUnit.MONTH) {
      numberOfUnits = numberOfMonths;
    } else {
      numberOfUnits = numberOfQuarters;
    }
    labelScale = style.getXLabelsScale();
    ticksScale = style.getXTicksScale();
    ticksScale2 = style.getXTicksScale2();
    if (labelUnit == GanttXUnit.QUARTER) {
      if (labelScale < 0) {
        if (style.isRelativeTimeValues() == true) {
          // Numbers doesn't need as much space as quarter labels (03/2010)
          labelScale = getScale(numberOfQuarters, 50, labelUnit);
        } else {
          labelScale = getScale(numberOfQuarters, 100, labelUnit);
        }
      }
      if (ticksScale < 0) {
        ticksScale = 4;
        if (labelScale <= ticksScale) {
          ticksScale = labelScale;
        }
      }
      if (ticksScale2 < 0) {
        ticksScale = 1;
      }
    } else {
      if (labelScale < 0) {
        if (style.isRelativeTimeValues() == true) {
          // Numbers doesn't need as much space as date labels (03/2010)
          labelScale = getScale(numberOfUnits, 50, labelUnit);
        } else {
          labelScale = getScale(numberOfUnits, 100, labelUnit);
        }
      }
      if (ticksScale < 0) {
        ticksScale = 5;
        if (labelScale <= ticksScale) {
          ticksScale = labelScale;
        }
      }
      if (ticksScale2 < 0) {
        ticksScale = 1;
      }
    }
    if (style.getXLabel() != null) {
      label = style.getXLabel();
    } else if (style.isRelativeTimeValues() == true) {
      label = ThreadLocalUserContext.getLocalizedString(labelUnit.getI18nKey());
    }
    this.showNonWorkingDays = style.isShowNonWorkingDays();
    if (this.showNonWorkingDays == true) {
      if (diagramWidth / fromToDays < 2) {
        // Don't show non working days due to the large scale. At minimum 3 pixels per day required.
        this.showNonWorkingDays = false;
      }
    }
  }

  public int getScale(final double numberOfUnits, final int minPixels, final GanttXUnit unit)
  {
    final int[] scales;
    if (unit == GanttXUnit.QUARTER) {
      scales = QUARTER_SCALES;
    } else if (unit == GanttXUnit.MONTH) {
      scales = MONTH_SCALES;
    } else {
      scales = SCALES;
    }
    double widthPerUnit = diagramWidth / numberOfUnits;
    int current = 1;
    for (int scale : scales) {
      current = scale;
      if (widthPerUnit * scale > minPixels) {
        break;
      }
    }
    return current;
  }

  /**
   * @param doc
   * @param g1
   * @param grid If null then no grid will be drawn.
   * @param xGridHeight
   */
  public void draw(final Document doc, final Element g1, final Element grid, final double xGridHeight)
  {
    if (label != null) {
      g1.appendChild(SVGHelper.createText(doc, diagramWidth / 2, 0, label));
    }
    final Element ticks = SVGHelper.createElement(doc, "g", "stroke", SVGColor.BLACK.getName(), "stroke-width", "1", "transform", "translate(0,10)");
    g1.appendChild(ticks);
    final DayHolder day = new DayHolder(fromDate);
    final DayHolder toDay = new DayHolder(toDate);
    int dayCounter = 0;
    int weekCounter = 0;
    int monthCounter = 0;
    int quarterCounter = 0;
    int lastDateLabel = 0;
    boolean nonWorkingDayDisplayed = false;
    while (day.before(toDay) == true) {
      if (dayCounter > 0) {
        if (showNonWorkingDays == true && xGridHeight > 0) {
          if (day.isWorkingDay() == true) {
            nonWorkingDayDisplayed = false;
          } else if (nonWorkingDayDisplayed == false) {
            // Non-working day:
            showNonWorkingDays(doc, grid, day.getDate(), toDay.getDate(), xGridHeight);
            nonWorkingDayDisplayed = true;
          }
        }
        int wc = -1;
        if (day.getDayOfWeek() == day.getCalendar().getFirstDayOfWeek()) {
          wc = ++weekCounter;
        }
        int mc = -1;
        int qc = -1;
        final int dayOfMonth = day.getDayOfMonth();
        final int month = day.getMonth();
        if (dayOfMonth == 1) {
          mc = ++monthCounter;
          if (month == Calendar.JANUARY || month == Calendar.APRIL || month == Calendar.JULY || month == Calendar.OCTOBER) {
            qc = ++quarterCounter;
          }
        }
        boolean drawLabel = match(labelScale, labelUnit, dayCounter, wc, mc, qc);
        boolean drawTick = match(ticksScale, ticksUnit, dayCounter, wc, mc, qc);
        boolean drawTick2 = match(ticksScale2, ticksUnit2, dayCounter, wc, mc, qc);
        boolean drawGrid = match(gridScale, gridUnit, dayCounter, wc, mc, qc);
        if (style.isRelativeTimeValues() == false) {
          if (labelUnit == GanttXUnit.DAY) {
            if (labelScale <= 5) {
              // So draw labels on 1st, 5th, 10th, 15th, 20th, 25th of month.
              drawLabel = (dayOfMonth == 1 || dayOfMonth == 5 || dayOfMonth == 10 || dayOfMonth == 15 || dayOfMonth == 20 || dayOfMonth == 25);
            } else if (labelScale <= 15) {
              // So draw labels on 1st, 15th.
              drawLabel = (dayOfMonth == 1 || dayOfMonth == 15);
            } else if (labelScale <= 15) {
              // So draw labels on 1st.
              drawLabel = (dayOfMonth == 1);
            } else {
              if (dayOfMonth == 1 && dayCounter > lastDateLabel + labelScale) {
                drawLabel = true;
                lastDateLabel = dayCounter;
              } else {
                drawLabel = false;
              }
            }
          } else if (labelUnit == GanttXUnit.WEEK) {
            if (dayOfMonth == 1 && dayCounter > lastDateLabel + labelScale) {
              drawLabel = true;
              lastDateLabel = dayCounter;
            } else {
              drawLabel = false;
            }
          }
        }
        if (drawLabel == true) {
          String label = null;
          if (style.isRelativeTimeValues() == false) {
            label = DateTimeFormatter.instance().getFormattedDate(day.getDate());
          } else if (labelUnit == GanttXUnit.DAY) {
            label = String.valueOf(dayCounter);
          } else if (labelUnit == GanttXUnit.WEEK) {
            label = String.valueOf(weekCounter);
          } else if (labelUnit == GanttXUnit.MONTH) {
            label = String.valueOf(monthCounter);
          } else {
            label = String.valueOf(quarterCounter);
          }
          g1.appendChild(SVGHelper.createText(doc, getXValue(day.getDate()), 22, label, "text-anchor", "middle"));
        }
        if (drawLabel == true) {
          ticks.appendChild(SVGHelper.createLine(doc, getXValue(day.getDate()), 12, getXValue(day.getDate()), 20));
        } else if (drawTick == true) {
          ticks.appendChild(SVGHelper.createLine(doc, getXValue(day.getDate()), 15, getXValue(day.getDate()), 20));
        } else if (drawTick2 == true) {
          ticks.appendChild(SVGHelper.createLine(doc, getXValue(day.getDate()), 18, getXValue(day.getDate()), 20));
        }
        if (grid != null && (drawGrid == true || drawLabel == true)) {
          grid.appendChild(SVGHelper.createLine(doc, getXValue(day.getDate()), 0, getXValue(day.getDate()), xGridHeight));
        }
      }
      day.add(Calendar.DAY_OF_MONTH, 1);
      if (dayCounter++ > 5000) {
        log.error("Endless loop detection while creating x tick labels. Breaking.");
        break;
      }
    }
    ticks.appendChild(SVGHelper.createLine(doc, 0, 0, diagramWidth, 0));
    ticks.appendChild(SVGHelper.createLine(doc, 0, 0, 0, 20));
  }

  /**
   * @param doc
   * @param g
   * @param day
   * @param toDate Last day of diagram.
   * @param lastNonWorkingDay
   * @param height
   */
  private void showNonWorkingDays(final Document doc, final Element g, final Date day, final Date toDate, final double height)
  {
    if (g == null) {
      return;
    }
    final DayHolder dh = new DayHolder(day);
    final double x1 = getXValue(day);
    for (int i = 0; i < 100; i++) { // End-less loop protection.
      dh.add(Calendar.DAY_OF_MONTH, 1);
      if (dh.isWorkingDay() == true || dh.before(toDate) == false) {
        break;
      }
    }
    final double x2 = getXValue(dh.getDate());
    g.appendChild(SVGHelper.createRect(doc, x1, 0, x2 - x1, height, SVGColor.LIGHT_GRAY, SVGColor.NONE));
  }

  private boolean match(final int scale, final GanttXUnit unit, final int dayCounter, final int weekCounter, final int monthCounter,
      final int quarterCounter)
  {
    if (unit == GanttXUnit.DAY) {
      if (dayCounter % scale == 0) {
        return true;
      }
    } else if (unit == GanttXUnit.WEEK) {
      if (weekCounter > 0 && weekCounter % scale == 0) {
        return true;
      }
    } else if (unit == GanttXUnit.MONTH) {
      if (monthCounter > 0 && monthCounter % scale == 0) {
        return true;
      }
    } else if (unit == GanttXUnit.QUARTER) {
      if (quarterCounter > 0 && quarterCounter % scale == 0) {
        return true;
      }
    }
    return false;
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
    return diagramWidth * days / fromToDays;
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
