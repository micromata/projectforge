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

package org.projectforge.web.wicket.components;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Response;
import org.projectforge.web.wicket.PresizedImage;

/**
 * An image as bookmarkable link with an href and with a tooltip.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ImageBookmarkablePageLinkPanel extends Panel
{
  private static final long serialVersionUID = -6763951791178572288L;

  private final BookmarkablePageLink<String> link;

  private ImageBookmarkablePageLinkPanel(final String id, final Class< ? extends WebPage> pageClass)
  {
    super(id);
    link = new BookmarkablePageLink<String>("link", pageClass);
    add(link);
  }

  public ImageBookmarkablePageLinkPanel(final String id, final Class< ? extends WebPage> pageClass, final Response response,
      final String relativeImagePath)
  {
    this(id, pageClass);
    link.add(new PresizedImage("image", relativeImagePath));
  }

  public ImageBookmarkablePageLinkPanel(final String id, final Class< ? extends WebPage> pageClass, final Response response,
      final String relativeImagePath, final String tooltip)
  {
    this(id, pageClass);
    link.add(new TooltipImage("image", relativeImagePath, tooltip));
  }

  public ImageBookmarkablePageLinkPanel(final String id, final Class< ? extends WebPage> pageClass, final Response response,
      final String relativeImagePath, final IModel<String> tooltip)
  {
    this(id, pageClass);
    link.add(new TooltipImage("image", relativeImagePath, tooltip));
  }

}
