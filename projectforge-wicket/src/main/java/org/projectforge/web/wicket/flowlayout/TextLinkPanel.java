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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.WicketUtils;

/**
 * Represents a simple anchor link: &lt;a href="#"&gt;link&lt;/a&gt;.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TextLinkPanel extends Panel
{
  private static final long serialVersionUID = 3317775585548133768L;

  public static final String LINK_ID = "link";

  private final Label label;

  private final AbstractLink link;

  /**
   * @param id
   * @param link Must have component id {@link #LINK_ID}
   * @param label The link text.
   */
  public TextLinkPanel(final String id, final AbstractLink link, final String label)
  {
    this(id, link, label, null);
  }

  /**
   * @param id
   * @param link Must have component id {@link #LINK_ID}
   * @param label The link text.
   * @param tooltip
   */
  public TextLinkPanel(final String id, final AbstractLink link, final String label, final String tooltip)
  {
    super(id);
    this.link = link;
    init(id, link, tooltip);
    this.label = new Label("text", label);
    link.add(this.label);
  }

  /**
   * @param id
   * @param link Must have component id {@link #LINK_ID}
   * @param label The link text.
   */
  public TextLinkPanel(final String id, final AbstractLink link, final IModel<String> label)
  {
    this(id, link, label, null);
  }

  /**
   * @param id
   * @param link Must have component id {@link #LINK_ID}
   * @param label The link text.
   */
  public TextLinkPanel(final String id, final AbstractLink link, final IModel<String> label, final String tooltip)
  {
    super(id);
    this.link = link;
    init(id, link, tooltip);
    this.label = new Label("text", label);
    link.add(this.label);
  }

  private void init(final String id, final AbstractLink link, final String tooltip)
  {
    add(link);
    if (tooltip != null) {
      WicketUtils.addTooltip(link, tooltip);
    }
  }

  /**
   * @return the link
   */
  public AbstractLink getLink()
  {
    return link;
  }

  /**
   * @return the label
   */
  public Label getLabel()
  {
    return label;
  }
}
