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
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.web.wicket.WicketUtils;

/**
 * A jquery button with an icon (e. g. at the top right corner).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class JQueryButtonPanel extends Panel
{
  private static final long serialVersionUID = 6460153798143225741L;

  private final Class< ? extends WebPage> pageClass;

  private final PageParameters params;

  private final JQueryButtonType type;

  private boolean initialized;

  private final String label;

  private boolean relExternal;

  private boolean relDialog, noText;

  private Alignment alignment;

  public JQueryButtonPanel(final String id, final JQueryButtonType type, final Class< ? extends WebPage> pageClass, final String label)
  {
    this(id, type, pageClass, null, label);
  }

  public JQueryButtonPanel(final String id, final JQueryButtonType type, final Class< ? extends WebPage> pageClass,
      final PageParameters params, final String label)
  {
    super(id);
    this.pageClass = pageClass;
    this.params = params;
    this.type = type;
    this.label = label;
  }

  public JQueryButtonPanel setRelExternal()
  {
    this.relExternal = true;
    return this;
  }

  public JQueryButtonPanel setRelDialog()
  {
    this.relDialog = true;
    return this;
  }

  public JQueryButtonPanel setNoText()
  {
    this.noText = true;
    return this;
  }

  public JQueryButtonPanel setAlignment(final Alignment alignment)
  {
    this.alignment = alignment;
    return this;
  }

  @Override
  protected void onBeforeRender()
  {
    if (initialized == false) {
      initialized = true;
      final BookmarkablePageLink<String> link;
      if (params == null) {
        link = new BookmarkablePageLink<String>("button", pageClass);
      } else {
        link = new BookmarkablePageLink<String>("button", pageClass, params);
      }
      if (type != null) {
        link.add(AttributeModifier.replace("data-icon", type.getCssId()));
      }
      add(link);
      if (label != null) {
        link.add(new Label("label", label));
      } else {
        link.add(WicketUtils.getInvisibleComponent("label"));
      }
      if (this.relExternal == true) {
        link.add(AttributeModifier.replace("rel", "external"));
      }
      if (this.relDialog == true) {
        link.add(AttributeModifier.replace("data-rel", "dialog"));
      }
      if (this.noText == true) {
        link.add(AttributeModifier.replace("data-iconpos", "notext"));
      }
      //      if (alignment == Alignment.LEFT) {
      //        link.add(AttributeModifier.add("class", "ui-btn-left"));
      //      }
    }
    super.onBeforeRender();
  }
}
