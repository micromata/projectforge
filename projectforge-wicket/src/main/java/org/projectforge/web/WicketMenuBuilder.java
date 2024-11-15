/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.annotation.PostConstruct;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.Model;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.menu.Menu;
import org.projectforge.menu.MenuItem;
import org.projectforge.menu.builder.FavoritesMenuCreator;
import org.projectforge.menu.builder.MenuCreatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;

/**
 * Build of the user's personal menu (depending on the access rights of the user).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class WicketMenuBuilder implements Serializable {
    private static WicketMenuBuilder instance;

    @Autowired
    private transient FavoritesMenuCreator favoritesMenuCreator;

    @Autowired
    private transient MenuItemRegistry menuItemRegistry;

    @PostConstruct
    private void init() {
        instance = this;
    }

    public WicketMenu getFavoriteMenu() {
        Menu menu = favoritesMenuCreator.getFavoriteMenu();
        return buildMenuTree(menu);
    }

    public WicketMenu getMenu(final PFUserDO user) {
        return buildMenuTree(user);
    }

    private WicketMenu buildMenuTree(final PFUserDO user) {
        if (user == null) {
            return null;
        }
        Menu menu = WicketSupport.getMenuCreator().build(new MenuCreatorContext(user, false));
        return buildMenuTree(menu);
    }

    private WicketMenu buildMenuTree(Menu menu) {
        WicketMenu wicketMenu = new WicketMenu();
        if (menu.getBadge() != null)
            wicketMenu.setTotalBadgeCounter(menu.getBadge().getCounter());
        else
            wicketMenu.setTotalBadgeCounter(0);
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
        else if (StringUtils.isNotBlank(item.getTitle()))
            entry.name = item.getTitle();
        else
            entry.name = "???";
        entry.pageClass = menuItemRegistry.getPageClass(item.getId());
        entry.url = item.getUrl();
        if (item.getBadge() != null) {
            entry.setBadgeCounter(new Model<Integer>() {
                @Override
                public Integer getObject() {
                    return item.getBadge().getCounter();
                }
            });
        }
        return entry;
    }

    @Serial
    private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
        this.favoritesMenuCreator = instance.favoritesMenuCreator;
        this.menuItemRegistry = instance.menuItemRegistry;
    }

    @Serial
    private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
        // Do nothing.
    }
}
