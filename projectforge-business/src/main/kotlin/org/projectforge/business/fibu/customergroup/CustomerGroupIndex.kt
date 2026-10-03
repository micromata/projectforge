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

/**
 * The customers a picked group or business unit stands for: customer entities by number, and free-text
 * customers (only for rows without a customer entity) by [TextPattern].
 *
 * @param projektIds Projects belonging to a business unit by their task: they count for a row only if its
 * customer leads to no business unit (see [CustomerGroupIndex.businessUnitOf]).
 */
data class ResolvedCustomers(
    val kundeIds: Set<Long> = emptySet(),
    val texts: List<TextPattern> = emptyList(),
    val projektIds: Set<Long> = emptySet(),
) {
    val isEmpty: Boolean
        get() = kundeIds.isEmpty() && texts.isEmpty() && projektIds.isEmpty()

    operator fun plus(other: ResolvedCustomers) = ResolvedCustomers(
        kundeIds + other.kundeIds,
        (texts + other.texts).distinct(),
        projektIds + other.projektIds,
    )

    companion object {
        val EMPTY = ResolvedCustomers()

        fun patternsOf(set: CustomerSet): List<TextPattern> = set.texts.mapNotNull { TextPattern.of(it) }.distinct()
    }
}

/**
 * Immutable lookup over a [CustomerGroupConfig] applied to the [CustomerDirectory]: to which group or business
 * unit a row's customer belongs, and which customers a picked group or business unit stands for.
 *
 * A customer entity belongs to a set by its number or by its name matching one of the set's [CustomerSet.texts];
 * a free text (only for rows without a customer entity) by the texts alone. The validator refuses a customer
 * two sets claim, so the order applied here (number, exact name, patterns in the configuration's order) only
 * matters for a configuration stored before a rule was added, or for a customer added after the save.
 *
 * Business units also claim projects by task ([BusinessUnit.tasks]): the fallback for a row whose customer
 * leads to no business unit, as the "Sonstige Tätigkeiten BU …" projects of an internal customer.
 */
class CustomerGroupIndex(
    val config: CustomerGroupConfig,
    directory: CustomerDirectory = CustomerDirectory.EMPTY,
) {
    private val groupsByKey = config.groups.filter { it.key != null }.associateBy { it.key!! }
    private val businessUnitsByKey = config.businessUnits.filter { it.key != null }.associateBy { it.key!! }
    private val groupMembers = Members(config.groups, directory.customers)
    private val businessUnitMembers = Members(config.businessUnits, directory.customers)
    private val businessUnitByGroupKey = buildMap {
        config.businessUnits.forEach { bu -> bu.groups.forEach { putIfAbsent(it, bu) } }
    }

    /** Every customer entity with a business unit, directly or through its group. */
    private val businessUnitByKundeId: Map<Long, BusinessUnit> =
        (directory.customers.keys + groupMembers.kundeIds + businessUnitMembers.kundeIds).mapNotNull { id ->
            customerBusinessUnitOf(id, null)?.let { id to it }
        }.toMap()

    /** Every project below a business unit's task, the deepest task deciding. */
    private val businessUnitByProjektId: Map<Long, BusinessUnit> = run {
        val byTask = buildMap { config.businessUnits.forEach { bu -> bu.tasks.forEach { putIfAbsent(it, bu) } } }
        if (byTask.isEmpty()) {
            return@run emptyMap()
        }
        directory.projects.mapNotNull { (id, project) ->
            val path = project.taskId?.let { directory.taskPath(it) } ?: return@mapNotNull null
            path.reversed().firstNotNullOfOrNull { byTask[it] }?.let { id to it }
        }.toMap()
    }

    val groups: List<CustomerGroup>
        get() = config.groups

    val businessUnits: List<BusinessUnit>
        get() = config.businessUnits

    val isEmpty: Boolean
        get() = config.groups.isEmpty() && config.businessUnits.isEmpty()

    /**
     * The customers having a business unit, for the criterion that a project only counts by its task for a row
     * whose customer has none (see [ResolvedCustomers.projektIds]).
     */
    val businessUnitCustomers: ResolvedCustomers by lazy {
        ResolvedCustomers(
            businessUnitByKundeId.keys,
            config.businessUnits.flatMap { textsOf(it) }.distinct(),
        )
    }

    /** The projects belonging to a business unit by their task, for the rows with none (see [businessUnitOf]). */
    val businessUnitProjects: Set<Long>
        get() = businessUnitByProjektId.keys

    fun getGroup(key: String?): CustomerGroup? = key?.let { groupsByKey[it] }

    fun getBusinessUnit(key: String?): BusinessUnit? = key?.let { businessUnitsByKey[it] }

    fun groupOf(kundeId: Long?, kundeText: String?): CustomerGroup? = groupMembers.of(kundeId, kundeText)

    /**
     * The business unit the customer belongs to directly, else the one of its group, else, if the row has a
     * project, the one whose task the project's task lies in.
     */
    fun businessUnitOf(kundeId: Long?, kundeText: String?, projektId: Long? = null): BusinessUnit? =
        customerBusinessUnitOf(kundeId, kundeText) ?: projektId?.let { businessUnitByProjektId[it] }

    /**
     * The customers each project is worked for ([ProjectRef.customers]), for the lists reaching their customer
     * only through the project (timesheets, projects, cost 2). Projects with none are left out.
     */
    val projectCustomers: Map<Long, List<CustomerKey>> =
        directory.projects.mapNotNull { (id, project) -> project.customers.takeIf { it.isNotEmpty() }?.let { id to it } }
            .toMap()

    /**
     * The business unit of a row known only by its project: that of the first of the [customers] having one (the
     * project's own customer, else the most recent order's), else the one of the project's task. One per
     * project, so the business units still partition the rows.
     */
    fun businessUnitOf(customers: List<CustomerKey>, projektId: Long?): BusinessUnit? =
        customers.firstNotNullOfOrNull { customerBusinessUnitOf(it.kundeId, it.kundeText) }
            ?: projektId?.let { businessUnitByProjektId[it] }

    /** [businessUnitOf] for the project's customers ([projectCustomers]). */
    fun businessUnitOfProject(projektId: Long): BusinessUnit? =
        businessUnitOf(projectCustomers[projektId] ?: emptyList(), projektId)

    /** The group of the first of the project's customers ([projectCustomers]) having one. */
    fun groupOfProject(projektId: Long): CustomerGroup? =
        projectCustomers[projektId]?.firstNotNullOfOrNull { groupOf(it.kundeId, it.kundeText) }

    /** @return The group's customers, or null for an unknown key (a deleted group in a saved filter). */
    fun resolveGroup(key: String?): ResolvedCustomers? {
        val group = getGroup(key) ?: return null
        return ResolvedCustomers(groupMembers.kundeIdsOf(group), ResolvedCustomers.patternsOf(group))
    }

    /**
     * @return The business unit's customers (directly and through its groups) and the projects below its
     * tasks, null for an unknown key.
     */
    fun resolveBusinessUnit(key: String?): ResolvedCustomers? {
        val bu = getBusinessUnit(key) ?: return null
        return ResolvedCustomers(
            businessUnitByKundeId.filterValues { it === bu }.keys,
            textsOf(bu),
            businessUnitByProjektId.filterValues { it === bu }.keys,
        )
    }

    /** The texts of the business unit and of its groups: a free text belongs to it through either. */
    private fun textsOf(bu: BusinessUnit): List<TextPattern> =
        (ResolvedCustomers.patternsOf(bu) + bu.groups.mapNotNull { getGroup(it) }
            .flatMap { ResolvedCustomers.patternsOf(it) }).distinct()

    private fun customerBusinessUnitOf(kundeId: Long?, kundeText: String?): BusinessUnit? =
        businessUnitMembers.of(kundeId, kundeText)
            ?: groupOf(kundeId, kundeText)?.key?.let { businessUnitByGroupKey[it] }

    /** The members of the sets of one list (the groups, or the business units). */
    private class Members<T : CustomerSet>(sets: List<T>, customers: Map<Long, String?>) {
        private val byText = mutableMapOf<String, T>()
        private val patterns = mutableListOf<Pair<TextPattern, T>>()
        private val byKundeId: Map<Long, T>

        init {
            val explicit = mutableMapOf<Long, T>()
            sets.forEach { set ->
                set.customers.forEach { explicit.putIfAbsent(it, set) }
                ResolvedCustomers.patternsOf(set).forEach { pattern ->
                    if (pattern.isPattern) {
                        patterns.add(pattern to set)
                    } else {
                        byText.putIfAbsent(pattern.plain.lowercase(), set)
                    }
                }
            }
            byKundeId = explicit + customers.mapNotNull { (id, name) ->
                if (explicit.containsKey(id)) null else ofText(name?.trim())?.let { id to it }
            }
        }

        val kundeIds: Set<Long>
            get() = byKundeId.keys

        fun kundeIdsOf(set: T): Set<Long> = byKundeId.filterValues { it === set }.keys

        fun of(kundeId: Long?, kundeText: String?): T? {
            if (kundeId != null) {
                return byKundeId[kundeId]
            }
            // Not trimmed: the SQL criteria compare the stored text as it is, and both must agree.
            return ofText(kundeText)
        }

        private fun ofText(text: String?): T? {
            text?.takeIf { it.isNotBlank() } ?: return null
            return byText[text.lowercase()] ?: patterns.firstOrNull { it.first.matches(text) }?.second
        }
    }

    companion object {
        val EMPTY = CustomerGroupIndex(CustomerGroupConfig())
    }
}
