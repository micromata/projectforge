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
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.extensions.abbreviate
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Builds the user list for Lanes & Planes: every active employee with an email, with the general Kost1 as cost
 * centers, the general Kost2 and the Kost2 the user may book time sheets on (restricted by the patterns of
 * [LanesAndPlanesSettings]) as cost units, the Kost1 of the employee as reference cost center and the account of
 * the employee as creditor account. Plus the additional users of the settings (no employees).
 */
@Service
open class LanesAndPlanesPayloadBuilder {
    class Result(val request: LanesAndPlanesUsersRequest, val skippedUsers: Int)

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    open fun build(settings: LanesAndPlanesSettings): Result {
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
        val generalKost1 = getGeneralKost1(settings.generalKost1Numbers)
        val generalKost2 = getGeneralKost2(settings.generalKost2Numbers)
        val kost2ByUser = getBookableKost2(employees.map { it.second }, settings.kost2Regexes)
        val users = employees.map { (employee, user, email) ->
            val kost2List = (generalKost2 + (kost2ByUser[user.id] ?: emptyList())).distinctBy { it.id }
            createUser(employee, user, email, generalKost1, kost2List, settings)
        }
        val additionalUsers = settings.additionalUserList.mapNotNull { additional ->
            val email = additional.email?.trim()?.takeIf { it.isNotEmpty() }
            if (email == null || additional.firstName.isNullOrBlank() || additional.lastName.isNullOrBlank()) {
                log.warn { "Additional Lanes & Planes user '${additional.email}' not sent: first name, last name or email missing." }
                skipped++
                return@mapNotNull null
            }
            if (!emails.add(email.lowercase())) {
                log.warn { "Additional Lanes & Planes user '$email' not sent: the email is used by an employee or another additional user." }
                skipped++
                return@mapNotNull null
            }
            createAdditionalUser(additional, email, settings)
        }
        return Result(LanesAndPlanesUsersRequest((users + additionalUsers).sortedBy { it.email.lowercase() }), skipped)
    }

    /**
     * Only name, email and invoice profiles: ident, middle name, abbreviation, personnel number, cost centers etc.
     * aren't sent, so they are
     * kept as maintained in Lanes & Planes.
     */
    internal fun createAdditionalUser(
        user: LanesAndPlanesSettings.AdditionalUser,
        email: String,
        settings: LanesAndPlanesSettings,
    ): LanesAndPlanesUser {
        return LanesAndPlanesUser(
            firstName = user.firstName!!.trim(),
            lastName = user.lastName!!.trim(),
            email = email,
            accountingInvoiceProfileIds = settings.invoiceProfileIds,
        )
    }

    /**
     * Numbers without dots (e.g. `50123401`), as the DATEV export of L&P needs them. The name follows the L&P
     * convention: number, project and Kost2 type (e.g. `50123401 Project - Diverses`), the description for a
     * Kost2 without project. At most [MAX_NAME_LENGTH] characters.
     */
    internal fun createCostObject(kost2: Kost2DO): LanesAndPlanesCostObject {
        val number = KostFormatter.instance.formatKost2(kost2, KostFormatter.FormatType.NUMBER)
        val projekt = caches.getProjektIfNotInitialized(kost2.projekt)
        val text = if (projekt != null) {
            listOfNotNull(projekt.name, caches.getKost2ArtIfNotInitialized(kost2.kost2Art)?.name)
                .filter { it.isNotBlank() }
                .joinToString(" - ")
        } else {
            kost2.description ?: ""
        }
        return LanesAndPlanesCostObject(ident = number, name = "$number ${text.trim()}".trim().abbreviate(MAX_NAME_LENGTH))
    }

    /** Number without dots and its description, e.g. `10050100 Travel costs`. */
    internal fun createCostObject(kost1: Kost1DO): LanesAndPlanesCostObject {
        val number = KostFormatter.instance.formatKost1(kost1, KostFormatter.FormatType.NUMBER)
        return LanesAndPlanesCostObject(ident = number, name = "$number ${kost1.description?.trim() ?: ""}".trim().abbreviate(MAX_NAME_LENGTH))
    }

    /**
     * @param kost1List The cost centers. If empty, they aren't sent at all (so kept as maintained in Lanes & Planes).
     */
    internal fun createUser(
        employee: EmployeeDO,
        user: PFUserDO,
        email: String,
        kost1List: Collection<Kost1DO>,
        kost2List: Collection<Kost2DO>,
        settings: LanesAndPlanesSettings,
    ): LanesAndPlanesUser {
        return LanesAndPlanesUser(
            ident = "${user.id}",
            firstName = user.firstname!!.trim(),
            middleName = "",
            lastName = user.lastname!!.trim(),
            email = email,
            abbreviation = user.nickname?.trim() ?: "",
            personnelNumber = employee.staffNumber?.trim()?.takeIf { it.isNotEmpty() },
            referenceCostCenter = kostCache.getKost1(employee.kost1?.id)?.let {
                KostFormatter.instance.formatKost1(it, KostFormatter.FormatType.NUMBER)
            },
            creditorAccount = kontoCache.getKontoIfNotInitialized(employee.konto)?.nummer?.toString(),
            costUnits = kost2List.sortedBy { it.formattedNumber }.map { createCostObject(it) },
            costCenters = kost1List.sortedBy { it.formattedNumber }.map { createCostObject(it) }.ifEmpty { null },
            accountingInvoiceProfileIds = settings.invoiceProfileIds,
        )
    }

    /**
     * The active Kost1 of the given formatted numbers, sent to all users. Unknown or inactive ones are skipped.
     */
    open fun getGeneralKost1(numbers: Collection<String>): List<Kost1DO> {
        return numbers.mapNotNull { number ->
            val kost1 = kostCache.getKost1(number)
            if (kost1 == null || !LanesAndPlanesSettings.isActive(kost1)) {
                log.warn { "Lanes & Planes: general Kost1 '$number' not found or not active, not sent." }
                null
            } else {
                kost1
            }
        }.distinctBy { it.id }
    }

    /**
     * The active Kost2 of the given formatted numbers, sent to all users. Unknown or inactive ones are skipped
     * (the configuration is validated on save, but a Kost2 may be closed later on).
     */
    open fun getGeneralKost2(numbers: Collection<String>): List<Kost2DO> {
        return numbers.mapNotNull { number ->
            val kost2 = kostCache.getKost2(number)
            if (kost2 == null || !LanesAndPlanesSettings.isActive(kost2)) {
                log.warn { "Lanes & Planes: general Kost2 '$number' not found or not active, not sent." }
                null
            } else {
                kost2
            }
        }
    }

    /**
     * The active Kost2 of all bookable tasks, the user may book own time sheets on (by the access rights of the
     * user's groups). Unlike the time sheet access check, membership of the admin group doesn't grant all tasks:
     * otherwise admins would get every Kost2 of the company.
     * @param patterns Only Kost2 whose formatted number matches one of them. Empty: all.
     * @return Map of user id to the bookable Kost2 (users without any Kost2 are missing).
     */
    open fun getBookableKost2(users: Collection<PFUserDO>, patterns: List<Regex>): Map<Long, Collection<Kost2DO>> {
        val groupsByUser = users.associate { it.id!! to (userGroupCache.getUserGroups(it) ?: emptySet()) }
        val groupIds = groupsByUser.values.flatten().toSet()
        // Kost2 by Kost2 id, bookable for the members of the group.
        val kost2ByGroup = mutableMapOf<Long, MutableMap<Long, Kost2DO>>()
        forEachTaskNode(taskTree.rootTaskNode) { node ->
            val permittedGroups = groupIds.filter { node.hasPermission(it, AccessType.OWN_TIMESHEETS, OperationType.INSERT) }
            if (permittedGroups.isEmpty() || !timesheetDao.checkTaskBookable(node, throwException = false)) {
                return@forEachTaskNode
            }
            val kost2List = taskTree.getKost2List(node.taskId)?.filter { isSendable(it, patterns) }
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

    private fun isSendable(kost2: Kost2DO, patterns: List<Regex>): Boolean {
        if (!LanesAndPlanesSettings.isActive(kost2)) {
            return false
        }
        return patterns.isEmpty() || patterns.any { it.matches(kost2.formattedNumber) }
    }

    private fun forEachTaskNode(node: TaskNode, block: (TaskNode) -> Unit) {
        block(node)
        node.children?.forEach { forEachTaskNode(it, block) }
    }

    companion object {
        /** Maximum length of a cost center name in L&P. */
        const val MAX_NAME_LENGTH = 102
    }
}
