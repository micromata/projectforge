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
import mu.KotlinLogging
import org.hibernate.search.mapper.orm.Search
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.history.HistoryEntryDO
import java.util.*

private val log = KotlinLogging.logger {}

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
        if (!searchParams.searchHistory.isNullOrBlank()) {
            log.warn(
                "Search string for history search is given but is ignored by criteria search. Use full text search instead: ${
                    ToStringUtil.toJsonString(
                        searchParams
                    )
                }"
            )
        }
        val query = entityManager.createQuery(cr.select(root.get("entityId")).where(*predicates.toTypedArray()))
        query.maxResults = MAX_RESULT_SIZE
        val result = query.resultList
        if (result.isNullOrEmpty()) {
            return emptySet()
        }
        return result.toSet()
    }

    fun searchHistoryEntryByFullTextQuery(
        entityManager: EntityManager,
        clazz: Class<*>,
        searchParams: DBHistorySearchParams
    ): Set<Long> {
        val result = Search.session(entityManager).search(HistoryEntryDO::class.java).where { q ->
            q.bool().with { bool ->
                bool.must { must ->
                    must.match().field("entityName").matching(clazz.name)
                    val searchString = searchParams.searchHistory
                    if (!searchString.isNullOrBlank()) {
                        log.warn(
                            "Search string for history search is given but is ignored by full text search. Use criteria search instead: ${
                                ToStringUtil.toJsonString(
                                    searchParams
                                )
                            }"
                        )
                        var str = searchString.replace('%', '*')
                        if (str.length > 1 && str[0].isLetterOrDigit()) {
                            str = "*$str"
                            if (!str.endsWith("*"))
                                str = "$str*"
                        }
                        must.wildcard().field("oldValue").matching(str)
                    }
                    searchParams.modifiedByUserId?.let { modifiedByUserId ->
                        must.match().field("modifiedBy").matching(modifiedByUserId.toString())
                    }
                    searchParams.modifiedFrom?.let { modifiedFrom ->
                        searchParams.modifiedTo?.let { modifiedTo ->
                            // Between:
                            must.range().field("modifiedAt").between(modifiedFrom.utilDate, modifiedTo.utilDate)
                        } ?: run {
                            must.range().field("modifiedAt").atLeast(modifiedFrom.utilDate)
                        }
                    } ?: run {
                        searchParams.modifiedTo?.let { modifiedTo ->
                            must.range().field("modifiedAt").atMost(modifiedTo.utilDate)
                        }
                    }
                }
            }
        }.fetchHits(MAX_RESULT_SIZE)

        if (result.isNullOrEmpty()) {
            return emptySet()
        }
        return result.map {
            (it as Array<*>)[0] as Long
        }.toSet()
    }
}
