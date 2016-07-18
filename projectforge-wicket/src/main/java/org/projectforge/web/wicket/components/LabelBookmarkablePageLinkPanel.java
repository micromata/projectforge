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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * An image as bookmarkable link with an href and with a tooltip.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LabelBookmarkablePageLinkPanel extends Panel
{
  private static final long serialVersionUID = -6763951791178572288L;

  private final BookmarkablePageLink<String> link;

  private LabelBookmarkablePageLinkPanel(final String id, final Class< ? extends WebPage> pageClass)
  {
    super(id);
    link = new BookmarkablePageLink<String>("link", pageClass);
    add(link);
  }

  private LabelBookmarkablePageLinkPanel(final String id, final Class< ? extends WebPage> pageClass, final PageParameters params)
  {
    super(id);
    link = new BookmarkablePageLink<String>("link", pageClass, params);
    add(link);
  }

  public LabelBookmarkablePageLinkPanel(final String id, final Class< ? extends WebPage> pageClass, final String label)
  {
    this(id, pageClass);
    link.add(new Label("label", label));
  }

  public LabelBookmarkablePageLinkPanel(final String id, final Class< ? extends WebPage> pageClass, final String label,
      final PageParameters params)
  {
    this(id, pageClass, params);
    link.add(new Label("label", label));
  }

  /**
   * Sets a html attribute to the enclosed link.
   * @param attribute
   * @param label
   * @return this for chaining.
   */
  public LabelBookmarkablePageLinkPanel addLinkAttribute(final String attribute, final String label)
  {
    link.add(AttributeModifier.replace(attribute, label));
    return this;
  }
}
