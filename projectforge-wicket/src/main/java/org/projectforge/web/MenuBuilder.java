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

import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Build of the user's personal menu (depending on the access rights of the user).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Service
public class MenuBuilder
{
  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private UserRightService userRights;

  @Autowired
  private MenuItemRegistry registry;

  private Menu buildMenuTree(final PFUserDO user, final boolean mobileMenu)
  {
    if (user == null) {
      return null;
    }
    final Menu menu = new Menu();
    final MenuBuilderContext context = new MenuBuilderContext(menu, user, mobileMenu, accessChecker, userRights);
    for (final MenuItemDef menuItemDef : registry.getMenuItemList()) {
      if (menuItemDef.isVisible(context) == false) {
        // Menu entry isn't visible for the user:
        continue;
      }
      menuItemDef.createMenuEntry(menu, context);
    }
    return menu;
  }

  public Menu getMenu(final PFUserDO user)
  {
    return getMenu(user, false);
  }

  public Menu getMobileMenu(final PFUserDO user)
  {
    return getMenu(user, true);
  }

  private Menu getMenu(final PFUserDO user, final boolean mobileMenu)
  {
    return buildMenuTree(user, mobileMenu);
  }
}