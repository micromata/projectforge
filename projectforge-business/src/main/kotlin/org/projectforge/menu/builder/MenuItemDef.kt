package org.projectforge.menu.builder

import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRight
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.menu.MenuBadge
import org.projectforge.menu.MenuItem

/**
 * Defines one menu item once. The [MenuCreator] creates the user menu item from this definition dynamically dependent
 * e. g. on the user's access.
 */
class MenuItemDef {
    /**
     * Usable for e. g. plugins without [MenuItemDef] available.
     * @param defId For getting the key and i18nKey.
     * @param url The target url.
     * @param checkAccess Dynamic check access for the logged in user. The menu is visible if [checkAccess] is null or returns true.
     */
    constructor(defId: MenuItemDefId,
                url: String? = null,
                badgeCounter: (() -> Int?)? = null,
                badgeTooltipKey: String? = null,
                checkAccess: (() -> Boolean)? = null,
                visibleForRestrictedUsers: Boolean = false,
                requiredUserRightId: IUserRightId? = null,
                requiredUserRight: UserRight? = null,
                requiredUserRightValues: Array<UserRightValue>? = null,
                vararg requiredGroups: ProjectForgeGroup) {
        this.id = defId.id
        this.i18nKey = defId.getI18nKey()
        this.badgeCounter = badgeCounter
        this.badgeTooltipKey = badgeTooltipKey
        this.url = url
        this.checkAccess = checkAccess
        this.visibleForRestrictedUsers = visibleForRestrictedUsers
        this.requiredGroups = arrayOf(*requiredGroups)
        this.requiredUserRightId = requiredUserRightId
        this.requiredUserRight = requiredUserRight
        this.requiredUserRightValues = requiredUserRightValues
    }

    /**
     * Usable for e. g. plugins without [MenuItemDef] available.
     * @param id Should be unique inside one top menu.
     * @param i18nKey Used for translation.
     */
    constructor(id: String,
                i18nKey: String) {
        this.id = id
        this.i18nKey = i18nKey
    }

    /**
     * Needed for unique keys for React frontend.
     */
    val id: String
    var title: String? = null
    var i18nKey: String? = null
    var url: String? = null
    var visibleForRestrictedUsers: Boolean = false

    var checkAccess: (() -> Boolean)? = null
    var requiredGroups: Array<ProjectForgeGroup>? = null
    var requiredUserRightId: IUserRightId? = null
    var requiredUserRight: UserRight? = null
    var requiredUserRightValues: Array<UserRightValue>? = null

    var badgeCounter: (() -> Int?)? = null
    private var badgeTooltipKey: String? = null

    internal var childs: MutableList<MenuItemDef>? = null

    @Synchronized
    internal fun add(item: MenuItemDef): MenuItemDef {
        if (childs == null)
            childs = mutableListOf()
        childs!!.add(item)
        return this
    }

    internal fun get(id: MenuItemDefId): MenuItemDef? {
        if (this.id == id.id) {
            return this
        }
        if (childs == null) {
            return null
        }
        childs!!.forEach {
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
        val menuItem = MenuItem(id, title = title!!, i18nKey = i18nKey, url = this.url)
        if (parentMenu != null)
            menuItem.key = "${parentMenu.key}.$id"
        else
            menuItem.key = id
        val counter = badgeCounter?.invoke()
        if (counter ?: -1 > 0) {
            menuItem.badge = MenuBadge(counter, style = "danger")
            if (badgeTooltipKey != null)
                menuItem.badge?.tooltip = translate(badgeTooltipKey)
        }
        return menuItem
    }
}
