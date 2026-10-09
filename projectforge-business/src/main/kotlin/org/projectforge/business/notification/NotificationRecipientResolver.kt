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


package org.projectforge.business.notification

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.EmployeeStatus
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/** Resolves the [NotificationRecipients] of a rule to users. */
@Service
class NotificationRecipientResolver {
    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /**
     * The users of the groups, the single users and the active employees (all or of the given status), minus the
     * employees of an excluded status, users with system access only. With [NotificationRecipients.onlyAffected]
     * only the [affectedUserIds] (all of them, if no recipient is given at all).
     * @param affectedUserIds The users concerned by the rule run, null for no such restriction (e.g. a manual rule).
     */
    fun resolve(recipients: NotificationRecipients, affectedUserIds: Collection<Long>?): List<PFUserDO> {
        val userIds = mutableSetOf<Long>()
        if (recipients.groupIds.isNotEmpty()) {
            val groupIds = recipients.groupIds.toSet()
            userGroupCache.getUserGroupIdMap().forEach { (userId, userGroupIds) ->
                if (userGroupIds.any { it in groupIds }) {
                    userIds += userId
                }
            }
        }
        userIds += recipients.userIds
        if (recipients.allEmployees || recipients.employeeStatus.isNotEmpty()) {
            employeeService.selectAllActive(checkAccess = false).forEach { employee ->
                val userId = employee.user?.id ?: return@forEach
                if (recipients.allEmployees
                    || employeeService.getEmployeeStatus(employee, checkAccess = false) in recipients.employeeStatus
                ) {
                    userIds += userId
                }
            }
        }
        if (affectedUserIds != null && recipients.onlyAffected) {
            if (recipients.isEmpty) {
                userIds += affectedUserIds
            } else {
                userIds.retainAll(affectedUserIds.toSet())
            }
        }
        val excluded = recipients.excludedEmployeeStatus.toSet()
        return userIds.mapNotNull { caches.getUser(it) }
            .filter { user -> !user.deleted && user.hasSystemAccess() }
            .filter { user -> excluded.isEmpty() || !isExcluded(user, excluded) }
            .sortedBy { it.id }
    }

    private fun isExcluded(user: PFUserDO, excluded: Set<EmployeeStatus>): Boolean {
        val employee = caches.getEmployeeByUserId(user.id) ?: return false
        return employeeService.getEmployeeStatus(employee, checkAccess = false) in excluded
    }
}
