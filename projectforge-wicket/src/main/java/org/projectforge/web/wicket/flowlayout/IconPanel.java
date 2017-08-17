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
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.projectforge.web.CSSColor;
import org.projectforge.web.wicket.WicketUtils;

/**
 * Represents an icon. Supports Ajax onclick behavior if {@link #enableAjaxOnClick()} is called.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class IconPanel extends Panel
{
  private static final long serialVersionUID = 3317775585548133768L;

  private final WebMarkupContainer div;

  private CSSColor color;

  private IModel<String> tooltipTitle;

  private IModel<String> tooltipText;

  /**
   * Get default color of icon type (if defined). E. g. red for {@link IconType#DENY}.
   */
  public static CSSColor getColor(final IconType type)
  {
    if (type == null) {
      return null;
    }
    if (type.isIn(IconType.REMOVE_SIGN, IconType.DENY) == true) {
      return CSSColor.RED;
    }
    if (type.isIn(IconType.ACCEPT, IconType.PLUS_SIGN) == true) {
      return CSSColor.GREEN;
    }
    return null;
  }

  public IconPanel(final String id, final IconType type)
  {
    this(id, type, (String) null);
  }

  public IconPanel(final String id, final IconType type, final String tooltip)
  {
    this(id, type, Model.of(tooltip));
  }

  public IconPanel(final String id, final IconType type, final IModel<String> tooltip)
  {
    this(id, type, null, tooltip);
  }

  public IconPanel(final String id, final IconType type, final IModel<String> title, final IModel<String> tooltip)
  {
    super(id);
    div = new WebMarkupContainer("div");
    add(div);
    appendAttribute("class", type.getClassAttrValue());
    this.tooltipTitle = title;
    this.tooltipText = tooltip;
    this.color = getColor(type);
  }

  public IconPanel setTooltip(final IModel<String> tooltip)
  {
    this.tooltipText = tooltip;
    return this;
  }

  public IconPanel setTooltip(final IModel<String> title, final IModel<String> tooltip)
  {
    this.tooltipTitle = title;
    this.tooltipText = tooltip;
    return this;
  }

  /**
   * @param color the color to set
   * @return this for chaining.
   */
  public IconPanel setColor(final CSSColor color)
  {
    this.color = color;
    return this;
  }

  /**
   * Enable Ajax onclick event. If clicked by the user {@link #onClick()} is called.
   */
  @SuppressWarnings("serial")
  public IconPanel enableAjaxOnClick()
  {
    appendAttribute("style", "cursor: pointer;");
    final AjaxEventBehavior behavior = new AjaxEventBehavior("click")
    {
      @Override
      protected void onEvent(final AjaxRequestTarget target)
      {
        IconPanel.this.onClick();
      }
    };
    div.add(behavior);
    return this;
  }

  /**
   * Appends attribute onclick and changes the cursor to pointer.
   *
   * @return
   */
  public IconPanel setOnClick(final String onclick)
  {
    appendAttribute("style", "cursor: pointer;");
    appendAttribute("onclick", onclick);
    return this;
  }

  /**
   * Appends attribute onclick and changes the cursor to pointer. onclick results in location.href.
   *
   * @param location  url to go on click.
   * @param newWindow If true then a new browser with the given url is opened.
   * @return
   */
  public IconPanel setOnClickLocation(final RequestCycle requestCycle, final String location, final boolean newWindow)
  {
    appendAttribute("style", "cursor: pointer;");
    final String url = WicketUtils.getUrl(requestCycle, location, false);
    if (newWindow == true) {
      appendAttribute("onclick", "window.open('" + url + "'); return false;");
    } else {
      appendAttribute("onclick", "location.href='" + url + "';");
    }
    return this;
  }

  /**
   * @see org.apache.wicket.Component#setMarkupId(java.lang.String)
   */
  @Override
  public Component setMarkupId(final String markupId)
  {
    div.setOutputMarkupId(true);
    return div.setMarkupId(markupId);
  }

  /**
   * @return the div
   */
  public WebMarkupContainer getDiv()
  {
    return div;
  }

  /**
   * @param attributeName
   * @param value
   * @return this for chaining.
   * @see AttributeModifier#append(String, java.io.Serializable)
   */
  public IconPanel appendAttribute(final String attributeName, final Serializable value)
  {
    div.add(AttributeModifier.append(attributeName, value));
    return this;
  }

  /**
   * Don't forget to call {@link #enableAjaxOnClick()}.
   */
  public void onClick()
  {
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    if (this.color != null) {
      div.add(AttributeAppender.append("class", this.color.getCssClass()));
    }
    if (tooltipText != null) {
      WicketUtils.addTooltip(div, tooltipTitle, tooltipText);
    }
    super.onInitialize();
  }
}
