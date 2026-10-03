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

package org.projectforge.business.fibu.customergroup

data class CustomerGroupError(val fieldId: String, val messageKey: String, val params: List<Any?> = emptyList())

/**
 * Checks a configuration before it is stored: names and keys, known customers and tasks, valid patterns, and
 * above all that no customer is claimed twice — by two groups, or by two business units.
 *
 * Claimed twice is checked twice over:
 * - by the rules themselves: the same customer number in two sets, or a name pattern covering another set's
 *   (`dhl*` and `dh*`, `ACME AG` and `ACME*`), whether a matching customer exists yet or not;
 * - by the data: every customer entity of the [CustomerDirectory] (by number and by name) and every free-text
 *   customer in use is run against all sets, which also finds what overlapping patterns of different kinds
 *   catch (`ACME*` and `*Logistics` both claiming "ACME Logistics").
 *
 * A business unit claims its own members and those of its groups; a customer directly in the business unit
 * its group is in too is redundant, not a conflict. A business unit's task must not lie in (or be) another
 * business unit's task.
 *
 * A conflict between two sets is reported on both, each naming the other: the user fixes it on either side,
 * and the side being edited is the one they look at.
 */
class CustomerGroupValidator(private val directory: CustomerDirectory) {
    fun validate(config: CustomerGroupConfig): List<CustomerGroupError> {
        val errors = mutableListOf<CustomerGroupError>()
        checkSets(config.groups, GROUPS, errors)
        checkSets(config.businessUnits, BUSINESS_UNITS, errors)
        val buOfGroup = checkBusinessUnitGroups(config, errors)
        checkTasks(config.businessUnits, errors)
        checkClaims(groupClaims(config.groups), GROUPS, ERROR_CUSTOMER_IN_TWO_GROUPS, errors)
        checkClaims(
            businessUnitClaims(config, buOfGroup), BUSINESS_UNITS, ERROR_MEMBER_IN_TWO_BUSINESS_UNITS, errors,
        )
        return errors
    }

    private fun checkSets(sets: List<CustomerSet>, list: String, errors: MutableList<CustomerGroupError>) {
        val firstByName = mutableMapOf<String, Int>()
        val duplicates = sortedSetOf<Int>()
        val keys = mutableSetOf<String>()
        sets.forEachIndexed { i, set ->
            val name = set.name?.trim()
            if (name.isNullOrEmpty()) {
                errors.add(CustomerGroupError("$list[$i].name", ERROR_NAME_REQUIRED))
            } else {
                firstByName.putIfAbsent(name.lowercase(), i)?.let { first -> duplicates += listOf(first, i) }
            }
            set.key?.let { key ->
                if (!KEY_REGEX.matches(key) || !keys.add(key)) {
                    errors.add(CustomerGroupError("$list[$i].name", ERROR_INVALID_KEY, listOf(key)))
                }
            }
            set.customers.filterNot { directory.customers.containsKey(it) }.forEach {
                errors.add(CustomerGroupError("$list[$i].customers", ERROR_UNKNOWN_CUSTOMER, listOf(it)))
            }
            set.texts.filter { TextPattern.of(it) == null }.forEach {
                errors.add(CustomerGroupError("$list[$i].texts", ERROR_INVALID_PATTERN, listOf(it)))
            }
        }
        duplicates.forEach { i ->
            errors.add(CustomerGroupError("$list[$i].name", ERROR_DUPLICATE_NAME, listOf(sets[i].name?.trim())))
        }
    }

    /** @return The business unit of each group, the first one if a group is (wrongly) in two. */
    private fun checkBusinessUnitGroups(
        config: CustomerGroupConfig,
        errors: MutableList<CustomerGroupError>,
    ): Map<String, BusinessUnit> {
        val groupsByKey = config.groups.filter { it.key != null }.associateBy { it.key!! }
        val buOfGroup = mutableMapOf<String, Int>()
        config.businessUnits.forEachIndexed { i, bu ->
            val field = "$BUSINESS_UNITS[$i].groups"
            bu.groups.distinct().forEach { key ->
                val group = groupsByKey[key]
                if (group == null) {
                    errors.add(CustomerGroupError(field, ERROR_UNKNOWN_GROUP, listOf(key)))
                    return@forEach
                }
                buOfGroup.putIfAbsent(key, i)?.let { other ->
                    val otherBu = config.businessUnits[other]
                    errors.add(CustomerGroupError(field, ERROR_GROUP_IN_TWO_BUSINESS_UNITS, listOf(group.name, otherBu.name)))
                    errors.add(
                        CustomerGroupError("$BUSINESS_UNITS[$other].groups", ERROR_GROUP_IN_TWO_BUSINESS_UNITS, listOf(group.name, bu.name))
                    )
                }
            }
        }
        return buOfGroup.mapValues { config.businessUnits[it.value] }
    }

    /** Known tasks only, none in (or equal to) a task of another business unit. */
    private fun checkTasks(businessUnits: List<BusinessUnit>, errors: MutableList<CustomerGroupError>) {
        val buOfTask = mutableMapOf<Long, Int>()
        businessUnits.forEachIndexed { i, bu -> bu.tasks.forEach { buOfTask.putIfAbsent(it, i) } }
        businessUnits.forEachIndexed { i, bu ->
            val field = "$BUSINESS_UNITS[$i].tasks"
            bu.tasks.distinct().forEach { taskId ->
                val path = directory.taskPath(taskId)
                if (path == null) {
                    errors.add(CustomerGroupError(field, ERROR_UNKNOWN_TASK, listOf(taskId)))
                    return@forEach
                }
                // Found from the business unit holding the task below (once for a shared task, by the later one),
                // reported on both.
                path.firstOrNull { ancestor ->
                    val other = buOfTask[ancestor] ?: return@firstOrNull false
                    other != i && (ancestor != taskId || other < i)
                }?.let { ancestor ->
                    val other = buOfTask[ancestor]!!
                    errors.add(
                        CustomerGroupError(
                            field, ERROR_TASK_OVERLAP, listOf(taskTitle(taskId), taskTitle(ancestor), businessUnits[other].name),
                        )
                    )
                    errors.add(
                        CustomerGroupError(
                            "$BUSINESS_UNITS[$other].tasks",
                            if (ancestor == taskId) ERROR_TASK_OVERLAP else ERROR_TASK_CONTAINS,
                            listOf(taskTitle(ancestor), taskTitle(taskId), bu.name),
                        )
                    )
                }
            }
        }
    }

    /** What one group or business unit claims, each member with the field of the editor it comes from. */
    private class Claims(val index: Int, val set: CustomerSet) {
        val customers = mutableMapOf<Long, String>()
        val texts = mutableListOf<Pair<TextPattern, String>>()

        /** The members of [source], marked as coming from [customersField] and [textsField]. */
        fun add(source: CustomerSet, customersField: String, textsField: String = customersField) {
            source.customers.forEach { customers.putIfAbsent(it, customersField) }
            ResolvedCustomers.patternsOf(source).forEach { pattern ->
                if (texts.none { it.first == pattern }) texts.add(pattern to textsField)
            }
        }

        /** The field by which this set claims the customer entity or free text, null if it doesn't. */
        fun fieldOf(kundeId: Long?, name: String?): String? {
            kundeId?.let { customers[it] }?.let { return it }
            name?.takeIf { it.isNotBlank() } ?: return null
            return texts.firstOrNull { it.first.matches(name) }?.second
        }
    }

    private fun groupClaims(groups: List<CustomerGroup>) =
        groups.mapIndexed { i, group -> Claims(i, group).also { it.add(group, "customers", "texts") } }

    private fun businessUnitClaims(config: CustomerGroupConfig, buOfGroup: Map<String, BusinessUnit>): List<Claims> {
        val groupsByKey = config.groups.filter { it.key != null }.associateBy { it.key!! }
        return config.businessUnits.mapIndexed { i, bu ->
            Claims(i, bu).also { claims ->
                claims.add(bu, "customers", "texts")
                // Only through the business unit a group is in: a group in two is reported as such.
                bu.groups.distinct().filter { buOfGroup[it] === bu }.mapNotNull { groupsByKey[it] }.forEach {
                    claims.add(it, "groups")
                }
            }
        }
    }

    private fun checkClaims(
        claims: List<Claims>,
        list: String,
        customerError: String,
        errors: MutableList<CustomerGroupError>,
    ) {
        // Keys (set, field, other set) already reported by a rule: the data would only repeat them.
        val reported = mutableSetOf<Triple<Int, String, Int>>()
        // One message of each kind per such key, e.g. the first of several customers two sets share.
        val shown = mutableSetOf<Pair<Triple<Int, String, Int>, String>>()
        fun report(claim: Claims, field: String, other: Claims, messageKey: String, params: List<Any?>) {
            val key = Triple(claim.index, field, other.index)
            reported.add(key)
            if (shown.add(key to messageKey)) {
                errors.add(CustomerGroupError("$list[${claim.index}].$field", messageKey, params))
            }
        }
        claims.forEach { later ->
            claims.filter { it.index < later.index }.forEach { earlier ->
                later.customers.forEach { (id, field) ->
                    val earlierField = earlier.customers[id] ?: return@forEach
                    report(later, field, earlier, customerError, listOf(customerName(id), earlier.set.name))
                    report(earlier, earlierField, later, customerError, listOf(customerName(id), later.set.name))
                }
                later.texts.forEach { (pattern, field) ->
                    val (covering, earlierField) = earlier.texts.firstOrNull { (other, _) ->
                        pattern.isCoveredBy(other) || other.isCoveredBy(pattern)
                    } ?: return@forEach
                    report(later, field, earlier, ERROR_PATTERN_OVERLAP, listOf(pattern.raw, covering.raw, earlier.set.name))
                    report(earlier, earlierField, later, ERROR_PATTERN_OVERLAP, listOf(covering.raw, pattern.raw, later.set.name))
                }
            }
        }
        checkClaimsByData(claims, list, reported, errors)
    }

    /** Every customer entity and every free text in use, run against all sets of the list. */
    private fun checkClaimsByData(
        claims: List<Claims>,
        list: String,
        reported: Set<Triple<Int, String, Int>>,
        errors: MutableList<CustomerGroupError>,
    ) {
        if (claims.size < 2) {
            return
        }
        val conflicts = linkedMapOf<Triple<Int, String, Int>, MutableList<String>>()
        fun collect(kundeId: Long?, name: String?, label: String) {
            val owners = claims.mapNotNull { claim -> claim.fieldOf(kundeId, name)?.let { claim to it } }
            if (owners.size < 2) return
            owners.forEach { (claim, field) ->
                owners.filter { it.first !== claim }.forEach { (other, _) ->
                    val key = Triple(claim.index, field, other.index)
                    if (key !in reported) conflicts.getOrPut(key) { mutableListOf() }.add(label)
                }
            }
        }
        directory.customers.forEach { (id, name) -> collect(id, name?.trim(), customerName(id)) }
        directory.freeTexts.forEach { collect(null, it, it.trim()) }
        conflicts.forEach { (key, labels) ->
            val (index, field, otherIndex) = key
            val sorted = labels.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
            val shown = sorted.take(MAX_NAMES_SHOWN).joinToString(", ") + if (sorted.size > MAX_NAMES_SHOWN) ", …" else ""
            errors.add(
                CustomerGroupError("$list[$index].$field", ERROR_CLAIMED_TWICE, listOf(shown, claims[otherIndex].set.name, sorted.size))
            )
        }
    }

    private fun customerName(id: Long): String = directory.customers[id]?.let { "$id $it" } ?: "$id"

    private fun taskTitle(id: Long): String = directory.taskTitle(id) ?: "$id"

    companion object {
        const val GROUPS = "groups"
        const val BUSINESS_UNITS = "businessUnits"

        /** Keys are assigned by the server (or a client for a group a business unit refers to in the same save). */
        val KEY_REGEX = Regex("^[a-z0-9]{4,20}$")

        /** Customers named per conflict; the message gives their total number. */
        private const val MAX_NAMES_SHOWN = 5

        const val ERROR_NAME_REQUIRED = "fibu.customerGroups.error.nameRequired"
        const val ERROR_DUPLICATE_NAME = "fibu.customerGroups.error.duplicateName"
        const val ERROR_INVALID_KEY = "fibu.customerGroups.error.invalidKey"
        const val ERROR_UNKNOWN_CUSTOMER = "fibu.customerGroups.error.unknownCustomer"
        const val ERROR_INVALID_PATTERN = "fibu.customerGroups.error.invalidPattern"
        const val ERROR_CUSTOMER_IN_TWO_GROUPS = "fibu.customerGroups.error.customerInTwoGroups"
        const val ERROR_PATTERN_OVERLAP = "fibu.customerGroups.error.patternOverlap"
        const val ERROR_CLAIMED_TWICE = "fibu.customerGroups.error.claimedTwice"
        const val ERROR_UNKNOWN_GROUP = "fibu.customerGroups.error.unknownGroup"
        const val ERROR_GROUP_IN_TWO_BUSINESS_UNITS = "fibu.customerGroups.error.groupInTwoBusinessUnits"
        const val ERROR_MEMBER_IN_TWO_BUSINESS_UNITS = "fibu.customerGroups.error.memberInTwoBusinessUnits"
        const val ERROR_UNKNOWN_TASK = "fibu.customerGroups.error.unknownTask"
        const val ERROR_TASK_OVERLAP = "fibu.customerGroups.error.taskOverlap"
        const val ERROR_TASK_CONTAINS = "fibu.customerGroups.error.taskContains"
        const val ERROR_TOO_LARGE = "fibu.customerGroups.error.tooLarge"
        const val ERROR_MODIFIED_MEANWHILE = "fibu.customerGroups.error.modifiedMeanwhile"
    }
}
