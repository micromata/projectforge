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

import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

/**
 * Represents a simple text panel. The enclosed span element is only shown if any behavior is added.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TextPanel extends Panel
{
  private static final long serialVersionUID = 4900467057369413432L;

  Label label;

  public TextPanel(final String id, final String text, final Behavior... behaviors)
  {
    super(id);
    label = new Label("text", text);
    init(behaviors);
  }

  public TextPanel(final String id, final Model<String> text, final Behavior... behaviors)
  {
    super(id);
    label = new Label("text", text);
    init(behaviors);
  }

  /**
   * @return the label
   */
  public Label getLabel()
  {
    return label;
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

  private void init(final Behavior... behaviors)
  {
    if (behaviors != null && behaviors.length > 0) {
      label.add(behaviors);
    } else {
      label.setRenderBodyOnly(true);
    }
    add(label);
  }
}
