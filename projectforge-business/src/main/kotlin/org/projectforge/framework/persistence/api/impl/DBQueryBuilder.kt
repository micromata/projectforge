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

import org.projectforge.business.multitenancy.TenantService
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.slf4j.LoggerFactory
import javax.persistence.EntityManager


class DBQueryBuilder<O : ExtendedBaseDO<Int>>(
        private val baseDao: BaseDao<O>,
        private val entityManager: EntityManager,
        tenantService: TenantService,
        private val queryFilter: QueryFilter,
        dbFilter: DBFilter,
        ignoreTenant: Boolean = false) {

    enum class Mode {
        /**
         * Standard full text search by using full text query builder.
         */
        FULLTEXT,
        /**
         * At default the query builder of the full text search is used. As an alternative, the query may be
         * defined as a query string, e. g. '+name:sch* +kassel...'.
         */
        MULTI_FIELD_FULLTEXT_QUERY, // Not yet implemented
        /**
         * Plain criteria search without full text search.
         */
        CRITERIA
    }

    private val log = LoggerFactory.getLogger(DBQueryBuilder::class.java)
    private var _dbQueryBuilderByCriteria: DBQueryBuilderByCriteria<O>? = null
    private val dbQueryBuilderByCriteria: DBQueryBuilderByCriteria<O>
        get() {
            if (_dbQueryBuilderByCriteria == null) _dbQueryBuilderByCriteria = DBQueryBuilderByCriteria(baseDao, entityManager, queryFilter)
            return _dbQueryBuilderByCriteria!!
        }
    private var _dbQueryBuilderByFullText: DBQueryBuilderByFullText<O>? = null
    private val dbQueryBuilderByFullText: DBQueryBuilderByFullText<O>
        get() {
            if (_dbQueryBuilderByFullText == null) _dbQueryBuilderByFullText = DBQueryBuilderByFullText(baseDao, entityManager, queryFilter, useMultiFieldQueryParser = mode == Mode.MULTI_FIELD_FULLTEXT_QUERY)
            return _dbQueryBuilderByFullText!!
        }
    private val mode: Mode

    /**
     * As an alternative to the query builder of the full text search, Hibernate search supports a simple query string,
     * e. g. '+name:sch* +kassel...'
     */
    //private val multiFieldParserQueryString = mutableListOf<String>()
    /**
     * matchers for filtering result list. Used e. g. for searching fields without index if criteria search is not
     * configured.
     */
    val resultPredicates = mutableListOf<DBPredicate>()

    private val criteriaSearchAvailable: Boolean
        get() = mode == Mode.CRITERIA

    private val fullTextSearch: Boolean
        get() = mode == Mode.FULLTEXT || mode == Mode.MULTI_FIELD_FULLTEXT_QUERY

    init {
        val stats = dbFilter.createStatistics(baseDao)
        mode =
                if (stats.multiFieldFullTextQueryRequired)
                    Mode.MULTI_FIELD_FULLTEXT_QUERY
                else if (stats.fullTextRequired)
                    Mode.FULLTEXT
                else
                    Mode.CRITERIA // Criteria search (no full text search entries found).

        if (!ignoreTenant && tenantService.isMultiTenancyAvailable) {
            val userContext = ThreadLocalUserContext.getUserContext()
            val currentTenant = userContext.currentTenant
            if (currentTenant != null) {
                if (currentTenant.isDefault) {
                    addMatcher(DBPredicate.Or(DBPredicate.Equal("tenant", userContext.currentTenant),
                            DBPredicate.IsNull("tenant")))
                } else {
                    addMatcher(DBPredicate.Equal("tenant", userContext.currentTenant))
                }
            }
        }
        dbFilter.predicates.forEach {
            addMatcher(it)
        }

        var maxOrder = 3
        for (sortProperty in dbFilter.sortProperties) {
            addOrder(sortProperty)
            if (--maxOrder <= 0)
                break // Add only 3 orders.
        }
        // TODO setCacheRegion(baseDao, criteria)

    }

    /**
     * Adds predicate to result matchers or, if criteria search is enabled, a new predicates for the criteria is appended.
     */
    private fun addMatcher(predicate: DBPredicate) {
        if (criteriaSearchAvailable) {
            dbQueryBuilderByCriteria.add(predicate)
        } else if (predicate.fullTextSupport) {
            if (!dbQueryBuilderByFullText.add(predicate)) {
                if (log.isDebugEnabled) log.debug("Adding result predicate: $predicate")
                resultPredicates.add(predicate)
            }
        } else {
            if (log.isDebugEnabled) log.debug("Adding result predicate: $predicate")
            resultPredicates.add(predicate)
        }
    }

    fun result(): DBResultIterator<O> {
        if (fullTextSearch) {
            return dbQueryBuilderByFullText.createResultIterator(resultPredicates)
        }
        return dbQueryBuilderByCriteria.createResultIterator(resultPredicates)
    }

    /**
     * Sorting for criteria query is done by the data base, for full text search by Kotlin after getting the result list.
     */
    fun addOrder(sortProperty: SortProperty) {
        if (fullTextSearch) {
            dbQueryBuilderByFullText.addOrder(sortProperty)
        } else {
            dbQueryBuilderByCriteria.addOrder(sortProperty)
        }
    }
}
