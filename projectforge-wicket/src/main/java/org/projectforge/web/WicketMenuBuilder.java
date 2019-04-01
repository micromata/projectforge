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

package org.projectforge.web;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.menu.Menu;
import org.projectforge.menu.MenuItem;
import org.projectforge.menu.builder.FavoritesMenuCreator;
import org.projectforge.menu.builder.MenuCreator;
import org.projectforge.menu.builder.MenuCreatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Build of the user's personal menu (depending on the access rights of the user).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class WicketMenuBuilder {
  @Autowired
  private MenuCreator menuCreator;

  @Autowired
  private FavoritesMenuCreator favoritesMenuCreator;

  @Autowired
  private MenuItemRegistry menuItemRegistry;

  public WicketMenu getFavoriteMenu() {
    Menu menu = favoritesMenuCreator.getDefaultFavoriteMenu();
    return buildMenuTree(menu);
  }

  public WicketMenu getMenu(final PFUserDO user) {
    return buildMenuTree(user);
  }

  private WicketMenu buildMenuTree(final PFUserDO user) {
    if (user == null) {
      return null;
    }
    Menu menu = menuCreator.build(new MenuCreatorContext(user, false));
    return buildMenuTree(menu);
  }

  private WicketMenu buildMenuTree(Menu menu) {
    WicketMenu wicketMenu = new WicketMenu();
    for (MenuItem item : menu.getMenuItems()) {
      WicketMenuEntry entry = createMenuEntry(item, menu);
      wicketMenu.addMenuEntry(entry);
      if (CollectionUtils.isNotEmpty(item.getSubMenu())) {
        buildMenuTree(entry, item, menu);
      }
    }
    return wicketMenu;
  }

  private void buildMenuTree(WicketMenuEntry parent, MenuItem parentItem, Menu menu) {
    for (MenuItem item : parentItem.getSubMenu()) {
      WicketMenuEntry entry = createMenuEntry(item, menu);
      parent.addMenuEntry(entry);
    }
  }

  private WicketMenuEntry createMenuEntry(MenuItem item, Menu menu) {
    WicketMenuEntry entry = new WicketMenuEntry();
    entry.id = item.getKey();
    if (item.getI18nKey() != null)
      entry.i18nKey = item.getI18nKey();
    else
    if (StringUtils.isNotBlank(item.getTitle()))
      entry.name = item.getTitle();
    else
      entry.name = "???";
    entry.pageClass = menuItemRegistry.getPageClass(item.getId());
    return entry;
  }
}