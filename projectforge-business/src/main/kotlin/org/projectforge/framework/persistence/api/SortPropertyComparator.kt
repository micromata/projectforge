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

package org.projectforge.framework.persistence.api

import mu.KotlinLogging
import org.apache.commons.lang3.builder.CompareToBuilder
import org.projectforge.common.BeanHelper
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.text.Collator

private val log = KotlinLogging.logger {}

/**
 * Sorts already loaded objects by a list of [SortProperty], the way the database would have.
 *
 * Two callers need this, for two reasons:
 * - a **full text query** returns its hits in relevance order, so the sort has to happen afterwards
 *   (`DBFullTextResultIterator`),
 * - a **computed column** is no database column, so no `ORDER BY` can express it — the order book's four
 *   sums, its person days and its position count are `@get:Transient` getters over `OrderInfo`. This is
 *   what the Wicket list pages have always done: `MyListPageSortableDataProvider` loads the complete list
 *   and sorts it with `MyBeanComparator`.
 *
 * The semantics match `DBQueryBuilderByCriteria.addOrder`, so a list sorted here and one sorted by the
 * database read the same:
 * - **Blank ranks lowest**, and therefore leads an ascending sort and trails a descending one — reversing
 *   the sort brings the entries without a value into view. `null` and the empty string are one thing:
 *   text columns hold both representations of "no value", and left apart it would depend on the record
 *   which blanks surface.
 * - **Strings compare through a [Collator]** of the user's locale, so "Ärger" sorts where a German reader
 *   looks for it rather than after "Zeder".
 *
 * A property that cannot be read is logged once and skipped, not thrown: the sort order is stored per user
 * and outlives the column it names.
 *
 * @param valueOf How a property of an object is read. Defaults to reflection over the bean, which covers
 * every persisted column and every getter. Override it for a value reflection cannot reach as cheaply —
 * an order's net sum is a map lookup in `AuftragsCache`, which is what makes sorting 7000 rows a matter of
 * milliseconds. Return `null` to fall back to the reflective read.
 * @param computedProperties The properties [valueOf] *owns*: for these a `null` it returns is a real "no
 * value" (blank, ranks lowest), not the signal to fall back to reflection. A transient/computed column
 * (an invoice's due-date-or-discount-maturity, a sum read from a cache) has no bean getter of that name, so
 * reflecting it would throw — and, thrown for the blank rows only, would make the comparison intransitive
 * ("Comparison method violates its general contract!"). Everything not named here keeps the default: a
 * `null` from [valueOf] means "not mine", and reflection answers instead.
 */
class SortPropertyComparator<T : Any>(
    private val sortProperties: List<SortProperty>,
    private val computedProperties: Set<String> = emptySet(),
    private val valueOf: ((T, String) -> Any?)? = null,
) : Comparator<T> {
    private val collator = Collator.getInstance(ThreadLocalUserContext.locale)

    /** One message per unreadable property, not one per comparison. */
    private val loggedErrors = mutableSetOf<String>()

    override fun compare(o1: T, o2: T): Int {
        if (sortProperties.isEmpty()) {
            return 0
        }
        val ctb = CompareToBuilder()
        for (sortProperty in sortProperties) {
            try {
                val val1 = read(o1, sortProperty.property)
                val val2 = read(o2, sortProperty.property)
                val blank1 = isBlank(val1)
                val blank2 = isBlank(val2)
                if (blank1 || blank2) {
                    if (blank1 != blank2) {
                        // Blank ranks lowest, so the order flips with the direction.
                        val rank1 = if (blank1) 0 else 1
                        val rank2 = if (blank2) 0 else 1
                        if (sortProperty.ascending) {
                            ctb.append(rank1, rank2)
                        } else {
                            ctb.append(rank2, rank1)
                        }
                    }
                    continue // Both blank: equal for this property, compare the next one.
                }
                if (val1 is String) {
                    // Locale dependent, especially for German umlauts.
                    if (sortProperty.ascending) {
                        ctb.append(val1, val2, collator)
                    } else {
                        ctb.append(val2, val1, collator)
                    }
                } else if (val1 is Comparable<*>) {
                    if (sortProperty.ascending) {
                        ctb.append(val1, val2)
                    } else {
                        ctb.append(val2, val1)
                    }
                } else {
                    if (sortProperty.ascending) {
                        ctb.append(val1.toString(), val2?.toString())
                    } else {
                        ctb.append(val2?.toString(), val1.toString())
                    }
                }
            } catch (ex: Exception) {
                if (loggedErrors.add("${ex.message}")) {
                    log.warn("Ignore sort property (OK): ${ex.message}")
                }
            }
        }
        return ctb.toComparison()
    }

    /** The caller's value if it has one for this property, the reflective read otherwise. */
    private fun read(obj: T, property: String): Any? {
        if (property in computedProperties) {
            // The caller owns this one: its null is a genuine blank, so no reflective fallback (there is no
            // bean getter of this name to read, and reflecting it would throw).
            return valueOf?.invoke(obj, property)
        }
        valueOf?.let { it(obj, property) }?.let { return it }
        return BeanHelper.getNestedProperty(obj, property)
    }

    /** `null` and the empty string are both "no value" — see the class KDoc. */
    private fun isBlank(value: Any?): Boolean = value == null || (value is String && value.isEmpty())

    companion object {
        /**
         * The given list sorted by [sortProperties], or the list itself if there is nothing to sort by.
         */
        fun <T : Any> sort(
            list: List<T>,
            sortProperties: List<SortProperty>,
            computedProperties: Set<String> = emptySet(),
            valueOf: ((T, String) -> Any?)? = null,
        ): List<T> {
            if (sortProperties.isEmpty()) {
                return list
            }
            return list.sortedWith(SortPropertyComparator(sortProperties, computedProperties, valueOf))
        }
    }
}
