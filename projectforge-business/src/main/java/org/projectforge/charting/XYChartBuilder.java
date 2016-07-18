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

package org.projectforge.charting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.general.Series;
import org.jfree.data.time.DateRange;
import org.jfree.data.xy.XYDataset;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

/**
 * Builder class for building JFree charts.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class XYChartBuilder
{
  private final XYPlot plot;

  private final JFreeChart chart;

  private final Shape circleShape = new Ellipse2D.Float(-2, -2, 4, 4);

  private final Shape strongCircleShape = new Ellipse2D.Float(-3, -3, 6, 6);

  private final Stroke strongStroke = new BasicStroke(3.0f);

  private final Stroke stroke = new BasicStroke(1.0f);

  private ValueAxis xAxis;

  private final Stroke dashedStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 3.0f, new float[] { 3.0f, 3.0f},
      1.0f);

  public XYChartBuilder(final String title, final String xAxisLabel, final String yAxisLabel, final XYDataset dataset, final boolean legend)
  {
    this(ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, legend, true, false));
  }

  public XYChartBuilder(final JFreeChart chart)
  {
    this.chart = chart;
    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setDomainGridlinePaint(Color.lightGray);
    plot.setRangeGridlinePaint(Color.lightGray);
    plot.setOutlineVisible(false);
  }

  public XYChartBuilder setDateXAxis(final boolean showAxisValues)
  {
    xAxis = new DateAxis();
    ((DateAxis) xAxis).setTickMarkPosition(DateTickMarkPosition.MIDDLE);
    xAxis.setLowerMargin(0.0);
    xAxis.setUpperMargin(0.0);
    xAxis.setVisible(showAxisValues);
    plot.setDomainAxis(xAxis);
    return this;
  }

  public XYChartBuilder setDateXAxisRange(final Date lower, final Date upper)
  {
    if (xAxis == null) {
      throw new IllegalArgumentException("Call set*XAxis first. No xAxis given yet.");
    }
    xAxis.setRange(new DateRange(lower, upper));
    return this;
  }

  public XYChartBuilder setYAxis(final boolean showAxisValues, final String valueAxisUnitKey)
  {
    final NumberAxis yAxis;
    if (showAxisValues == true && valueAxisUnitKey != null) {
      yAxis = new NumberAxis(ThreadLocalUserContext.getLocalizedString(valueAxisUnitKey));
    } else {
      yAxis = new NumberAxis();
    }
    yAxis.setVisible(showAxisValues);
    plot.setRangeAxis(yAxis);
    return this;
  }

  public XYChartBuilder setDataset(final int index, final XYDataset dataset)
  {
    plot.setDataset(index, dataset);
    return this;
  }

  /**
   * Applies {@link #strongCircleShape} and {@link #strongStroke} if set to all series entries.
   * @param renderer
   * @param visibleInLegend
   * @param series
   * @return
   */
  public XYChartBuilder setStrongStyle(final XYItemRenderer renderer, final boolean visibleInLegend, final Series... series)
  {
    if (series == null || series.length == 0) {
      return this;
    }
    for (int i = 0; i < series.length; i++) {
      renderer.setSeriesShape(i, strongCircleShape);
    }
    for (int i = 0; i < series.length; i++) {
      renderer.setSeriesStroke(i, strongStroke);
      renderer.setSeriesVisibleInLegend(i, visibleInLegend);
    }
    return this;
  }

  /**
   * Applies {@link #strongCircleShape} and {@link #strongStroke} if set to all series entries.
   * @param renderer
   * @param visibleInLegend
   * @param series
   * @return
   */
  public XYChartBuilder setNormalStyle(final XYItemRenderer renderer, final boolean visibleInLegend, final Series... series)
  {
    if (series == null || series.length == 0) {
      return this;
    }
    for (int i = 0; i < series.length; i++) {
      renderer.setSeriesShape(i, circleShape);
    }
    for (int i = 0; i < series.length; i++) {
      renderer.setSeriesStroke(i, stroke);
      renderer.setSeriesVisibleInLegend(i, visibleInLegend);
    }
    return this;
  }

  /**
   * @return the chart
   */
  public JFreeChart getChart()
  {
    return chart;
  }

  public XYChartBuilder setRenderer(final int index, final XYItemRenderer renderer)
  {
    plot.setRenderer(index, renderer);
    return this;
  }

  /**
   * @return the dashedStroke
   */
  public Stroke getDashedStroke()
  {
    return dashedStroke;
  }

  /**
   * @return the stroke
   */
  public Stroke getStroke()
  {
    return stroke;
  }

  public Color getRedFill()
  {
    return new Color(238, 176, 176);
  }

  public Color getGreenFill()
  {
    return new Color(135, 206, 112);
  }

  public Color getRedMarker()
  {
    return new Color(222, 23, 33);
  }

  public Color getGreenMarker()
  {
    return new Color(64, 169, 59);
  }

  public Color getGrayMarker()
  {
    return new Color(55, 55, 55);
  }
}
