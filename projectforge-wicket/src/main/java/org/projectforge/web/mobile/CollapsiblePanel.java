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

package org.projectforge.web.mobile;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;

/**
 * A Collapsible panel has a heading and a content (as repeating view). With a click on the heading the user is able to collapse and
 * uncollapse the content.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class CollapsiblePanel extends Panel
{
  private static final long serialVersionUID = 4624904578211630743L;

  private final RepeatingView repeater;

  private boolean hasChildren;

  private final WebMarkupContainer mainContainer;

  private Label headingLabel;

  private boolean initialized;

  public CollapsiblePanel(final String id, final String heading)
  {
    super(id);
    mainContainer = new WebMarkupContainer("mainContainer");
    super.add(mainContainer);
    if (heading != null) {
      headingLabel = new Label("heading", heading);
    }
    repeater = new RepeatingView("contentRepeater");
    mainContainer.add(repeater);
  }

  /**
   * @param heading
   * @return this for chaining.
   */
  public CollapsiblePanel setHeadingLabel(final String heading)
  {
    this.headingLabel = new Label("heading", heading);
    return this;
  }

  /**
   * @param theme
   * @return this for chaining.
   */
  public CollapsiblePanel setTheme(final ThemeType theme)
  {
    mainContainer.add(AttributeModifier.replace("data-theme", theme.getCssId()));
    return this;
  }

  /**
   * Set collapsed at default.
   * @return this for chaining.
   */
  public CollapsiblePanel setCollapsed()
  {
    mainContainer.add(AttributeModifier.replace("data-collapsed", "true"));
    return this;
  }

  public String newChildId()
  {
    return repeater.newChildId();
  }

  /**
   * @param child
   * @return this for chaining.
   */
  public CollapsiblePanel add(final Component child)
  {
    repeater.add(child);
    hasChildren = true;
    return this;
  }

  public boolean hasChildren()
  {
    return hasChildren;
  }

  @Override
  protected void onBeforeRender()
  {
    if (initialized == false) {
      initialized = true;
      if (headingLabel != null) {
        mainContainer.add(headingLabel);
      } else {
        mainContainer.add(new Label("heading", "&nbsp;").setEscapeModelStrings(false));
      }
    }
    super.onBeforeRender();
  }
}
