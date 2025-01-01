/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.WicketMenu;
import org.projectforge.web.WicketMenuBuilder;
import org.projectforge.web.WicketMenuEntry;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.WicketUtils;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class NavAbstractPanel extends Panel {
  private static final long serialVersionUID = -1019454504282157440L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NavAbstractPanel.class);

  protected WicketMenu menu;

  protected WicketMenu favoritesMenu;

  public NavAbstractPanel(final String id) {
    super(id);
  }

  static public AbstractLink getMenuEntryLink(final WicketMenuEntry menuEntry, final boolean showModifiedNames) {
    final AbstractLink link;
    if (menuEntry.isWicketPage() == true) {
      link = new BookmarkablePageLink<String>("link", menuEntry.getPageClass());
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

  static protected Label getSuffixLabel(final WicketMenuEntry menuEntry) {
    final Label suffixLabel;
    final IModel<Integer> newCounterModel = menuEntry != null ? menuEntry.getBadgeCounter() : null;
    if (newCounterModel != null && newCounterModel.getObject() != null) {
      suffixLabel = new MenuSuffixLabel(newCounterModel);
      if (menuEntry != null && menuEntry.getBadgeCounterTooltip() != null) {
        WicketUtils.addTooltip(suffixLabel, new ResourceModel(menuEntry.getBadgeCounterTooltip()));
      }
    } else {
      suffixLabel = new Label("suffix");
      suffixLabel.setVisible(false);
    }
    return suffixLabel;
  }

  public WicketMenu getMenu() {
    if (menu == null) {
      menu = WicketSupport.get(WicketMenuBuilder.class).getMenu(ThreadLocalUserContext.getLoggedInUser());
    }
    return menu;
  }
}
