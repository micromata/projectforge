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

import org.projectforge.business.PfCaches
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.utils.StringComparator
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.UIFilterListValue

/**
 * The customer checklist of a list (order book, invoices, projects, cost 2): the rows of any of the chosen
 * customers, offered as the rows the list's other criteria match (see `AbstractEntityRest.checklistFilter`).
 *
 * A customer is either a customer entity or, where the entity has one ([kundeTextPath]), the free text naming a
 * customer that is not in the customer list, as the list's customer cell shows it. Both travel as keys in
 * `MagicFilterEntry.value.values`: [ENTITY_PREFIX] plus the customer's number, or [TEXT_PREFIX] plus the free
 * text.
 *
 * @param valuesUrl Endpoint of the selectable customers, relative to `/rs/`.
 * @param kundePath Path of the customer from the listed entity: `kunde`, or `projekt.kunde` for cost 2.
 * @param kundeTextPath Path of the free-text customer, if the entity has one.
 */
internal class CustomerChecklistFilter(
    private val valuesUrl: String,
    private val kundePath: String = "kunde",
    private val kundeTextPath: String? = "kundeText",
) {
    /** Pinned (`defaultFilter`): always on the filter row, not only once picked from the field list. */
    fun element(): UIFilterListElement =
        UIFilterListElement(FIELD, label = translate("fibu.kunde"), multi = true).also {
            it.valuesUrl = valuesUrl
            it.defaultFilter = true
        }

    /**
     * Turns the [FIELD] entry into `kunde IN (…) OR (kunde IS NULL AND kundeText IN (…))`. Synthetic, because
     * the field is no property of the entity, so `MagicFilterProcessor` cannot derive it. An entry without a
     * known key filters nothing.
     */
    fun addCriterion(target: QueryFilter, source: MagicFilter) {
        val entry = source.entries.find { it.field == FIELD } ?: return
        entry.synthetic = true
        buildPredicate(entry.value.values)?.let { target.add(it) }
    }

    internal fun buildPredicate(keys: Array<String>?): DBPredicate? {
        keys ?: return null
        val ids = keys.filter { it.startsWith(ENTITY_PREFIX) }.mapNotNull { it.removePrefix(ENTITY_PREFIX).toLongOrNull() }
        val byEntity = ids.takeIf { it.isNotEmpty() }?.let { DBPredicate.IsIn("$kundePath.id", it) }
        val byText = kundeTextPath?.let { textPath ->
            val texts = keys.filter { it.startsWith(TEXT_PREFIX) }.map { it.removePrefix(TEXT_PREFIX) }
            texts.takeIf { it.isNotEmpty() }?.let {
                DBPredicate.And(DBPredicate.IsNull(kundePath), DBPredicate.IsIn(textPath, it))
            }
        }
        return when {
            byEntity != null && byText != null -> DBPredicate.Or(byEntity, byText)
            else -> byEntity ?: byText
        }
    }

    companion object {
        /** Id of the filter entry, a pseudo field (see [addCriterion]). */
        const val FIELD = "customers"

        private const val ENTITY_PREFIX = "k:"
        private const val TEXT_PREFIX = "t:"

        /**
         * The customers of the given rows, each once, sorted by name: a customer entity's number, or `null`
         * plus the free text. A free-text customer only counts for a row without a customer entity: with one,
         * the cell shows the entity and so does this list.
         */
        fun valuesOf(customers: Sequence<Pair<Long?, String?>>): List<UIFilterListValue> {
            val customerIds = mutableSetOf<Long>()
            val texts = mutableSetOf<String>()
            customers.forEach { (kundeId, kundeText) ->
                if (kundeId != null) {
                    customerIds.add(kundeId)
                } else {
                    kundeText?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
                }
            }
            val entities = customerIds.map { id ->
                UIFilterListValue(ENTITY_PREFIX + id, PfCaches.instance.getKunde(id)?.displayName ?: id.toString())
            }
            val freeTexts = texts.map { UIFilterListValue(TEXT_PREFIX + it, it.trim(), freeText = true) }
            return (entities + freeTexts).sortedWith { a, b -> StringComparator.compare(a.displayName, b.displayName) }
        }
    }
}

/**
 * Removes the free-text pills `searchFields` derives from the given properties (`kunde` yields `kunde.name`,
 * `kunde.division`, …; a plain property such as `kundeText` itself): a customer or project checklist asks the
 * same question by picking instead of by a name fragment.
 */
internal fun MutableList<UILabelledElement>.removeTextFilters(vararg properties: String) {
    removeIf { element ->
        element is UIFilterElement && properties.any { element.id == it || element.id.startsWith("$it.") }
    }
}
