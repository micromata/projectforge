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

package org.projectforge.menu.builder

import mu.KotlinLogging
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.menu.MenuBadge
import org.projectforge.menu.MenuItem

private val log = KotlinLogging.logger {}

/**
 * Defines one menu item once. The [MenuCreator] creates the user menu item from this definition dynamically dependent
 * e.g. on the user's access.
 */
class MenuItemDef
@JvmOverloads
constructor(
    /**
     * Needed for unique keys for React frontend.
     */
    val id: String,
    val i18nKey: String,
    var url: String? = null,
    var badgeCounter: (() -> Int?)? = null,
    private var badgeTooltipKey: String? = null,
    var checkAccess: (() -> Boolean)? = null,
    var visibleForRestrictedUsers: Boolean = false,
    var requiredUserRightId: IUserRightId? = null,
    var requiredUserRightValues: Array<UserRightValue>? = null,
    vararg requiredGroups: ProjectForgeGroup
) {

    var requiredGroups: Array<ProjectForgeGroup>? = null

    var menuItemDefId: MenuItemDefId? = null
        private set
    internal var children: MutableList<MenuItemDef>? = null

    init {
        this.requiredGroups = arrayOf(*requiredGroups)
    }

    /**
     * Usable for e. g. plugins without [MenuItemDef] available.
     * @param defId For getting the key and i18nKey.
     * @param url The target url.
     * @param checkAccess Dynamic check access for the logged in user. The menu is visible if [checkAccess] is null or returns true.
     */
    constructor(
        defId: MenuItemDefId,
        badgeCounter: (() -> Int?)? = null,
        badgeTooltipKey: String? = null,
        checkAccess: (() -> Boolean)? = null,
        visibleForRestrictedUsers: Boolean = false,
        requiredUserRightId: IUserRightId? = null,
        requiredUserRightValues: Array<UserRightValue>? = null,
        vararg requiredGroups: ProjectForgeGroup
    )
            : this(
        defId.id,
        defId.i18nKey,
        url = defId.url,
        badgeCounter = badgeCounter,
        badgeTooltipKey = badgeTooltipKey,
        visibleForRestrictedUsers = visibleForRestrictedUsers,
        checkAccess = checkAccess,
        requiredUserRightId = requiredUserRightId,
        requiredUserRightValues = requiredUserRightValues,
        requiredGroups = requiredGroups
    ) {
        this.menuItemDefId = defId
    }

    @Synchronized
    internal fun add(item: MenuItemDef): MenuItemDef {
        if (children == null) {
            children = mutableListOf()
        } else {
            children?.let { childs ->
                if (childs.any { it.id == item.id }) {
                    log.error { "Menu item registered twice (ignoring): $this" }
                    return item
                }
            }
        }
        children!!.add(item)
        return this
    }

    internal fun get(id: MenuItemDefId): MenuItemDef? {
        if (this.id == id.id) {
            return this
        }
        if (children == null) {
            return null
        }
        children!!.forEach {
            if (it.id == id.id)
                return it
        }
        return null
    }

    /**
     * @param parentMenu Only needed for building unique keys
     * @param menuCreatorContext
     */
    internal fun createMenu(parentMenu: MenuItem?, menuCreatorContext: MenuCreatorContext): MenuItem {
        val title = if (menuCreatorContext.translate) translate(i18nKey) else i18nKey
        val menuItem = MenuItem(id, title = title, i18nKey = i18nKey, url = this.url)
        if (parentMenu != null)
            menuItem.key = "${parentMenu.key}.$id"
        else
            menuItem.key = id
        val counter = badgeCounter?.invoke()
        if ((counter ?: -1) > 0) {
            menuItem.badge = MenuBadge(counter, style = "danger")
            if (badgeTooltipKey != null)
                menuItem.badge?.tooltip = translate(badgeTooltipKey)
        }
        return menuItem
    }

    companion object {
        /**
         * For Java code, because Kotlin constructors with named parameter doesn't work in Java code.
         */
        @JvmStatic
        fun create(id: String, i18nKey: String): MenuItemDef {
            return MenuItemDef(id, i18nKey)
        }

        /**
         * For Java code, because Kotlin constructors with named parameter doesn't work in Java code.
         */
        @JvmStatic
        fun create(id: String, i18nKey: String, url: String): MenuItemDef {
            return MenuItemDef(id, i18nKey, url = url)
        }
    }
}
