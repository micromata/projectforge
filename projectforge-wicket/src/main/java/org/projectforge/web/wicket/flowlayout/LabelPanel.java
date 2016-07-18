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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Represents an icon.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LabelPanel extends Panel
{
  private static final long serialVersionUID = -5119098583740700288L;

  public static final String WICKET_ID = "label";

  private Label label;

  public LabelPanel(final String id, final String labelText)
  {
    super(id);
    add(label = new Label(WICKET_ID, labelText));
  }

  /**
   * @param componentId
   * @return this for chaining.
   */
  public LabelPanel setLabelFor(final String componentId)
  {
    label.add(AttributeModifier.replace("for", componentId));
    return this;
  }

  /**
   * @return the label
   */
  public Label getLabel()
  {
    return label;
  }
}
