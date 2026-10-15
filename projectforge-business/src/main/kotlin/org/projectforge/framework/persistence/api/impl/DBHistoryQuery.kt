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

package org.projectforge.framework.persistence.api.impl

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import org.projectforge.framework.persistence.history.HistoryEntryDO
import java.util.*

internal object DBHistoryQuery {
    private const val MAX_RESULT_SIZE = 100_000 // Limit result list to 100_000

    fun searchHistoryEntryByCriteria(
        entityManager: EntityManager,
        clazz: Class<*>,
        searchParams: DBHistorySearchParams
    ): Set<Long> {
        val cb = entityManager.criteriaBuilder
        val cr = cb.createQuery(Long::class.java)
        val root = cr.from(HistoryEntryDO::class.java)
        val predicates = mutableListOf<Predicate>()
        predicates.add(cb.equal(root.get<String>("entityName"), clazz.name))
        if (searchParams.modifiedByUserId != null) {
            predicates.add(cb.equal(root.get<String>("modifiedBy"), "${searchParams.modifiedByUserId}"))
        }
        if (searchParams.modifiedFrom != null) {
            if (searchParams.modifiedTo != null) {
                // Between:
                predicates.add(
                    cb.between(
                        root.get<Date>("modifiedAt"),
                        searchParams.modifiedFrom!!.utilDate,
                        searchParams.modifiedTo!!.utilDate
                    )
                )
            } else {
                predicates.add(
                    cb.greaterThanOrEqualTo(
                        root.get<Date>("modifiedAt"),
                        searchParams.modifiedFrom!!.utilDate
                    )
                )
            }
        } else if (searchParams.modifiedTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get<Date>("modifiedAt"), searchParams.modifiedTo!!.utilDate))
        }
        searchParams.searchHistory?.takeIf { it.isNotBlank() }?.let { searchString ->
            // The searched value doesn't live on HistoryEntryDO but on its attributes, so the search
            // needs a join. Both columns are searched: an insert entry has no old value at all, so
            // matching only old_value would never find a value that was just set.
            val attributes = root.join<HistoryEntryDO, Any>("attributes")
            val pattern = likePatternOf(searchString)
            predicates.add(
                cb.or(
                    cb.like(cb.lower(attributes.get<String>("value")), pattern),
                    cb.like(cb.lower(attributes.get<String>("oldValue")), pattern),
                )
            )
        }
        val query = entityManager.createQuery(
            cr.select(root.get("entityId")).where(*predicates.toTypedArray()).distinct(true)
        )
        query.maxResults = MAX_RESULT_SIZE
        val result = query.resultList
        if (result.isNullOrEmpty()) {
            return emptySet()
        }
        return result.toSet()
    }

    /**
     * The pattern of a `like` for the given search string: lower case, `%` and `*` both standing for any
     * text, and surrounded by `%` unless the user placed a wildcard themselves.
     */
    private fun likePatternOf(searchString: String): String {
        val str = searchString.trim().lowercase().replace('*', '%')
        return if (str.contains('%')) str else "%$str%"
    }
}
