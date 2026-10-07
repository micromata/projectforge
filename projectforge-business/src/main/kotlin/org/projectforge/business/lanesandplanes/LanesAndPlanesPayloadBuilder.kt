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

package org.projectforge.business.lanesandplanes

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Builds the user list for Lanes & Planes: every active employee with an email, with the Kost2 the user may book
 * time sheets on as cost units and the Kost1 of the employee as reference cost center.
 */
@Service
open class LanesAndPlanesPayloadBuilder {
    class Result(val request: LanesAndPlanesUsersRequest, val skippedUsers: Int)

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    open fun build(accountingInvoiceProfileIds: List<Long>, kost2ArtIds: Collection<Long>): Result {
        var skipped = 0
        val emails = mutableSetOf<String>()
        val employees = employeeService.selectAllActive(checkAccess = false).mapNotNull { employee ->
            val user = employee.user?.takeIf { !it.deactivated && !it.deleted } ?: return@mapNotNull null
            val email = user.email?.trim()?.takeIf { it.isNotEmpty() }
            if (email == null || user.firstname.isNullOrBlank() || user.lastname.isNullOrBlank()) {
                log.warn { "Employee #${employee.id} (user '${user.username}') not sent to Lanes & Planes: first name, last name or email missing." }
                skipped++
                return@mapNotNull null
            }
            if (!emails.add(email.lowercase())) {
                log.warn { "Employee #${employee.id} (user '${user.username}') not sent to Lanes & Planes: email '$email' is used by another employee." }
                skipped++
                return@mapNotNull null
            }
            Triple(employee, user, email)
        }
        val kost2ByUser = getBookableKost2(employees.map { it.second }, kost2ArtIds)
        val users = employees.map { (employee, user, email) ->
            createUser(employee, user, email, kost2ByUser[user.id] ?: emptyList(), accountingInvoiceProfileIds)
        }.sortedBy { it.email.lowercase() }
        return Result(LanesAndPlanesUsersRequest(users), skipped)
    }

    internal fun createUser(
        employee: EmployeeDO,
        user: PFUserDO,
        email: String,
        kost2List: Collection<Kost2DO>,
        accountingInvoiceProfileIds: List<Long>,
    ): LanesAndPlanesUser {
        return LanesAndPlanesUser(
            ident = "${user.id}",
            firstName = user.firstname!!.trim(),
            lastName = user.lastname!!.trim(),
            email = email,
            personnelNumber = employee.staffNumber?.trim()?.takeIf { it.isNotEmpty() },
            referenceCostCenter = kostCache.getKost1(employee.kost1?.id)?.formattedNumber,
            costUnits = kost2List.sortedBy { it.formattedNumber }.map {
                LanesAndPlanesCostObject(
                    ident = it.formattedNumber,
                    name = KostFormatter.instance.formatKost2(it, KostFormatter.FormatType.TEXT),
                )
            },
            accountingInvoiceProfileIds = accountingInvoiceProfileIds,
        )
    }

    /**
     * The active Kost2 of all bookable tasks, the user may book own time sheets on (by the access rights of the
     * user's groups). Unlike the time sheet access check, membership of the admin group doesn't grant all tasks:
     * otherwise admins would get every Kost2 of the company.
     * @return Map of user id to the bookable Kost2 (users without any Kost2 are missing).
     */
    open fun getBookableKost2(users: Collection<PFUserDO>, kost2ArtIds: Collection<Long>): Map<Long, Collection<Kost2DO>> {
        val groupsByUser = users.associate { it.id!! to (userGroupCache.getUserGroups(it) ?: emptySet()) }
        val groupIds = groupsByUser.values.flatten().toSet()
        // Kost2 by Kost2 id, bookable for the members of the group.
        val kost2ByGroup = mutableMapOf<Long, MutableMap<Long, Kost2DO>>()
        forEachTaskNode(taskTree.rootTaskNode) { node ->
            val permittedGroups = groupIds.filter { node.hasPermission(it, AccessType.OWN_TIMESHEETS, OperationType.INSERT) }
            if (permittedGroups.isEmpty() || !timesheetDao.checkTaskBookable(node, throwException = false)) {
                return@forEachTaskNode
            }
            val kost2List = taskTree.getKost2List(node.taskId)?.filter { isSendable(it, kost2ArtIds) }
            if (kost2List.isNullOrEmpty()) {
                return@forEachTaskNode
            }
            permittedGroups.forEach { groupId ->
                val map = kost2ByGroup.getOrPut(groupId) { mutableMapOf() }
                kost2List.forEach { map[it.id!!] = it }
            }
        }
        return groupsByUser.mapNotNull { (userId, userGroupIds) ->
            val kost2List = userGroupIds.mapNotNull { kost2ByGroup[it] }.flatMap { it.values }.distinctBy { it.id }
            if (kost2List.isEmpty()) null else userId to kost2List
        }.toMap()
    }

    private fun isSendable(kost2: Kost2DO, kost2ArtIds: Collection<Long>): Boolean {
        if (kost2.deleted) {
            return false
        }
        val status = kost2.effectiveKostentraegerStatus ?: KostentraegerStatus.ACTIVE
        if (status != KostentraegerStatus.ACTIVE) {
            return false
        }
        return kost2ArtIds.isEmpty() || kost2ArtIds.contains(kost2.kost2Art?.id)
    }

    private fun forEachTaskNode(node: TaskNode, block: (TaskNode) -> Unit) {
        block(node)
        node.children?.forEach { forEachTaskNode(it, block) }
    }
}
