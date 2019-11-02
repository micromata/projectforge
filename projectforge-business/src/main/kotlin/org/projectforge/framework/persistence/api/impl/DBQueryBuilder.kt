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
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.slf4j.LoggerFactory


class DBQueryBuilder<O : ExtendedBaseDO<Int>>(
        val baseDao: BaseDao<O>,
        tenantService: TenantService,
        val dbFilter: DBFilter,
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
    private var dbQueryBuilderByCriteria_: DBQueryBuilderByCriteria<O>? = null
    private val dbQueryBuilderByCriteria: DBQueryBuilderByCriteria<O>
        get() {
            if (dbQueryBuilderByCriteria_ == null) dbQueryBuilderByCriteria_ = DBQueryBuilderByCriteria<O>(baseDao)
            return dbQueryBuilderByCriteria_!!
        }
    private var dbQueryBuilderByFullText_: DBQueryBuilderByFullText<O>? = null
    private val dbQueryBuilderByFullText: DBQueryBuilderByFullText<O>
        get() {
            if (dbQueryBuilderByFullText_ == null) dbQueryBuilderByFullText_ = DBQueryBuilderByFullText<O>(baseDao, useMultiFieldQueryParser = mode == Mode.MULTI_FIELD_FULLTEXT_QUERY)
            return dbQueryBuilderByFullText_!!
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
    private val dbPredicates = mutableListOf<DBPredicate>()

    private val criteriaSearchAvailable: Boolean
        get() = mode == Mode.CRITERIA

    private val fullTextSearch: Boolean
        get() = mode == Mode.FULLTEXT || mode == Mode.MULTI_FIELD_FULLTEXT_QUERY

    init {
        val stats = dbFilter.createStatistics(baseDao)
        mode = if (stats.fullTextRequired) {
            DBQueryBuilder.Mode.FULLTEXT
            //DBQueryBuilder.Mode.MULTI_FIELD_FULLTEXT_QUERY
        } else {
            DBQueryBuilder.Mode.CRITERIA // Criteria search (no full text search entries found).
        }
        if (!ignoreTenant && tenantService.isMultiTenancyAvailable) {
            val userContext = ThreadLocalUserContext.getUserContext()
            val currentTenant = userContext.currentTenant
            if (currentTenant != null) {
                if (currentTenant.isDefault) {
                    addMatcher(DBPredicate.Or(DBPredicate.Equals("tenant", userContext.currentTenant),
                            DBPredicate.IsNull("tenant")))
                } else {
                    addMatcher(DBPredicate.Equals("tenant", userContext.currentTenant))
                }
            }
        }
        dbFilter.predicates.forEach {
            addMatcher(it)
        }

        var maxOrder = 3
        for (sortProperty in dbFilter.sortProperties) {
            var prop = sortProperty.property
            if (prop.indexOf('.') > 0)
                prop = prop.substring(prop.indexOf('.') + 1)
            addOrder(SortBy(prop, sortProperty.sortOrder == SortOrder.ASCENDING))
            if (--maxOrder <= 0)
                break // Add only 3 orders.
        }
        // TODO setCacheRegion(baseDao, criteria)

    }

    fun equal(field: String, value: Any) {
        if (criteriaSearchAvailable) {
            dbQueryBuilderByCriteria.addEqualPredicate(field, value)
            return
        }
        // Full text search
        if (dbQueryBuilderByFullText.fieldSupported(field)) {
            dbQueryBuilderByFullText.equal(field, value)
        } else {
            dbPredicates.add(DBPredicate.Equals(field, value))
        }
    }

    fun notEqual(field: String, value: Any) {
        if (criteriaSearchAvailable) {
            dbQueryBuilderByCriteria.addNotEqualPredicate(field, value)
            return
        }
        // Full text search
        if (dbQueryBuilderByFullText.fieldSupported(field)) {
            dbQueryBuilderByFullText.notEqual(field, value)
        } else {
            dbPredicates.add(DBPredicate.NotEquals(field, value))
        }
    }

    fun ilike(field: String, value: String) {
        if (fullTextSearch && dbQueryBuilderByFullText.fieldSupported(field)) {
            dbQueryBuilderByFullText.ilike(field, value)
        } else {
            addMatcher(DBPredicate.Like(field, value))
        }
    }

    fun isNull(field: String) {
        val matcher = DBPredicate.IsNull(field)
        if (criteriaSearchAvailable) {
            dbQueryBuilderByCriteria.add(matcher)
            return
        }
        // Full text search doesn't support feature 'isNull'.
        dbPredicates.add(matcher)
    }

    fun isNotNull(field: String) {
        // Full text search doesn't support feature 'isNotNull'.
        dbPredicates.add(DBPredicate.IsNotNull(field))
    }

    fun <O> isIn(field: String, vararg values: O) {
        addMatcher(DBPredicate.IsIn<O>(field, *values))
    }

    fun <O : Comparable<O>> between(field: String, from: O, to: O) {
        if (fullTextSearch && dbQueryBuilderByFullText.fieldSupported(field)) {
            dbQueryBuilderByFullText.between<O>(field, from, to)
        } else {
            addMatcher(DBPredicate.Between(field, from, to))
        }
    }

    fun <O : Comparable<O>> greater(field: String, from: O) {
        if (fullTextSearch && dbQueryBuilderByFullText.fieldSupported(field)) {
            dbQueryBuilderByFullText.greater<O>(field, from)
        } else {
            addMatcher(DBPredicate.Greater(field, from))
        }
    }

    fun <O : Comparable<O>> greaterEqual(field: String, from: O) {
        if (fullTextSearch && dbQueryBuilderByFullText.fieldSupported(field)) {
            dbQueryBuilderByFullText.greaterEqual<O>(field, from)
        } else {
            addMatcher(DBPredicate.GreaterEqual(field, from))
        }
    }

    fun <O : Comparable<O>> less(field: String, to: O) {
        if (fullTextSearch && dbQueryBuilderByFullText.fieldSupported(field)) {
            dbQueryBuilderByFullText.less<O>(field, to)
        } else {
            addMatcher(DBPredicate.Less(field, to))
        }
    }

    fun <O : Comparable<O>> lessEqual(field: String, to: O) {
        if (fullTextSearch && dbQueryBuilderByFullText.fieldSupported(field)) {
            dbQueryBuilderByFullText.lessEqual<O>(field, to)
        } else {
            addMatcher(DBPredicate.LessEqual(field, to))
        }
    }

    /**
     * Adds predicate to result matchers or, if criteria search is enabled, a new predicates for the criteria is appended.
     */
    internal fun addMatcher(predicate: DBPredicate) {
        if (criteriaSearchAvailable) {
            dbQueryBuilderByCriteria.add(predicate)
        } else if (predicate.fullTextSupport) {
        } else {
            dbPredicates.add(predicate)
        }
    }

    fun result(): DBResultIterator<O> {
        if (fullTextSearch) {
            return dbQueryBuilderByFullText.createResultIterator(dbPredicates)
        }
        return dbQueryBuilderByCriteria.createResultIterator()
    }

    /**
     * Not supported.
     */
    fun fulltextSearch(searchString: String) {
        if (fullTextSearch) {
            dbQueryBuilderByFullText.fulltextSearch(searchString)
        } else {
            throw UnsupportedOperationException("Internal error: FullTextQuery not available for string: " + searchString)
        }
    }

    fun close() {
        if (fullTextSearch) {
            dbQueryBuilderByFullText.close()
        }
    }

    /**
     * Sorting for criteria query is done by the data base, for full text search by Kotlin after getting the result list.
     */
    fun addOrder(sortBy: SortBy) {
        if (fullTextSearch) {
            dbQueryBuilderByFullText.addOrder(sortBy)
        } else {
            dbQueryBuilderByCriteria.addOrder(sortBy)
        }
    }
}
