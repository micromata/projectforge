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

package org.projectforge.web.wicket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.http.WebResponse.CacheScope;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.util.time.Duration;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.projectforge.export.JFreeChartImageType;

public class JFreeChartImage extends Image
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(JFreeChartImage.class);

  private static final long serialVersionUID = 7083627817914127250L;

  private final int width;

  private final int height;

  private final JFreeChartImageType imageType;

  public JFreeChartImage(final String id, final JFreeChart chart, final int width, final int height)
  {
    this(id, chart, null, width, height);
  }

  public JFreeChartImage(final String id, final JFreeChart chart, final JFreeChartImageType imageType, final int width, final int height)
  {
    super(id, new Model<JFreeChart>(chart));
    this.width = width;
    this.height = height;
    this.imageType = imageType;
  }

  @SuppressWarnings("serial")
  @Override
  protected AbstractResource getImageResource()
  {
    final String format = this.imageType == JFreeChartImageType.JPEG ? "jpg" : "png";
    return new DynamicImageResource() {

      @Override
      protected byte[] getImageData(final Attributes attributes)
      {
        try {
          final JFreeChart chart = (JFreeChart) getDefaultModelObject();
          final ByteArrayOutputStream baos = new ByteArrayOutputStream();
          if (imageType == JFreeChartImageType.JPEG) {
            ChartUtilities.writeChartAsJPEG(baos, chart, width, height);
          } else {
            ChartUtilities.writeChartAsPNG(baos, chart, width, height);
          }
          final byte[] ba = baos.toByteArray();
          return ba;
        } catch (final IOException ex) {
          log.error(ex.getMessage(), ex);
          return null;
        }
      }

      @Override
      protected void configureResponse(final ResourceResponse response, final Attributes attributes)
      {
        super.configureResponse(response, attributes);

        // if (isCacheable() == false) {
        response.setCacheDuration(Duration.NONE);
        response.setCacheScope(CacheScope.PRIVATE);
        // }
      }
    };
  }

}
