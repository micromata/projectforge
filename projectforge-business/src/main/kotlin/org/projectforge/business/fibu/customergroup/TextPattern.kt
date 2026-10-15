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

import org.projectforge.framework.persistence.api.impl.DBPredicate

/**
 * A customer name of a group or business unit, matched against customer entities and free-text customers
 * alike: an exact name (`ACME Holding GmbH`) or a pattern with a
 * leading and/or trailing `*` (`ACME*`, `*Logistics`, `*ACME*`). Both are case-insensitive.
 *
 * Only what [DBPredicate.Like] does the same in SQL and in memory: no `*` in the middle, no SQL wildcards
 * (`%`, `_`) in a pattern, and at least [MIN_LENGTH] characters besides the `*`. [matches] (which customers a
 * list offers as members) and [predicate] (which rows a pick finds) must agree.
 */
class TextPattern private constructor(
    /** The trimmed text as entered, with its `*`. */
    val raw: String,
    /** The text without the `*`. */
    val plain: String,
    val matchType: MatchType,
) {
    enum class MatchType { EXACT, STARTS_WITH, ENDS_WITH, CONTAINS }

    val isPattern: Boolean
        get() = matchType != MatchType.EXACT

    fun matches(text: String?): Boolean {
        text ?: return false
        return when (matchType) {
            MatchType.EXACT -> text.equals(plain, ignoreCase = true)
            MatchType.STARTS_WITH -> text.startsWith(plain, ignoreCase = true)
            MatchType.ENDS_WITH -> text.endsWith(plain, ignoreCase = true)
            MatchType.CONTAINS -> text.contains(plain, ignoreCase = true)
        }
    }

    /**
     * Whether every name this pattern matches is matched by [other] as well: `dhl*` by `dh*`, `ACME AG` by
     * `ACME*`, anything containing "post" by `*post*`. Two groups with such patterns claim the same customers
     * by construction, whether any exists yet or not.
     */
    fun isCoveredBy(other: TextPattern): Boolean {
        return when (other.matchType) {
            MatchType.EXACT -> matchType == MatchType.EXACT && plain.equals(other.plain, ignoreCase = true)
            MatchType.STARTS_WITH -> (matchType == MatchType.EXACT || matchType == MatchType.STARTS_WITH) &&
                    plain.startsWith(other.plain, ignoreCase = true)

            MatchType.ENDS_WITH -> (matchType == MatchType.EXACT || matchType == MatchType.ENDS_WITH) &&
                    plain.endsWith(other.plain, ignoreCase = true)

            MatchType.CONTAINS -> plain.contains(other.plain, ignoreCase = true)
        }
    }

    /** The criterion on the free-text property at [path], e.g. `kundeText`. */
    fun predicate(path: String): DBPredicate {
        return if (isPattern) {
            DBPredicate.Like(path, raw, ignoreCase = true)
        } else {
            DBPredicate.Equal(path, plain, ignoreCase = true)
        }
    }

    override fun equals(other: Any?): Boolean =
        other is TextPattern && other.matchType == matchType && other.plain.equals(plain, ignoreCase = true)

    override fun hashCode(): Int = 31 * matchType.hashCode() + plain.lowercase().hashCode()

    override fun toString(): String = raw

    companion object {
        const val WILDCARD = '*'
        const val MIN_LENGTH = 2

        /** @return The pattern, or null if [raw] is none (see the class comment). */
        fun of(raw: String?): TextPattern? {
            val text = raw?.trim() ?: return null
            val leading = text.startsWith(WILDCARD)
            val trailing = text.length > 1 && text.endsWith(WILDCARD)
            val plain = text.removePrefix(if (leading) "$WILDCARD" else "")
                .let { if (trailing) it.removeSuffix("$WILDCARD") else it }
            if (plain.trim().length < MIN_LENGTH || plain.trim() != plain || plain.contains(WILDCARD)) {
                return null
            }
            val matchType = when {
                leading && trailing -> MatchType.CONTAINS
                leading -> MatchType.ENDS_WITH
                trailing -> MatchType.STARTS_WITH
                else -> MatchType.EXACT
            }
            if (matchType != MatchType.EXACT && (plain.contains('%') || plain.contains('_'))) {
                return null
            }
            return TextPattern(text, plain, matchType)
        }
    }
}
