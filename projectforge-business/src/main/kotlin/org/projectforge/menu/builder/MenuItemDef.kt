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
class MenuItemDef(
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
        var requiredUserRight: UserRight? = null,
        var requiredUserRightValues: Array<UserRightValue>? = null,
        vararg requiredGroups: ProjectForgeGroup) {

    var requiredGroups: Array<ProjectForgeGroup>? = null

    internal var childs: MutableList<MenuItemDef>? = null

    init {
        this.requiredGroups = arrayOf(*requiredGroups)
    }

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
                vararg requiredGroups: ProjectForgeGroup)
            : this(
            defId.id,
            defId.getI18nKey(),
            url = url,
            badgeCounter = badgeCounter,
            badgeTooltipKey = badgeTooltipKey,
            visibleForRestrictedUsers = visibleForRestrictedUsers,
            checkAccess = checkAccess,
            requiredUserRightId = requiredUserRightId,
            requiredUserRight = requiredUserRight,
            requiredUserRightValues = requiredUserRightValues,
            requiredGroups = *requiredGroups)

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
        val menuItem = MenuItem(id, title = title, i18nKey = i18nKey, url = this.url)
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
