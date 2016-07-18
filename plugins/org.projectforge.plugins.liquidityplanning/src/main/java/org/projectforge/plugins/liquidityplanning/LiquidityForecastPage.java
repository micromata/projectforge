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

package org.projectforge.plugins.liquidityplanning;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jfree.chart.JFreeChart;
import org.projectforge.business.fibu.EingangsrechnungDao;
import org.projectforge.business.fibu.RechnungDao;
import org.projectforge.web.wicket.AbstractStandardFormPage;
import org.projectforge.web.wicket.JFreeChartImage;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.flowlayout.ImagePanel;

public class LiquidityForecastPage extends AbstractStandardFormPage
{
  private static final long serialVersionUID = 6510134821712582764L;

  private static final int IMAGE_WIDTH = 800;

  private static final int IMAGE_HEIGHT = 400;

  @SpringBean
  private LiquidityEntryDao liquidityEntryDao;

  @SpringBean
  private RechnungDao rechnungDao;

  @SpringBean
  private EingangsrechnungDao eingangsrechnungDao;

  @SpringBean
  private LiquidityForecast forecast;

  private final GridBuilder gridBuilder;

  private ImagePanel xyPlotImage, barChartImage;

  private final LiquidityForecastForm form;

  public LiquidityForecastPage(final PageParameters parameters)
  {
    super(parameters);
    form = new LiquidityForecastForm(this);
    body.add(form);
    form.init();
    gridBuilder = new GridBuilder(body, "flowgrid");
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredPage#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    xyPlotImage = new ImagePanel(gridBuilder.getPanel().newChildId());
    gridBuilder.getPanel().add(xyPlotImage);
    gridBuilder.newGridPanel();
    barChartImage = new ImagePanel(gridBuilder.getPanel().newChildId());
    gridBuilder.getPanel().add(barChartImage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#onBeforeRender()
   */
  @Override
  protected void onBeforeRender()
  {
    //    if (forecast == null) {
    //      forecast = LiquidityEntryListPage.getForecast();
    //    }
    super.onBeforeRender();
    final LiquidityChartBuilder chartBuilder = new LiquidityChartBuilder();
    {
      final JFreeChart chart = chartBuilder.createXYPlot(forecast, form.getSettings());
      final JFreeChartImage image = new JFreeChartImage(ImagePanel.IMAGE_ID, chart, IMAGE_WIDTH, IMAGE_HEIGHT);
      image.add(AttributeModifier.replace("width", String.valueOf(IMAGE_WIDTH)));
      image.add(AttributeModifier.replace("height", String.valueOf(IMAGE_HEIGHT)));
      xyPlotImage.replaceImage(image);
    }
    {
      final JFreeChart chart = chartBuilder.createBarChart(forecast, form.getSettings());
      final JFreeChartImage image = new JFreeChartImage(ImagePanel.IMAGE_ID, chart, IMAGE_WIDTH, IMAGE_HEIGHT);
      image.add(AttributeModifier.replace("width", String.valueOf(IMAGE_WIDTH)));
      image.add(AttributeModifier.replace("height", String.valueOf(IMAGE_HEIGHT)));
      barChartImage.replaceImage(image);
    }
  }

  /**
   * @param forecast the forecast to set
   * @return this for chaining.
   */
  public LiquidityForecastPage setForecast(final LiquidityForecast forecast)
  {
    this.forecast = forecast;
    return this;
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.liquidityplanning.forecast");
  }
}
