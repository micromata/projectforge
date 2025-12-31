/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.builder.MenuItemDefId

private val log = KotlinLogging.logger {}

/**
 * Visibility configuration for a menu item.
 * @param id The id of the menu item.
 * @param visibiltyGroupsString The groups that are allowed to see the menu item. Comma separated list of group ids or group names.
 * If the string is empty or null, the menu item is visible to all users.
 * If the string is "NONE", the menu item is not visible to any user.
 * If the string is "ALL", the menu item is visible to all users.
 * If the string is a comma separated list of group ids or group names, the menu item is visible to users in these groups.
 */
class MenuVisibility(val id: String, val visibiltyGroupsString: String?, val menuItemDefId: MenuItemDefId? = null) {
    private val userGroupCache: UserGroupCache by lazy { UserGroupCache.getInstance() }

    var allUsersAllowed: Boolean = false
        private set
    var allowedGroupIds: List<Long> = emptyList()
        private set


    init {
        run {
            visibiltyGroupsString?.split(",;")?.forEach { entry ->
                val trimmed = entry.trim()
                if (trimmed.isEmpty()) {
                    return@forEach
                } else if (trimmed.equals("ALL", ignoreCase = true)) {
                    allowedGroupIds = emptyList()
                    allUsersAllowed = true
                    return@run // No need to continue. break
                } else if (trimmed.equals("NONE", ignoreCase = true)) {
                    allowedGroupIds = emptyList()
                    return@run // No need to continue. break
                }
                val groupId = trimmed.toLongOrNull()
                if (groupId != null) {
                    userGroupCache.getGroup(groupId)?.id.let { id ->
                        if (id != null) {
                            allowedGroupIds += id
                        } else {
                            log.warn("No group with id $groupId found for menu '${this.id}' in projectforge.properties:projectforge.menu.visibility.$id=$visibiltyGroupsString")
                        }
                    }
                    allowedGroupIds += groupId
                } else {
                    userGroupCache.getGroupByName(trimmed)?.id.let { id ->
                        if (id != null) {
                            allowedGroupIds += id
                        } else {
                            log.warn("No group with name '$trimmed' found for menu '${this.id}' in projectforge.properties:projectforge.menu.visibility.$id=$visibiltyGroupsString")
                        }
                    }
                }
            }
        }
        if (allUsersAllowed) {
            // OK, nothing to log.
        } else if (allowedGroupIds.isEmpty()) {
            log.info("No groups found for menu '${this.id}' in projectforge.properties:projectforge.menu.visibility.$id=$visibiltyGroupsString")
        } else {
            log.info {
                "Allowed groups for menu '${this.id}': ${
                    allowedGroupIds.joinToString { userGroupCache.getGroup(it)?.name ?: "???" }
                }"
            }
        }
    }

    fun isVisible(): Boolean {
        if (allUsersAllowed) {
            return true
        }
        userGroupCache.getUserGroups(ThreadLocalUserContext.requiredLoggedInUser)?.forEach { groupId ->
            if (allowedGroupIds.contains(groupId)) {
                return true
            }
        }
        return false
    }
}
