/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.export;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import java.io.IOException;
import java.io.OutputStream;

public class ExportJFreeChart
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExportJFreeChart.class);

  private final JFreeChart jFreeChart;

  private final int width;

  private final int height;

  private JFreeChartImageType imageType = JFreeChartImageType.JPEG;

  public ExportJFreeChart(final JFreeChart jFreeChart, final int width, final int height)
  {
    this.jFreeChart = jFreeChart;
    this.width = width;
    this.height = height;
  }

  /**
   * @param out
   * @return extension png or jpg for usage as file name extension.
   */
  public String write(final OutputStream out)
  {
    final JFreeChart chart = getJFreeChart();
    final int width = getWidth();
    final int height = getHeight();
    String extension = null;
    try {
      if (getImageType() == JFreeChartImageType.PNG) {
        extension = "png";
        ChartUtils.writeChartAsPNG(out, chart, width, height);
      } else {
        extension = "jpg";
        ChartUtils.writeChartAsJPEG(out, chart, width, height);
      }
    } catch (final IOException ex) {
      log.error("Exception encountered " + ex, ex);
    }
    return extension;
  }

  public JFreeChart getJFreeChart()
  {
    return jFreeChart;
  }

  public int getWidth()
  {
    return width;
  }

  public int getHeight()
  {
    return height;
  }

  public JFreeChartImageType getImageType()
  {
    return imageType;
  }

  public ExportJFreeChart setImageType(final JFreeChartImageType imageType)
  {
    this.imageType = imageType;
    return this;
  }
}
