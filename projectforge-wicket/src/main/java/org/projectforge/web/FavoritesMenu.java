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
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.projectforge.business.user.UserXmlPreferencesDO;
import org.projectforge.business.user.service.UserPreferencesService;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.menu.builder.MenuCreator;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.business.user.service.UserPreferencesHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * The customizable menu of the user (stored in the data-base and customizable).
 */
public class FavoritesMenu implements Serializable {
  public static final String USER_PREF_FAVORITES_MENU_KEY = "usersFavoritesMenu";

  static final String USER_PREF_FAVORITES_MENU_ENTRIES_KEY = "usersFavoriteMenuEntries";

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FavoritesMenu.class);

  private static final long serialVersionUID = -4954464926815538198L;

  private List<MenuEntry> menuEntries;

  private Menu menu;

  private MenuCreator menuCreator;

  private AccessChecker accessChecker;

  private UserPreferencesService userPreferencesService;

  public static FavoritesMenu get(MenuCreator menuCreator, MenuBuilder menuBuilder, AccessChecker accessChecker) {
    FavoritesMenu favoritesMenu = (FavoritesMenu) UserPreferencesHelper.getEntry(USER_PREF_FAVORITES_MENU_KEY);
    if (favoritesMenu != null) {
      return favoritesMenu;
    }
    favoritesMenu = new FavoritesMenu(menuCreator, menuBuilder, accessChecker);
    UserPreferencesHelper.putEntry(USER_PREF_FAVORITES_MENU_KEY, favoritesMenu, false);
    return favoritesMenu;
  }

  /**
   * @param accessChecker For building the menu entries regarding the access rights of the logged-in user.
   */
  FavoritesMenu(MenuCreator menuCreator, MenuBuilder menuBuilder, AccessChecker accessChecker) {
    this.menu = menuBuilder.getMenu(ThreadLocalUserContext.getUser());
    this.menuCreator = menuCreator;
    this.accessChecker = accessChecker;
    init();
  }

  public List<MenuEntry> getMenuEntries() {
    return this.menuEntries;
  }

  /**
   * Only for test cases.
   *
   * @param menu the menu to set
   * @return this for chaining.
   */
  FavoritesMenu setMenu(final Menu menu) {
    this.menu = menu;
    return this;
  }

  public void readFromXml(final String menuAsXml) {
    if (menu == null) {
      log.error("User's menu is null, can't get FavoritesMenu!");
      return;
    }
    if (log.isDebugEnabled() == true) {
      log.debug("readFromXml: " + menuAsXml);
    }
    Document document = null;
    try {
      document = DocumentHelper.parseText(menuAsXml);
    } catch (final DocumentException ex) {
      log.error("Exception encountered " + ex, ex);
      return;
    }
    final Element root = document.getRootElement();
    menuEntries = new ArrayList<>();
    for (final Iterator<?> it = root.elementIterator("item"); it.hasNext(); ) {
      final Element item = (Element) it.next();
      final MenuEntry menuEntry = readFromXml(item);
      menuEntries.add(menuEntry);
    }
  }

  private MenuEntry readFromXml(final Element item) {
    if ("item".equals(item.getName()) == false) {
      log.error("Tag 'item' expected instead of '" + item.getName() + "'. Ignoring this tag.");
      return null;
    }
    String id = item.attributeValue("id");
    MenuEntry srcMenuEntry = null;
    if (id != null && id.startsWith("c-") == true) {
      id = id.substring(2);
    }
    if (id != null && menu != null) { // menu is only null for FavoritesMenuTest.
      srcMenuEntry = menu.findById(id);
    }
    final MenuEntry menuEntry = new MenuEntry();
    if (srcMenuEntry != null) {
      menuEntry.id = srcMenuEntry.id;
      menuEntry.url = srcMenuEntry.url;
      menuEntry.pageClass = srcMenuEntry.pageClass;
      menuEntry.i18nKey = srcMenuEntry.i18nKey;
      menuEntry.newCounterModel = srcMenuEntry.newCounterModel;
    }
    if (item != null) {
      final String trimmedTitle = item.getTextTrim();
      if (trimmedTitle != null) {
        // menuEntry.setName(StringEscapeUtils.escapeXml(trimmedTitle));
        if (StringUtils.isBlank(trimmedTitle) == true) {
          menuEntry.setName("???");
        } else {
          menuEntry.setName(trimmedTitle);
        }
      }
    }
    for (final Iterator<?> it = item.elementIterator("item"); it.hasNext(); ) {
      if (menuEntry != null) {
        log.warn("Menu entry shouldn't have children, because it's a leaf node.");
      }
      final Element child = (Element) it.next();
      final MenuEntry childMenuEntry = readFromXml(child);
      if (childMenuEntry != null) {
        menuEntry.addMenuEntry(childMenuEntry);
      }
    }
    return menuEntry;
  }

  private void init() {
    this.menuEntries = new ArrayList<>();
    final String userPrefString = (String) UserPreferencesHelper.getEntry(USER_PREF_FAVORITES_MENU_ENTRIES_KEY);
    if (StringUtils.isBlank(userPrefString) == false) {
      if (userPrefString.contains("<root>") == false) {
        // Old format:
        buildFromOldUserPrefFormat(userPrefString);
      } else {
        readFromXml(userPrefString);
      }
    }
    if (this.menuEntries.size() == 0) {
      if (accessChecker.isLoggedInUserMemberOfAdminGroup() == true) {
        final MenuEntry adminMenu = new MenuEntry()
                .setName(ThreadLocalUserContext.getLocalizedString(MenuItemDefId.ADMINISTRATION.getI18nKey()));
        menuEntries.add(adminMenu);
        addFavoriteMenuEntry(adminMenu, menuCreator.findById(MenuItemDefId.ACCESS_LIST));
        addFavoriteMenuEntry(adminMenu, menuCreator.findById(MenuItemDefId.USER_LIST));
        addFavoriteMenuEntry(adminMenu, menuCreator.findById(MenuItemDefId.GROUP_LIST));
        addFavoriteMenuEntry(adminMenu, menuCreator.findById(MenuItemDefId.SYSTEM));
      }
      if (accessChecker.isRestrictedUser() == true) {
        // Restricted users see only the change password menu entry (as favorite).
        addFavoriteMenuEntry(menuCreator.findById(MenuItemDefId.CHANGE_PASSWORD));
      } else {
        final MenuEntry projectManagementMenu = new MenuEntry()
                .setName(ThreadLocalUserContext.getLocalizedString(MenuItemDefId.PROJECT_MANAGEMENT
                        .getI18nKey()));
        menuEntries.add(projectManagementMenu);
        addFavoriteMenuEntry(projectManagementMenu, menuCreator.findById(MenuItemDefId.MONTHLY_EMPLOYEE_REPORT));
        addFavoriteMenuEntry(projectManagementMenu, menuCreator.findById(MenuItemDefId.TIMESHEET_LIST));
        addFavoriteMenuEntry(menuCreator.findById(MenuItemDefId.TASK_TREE));
        addFavoriteMenuEntry(menuCreator.findById(MenuItemDefId.CALENDAR));
        addFavoriteMenuEntry(menuCreator.findById(MenuItemDefId.ADDRESS_LIST));
        addFavoriteMenuEntry(menuCreator.findById(MenuItemDefId.BOOK_LIST));
        addFavoriteMenuEntry(menuCreator.findById(MenuItemDefId.PHONE_CALL));
      }
    }
  }


  private void addFavoriteMenuEntry(final MenuEntry parent, final MenuItemDef menuItemDef) {
    if (menu == null || menuItemDef == null) {
      return;
    }
    final MenuEntry menuEntry = menu.findById(menuItemDef.getId());
    if (menuEntry == null) {
      return;
    }
    parent.addMenuEntry(menuEntry);
  }

  private void addFavoriteMenuEntry(final MenuItemDef menuItemDef) {
    if (menu == null || menuItemDef == null) {
      return;
    }
    final MenuEntry menuEntry = menu.findById(menuItemDef.getId());
    if (menuEntry == null) {
      return;
    }
    for (final MenuEntry entry : this.menuEntries) {
      if (StringUtils.equalsIgnoreCase(entry.id, menuItemDef.getId())) {
        // Entry does already exist, ignore it.
        return;
      }
    }
    this.menuEntries.add(menuEntry);
  }

  /**
   * @param userPrefEntry coma separated list of MenuItemDefs.
   */
  private void buildFromOldUserPrefFormat(final String userPrefEntry) {
    this.menuEntries = new ArrayList<>();
    if (userPrefEntry == null) {
      return;
    }
    final StringTokenizer tokenizer = new StringTokenizer(userPrefEntry, ",");
    while (tokenizer.hasMoreTokens() == true) {
      String token = tokenizer.nextToken();
      if (token.startsWith("M_") == true) {
        token = token.substring(2);
      }
      try {
        final MenuItemDef menuItemDef = menuCreator.findById(token);
        if (menuItemDef == null) {
          continue;
        }
        addFavoriteMenuEntry(menuItemDef);
      } catch (final Exception ex) {
        log.info("Menu '" + token + "' not found: " + ex.getMessage(), ex);
      }
    }
  }

  public void storeAsUserPref() {
    if (CollectionUtils.isEmpty(menuEntries) == true) {
      UserPreferencesHelper.putEntry(USER_PREF_FAVORITES_MENU_ENTRIES_KEY, "", true);
      UserPreferencesHelper.removeEntry(USER_PREF_FAVORITES_MENU_KEY);
      return;
    }
    final Document document = DocumentHelper.createDocument();
    final Element root = document.addElement("root");
    for (final MenuEntry menuEntry : menuEntries) {
      buildElement(root.addElement("item"), menuEntry);
    }
    final String xml = document.asXML();
    if (xml.length() > UserXmlPreferencesDO.MAX_SERIALIZED_LENGTH) {
      throw new UserException("menu.favorite.maxSizeExceeded");
    }
    UserPreferencesHelper.putEntry(USER_PREF_FAVORITES_MENU_ENTRIES_KEY, xml, true);
    UserPreferencesHelper.putEntry(USER_PREF_FAVORITES_MENU_KEY, this, false);
    log.info("Favorites menu stored: " + xml);
  }

  private void buildElement(final Element element, final MenuEntry menuEntry) {
    if (menuEntry.getId() != null) {
      element.addAttribute("id", menuEntry.getId());
    }
    if (menuEntry.getName() != null) {
      element.addText(menuEntry.getName());
    }
    if (menuEntry.hasSubMenuEntries() == true) {
      for (final MenuEntry subMenuEntry : menuEntry.getSubMenuEntries()) {
        buildElement(element.addElement("item"), subMenuEntry);
      }
    }
  }
}
