/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.hibernate.search.query.dsl.BooleanJunction
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO
import org.slf4j.LoggerFactory
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.criteria.Predicate

internal object DBHistoryQuery {
    private const val MAX_RESULT_SIZE = 100000 // Limit result list to 100000

    fun searchHistoryEntryByCriteria(entityManager: EntityManager, clazz: Class<*>, searchParams: DBHistorySearchParams): Set<Long> {
        val cb = entityManager.criteriaBuilder
        val cr = cb.createQuery(Long::class.java)
        val root = cr.from(PfHistoryMasterDO::class.java)
        val predicates = mutableListOf<Predicate>()
        predicates.add(cb.equal(root.get<String>("entityName"), clazz.name))
        if (searchParams.modifiedByUserId != null) {
            predicates.add(cb.equal(root.get<String>("modifiedBy"), "${searchParams.modifiedByUserId}"))
        }
        if (searchParams.modifiedFrom != null) {
            if (searchParams.modifiedTo != null) {
                // Between:
                predicates.add(cb.between(root.get<Date>("modifiedAt"), searchParams.modifiedFrom!!.utilDate, searchParams.modifiedTo!!.utilDate))
            } else {
                predicates.add(cb.greaterThanOrEqualTo(root.get<Date>("modifiedAt"), searchParams.modifiedFrom!!.utilDate))
            }
        } else if (searchParams.modifiedTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get<Date>("modifiedAt"), searchParams.modifiedTo!!.utilDate))
        }
        if (!searchParams.searchHistory.isNullOrBlank()) {
            log.warn("Search string for history search is given but is ignored by criteria search. Use full text search instead: ${ToStringUtil.toJsonString(searchParams)}")
        }
        val query = entityManager.createQuery(cr.select(root.get("entityId")).where(*predicates.toTypedArray()))
        query.maxResults = MAX_RESULT_SIZE
        val result = query.resultList
        if (result.isNullOrEmpty()) {
            return emptySet()
        }
        return result.toSet()
    }

    fun searchHistoryEntryByFullTextQuery(entityManager: EntityManager, clazz: Class<*>, searchParams: DBHistorySearchParams): Set<Long> {
        val fullTextEntityManager = org.hibernate.search.jpa.Search.getFullTextEntityManager(entityManager)
        val queryBuilder = fullTextEntityManager.searchFactory.buildQueryBuilder().forEntity(PfHistoryMasterDO::class.java).get()
        var boolJunction: BooleanJunction<*> = queryBuilder.bool()
        boolJunction = boolJunction.must(queryBuilder.keyword().onField("entityName").matching(clazz.name).createQuery())
        if (searchParams.modifiedByUserId != null) {
            boolJunction = boolJunction.must(queryBuilder.keyword().onField("modifiedBy").matching(searchParams.modifiedByUserId).createQuery())
        }
        if (searchParams.modifiedFrom != null) {
            if (searchParams.modifiedTo != null) {
                // Between:
                boolJunction = boolJunction.must(queryBuilder.range().onField("modifiedAt")
                        .from(searchParams.modifiedFrom!!.utilDate)
                        .to(searchParams.modifiedTo!!.utilDate)
                        .createQuery())
            } else {
                boolJunction = boolJunction.must(queryBuilder.range().onField("modifiedAt")
                        .above(searchParams.modifiedFrom!!.utilDate)
                        .createQuery())
            }
        } else if (searchParams.modifiedTo != null) {
            boolJunction = boolJunction.must(queryBuilder.range().onField("modifiedAt")
                    .below(searchParams.modifiedTo!!.utilDate)
                    .createQuery())
        }
        val searchString = searchParams.searchHistory
        if (!searchString.isNullOrBlank()) {
            var str = searchString.replace('%', '*')
            if (!str.startsWith("*"))
                str = "*$str"
            if (!str.endsWith("*"))
                str = "$str*"
            boolJunction = boolJunction.must(queryBuilder.keyword().wildcard().onField("oldValue")
                    .matching(str)
                    .createQuery())
        }
        val fullTextQuery = fullTextEntityManager.createFullTextQuery(boolJunction.createQuery(), PfHistoryMasterDO::class.java)
        fullTextQuery.maxResults = MAX_RESULT_SIZE
        fullTextQuery.setProjection("entityId")
        @Suppress("UNCHECKED_CAST")
        val result = fullTextQuery.getResultList()// as List<Long> // return a list of managed objects

        if (result.isNullOrEmpty()) {
            return emptySet()
        }
        return result.map {
            (it as Array<*>)[0] as Long
        }.toSet()
    }

    private val log = LoggerFactory.getLogger(DBHistoryQuery::class.java)
}
