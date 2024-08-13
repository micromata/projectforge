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

import org.apache.commons.lang3.StringUtils
import org.hibernate.search.bridge.TwoWayStringBridge
import org.projectforge.common.StringHelper
import org.projectforge.database.DatabaseSupport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Users and groups bridge for hibernate search.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class HibernateSearchUsersGroupsBridge : TwoWayStringBridge {
    private val groupsComparator: GroupsComparator = GroupsComparator()

    private val usersComparator: UsersComparator = UsersComparator()

    /**
     * Get all names of groups and users and creates an index containing all user and group names separated by '|'. <br></br>
     */
    override fun objectToString(`object`: Any): String {
        if (`object` is String) return `object`
        val userGroupCache: UserGroupCache = UserGroupCache.getInstance()
        val doObject: BaseUserGroupRightsDO = `object` as BaseUserGroupRightsDO
        val sb = StringBuilder()
        // query information in Bridge results in a deadlock in HSQLDB
        if (DatabaseSupport.getInstance().dialect != DatabaseDialect.HSQL) {
            appendGroups(getSortedGroups(userGroupCache, doObject.fullAccessGroupIds), sb)
            appendGroups(getSortedGroups(userGroupCache, doObject.readonlyAccessGroupIds), sb)
            appendGroups(getSortedGroups(userGroupCache, doObject.minimalAccessGroupIds), sb)
            appendUsers(getSortedUsers(userGroupCache, doObject.fullAccessUserIds), sb)
            appendUsers(getSortedUsers(userGroupCache, doObject.readonlyAccessUserIds), sb)
            appendUsers(getSortedUsers(userGroupCache, doObject.minimalAccessUserIds), sb)
        }

        if (log.isDebugEnabled) {
            log.debug(sb.toString())
        }
        return sb.toString()
    }

    override fun stringToObject(stringValue: String?): Any? {
        // Not supported.
        return null
    }

    private fun getSortedGroups(userGroupCache: UserGroupCache, groupIds: String): Collection<GroupDO>? {
        if (StringUtils.isEmpty(groupIds)) {
            return null
        }
        val sortedGroups: MutableCollection<GroupDO> = TreeSet<GroupDO>(groupsComparator)
        val ids = StringHelper.splitToInts(groupIds, ",", false)
        for (id in ids) {
            val group: GroupDO = userGroupCache.getGroup(id)
            if (group != null) {
                sortedGroups.add(group)
            } else {
                log.warn("Group with id '$id' not found in UserGroupCache. groupIds string was: $groupIds")
            }
        }
        return sortedGroups
    }

    private fun getSortedUsers(userGroupCache: UserGroupCache, userIds: String): Collection<PFUserDO>? {
        if (StringUtils.isEmpty(userIds)) {
            return null
        }
        val sortedUsers: MutableCollection<PFUserDO> = TreeSet<PFUserDO>(usersComparator)
        val ids = StringHelper.splitToInts(userIds, ",", false)
        for (id in ids) {
            val user: PFUserDO = userGroupCache.getUser(id)
            if (user != null) {
                sortedUsers.add(user)
            } else {
                log.warn("Group with id '$id' not found in UserGroupCache. groupIds string was: $userIds")
            }
        }
        return sortedUsers
    }

    private fun appendGroups(groups: Collection<GroupDO>?, sb: StringBuilder) {
        if (groups == null) {
            return
        }
        for (group in groups) {
            sb.append(group.name).append("|")
        }
    }

    private fun appendUsers(users: Collection<PFUserDO>?, sb: StringBuilder) {
        if (users == null) {
            return
        }
        for (user in users) {
            sb.append(user.getFullname()).append(user.username).append("|")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory
            .getLogger(HibernateSearchUsersGroupsBridge::class.java)
    }
}
