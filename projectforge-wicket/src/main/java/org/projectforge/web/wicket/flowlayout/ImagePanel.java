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

package org.projectforge.web.wicket.flowlayout;

import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.projectforge.web.wicket.ImageDef;
import org.projectforge.web.wicket.PresizedImage;
import org.projectforge.web.wicket.components.TooltipImage;

/**
 * Represents a field set panel. A form or page can contain multiple field sets.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ImagePanel extends Panel
{
  private static final long serialVersionUID = -2323278625643532689L;

  private WebComponent image;

  /**
   * Wicket id.
   */
  public static final String IMAGE_ID = "image";

  public ImagePanel(final String id, final ImageDef imageDef)
  {
    super(id, null);
    addImage(new PresizedImage(IMAGE_ID, imageDef.getPath()));
  }

  public ImagePanel(final String id, final ImageDef imageDef, final String tooltip)
  {
    super(id, null);
    addImage(new TooltipImage(IMAGE_ID, imageDef.getPath(), tooltip));
  }

  public ImagePanel(final String id, final ContextImage image)
  {
    super(id, null);
    addImage(image);
  }

  public ImagePanel(final String id, final Image image)
  {
    super(id, null);
    addImage(image);
  }

  public ImagePanel(final String id)
  {
    super(id, null);
  }

  /**
   * @param image
   * @return this for chaining.
   */
  public ImagePanel addImage(final WebComponent image)
  {
    add(this.image = image);
    return this;
  }

  /**
   * @param image
   * @return this for chaining.
   */
  public ImagePanel removeImage()
  {
    if (image != null) {
      remove(image);
      this.image = null;
    }
    return this;
  }

  /**
   * @param image
   * @return this for chaining.
   */
  public ImagePanel replaceImage(final WebComponent image)
  {
    removeImage();
    add(this.image = image);
    return this;
  }
}
