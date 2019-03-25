package org.projectforge.menu.builder

import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRight
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.IUserRightId
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
                checkAccess: (() -> Boolean)? = null,
                visibleForRestrictedUsers: Boolean = false,
                requiredUserRightId: IUserRightId? = null,
                requiredUserRight: UserRight? = null,
                requiredUserRightValues: Array<UserRightValue>? = null,
                vararg requiredGroups : ProjectForgeGroup) {
        this.key = defId.id
        this.i18nKey = defId.getI18nKey()
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
     * @param key Should be unique inside one top menu.
     * @param i18nKey Used for translation.
     * @param url The target url.
     * @param checkAccess Dynamic check access for the logged in user. The menu is visible if [checkAccess] is null or returns true.
     */
    constructor(key: String, i18nKey: String,
                url: String? = null,
                checkAccess: (() -> Boolean)? = null,
                visibleForRestrictedUsers: Boolean = false,
                requiredUserRightId: IUserRightId? = null,
                requiredUserRight: UserRight? = null,
                requiredUserRightValues: Array<UserRightValue>? = null,
                vararg requiredGroups : ProjectForgeGroup) {
        this.key = key
        this.i18nKey = i18nKey
        this.url = url
        this.checkAccess = checkAccess
        this.visibleForRestrictedUsers = visibleForRestrictedUsers
        this.requiredGroups = arrayOf(*requiredGroups)
        this.requiredUserRightId = requiredUserRightId
        this.requiredUserRight = requiredUserRight
        this.requiredUserRightValues = requiredUserRightValues
    }

    /**
     * Needed for unique keys for React frontend.
     */
    val key: String
    var title: String? = null
    var i18nKey: String? = null
    var url: String? = null
    var visibleForRestrictedUsers: Boolean = false

    var checkAccess: (() -> Boolean)? = null
    var requiredGroups: Array<ProjectForgeGroup>? = null
    var requiredUserRightId: IUserRightId? = null
    var requiredUserRight: UserRight? = null
    var requiredUserRightValues: Array<UserRightValue>? = null

    internal var childs: MutableList<MenuItemDef>? = null

    @Synchronized
    internal fun add(item: MenuItemDef): MenuItemDef {
        if (childs == null)
            childs = mutableListOf()
        childs!!.add(item)
        return this
    }

    internal fun get(id: MenuItemDefId): MenuItemDef? {
        if (this.key == id.id) {
            return this
        }
        if (childs == null) {
            return null
        }
        childs!!.forEach {
            if (it.key == id.id)
                return it
        }
        return null
    }

    /**
     * @param parentMenu Only needed for building unique keys
     * @param menuBuilderContext
     */
    internal fun createMenu(parentMenu: MenuItem, menuBuilderContext: MenuCreatorContext): MenuItem {
        val menuItem = MenuItem(translate(i18nKey), url = this.url)
        if (parentMenu.title != "root")
            menuItem.key = "${parentMenu.key}.${key}"
        else
            menuItem.key = "${key}"
        return menuItem
    }
}
