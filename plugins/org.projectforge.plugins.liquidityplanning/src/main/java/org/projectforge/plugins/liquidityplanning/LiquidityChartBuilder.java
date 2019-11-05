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

package org.projectforge.plugins.liquidityplanning;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.projectforge.business.scripting.I18n;
import org.projectforge.charting.XYChartBuilder;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DayHolder;

import java.awt.*;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LiquidityChartBuilder
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LiquidityChartBuilder.class);

  /**
   * @param forecast
   * @param settings (next days)
   * @return
   */
  public JFreeChart createXYPlot(final LiquidityForecast forecast, final LiquidityForecastSettings settings)
  {
    if (!(settings.getNextDays() > 0 && settings.getNextDays() <= LiquidityForecastSettings.MAX_FORECAST_DAYS)) {
      settings.setNextDays(LiquidityForecastSettings.DEFAULT_FORECAST_DAYS);
    }

    final LiquidityForecastCashFlow cashFlow = new LiquidityForecastCashFlow(forecast, settings.getNextDays());

    final TimeSeries accumulatedSeries = new TimeSeries(I18n.getString("plugins.liquidityplanning.forecast.dueDate"));
    final TimeSeries accumulatedSeriesExpected = new TimeSeries(
        ThreadLocalUserContext.getLocalizedString("plugins.liquidityplanning.forecast.expected"));
    final TimeSeries worstCaseSeries = new TimeSeries(I18n.getString("plugins.liquidityplanning.forecast.worstCase"));
    double accumulatedExpected = settings.getStartAmount().doubleValue();
    double accumulated = accumulatedExpected;
    double worstCase = accumulated;

    final DayHolder dh = new DayHolder();
    final Date lower = dh.getDate();
    for (int i = 0; i < settings.getNextDays(); i++) {
      if (log.isDebugEnabled()) {
        log.debug("day: " + i + ", credits=" + cashFlow.getCredits()[i] + ", debits=" + cashFlow.getDebits()[i]);
      }
      final Day day = new Day(dh.getDayOfMonth(), dh.getMonth() + 1, dh.getYear());
      if (i > 0) {
        accumulated += cashFlow.getDebits()[i - 1].doubleValue() + cashFlow.getCredits()[i - 1].doubleValue();
        accumulatedExpected += cashFlow.getDebitsExpected()[i - 1].doubleValue() + cashFlow.getCreditsExpected()[i - 1].doubleValue();
        worstCase += cashFlow.getCredits()[i - 1].doubleValue();
      }
      accumulatedSeries.add(day, accumulated);
      accumulatedSeriesExpected.add(day, accumulatedExpected);
      worstCaseSeries.add(day, worstCase);
      dh.add(Calendar.DATE, 1);
    }
    dh.add(Calendar.DATE, -1);
    final XYChartBuilder cb = new XYChartBuilder(null, null, null, null, true);

    int counter = 0;

    final TimeSeriesCollection xyDataSeries = new TimeSeriesCollection();
    xyDataSeries.addSeries(accumulatedSeries);
    xyDataSeries.addSeries(worstCaseSeries);
    final XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
    lineRenderer.setSeriesPaint(0, Color.BLACK);
    lineRenderer.setSeriesVisibleInLegend(0, true);
    lineRenderer.setSeriesPaint(1, cb.getGrayMarker());
    lineRenderer.setSeriesStroke(1, cb.getDashedStroke());
    lineRenderer.setSeriesVisibleInLegend(1, true);
    cb.setRenderer(counter, lineRenderer).setDataset(counter++, xyDataSeries);

    final TimeSeriesCollection accumulatedSet = new TimeSeriesCollection();
    accumulatedSet.addSeries(accumulatedSeriesExpected);
    final XYDifferenceRenderer diffRenderer = new XYDifferenceRenderer(cb.getGreenFill(), cb.getRedFill(), true);
    diffRenderer.setSeriesPaint(0, cb.getRedMarker());
    cb.setRenderer(counter, diffRenderer).setDataset(counter++, accumulatedSet)
    .setStrongStyle(diffRenderer, false, accumulatedSeriesExpected);
    diffRenderer.setSeriesVisibleInLegend(0, true);

    cb.setDateXAxis(true).setDateXAxisRange(lower, dh.getDate()).setYAxis(true, null);
    return cb.getChart();
  }

  /**
   * @param forecast
   * @param settings (next days)
   * @return
   */
  public JFreeChart createBarChart(final LiquidityForecast forecast, final LiquidityForecastSettings settings)
  {
    if (!(settings.getNextDays() > 0 && settings.getNextDays() <= LiquidityForecastSettings.MAX_FORECAST_DAYS)) {
      settings.setNextDays(LiquidityForecastSettings.DEFAULT_FORECAST_DAYS);
    }
    final LiquidityForecastCashFlow cashFlow = new LiquidityForecastCashFlow(forecast, settings.getNextDays());
    final TimeSeries accumulatedSeriesExpected = new TimeSeries(I18n.getString("plugins.liquidityplanning.forecast.expected"));
    final TimeSeries creditSeries = new TimeSeries(I18n.getString("plugins.liquidityplanning.common.credit"));
    final TimeSeries debitSeries = new TimeSeries(I18n.getString("plugins.liquidityplanning.common.debit"));
    double accumulatedExpected = settings.getStartAmount().doubleValue();

    final DayHolder dh = new DayHolder();
    final Date lower = dh.getDate();
    for (int i = 0; i < settings.getNextDays(); i++) {
      final Day day = new Day(dh.getDayOfMonth(), dh.getMonth() + 1, dh.getYear());
      if (i > 0) {
        accumulatedExpected += cashFlow.getDebitsExpected()[i - 1].doubleValue() + cashFlow.getCreditsExpected()[i - 1].doubleValue();
      }
      accumulatedSeriesExpected.add(day, accumulatedExpected);
      creditSeries.add(day, cashFlow.getCreditsExpected()[i].doubleValue());
      debitSeries.add(day, cashFlow.getDebitsExpected()[i].doubleValue());
      dh.add(Calendar.DATE, 1);
    }
    dh.add(Calendar.DATE, -1);
    final XYChartBuilder cb = new XYChartBuilder(ChartFactory.createXYBarChart(null, null, false, null, null, PlotOrientation.VERTICAL,
        false, false, false));
    int counter = 0;

    final TimeSeriesCollection xyDataSeries = new TimeSeriesCollection();
    xyDataSeries.addSeries(accumulatedSeriesExpected);
    final XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, true);
    lineRenderer.setSeriesPaint(0, cb.getRedMarker());
    lineRenderer.setSeriesVisibleInLegend(0, true);
    cb.setRenderer(counter, lineRenderer).setDataset(counter++, xyDataSeries)
    .setStrongStyle(lineRenderer, false, accumulatedSeriesExpected);

    final TimeSeriesCollection cashflowSet = new TimeSeriesCollection();
    cashflowSet.addSeries(debitSeries);
    cashflowSet.addSeries(creditSeries);
    final XYBarRenderer barRenderer = new XYBarRenderer(.2);
    barRenderer.setSeriesPaint(0, cb.getGreenFill());
    barRenderer.setSeriesPaint(1, cb.getRedFill());
    barRenderer.setShadowVisible(false);
    cb.setRenderer(counter, barRenderer).setDataset(counter++, cashflowSet);

    cb.setDateXAxis(true).setDateXAxisRange(lower, dh.getDate()).setYAxis(true, null);
    return cb.getChart();
  }
}
