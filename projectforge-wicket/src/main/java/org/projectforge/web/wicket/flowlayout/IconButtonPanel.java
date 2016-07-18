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

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.WicketUtils;

/**
 * Represents an icon.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class IconButtonPanel extends Panel
{
  private static final long serialVersionUID = 3317775585548133768L;

  private final Button button;

  private WebMarkupContainer icon;

  public IconButtonPanel(final String id, final IconType type)
  {
    this(id, type, (String) null);
  }

  public IconButtonPanel(final String id, final IconType type, final String tooltip)
  {
    super(id);
    button = createButton("button");
    add(button);
    init(type, tooltip);
  }

  public IconButtonPanel(final String id, final IconType type, final IModel<String> tooltip)
  {
    super(id);
    button = createButton("button");
    add(button);
    init(type, null);
    if (tooltip != null) {
      WicketUtils.addTooltip(button, tooltip);
    }
  }

  public IconButtonPanel(final String id, final Button button, final IconType type, final String tooltip)
  {
    super(id);
    this.button = button;
    add(button);
    init(type, tooltip);
  }

  /**
   * Sets "light" as class attribute for having light grey colored buttons.
   * @return this for chaining.
   */
  public IconButtonPanel setLight()
  {
    icon.add(AttributeModifier.append("class", "glyphicon-white"));
    return this;
  }

  /**
   * @param defaultFormProcessing
   * @return this for chaining.
   * @see Button#setDefaultFormProcessing(boolean)
   */
  public IconButtonPanel setDefaultFormProcessing(final boolean defaultFormProcessing)
  {
    button.setDefaultFormProcessing(defaultFormProcessing);
    return this;
  }

  /**
   * @return the button
   */
  public Button getButton()
  {
    return button;
  }

  /**
   * @param attributeName
   * @param value
   * @return this for chaining.
   * @see AttributeModifier#append(String, java.io.Serializable)
   */
  public IconButtonPanel oldAppendAttribute(final String attributeName, final Serializable value)
  {
    button.add(AttributeModifier.append(attributeName, value));
    return this;
  }

  /**
   * @see org.apache.wicket.markup.html.form.Button#onSubmit()
   */
  protected void onSubmit()
  {
  };

  private void init(final IconType type, final String tooltip)
  {
    icon = new WebMarkupContainer("icon");
    button.add(icon);
    button.add(new Label("text").setVisible(false));
    icon.add(AttributeModifier.append("class", type.getClassAttrValue()));
    if (tooltip != null) {
      WicketUtils.addTooltip(button, tooltip);
    }
  }

  /**
   * @param string
   * @return
   */
  protected Button createButton(final String string)
  {
    return new Button("button") {
      private static final long serialVersionUID = 1L;

      /**
       * @see org.apache.wicket.markup.html.form.Button#onSubmit()
       */
      @Override
      public void onSubmit()
      {
        IconButtonPanel.this.onSubmit();
      }

      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        return IconButtonPanel.this.isButtonVisible();
      }
    };
  }

  protected boolean isButtonVisible()
  {
    return super.isVisible();
  }

}
