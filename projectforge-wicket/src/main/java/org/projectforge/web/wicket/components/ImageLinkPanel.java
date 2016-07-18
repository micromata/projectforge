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

package org.projectforge.web.wicket.components;

import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.ImageDef;
import org.projectforge.web.wicket.PresizedImage;

/**
 * An image as link with an href and with a tooltip.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ImageLinkPanel extends Panel
{
  private static final long serialVersionUID = 1333929048394636569L;

  public static final String LINK_WICKET_ID = "link";

  private AbstractLink link;

  @SuppressWarnings("serial")
  private ImageLinkPanel(final String id)
  {
    super(id);
    link = new Link<Void>("link") {
      @Override
      public void onClick()
      {
        ImageLinkPanel.this.onClick();
      };
    };
    add(link);
  }

  public ImageLinkPanel(final String id, final ImageDef image)
  {
    this(id, image.getPath());
  }

  public ImageLinkPanel(final String id, final String relativeImagePath)
  {
    this(id);
    link.add(new PresizedImage("image", relativeImagePath));
  }

  public ImageLinkPanel(final String id, final String relativeImagePath, final String tooltip)
  {
    this(id);
    link.add(new TooltipImage("image", relativeImagePath, tooltip));
  }

  public ImageLinkPanel(final String id, final String relativeImagePath, final IModel<String> tooltip)
  {
    this(id);
    link.add(new TooltipImage("image", relativeImagePath, tooltip));
  }

  public ImageLinkPanel(final String id, final AbstractLink link, final ImageDef image, final String tooltip)
  {
    this(id, link, image.getPath(), tooltip);
  }

  public ImageLinkPanel(final String id, final AbstractLink link, final String relativeImagePath, final String tooltip)
  {
    super(id);
    add(link);
    link.add(new TooltipImage("image", relativeImagePath, tooltip));
  }

  public void onClick()
  {
  };
}
