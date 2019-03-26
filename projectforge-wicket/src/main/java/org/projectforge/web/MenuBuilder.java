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
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.menu.MenuItem;
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
public class MenuBuilder {
  @Autowired
  private MenuCreator menuCreator;

  @Autowired
  private MenuItemRegistry menuItemRegistry;

  public Menu getMenu(final PFUserDO user) {
    return buildMenuTree(user);
  }

  private Menu buildMenuTree(final PFUserDO user) {
    if (user == null) {
      return null;
    }
    final Menu menu = new Menu();
    List<MenuItem> topMenus = menuCreator.build(new MenuCreatorContext(user, false));
    for (MenuItem item : topMenus) {
      MenuEntry entry = createMenuEntry(item, menu);
      menu.addMenuEntry(entry);
      if (CollectionUtils.isNotEmpty(item.getSubMenu())) {
        buildMenuTree(entry, item, menu);
      }
    }
    return menu;
  }

  private void buildMenuTree(MenuEntry parent, MenuItem parentItem, Menu menu) {
    for (MenuItem item : parentItem.getSubMenu()) {
      MenuEntry entry = createMenuEntry(item, menu);
      parent.addMenuEntry(entry);
    }
  }

  private MenuEntry createMenuEntry(MenuItem item, Menu menu) {
    MenuEntry entry = new MenuEntry();
    entry.id = item.getKey();
    entry.pageClass = menuItemRegistry.getPageClass(item.getId());
    entry.i18nKey = item.getTitle();
    entry.setMenu(menu);
    return entry;
  }
}