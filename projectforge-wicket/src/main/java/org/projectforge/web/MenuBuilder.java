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

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.projectforge.business.user.UserChangedListener;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.core.NavAbstractPanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Build of the user's personal menu (depending on the access rights of the user).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Service
public class MenuBuilder implements Serializable, UserChangedListener
{
  private static final long serialVersionUID = -924049082728488113L;

  @Autowired
  UserDao userDao;

  @Autowired
  AccessChecker accessChecker;

  @Autowired
  UserRightService userRights;

  @Autowired
  MenuItemRegistry registry;

  @Autowired
  UserXmlPreferencesCache userXmlPreferencesCache;

  private final MenuCache menuCache = new MenuCache();

  @PostConstruct
  public void init()
  {
    userDao.register(this);
  }

  public void expireMenu(final Integer userId)
  {
    menuCache.removeMenu(userId);
  }

  public void refreshAllMenus()
  {
    menuCache.setExpired();
  }

  private void buildMenuTree(final Menu menu, final PFUserDO user, final boolean mobileMenu)
  {
    if (user == null) {
      return;
    }
    final MenuBuilderContext context = new MenuBuilderContext(menu, user, mobileMenu, accessChecker, userRights);
    for (final MenuItemDef menuItemDef : registry.getMenuItemList()) {
      if (menuItemDef.isVisible(context) == false) {
        // Menu entry isn't visible for the user:
        continue;
      }
      final MenuEntry menuEntry = menuItemDef.createMenuEntry(menu, context);
      if (menuEntry == null) {
        continue;
      }
      // Nothing needed to be done.
    }
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
    Menu menu = null;
    if (user != null) {
      if (mobileMenu == true) {
        menu = menuCache.getMobileMenu(user.getId());
      } else {
        menu = menuCache.getMenu(user.getId());
      }
      if (menu != null) {
        return menu;
      }
    }
    menu = new Menu();
    buildMenuTree(menu, user, mobileMenu);
    if (user != null) {
      if (mobileMenu == true) {
        menuCache.putMobileMenu(user.getId(), menu);
      } else {
        menuCache.putMenu(user.getId(), menu);
      }
    }
    return menu;
  }

  /**
   * @see org.projectforge.business.user.UserChangedListener#afterUserChanged(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      org.projectforge.framework.access.OperationType)
   */
  @Override
  public void afterUserChanged(final PFUserDO user, final OperationType operationType)
  {
    if (user != null) {
      expireMenu(user.getId());
      // Force reloading the users menu from cache:
      userXmlPreferencesCache.removeEntry(user.getId(), NavAbstractPanel.USER_PREF_MENU_KEY);
    }
  }
}
