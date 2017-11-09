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

package org.projectforge.web.core;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.FavoritesMenu;
import org.projectforge.web.Menu;
import org.projectforge.web.MenuBuilder;
import org.projectforge.web.MenuEntry;
import org.projectforge.web.wicket.WicketUtils;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class NavAbstractPanel extends Panel
{
  private static final long serialVersionUID = -1019454504282157440L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(NavAbstractPanel.class);

  protected Menu menu;

  protected FavoritesMenu favoritesMenu;

  @SpringBean
  private MenuBuilder menuBuilder;

  public NavAbstractPanel(final String id)
  {
    super(id);
  }

  static public AbstractLink getMenuEntryLink(final MenuEntry menuEntry, final boolean showModifiedNames)
  {
    final AbstractLink link;
    if (menuEntry.isWicketPage() == true) {
      if (menuEntry.getParams() == null) {
        link = new BookmarkablePageLink<String>("link", menuEntry.getPageClass());
      } else {
        final PageParameters params = WicketUtils.getPageParameters(menuEntry.getParams());
        link = new BookmarkablePageLink<String>("link", menuEntry.getPageClass(), params);
      }
    } else {
      final String url = menuEntry.getUrl();
      if (url != null) {
        link = new ExternalLink("link", WicketUtils.getUrl(RequestCycle.get(), url, true));
      } else {
        link = new ExternalLink("link", "#");
        if (menuEntry.hasSubMenuEntries() == false) {
          // TODO Add empty
        }
      }
    }
    if (menuEntry.isNewWindow() == true) {
      link.add(AttributeModifier.replace("target", "_blank"));
    }
    final String i18nKey = menuEntry.getI18nKey();
    if (showModifiedNames == true && StringUtils.isNotBlank(menuEntry.getName()) == true || i18nKey == null) {
      if (StringUtils.isNotBlank(menuEntry.getName()) == true) {
        link.add(new Label("label", menuEntry.getName()).setRenderBodyOnly(true));
      } else {
        // Neither i18nKey nor name is given:
        link.add(new Label("label", "???").setRenderBodyOnly(true));
      }
    } else {
      link.add(new Label("label", new ResourceModel(i18nKey)).setRenderBodyOnly(true));
    }
    link.add(AttributeModifier.append("ref", menuEntry.getId()));
    final Label menuSuffixLabel = getSuffixLabel(menuEntry);
    link.add(menuSuffixLabel);
    return link;
  }

  static protected Label getSuffixLabel(final MenuEntry menuEntry)
  {
    final Label suffixLabel;
    final IModel<Integer> newCounterModel = menuEntry != null ? menuEntry.getNewCounterModel() : null;
    if (newCounterModel != null && newCounterModel.getObject() != null) {
      suffixLabel = new MenuSuffixLabel(newCounterModel);
      if (menuEntry != null && menuEntry.getNewCounterTooltip() != null) {
        WicketUtils.addTooltip(suffixLabel, new ResourceModel(menuEntry.getNewCounterTooltip()));
      }
    } else {
      suffixLabel = new Label("suffix");
      suffixLabel.setVisible(false);
    }
    return suffixLabel;
  }

  public Menu getMenu()
  {
    if (menu == null) {
      menu = menuBuilder.getMenu(ThreadLocalUserContext.getUser());
    }
    return menu;
  }
}
