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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.customergroup.CustomerGroupIndex
import org.projectforge.business.fibu.customergroup.CustomerGroupService
import org.projectforge.business.fibu.customergroup.ResolvedCustomers
import org.projectforge.business.fibu.customergroup.TextPattern
import org.projectforge.framework.persistence.api.impl.DBPredicate

/** A listed row's customer, as the checklists offer it: entity number, free text, and the row's project. */
internal data class CustomerRow(val kundeId: Long?, val kundeText: String?, val projektId: Long? = null)

/**
 * Where a listed entity keeps its customer, shared by the customer and the business-unit checklists.
 *
 * @param kundePath Path of the customer entity: `kunde`, or `projekt.kunde` for cost 2.
 * @param kundeTextPath Path of the free-text customer, if the entity has one: texts are ignored otherwise.
 * @param projektIdPath Path of the row's project id: `projekt.id`, or `id` for the projects themselves.
 */
internal class CustomerPaths(
    val kundePath: String = "kunde",
    val kundeTextPath: String? = "kundeText",
    val projektIdPath: String? = "projekt.id",
) {
    /**
     * `kunde IN (ids) OR (kunde IS NULL AND (kundeText IN (exactTexts) OR <patterns>))`: a free text only counts
     * for a row without a customer entity, as in the list's customer cell.
     *
     * @param exactTexts Free texts compared as they are (the `t:` keys, taken from the rows themselves).
     * @param patterns Free texts of a group or business unit, case-insensitive (see [TextPattern]).
     * @return null if there is nothing to look for.
     */
    fun predicate(
        ids: Collection<Long>,
        exactTexts: Collection<String> = emptyList(),
        patterns: Collection<TextPattern> = emptyList(),
    ): DBPredicate? {
        val byEntity = ids.takeIf { it.isNotEmpty() }?.let { DBPredicate.IsIn("$kundePath.id", it.distinct()) }
        val byText = kundeTextPath?.let { textPath ->
            val texts = mutableListOf<DBPredicate>()
            exactTexts.takeIf { it.isNotEmpty() }?.let { texts.add(DBPredicate.IsIn(textPath, it.distinct())) }
            patterns.forEach { texts.add(it.predicate(textPath)) }
            texts.takeIf { it.isNotEmpty() }?.let {
                DBPredicate.And(DBPredicate.IsNull(kundePath), it.singleOrNull() ?: DBPredicate.Or(*it.toTypedArray()))
            }
        }
        return when {
            byEntity != null && byText != null -> DBPredicate.Or(byEntity, byText)
            else -> byEntity ?: byText
        }
    }

    /**
     * The rows of the [resolved] customers or, by [ResolvedCustomers.projektIds], of their projects, a project
     * counting only for a row whose customer is none of [assigned] (the customers having a business unit):
     * the row's own customer decides first, as in [org.projectforge.business.fibu.customergroup.CustomerGroupIndex.businessUnitOf].
     */
    fun predicate(resolved: ResolvedCustomers, assigned: ResolvedCustomers = ResolvedCustomers.EMPTY): DBPredicate? {
        val byCustomer = predicate(resolved.kundeIds, patterns = resolved.texts)
        val byProject = projektIdPath?.takeIf { resolved.projektIds.isNotEmpty() }?.let {
            DBPredicate.And(DBPredicate.IsIn(it, resolved.projektIds.toList()), unassigned(assigned))
        }
        return when {
            byCustomer != null && byProject != null -> DBPredicate.Or(byCustomer, byProject)
            else -> byCustomer ?: byProject
        }
    }

    /**
     * The rows [CustomerGroupIndex.businessUnitOf] gives no business unit: the customer is none of [assigned]
     * (the customers having a business unit), and the project, if any, none of [projektIds] (those belonging to
     * one by their task).
     */
    fun withoutBusinessUnit(assigned: ResolvedCustomers, projektIds: Collection<Long>): DBPredicate {
        val customer = unassigned(assigned)
        val project = projektIdPath?.takeIf { projektIds.isNotEmpty() }?.let {
            // As in [unassigned]: a row without a project must be asked for, the NOT IN of SQL drops it.
            DBPredicate.Or(DBPredicate.IsNull(it), DBPredicate.Not(DBPredicate.IsIn(it, projektIds.toList())))
        }
        return project?.let { DBPredicate.And(customer, it) } ?: customer
    }

    /**
     * A row whose customer is none of [assigned]. Written out for both kinds of customer, since a NOT over a
     * null column is not true in SQL, while it is in memory.
     */
    private fun unassigned(assigned: ResolvedCustomers): DBPredicate {
        val entity = DBPredicate.IsNotNull(kundePath).let { notNull ->
            assigned.kundeIds.takeIf { it.isNotEmpty() }?.let {
                DBPredicate.And(notNull, DBPredicate.Not(DBPredicate.IsIn("$kundePath.id", it.toList())))
            } ?: notNull
        }
        val text = kundeTextPath?.takeIf { assigned.texts.isNotEmpty() }?.let { textPath ->
            val matches = assigned.texts.map { it.predicate(textPath) }
            DBPredicate.Or(
                DBPredicate.IsNull(textPath),
                DBPredicate.Not(matches.singleOrNull() ?: DBPredicate.Or(*matches.toTypedArray())),
            )
        }
        val freeText = text?.let { DBPredicate.And(DBPredicate.IsNull(kundePath), it) } ?: DBPredicate.IsNull(kundePath)
        return DBPredicate.Or(entity, freeText)
    }

    companion object {
        /** The configured customer groups; none outside a Spring context (unit tests). */
        val defaultIndex: () -> CustomerGroupIndex =
            { CustomerGroupService.instanceOrNull?.index ?: CustomerGroupIndex.EMPTY }
    }
}
