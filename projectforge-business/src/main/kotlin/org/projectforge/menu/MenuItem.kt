/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.menu

import org.projectforge.framework.i18n.autoTranslate
import org.projectforge.framework.i18n.translate
import org.projectforge.menu.builder.MenuItemDef

class MenuItem(var id: String? = null,
               var title: String? = null,
               var i18nKey: String? = null,
               var tooltip: String? = null,
               var tooltipTitle: String? = null,
               var url: String? = null,
               /**
                * Unique key usable by React. It's also unique for multiple menu items (in different main categories).
                */
               var key: String? = null,
               var badge: MenuBadge? = null,
               /**
                * If not given, the client should assume [MenuItemTargetType.REDIRECT]
                */
               var type: MenuItemTargetType? = null) {
    constructor(menuItemDef: MenuItemDef?) : this() {
        if (menuItemDef == null)
            return
        id = menuItemDef.id
        title = translate(menuItemDef.i18nKey)
        i18nKey = menuItemDef.i18nKey
        key = menuItemDef.id
        url = menuItemDef.url
        if (menuItemDef.badgeCounter != null) {
            badge = MenuBadge(counter = menuItemDef.badgeCounter?.invoke())
        }
    }

    var subMenu: MutableList<MenuItem>? = null

    fun isLeaf(): Boolean {
        return !url.isNullOrBlank()
    }

    fun add(menuItem: MenuItem) {
        if (subMenu == null) {
            subMenu = mutableListOf()
        }
        subMenu?.add(menuItem)
    }

    fun add(menuItemDef: MenuItemDef?) {
        if (menuItemDef == null)
            return // Do nothing.
        add(MenuItem(menuItemDef))
    }

    fun get(id: String): MenuItem? {
        if (this.id == id)
            return this
        subMenu?.forEach {
            val found = it.get(id)
            if (found != null)
                return found
        }
        return null
    }

    /**
     * Translates the i18n as title if title isn't given.
     * Removes all super menu items without children. This will happen, if the user hasn't the user rights to see
     * any children of a super menu item.
     *
     * Accumulates badge counter in parent menus by summarizing all badge counters from child menus.
     */
    fun postProcess() {
        if (title == null)
            title = translate(i18nKey)
        if (tooltip != null)
            tooltip = autoTranslate(tooltip)
        if (tooltipTitle != null)
            tooltipTitle = autoTranslate(tooltipTitle)
        if (subMenu.isNullOrEmpty())
            return
        var badgeCounter = 0
        subMenu?.forEach {
            if (it.badge?.counter ?: -1 > 0)
                badgeCounter += it.badge?.counter ?: 0
            it.postProcess()
        }
        if (badgeCounter > 0)
            badge = MenuBadge(badgeCounter, style = "danger")
        subMenu?.removeIf { !it.isLeaf() && it.subMenu.isNullOrEmpty() }
    }
}
