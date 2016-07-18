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

package org.projectforge.web.wicket.components;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.projectforge.web.wicket.flowlayout.MyComponentsRepeater;

/**
 * Panel for using as content top menu entry (needed for css decoration).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ContentMenuEntryPanel extends Panel
{
  private static final long serialVersionUID = -5507326592369611604L;

  public static final String LINK_ID = "link";

  /**
   * Should also be used for icon panels.
   */
  public static final String LABEL_ID = "label";

  private AbstractLink link;

  private boolean hasLink;

  private String label;

  private Component labelComponent;

  private WebMarkupContainer li, caret;

  private final WebMarkupContainer dropDownMenu;

  /**
   * List to create content menu in the desired order before creating the RepeatingView.
   */
  protected MyComponentsRepeater<ContentMenuEntryPanel> subMenu;

  private ContentMenuEntryPanel(final String id)
  {
    super(id);
    add(li = new WebMarkupContainer("li"));
    add(li);
    li.setMarkupId("addEntry").setOutputMarkupId(true);
    dropDownMenu = new WebMarkupContainer("dropdownMenu");
    li.add(dropDownMenu);
    subMenu = new MyComponentsRepeater<ContentMenuEntryPanel>("subMenu");
    dropDownMenu.add(subMenu.getRepeatingView());

  }

  public ContentMenuEntryPanel(final String id, final AbstractLink link, final String label)
  {
    this(id);
    this.link = link;
    labelComponent = new Label("label", label).setRenderBodyOnly(true);
    this.label = label;
  }

  public ContentMenuEntryPanel(final String id, final AbstractLink link, final IconType iconType)
  {
    this(id);
    this.link = link;
    labelComponent = new IconPanel(LABEL_ID, iconType);
  }

  public ContentMenuEntryPanel(final String id, final String label)
  {
    this(id);
    labelComponent = new Label("label", label).setRenderBodyOnly(true);
    this.label = label;
  }

  public ContentMenuEntryPanel(final String id, final IconType iconType)
  {
    this(id);
    labelComponent = new IconPanel(LABEL_ID, iconType);
  }

  public String newSubMenuChildId()
  {
    return subMenu.newChildId();
  }


  public ContentMenuEntryPanel addSubMenuEntry(final ContentMenuEntryPanel menuEntry)
  {
    subMenu.add(menuEntry);
    return this;
  }

  /**
   * Adds html attribute "accesskey".
   * @param ch
   * @return this for chaining.
   */
  public ContentMenuEntryPanel setAccessKey(final char ch)
  {
    link.add(AttributeModifier.replace("accesskey", String.valueOf(ch)));
    return this;
  }

  /**
   * @param tooltip
   * @return this for chaining.
   * @see WicketUtils#addTooltip(org.apache.wicket.Component, String)
   */
  public ContentMenuEntryPanel setTooltip(final String tooltip)
  {
    WicketUtils.addTooltip(link, tooltip, true);
    return this;
  }

  /**
   * @param title
   * @param text
   * @return this for chaining.
   * @see WicketUtils#addTooltip(org.apache.wicket.Component, String, String)
   */
  public ContentMenuEntryPanel setTooltip(final String title, final String text)
  {
    if (labelComponent instanceof IconPanel) {
      // Needed because hover doesn't work if a tag contains embedded i tag.
      ((IconPanel)labelComponent).setTooltip(Model.of(title), Model.of(text));
    }
    WicketUtils.addTooltip(link, title, text, true);
    return this;
  }

  /**
   * @param title
   * @param text
   * @return this for chaining.
   * @see WicketUtils#addTooltip(org.apache.wicket.Component, String, String)
   */
  public ContentMenuEntryPanel setTooltip(final IModel<String> title, final IModel<String> text)
  {
    WicketUtils.addTooltip(link, title, text);
    return this;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @SuppressWarnings("serial")
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    if (link == null) {
      this.link = new AbstractLink(LINK_ID) {
      };
    } else {
      this.hasLink = true;
    }
    li.add(link);
    link.add(labelComponent);
    caret = new WebMarkupContainer("caret");
    if (subMenu.hasEntries() == true) {
      // Children available.
      link.add(AttributeModifier.append("class", "dropdown-toggle"));
      link.add(AttributeModifier.append("data-toggle", "dropdown"));
      li.add(AttributeModifier.append("class", "dropdown"));
    } else {
      dropDownMenu.setVisible(false);
      caret.setVisible(false);
    }
    link.add(caret);
  }

  @Override
  protected void onBeforeRender()
  {
    subMenu.render();
    super.onBeforeRender();
  }

  /**
   * @see org.apache.wicket.Component#isVisible()
   */
  @Override
  public boolean isVisible()
  {
    return super.isVisible() && (this.hasLink == true || subMenu.hasEntries());
  }

  /**
   * @return the label
   */
  public String getLabel()
  {
    return label;
  }
}
