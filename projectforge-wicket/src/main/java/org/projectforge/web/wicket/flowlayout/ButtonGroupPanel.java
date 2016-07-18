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
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;

/**
 * Panel containing buttons. <br/>
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class ButtonGroupPanel extends Panel
{
  private final RepeatingView repeater;

  private final WebMarkupContainer container;

  public ButtonGroupPanel(final String id)
  {
    super(id);
    container = new WebMarkupContainer("container");
    add(container);
    repeater = new RepeatingView("repeater");
    container.add(repeater);
  }

  /**
   * For grouping toggle check boxes. Adds attribute 'data-toggle="buttons"' in html markup.
   * @return this for chaining.
   */
  public ButtonGroupPanel setToggleButtons()
  {
    container.add(AttributeModifier.append("data-toggle", "buttons"));
    return this;
  }

  /**
   * @param component
   * @return this for chaining.
   */
  public ButtonGroupPanel addButton(final Component component)
  {
    repeater.add(component);
    return this;
  }

  public String newChildId()
  {
    return repeater.newChildId();
  }
}
