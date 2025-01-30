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

package org.projectforge.plugins.licensemanagement

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class LicensePluginService {
    /**
     * List of groups that are allowed to manage and see licenses.
     * ALL for all users, empty/NONE for no user, or csv list of group pk's or group names.
     */
    @Value("\${projectforge.plugins.license.allowedGroups}")
    var allowedGroups: String? = null

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    private var allowedGroupIds = emptyList<Long>()

    private var allUsersAllowed = false

    @PostConstruct
    fun postConstruct() {
        instance = this
        run {
            allowedGroups?.split(",;")?.forEach { entry ->
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
                            log.warn("No group with id $groupId found for license management in projectforge.properties:projectforge.plugins.license.allowedGroups=$allowedGroups")
                        }
                    }
                    allowedGroupIds += groupId
                } else {
                    userGroupCache.getGroupByName(trimmed)?.id.let { id ->
                        if (id != null) {
                            allowedGroupIds += id
                        } else {
                            log.warn("No group with name '$trimmed' found for license management in projectforge.properties:projectforge.plugins.license.allowedGroups=$allowedGroups")
                        }
                    }
                }
            }
        }
        if (allUsersAllowed) {
            log.info { "Allowed groups for license management: ALL users." }
        } else if (allowedGroupIds.isEmpty()) {
            log.info { "No groups allowed for license management (not visible and usable for any user)." }
        } else {
            log.info {
                "Allowed groups for license management: ${
                    allowedGroupIds.joinToString {
                        userGroupCache.getGroup(
                            it
                        )?.name ?: "???"
                    }
                }"
            }
        }
    }

    fun hasAccess(): Boolean {
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

    companion object {
        @JvmStatic
        lateinit var instance: LicensePluginService
            private set
    }
}
