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
import org.apache.wicket.model.Model;

/**
 * Represents an icon.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class HtmlCommentPanel extends Panel
{
  private static final long serialVersionUID = -4126462093466172226L;

  @SuppressWarnings("serial")
  public HtmlCommentPanel(final String id, final Model<String> comment)
  {
    super(id);
    add(new Label("comment", new Model<String>() {
      @Override
      public String getObject()
      {
        return "<!-- " + comment.getObject() + " -->";
      }
    }).setRenderBodyOnly(true).setEscapeModelStrings(false));
  }

  @SuppressWarnings("serial")
  public HtmlCommentPanel(final String id, final String comment)
  {
    super(id);
    add(new Label("comment", new Model<String>() {
      @Override
      public String getObject()
      {
        return "<!-- " + comment + " -->";
      }
    }).setRenderBodyOnly(true).setEscapeModelStrings(false));
  }
}
