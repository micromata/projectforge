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

package org.projectforge.business.teamcal.admin.model

import mu.KotlinLogging
import org.hibernate.search.engine.backend.document.DocumentElement
import org.hibernate.search.mapper.pojo.bridge.TypeBridge
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext
import org.projectforge.business.common.BaseUserGroupRightsDO
import org.projectforge.business.user.GroupsComparator
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UsersComparator
import org.projectforge.common.DatabaseDialect
import org.projectforge.common.StringHelper
import org.projectforge.database.DatabaseSupport
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*

private val log = KotlinLogging.logger {}

class HibernateSearchUsersGroupsBridge : TypeBridge<BaseUserGroupRightsDO> {

    private val groupsComparator = GroupsComparator()
    private val usersComparator = UsersComparator()

    override fun write(
        target: DocumentElement,
        bridgedElement: BaseUserGroupRightsDO,
        context: TypeBridgeWriteContext
    ) {
        val userGroupCache = UserGroupCache.getInstance()
        val sb = StringBuilder()

        if (DatabaseSupport.getInstance().getDialect() != DatabaseDialect.HSQL) {
            appendGroups(getSortedGroups(userGroupCache, bridgedElement.fullAccessGroupIds), sb)
            appendGroups(getSortedGroups(userGroupCache, bridgedElement.readonlyAccessGroupIds), sb)
            appendGroups(getSortedGroups(userGroupCache, bridgedElement.minimalAccessGroupIds), sb)
            appendUsers(getSortedUsers(userGroupCache, bridgedElement.fullAccessUserIds), sb)
            appendUsers(getSortedUsers(userGroupCache, bridgedElement.readonlyAccessUserIds), sb)
            appendUsers(getSortedUsers(userGroupCache, bridgedElement.minimalAccessUserIds), sb)
        }

        if (log.isDebugEnabled) {
            log.debug(sb.toString())
        }
        target.addValue("usersgroups", sb.toString())
    }

    private fun getSortedGroups(
        userGroupCache: UserGroupCache,
        groupIds: String?
    ): Collection<GroupDO>? {
        if (groupIds.isNullOrEmpty()) {
            return null
        }
        val sortedGroups: MutableCollection<GroupDO> = TreeSet(groupsComparator)
        val ids = StringHelper.splitToLongs(groupIds, ",", false)
        for (id in ids) {
            val group = userGroupCache.getGroup(id)
            if (group != null) {
                sortedGroups.add(group)
            } else {
                log.warn("Group with id '$id' not found in UserGroupCache. groupIds string was: $groupIds")
            }
        }
        return sortedGroups
    }

    private fun getSortedUsers(
        userGroupCache: UserGroupCache,
        userIds: String?
    ): Collection<PFUserDO>? {
        if (userIds.isNullOrEmpty()) {
            return null
        }
        val sortedUsers: MutableCollection<PFUserDO> = TreeSet(usersComparator)
        val ids = StringHelper.splitToLongs(userIds, ",", false)
        for (id in ids) {
            val user = userGroupCache.getUser(id)
            if (user != null) {
                sortedUsers.add(user)
            } else {
                log.warn("User with id '$id' not found in UserGroupCache. userIds string was: $userIds")
            }
        }
        return sortedUsers
    }

    private fun appendGroups(groups: Collection<GroupDO>?, sb: StringBuilder) {
        groups?.forEach { group -> sb.append(group.name).append("|") }
    }

    private fun appendUsers(users: Collection<PFUserDO>?, sb: StringBuilder) {
        users?.forEach { user -> sb.append(user.getFullname()).append(user.username).append("|") }
    }
}
