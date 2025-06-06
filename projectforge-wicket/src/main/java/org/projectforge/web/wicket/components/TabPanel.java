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

package org.projectforge.web.wicket.components;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.AbstractItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.projectforge.web.wicket.bootstrap.GridBuilder;

import java.util.HashMap;
import java.util.Map;

public class TabPanel extends Panel
{
  private final RepeatingView tabs = new RepeatingView("tabs");

  private final RepeatingView tabPanes = new RepeatingView("tabPanes");

  private final Map<String, GridBuilder> tabContainerCache = new HashMap<>();

  private GridBuilder defaultTabContainer;

  public TabPanel(String id)
  {
    super(id);
    add(tabs);
    add(tabPanes);
  }

  public GridBuilder getOrCreateTab(final String i18nKey)
  {
    return getOrCreateTab(i18nKey, false);
  }

  public GridBuilder getOrCreateTab(final String i18nKey, final boolean activeAndDefault)
  {
    if (StringUtils.isEmpty(i18nKey)) {
      return defaultTabContainer;
    }

    if (tabContainerCache.containsKey(i18nKey)) {
      return tabContainerCache.get(i18nKey);
    }

    final String markupId = "tab_" + i18nKey.replace('.', '_');

    final AbstractItem tab = new AbstractItem(tabs.newChildId());
    tab.add(new ExternalLink("tabLink", "#" + markupId, getString(i18nKey)));
    tabs.add(tab);

    final AbstractItem tabPane = new AbstractItem(tabPanes.newChildId());
    tabPane.add(AttributeAppender.replace("id", markupId));
    tabPanes.add(tabPane);

    final GridBuilder tabContainer = new GridBuilder(tabPane, "tabContainer");
    tabContainerCache.put(i18nKey, tabContainer);

    if (activeAndDefault) {
      tab.add(AttributeAppender.append("class", "active"));
      tabPane.add(AttributeAppender.append("class", "active"));
      defaultTabContainer = tabContainer;
    }

    return tabContainer;
  }
}
