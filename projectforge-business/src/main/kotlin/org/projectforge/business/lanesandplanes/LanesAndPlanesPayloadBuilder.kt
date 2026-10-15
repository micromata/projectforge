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
    /**
     * @param newUserEmails The emails of the sent employees without an entry in [LanesAndPlanesSettings.userEmails]
     * by user id: stored there after a successful push, so they are kept, if the email changes in ProjectForge.
     * @param differingEmails The number of employees sent with an email of the settings differing from the one
     * in ProjectForge.
     */
    class Result(
        val request: LanesAndPlanesUsersRequest,
        val skippedUsers: Int,
        val newUserEmails: Map<Long, String> = emptyMap(),
        val differingEmails: Int = 0,
    )

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
        val emailsByUserId = settings.emailsByUserId
        // The emails of the settings belong to their users (sent or not, e.g. a former employee) and the ones of the
        // additional users to these: otherwise another employee having one in ProjectForge would take over that
        // user in L&P (and be stored with it in the settings).
        val reservedEmails = emailsByUserId.entries.associate { (userId, email) -> email.lowercase() to userId } +
                settings.additionalUserList.mapNotNull { it.email?.trim()?.lowercase() }.associateWith { null }
        val employees = employeeService.selectAllActive(checkAccess = false)
            .mapNotNull { employee ->
                val user = employee.user?.takeIf { !it.deactivated && !it.deleted } ?: return@mapNotNull null
                // The email known to L&P wins: L&P identifies users by their email only.
                val email = emailsByUserId[user.id] ?: user.email?.trim()?.takeIf { it.isNotEmpty() }
                if (email == null || user.firstname.isNullOrBlank() || user.lastname.isNullOrBlank()) {
                    log.warn { "Employee #${employee.id} (user '${user.username}') not sent to Lanes & Planes: first name, last name or email missing." }
                    skipped++
                    return@mapNotNull null
                }
                val lowerEmail = email.lowercase()
                if (lowerEmail in reservedEmails && reservedEmails[lowerEmail] != user.id) {
                    val owner = reservedEmails[lowerEmail]?.let { "user #$it" } ?: "an additional user"
                    log.warn { "Employee #${employee.id} (user '${user.username}') not sent to Lanes & Planes: email '$email' is the one of $owner in the configuration 'lanesAndPlanes'." }
                    skipped++
                    return@mapNotNull null
                }
                if (!emails.add(lowerEmail)) {
                    log.warn { "Employee #${employee.id} (user '${user.username}') not sent to Lanes & Planes: email '$email' is used by another employee." }
                    skipped++
                    return@mapNotNull null
                }
                Triple(employee, user, email)
            }
        val generalKost1 = getGeneralKost1(settings.generalKost1Numbers)
        val generalKost2 = getGeneralKost2(settings.generalKost2Numbers)
        val kost2ByUser = getBookableKost2(employees.map { it.second }, settings.kost2PatternGroups)
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
        val newUserEmails = employees.filter { (_, user, _) -> user.id !in emailsByUserId }
            .associate { (_, user, email) -> user.id!! to email }
        val differingEmails = employees.count { (_, user, email) ->
            user.id in emailsByUserId && !email.equals(user.email?.trim(), ignoreCase = true)
        }
        return Result(
            LanesAndPlanesUsersRequest((users + additionalUsers).sortedBy { it.email.lowercase() }),
            skipped,
            newUserEmails,
            differingEmails,
        )
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
     * @param existingNames The names already existing in L&P by number, see [existingCostObject].
     */
    internal fun createCostObject(kost2: Kost2DO, existingNames: Map<String, String> = emptyMap()): LanesAndPlanesCostObject {
        val number = KostFormatter.instance.formatKost2(kost2, KostFormatter.FormatType.NUMBER)
        existingCostObject(number, existingNames)?.let { return it }
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
    internal fun createCostObject(kost1: Kost1DO, existingNames: Map<String, String> = emptyMap()): LanesAndPlanesCostObject {
        val number = KostFormatter.instance.formatKost1(kost1, KostFormatter.FormatType.NUMBER)
        existingCostObject(number, existingNames)?.let { return it }
        return LanesAndPlanesCostObject(ident = number, name = "$number ${kost1.description?.trim() ?: ""}".trim().abbreviate(MAX_NAME_LENGTH))
    }

    /**
     * A cost object already existing in L&P is reused only, if its name is sent as ident and name (L&P refuses a
     * new ident with a used name, deleted entries included).
     */
    private fun existingCostObject(number: String, existingNames: Map<String, String>): LanesAndPlanesCostObject? {
        return existingNames[number]?.let { LanesAndPlanesCostObject(ident = it, name = it) }
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
        val existingNames = settings.existingNamesByNumber
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
            costUnits = kost2List.sortedBy { it.formattedNumber }.map { createCostObject(it, existingNames) },
            costCenters = kost1List.sortedBy { it.formattedNumber }.map { createCostObject(it, existingNames) }.ifEmpty { null },
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
     * @param patternGroups Only Kost2 whose formatted number matches one of them, see
     * [LanesAndPlanesSettings.kost2PatternGroups]. Empty: all.
     * @return Map of user id to the bookable Kost2 (users without any Kost2 are missing).
     */
    open fun getBookableKost2(
        users: Collection<PFUserDO>,
        patternGroups: List<List<Regex>>,
    ): Map<Long, Collection<Kost2DO>> {
        val patterns = patternGroups.flatten()
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
                .let { selectAlternatives(it, patternGroups) }
            if (kost2List.isEmpty()) null else userId to kost2List
        }.toMap()
    }

    /**
     * Of the Kost2 of the same project (formatted number without the Kost2 type) matching a group with
     * alternatives, only the ones of the first matching alternative are kept (e.g. travel time invoiced instead
     * of not invoiced).
     */
    internal fun selectAlternatives(kost2List: List<Kost2DO>, patternGroups: List<List<Regex>>): List<Kost2DO> {
        var result = kost2List
        patternGroups.filter { it.size > 1 }.forEach { alternatives ->
            val removed = result.filter { kost2 -> alternatives.any { it.matches(kost2.formattedNumber) } }
                .groupBy { it.formattedNumber.substringBeforeLast('.') }
                .values.flatMap { sameProject ->
                    val first = alternatives.first { regex -> sameProject.any { regex.matches(it.formattedNumber) } }
                    sameProject.filter { !first.matches(it.formattedNumber) }
                }.map { it.id }.toSet()
            result = result.filter { it.id !in removed }
        }
        return result
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
