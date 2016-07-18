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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.CSSColor;

/**
 * Represents an icon.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class IconLinkPanel extends Panel
{
  private static final long serialVersionUID = 3317775585548133768L;

  public static final String LINK_ID = "link";

  private AbstractLink link;

  private WebMarkupContainer icon;

  private final IconType type;

  private IModel<String> tooltipTitle;

  private IModel<String> tooltip;

  private CSSColor color;

  private boolean buttonStyle;

  public IconLinkPanel(final String id, final IconType type)
  {
    this(id, type, (IModel<String>) null);
  }

  public IconLinkPanel(final String id, final IconType type, final AbstractLink link)
  {
    this(id, type, null, link);
  }

  /**
   * @param id
   * @param type
   * @param link Must have component id {@link #LINK_ID}
   * @param tooltip
   */
  public IconLinkPanel(final String id, final IconType type, final IModel<String> tooltip, final AbstractLink link)
  {
    this(id, type, tooltip);
    setLink(link);
  }

  /**
   * @param id
   * @param type
   * @param link Must have component id {@link #LINK_ID}
   * @param tooltip
   */
  public IconLinkPanel(final String id, final IconType type, final IModel<String> tooltip)
  {
    super(id);
    this.type = type;
    this.tooltip = tooltip;
    this.color = IconPanel.getColor(type);
  }

  public IconLinkPanel setLink(final AbstractLink link)
  {
    this.link = link;
    add(link);
    icon = new WebMarkupContainer("icon");
    icon.add(AttributeModifier.append("class", type.getClassAttrValue()));
    link.add(icon);
    return this;
  }

  public IconLinkPanel setTooltip(final IModel<String> tooltip)
  {
    this.tooltip = tooltip;
    return this;
  }

  public IconLinkPanel setTooltip(final IModel<String> title, final IModel<String> tooltip)
  {
    this.tooltipTitle = title;
    this.tooltip = tooltip;
    return this;
  }

  /**
   * @param color the color to set
   * @return this for chaining.
   */
  public IconLinkPanel setColor(final CSSColor color)
  {
    this.color = color;
    return this;
  }

  /**
   * The css class "btn" will be added, so the link looks like a button.
   * @return this for chaining.
   */
  public IconLinkPanel setButtonStyle()
  {
    this.buttonStyle = true;
    return this;
  }

  /**
   * 
   * @param attributeName
   * @param value
   * @return this for chaining.
   * @see AttributeModifier#append(String, java.io.Serializable)
   */
  public IconLinkPanel appendAttribute(final String attributeName, final Serializable value)
  {
    link.add(AttributeModifier.append(attributeName, value));
    return this;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    if (this.color != null) {
      icon.add(AttributeAppender.append("class", this.color.getCssClass()));
    }
    if (this.buttonStyle == true) {
      icon.add(AttributeAppender.append("class", "btn"));
    }
    if (tooltip != null) {
      WicketUtils.addTooltip(link, tooltipTitle, tooltip);
    }
    super.onInitialize();
  }
}
