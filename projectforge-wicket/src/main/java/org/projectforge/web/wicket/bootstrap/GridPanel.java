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

package org.projectforge.web.wicket.bootstrap;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class GridPanel extends Panel
{
  private static final long serialVersionUID = -8628272536004694077L;

  /**
   * Use this only if this panel contains only one child. Otherwise a RepeatingView is used and is added automatically.
   */
  public static final String CHILD_ID = "child";

  RepeatingView repeater;

  WebMarkupContainer div;

  private boolean childAdded;

  /**
   * @param id
   */
  public GridPanel(final String id, final GridType... gridTypes)
  {
    super(id);
    div = new WebMarkupContainer("div");
    final String classAttr = GridType.gridTypesToClassString(gridTypes);
    if (classAttr != null) {
      div.add(AttributeModifier.append("class", classAttr));
    }
    super.add(div);
  }

  @Override
  public GridPanel setMarkupId(final String id)
  {
    div.setMarkupId(id);
    return this;
  }

  /**
   * @see org.apache.wicket.MarkupContainer#add(org.apache.wicket.Component[])
   */
  @Override
  public GridPanel add(final Component... childs)
  {
    if (repeater == null) {
      if (childAdded == true) {
        throw new IllegalArgumentException("You can't add multiple children, please call newChildId instead for using a RepeatingView.");
      }
      childAdded = true;
      div.add(childs);
      return this;
    } else {
      childAdded = true;
      repeater.add(childs);
      return this;
    }
  }

  /**
   * @see org.apache.wicket.MarkupContainer#remove(org.apache.wicket.Component)
   */
  @Override
  public MarkupContainer remove(final Component component)
  {
    if (repeater == null) {
      div.remove(component);
      return this;
    } else {
      repeater.remove(component);
      return this;
    }
  }

  /**
   * @see org.apache.wicket.MarkupContainer#remove(org.apache.wicket.Component)
   */
  @Override
  public GridPanel replace(final Component component)
  {
    div.replace(component);
    return this;
  }

  public boolean hasChilds()
  {
    return childAdded;
  }

  /**
   * Calls div.add(...);
   * @see org.apache.wicket.Component#add(org.apache.wicket.behavior.Behavior[])
   */
  @Override
  public Component add(final Behavior... behaviors)
  {
    return div.add(behaviors);
  }

  public GridPanel add(final GridType... gridTypes)
  {
    final GridPanel panel = new GridPanel(newChildId(), gridTypes);
    add(panel);
    return panel;
  }

  /**
   * Adds a repeater as child if not already exist. You can't use both: {@link #newChildId()} and add a child with {@link #CHILD_ID}.
   * @see RepeatingView#newChildId()
   */
  public String newChildId()
  {
    if (repeater == null) {
      repeater = new RepeatingView("child");
      div.add(repeater);
    }
    return repeater.newChildId();
  }

  /**
   * @return the div
   */
  public WebMarkupContainer getDiv()
  {
    return div;
  }
}
