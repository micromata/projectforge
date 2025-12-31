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

package org.projectforge.business.user

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.utils.CollectionDebugUtils

/**
 * For debugging purposes of [UserGroupCache]
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object UserGroupCacheDebug {
    /**
     * For serializing and deserializing.
     */
    class Data(src: UserGroupCache? = null) {
        var userGroupIdMap = src?.userGroupIdMap
        var groupMap = src?.groupMap
        var rightMap = src?.rightMap
        var userMap = src?.internalGetCopyOfUserMap()
        var adminUsers = src?.adminUsers
        var financeUsers = src?.financeUsers
        var controllingUsers = src?.controllingUsers
        var projectManagers = src?.projectManagers
        var projectAssistants = src?.projectAssistants
        var marketingUsers = src?.marketingUsers
        var orgaUsers = src?.orgaUsers
        var hrUsers = src?.hrUsers

        var allUsers = src?.allUsers
        var allGroups = src?.allGroups

        // For backward compatibility (UserGroupCache was serialized with these fields)
        @JsonIgnore
        var isUserMemberOfAdminGroup: Boolean = false

        @JsonIgnore
        var isUserMemberOfFinanceGroup: Boolean = false

        @JsonIgnore
        var isUserMemberOfProjectManagers: Boolean = false

        @JsonIgnore
        var isUserMemberOfProjectAssistant: Boolean = false

        @JsonIgnore
        var isUserMemberOfControllingGroup: Boolean = false

        @JsonIgnore
        var isUserMemberOfMarketingGroup: Boolean = false

        @JsonIgnore
        var isUserMemberOfOrgaGroup: Boolean = false

        @JsonIgnore
        var isRefreshInProgress: Boolean = false

        @JsonIgnore
        var initialized: Boolean = false
    }

    fun internalGetStateAsJson(orig: UserGroupCache): String {
        val clone = Data(orig)
        return JsonUtils.toJson(clone)
    }

    fun internalCompareWith(data: Data, other: Data): String {
        val sb = StringBuilder()
        append(sb, data.adminUsers, other.adminUsers, prefix = "adminUsers")
        append(sb, data.hrUsers, other.hrUsers, prefix = "hrUsers")
        append(sb, data.orgaUsers, other.orgaUsers, prefix = "orgaUsers")
        append(sb, data.financeUsers, other.financeUsers, prefix = "financeUsers")
        append(sb, data.marketingUsers, other.marketingUsers, prefix = "marketingUsers")
        append(sb, data.controllingUsers, other.controllingUsers, prefix = "controllingUsers")
        append(sb, data.projectAssistants, other.projectAssistants, prefix = "projectAssistants")
        append(sb, data.projectManagers, other.projectManagers, prefix = "projectManagers")
        append(sb, data.userGroupIdMap?.keys, other.userGroupIdMap?.keys, prefix = "userGroupIdMap.keys")
        append(sb, data.userGroupIdMap?.values, other.userGroupIdMap?.values, prefix = "userGroupIdMap.values")
        append(sb, data.userMap?.keys, other.userMap?.keys, prefix = "userMap.keys")
        append(sb, data.userMap?.values, other.userMap?.values, prefix = "userMap.values")
        append(sb, data.allUsers, other.allUsers, prefix = "allUsers")
        append(sb, data.allGroups, other.allGroups, prefix = "allGroups")
        return sb.toString()
    }

    private fun append(sb: StringBuilder, src: Collection<Any?>?, dest: Collection<Any?>?, prefix: String) {
        CollectionDebugUtils.showCompareDiff(src, dest, withKept = true, prefix = "$prefix: ")?.let {
            sb.append(it)
        }
    }
}
