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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.projectforge.export.SVGHelper;
import org.projectforge.framework.renderer.BatikImageRenderer;
import org.projectforge.framework.renderer.ImageFormat;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.test.AbstractTestNGBase;
import org.projectforge.test.WorkFileHelper;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GanttChartXLabelBarRendererTest extends AbstractTestNGBase
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(GanttChartXLabelBarRendererTest.class);

  private static final int ROW_HEIGHT = 50;

  @Test
  public void test()
  {
    final GanttChartStyle style = new GanttChartStyle();
    style.setWidth(800);
    final double height = 1000;
    final Document doc = SVGHelper.createDocument(style.getWidth(), height);
    final Element root = doc.getDocumentElement();
    int row = -2;
    drawBothLabelBars(doc, root, row += 2, date(2010, Calendar.JANUARY, 1), date(2010, Calendar.JANUARY, 31), style);
    drawBothLabelBars(doc, root, row += 2, date(2010, Calendar.JANUARY, 1), date(2010, Calendar.MAY, 31), style);
    drawBothLabelBars(doc, root, row += 2, date(2010, Calendar.JANUARY, 1), date(2010, Calendar.JULY, 31), style);
    drawBothLabelBars(doc, root, row += 2, date(2010, Calendar.JANUARY, 1), date(2010, Calendar.OCTOBER, 31), style);
    drawBothLabelBars(doc, root, row += 2, date(2010, Calendar.JANUARY, 1), date(2010, Calendar.DECEMBER, 31), style);
    drawBothLabelBars(doc, root, row += 2, date(2010, Calendar.JANUARY, 1), date(2012, Calendar.DECEMBER, 31), style);
    drawBothLabelBars(doc, root, row += 2, date(2010, Calendar.JANUARY, 1), date(2015, Calendar.DECEMBER, 31), style);
    drawBothLabelBars(doc, root, row += 2, date(2010, Calendar.JANUARY, 1), date(2018, Calendar.DECEMBER, 31), style);
    final byte[] ba = BatikImageRenderer.getByteArray(doc, style.getWidth(), ImageFormat.PNG);
    final File file = WorkFileHelper.getWorkFile("ganttXBarTest.png");
    log.info("Writing Gantt test image to work directory: " + file.getAbsolutePath());
    try {
      final FileOutputStream out = new FileOutputStream(file);
      out.write(ba);
      out.close();
    } catch (final FileNotFoundException ex) {
      log.error("Exception encountered " + ex, ex);
    } catch (final IOException ex) {
      log.error("Exception encountered " + ex, ex);
    }
  }

  private void drawBothLabelBars(final Document doc, final Element el, final int row, final Date fromDate,
      final Date toDate,
      final GanttChartStyle style)
  {
    drawLabelBar(doc, el, row, fromDate, toDate, style.setRelativeTimeValues(false));
    drawLabelBar(doc, el, row + 1, fromDate, toDate, style.setRelativeTimeValues(true));
  }

  private void drawLabelBar(final Document doc, final Element el, final int row, final Date fromDate, final Date toDate,
      final GanttChartStyle style)
  {
    final Element g = SVGHelper.createElement(doc, "g", "transform", "translate(0," + (row * ROW_HEIGHT) + ")");
    el.appendChild(g);
    final Element grid = SVGHelper.createElement(doc, "g", "stroke", "green", "stroke-width", "1", "stroke-dasharray",
        "5,5");
    g.appendChild(grid);
    final Element g1 = SVGHelper.createElement(doc, "g", "text-anchor", "middle", "font-family", "Verdana", "font-size",
        "9pt");
    g.appendChild(g1);
    final GanttChartXLabelBarRenderer renderer = new GanttChartXLabelBarRenderer(fromDate, toDate, style.getWidth(),
        style);
    renderer.draw(doc, g1, grid, 10);
    log.info("GanttChartXLabelBarRenderer: fromToDays=" + renderer.fromToDays + ", labelUnit="
        + renderer.labelUnit
        + ", labelScale="
        + renderer.labelScale
        + ", ticksUnit="
        + renderer.ticksUnit
        + ", ticksScale="
        + renderer.ticksScale);
  }

  private Date date(final int year, final int month, final int dayOfMonth)
  {
    final DayHolder day = new DayHolder();
    day.setDate(year, month, dayOfMonth);
    return day.getDate();
  }

}
