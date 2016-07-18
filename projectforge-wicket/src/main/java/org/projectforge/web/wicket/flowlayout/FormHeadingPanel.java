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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Represents a simple text panel enclosed in a div element.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class FormHeadingPanel extends Panel
{
  private static final long serialVersionUID = 4900467057369413432L;

  public static final String WICKET_ID = "heading";

  private final Label label;

  public FormHeadingPanel(final String id, final String text)
  {
    super(id);
    add(label = new Label(WICKET_ID, text));
  }

  public FormHeadingPanel(final String id, final IModel<String> text)
  {
    super(id);
    add(label = new Label(WICKET_ID, text));
  }

  public FormHeadingPanel(final String id, final Label label)
  {
    super(id);
    this.label = label;
    add(label);
  }

  /**
   * Calls setRenderBodyOnly(false) and setOutputMarkupId(true) for the enclosed label.
   * @return the label
   */
  public Label getLabel4Ajax()
  {
    label.setRenderBodyOnly(false).setOutputMarkupId(true);
    return label;
  }

  /**
   * @see org.apache.wicket.Component#setMarkupId(java.lang.String)
   */
  @Override
  public FormHeadingPanel setMarkupId(final String markupId)
  {
    setOutputMarkupId(true);
    super.setMarkupId(markupId);
    return this;
  }
}
