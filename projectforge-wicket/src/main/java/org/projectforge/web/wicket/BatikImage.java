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

import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.http.WebResponse.CacheScope;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.util.time.Duration;
import org.projectforge.framework.renderer.BatikImageRenderer;
import org.projectforge.framework.renderer.ImageFormat;
import org.w3c.dom.Document;

public class BatikImage extends Image
{
  private static final long serialVersionUID = -167624996888880342L;

  private final int width;

  private byte[] ba;

  private transient Document document;

  public BatikImage(final String id, final Document document, final int width)
  {
    super(id);
    this.document = document;
    this.width = width;
  }

  private byte[] getByteArray()
  {
    if (ba == null) {
      ba = BatikImageRenderer.getByteArray(document, width, ImageFormat.PNG);
    }
    return ba;
  }

  @SuppressWarnings("serial")
  @Override
  protected AbstractResource getImageResource()
  {
    return new DynamicImageResource() {

      @Override
      protected byte[] getImageData(final Attributes attributes)
      {
        return getByteArray();
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
