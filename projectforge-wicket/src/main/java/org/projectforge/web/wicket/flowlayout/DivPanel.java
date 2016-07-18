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
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.bootstrap.GridType;

/**
 * Represents a entry of a group panel. This can be a label, text field or other form components.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DivPanel extends Panel
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DivPanel.class);

  private static final long serialVersionUID = 6130552547273354134L;

  RepeatingView repeater;

  WebMarkupContainer div;

  private GridSize gridSize = null;

  private DivPanelVisibility visibility;

  private int childCounter = 0;

  private boolean buttonGroup = false;

  /**
   * @param id
   */
  public DivPanel(final String id, final DivType... cssClasses)
  {
    this(id);
    addCssClasses(cssClasses);
  }

  /**
   * @param id
   */
  public DivPanel(final String id)
  {
    super(id);
    div = new WebMarkupContainer("div");
    super.add(div);
    repeater = new RepeatingView("child");
    div.add(repeater);
  }

  /**
   * @param id
   */
  public DivPanel(final String id, final GridType... cssClasses)
  {
    this(id);
    addCssClasses(cssClasses);
  }

  /**
   * @param id
   */
  public DivPanel(final String id, final GridSize divSize, final GridType... cssClasses)
  {
    super(id);
    this.gridSize = divSize;
    div = new WebMarkupContainer("div");
    super.add(div);
    div.add(AttributeModifier.append("class", divSize.getClassAttrValue()));
    addCssClasses(cssClasses);
    repeater = new RepeatingView("child");
    div.add(repeater);
  }

  public DivPanel addCssClasses(final DivType... cssClasses)
  {
    if (cssClasses != null) {
      for (final DivType cssClass : cssClasses) {
        if (cssClass != null) {
          div.add(AttributeModifier.append("class", cssClass.getClassAttrValue()));
          if (cssClass == DivType.BTN_GROUP) {
            add(AttributeModifier.append("data-toggle", "buttons"));
            buttonGroup = true;
          }
        }
      }
    }
    return this;
  }

  /**
   * @param visibility the visibility to set
   * @return this for chaining.
   */
  public DivPanel setVisibility(final DivPanelVisibility visibility)
  {
    this.visibility = visibility;
    return this;
  }

  /**
   * @return the gridSize
   */
  public GridSize getGridSize()
  {
    return gridSize;
  }

  public DivPanel addCssClasses(final GridType... cssClasses)
  {
    if (cssClasses != null) {
      for (final GridType cssClass : cssClasses) {
        if (cssClass != null) {
          div.add(AttributeModifier.append("class", cssClass.getClassAttrValue()));
        }
      }
    }
    return this;
  }

  @Override
  public DivPanel setMarkupId(final String id)
  {
    div.setMarkupId(id);
    return this;
  }

  /**
   * @see org.apache.wicket.MarkupContainer#add(org.apache.wicket.Component[])
   */
  @Override
  public DivPanel add(final Component... childs)
  {
    if (WebConfiguration.isDevelopmentMode() == true) {
      for (final Component child : childs) {
        if (child instanceof CheckBoxButton) {
          if (buttonGroup == false) {
            log.warn("*** Dear developer: this DivPanel should be use css class "
                + DivType.BTN_GROUP
                + " if CheckBoxButtons are added! Otherwise check box buttons doesn't work.");
          }
        }
        if (child instanceof RadioGroupPanel) {
          if (buttonGroup == false) {
            log.warn("*** Dear developer: this DivPanel should be use css class "
                + DivType.BTN_GROUP
                + " if RadioGroupPanel are added! Otherwise radio box buttons doesn't work.");
          }
        }
      }
    }
    repeater.add(childs);
    return this;

  }

  /**
   * @see org.apache.wicket.MarkupContainer#remove(org.apache.wicket.Component)
   */
  @Override
  public MarkupContainer remove(final Component component)
  {
    repeater.remove(component);
    return this;
  }

  /**
   * @see org.apache.wicket.MarkupContainer#remove(org.apache.wicket.Component)
   */
  @Override
  public DivPanel replace(final Component component)
  {
    div.replace(component);
    return this;
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

  /**
   * Adds a repeater as child if not already exist. You can't use both: {@link #newChildId()} and add a child with {@link #CHILD_ID}.
   * @see RepeatingView#newChildId()
   */
  public String newChildId()
  {
    childCounter++;
    return repeater.newChildId();
  }

  public CheckBoxButton addCheckBoxButton(final IModel<Boolean> model, final String labelString)
  {
    return addCheckBoxButton(model, labelString, null);
  }

  public CheckBoxButton addCheckBoxButton(final IModel<Boolean> model, final String labelString, final String tooltip)
  {
    final CheckBoxButton checkBox = new CheckBoxButton(newChildId(), model, labelString);
    if (tooltip != null) {
      checkBox.setTooltip(tooltip);
    }
    add(checkBox);
    return checkBox;
  }

  /**
   * @return the div
   */
  public WebMarkupContainer getDiv()
  {
    return div;
  }

  /**
   * @see org.apache.wicket.Component#isVisible()
   */
  @Override
  public boolean isVisible()
  {
    if (visibility != null) {
      return visibility.isVisible();
    } else {
      return super.isVisible();
    }
  }

  public boolean hasChilds()
  {
    return childCounter > 0;
  }
}
