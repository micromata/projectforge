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
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.slf4j.LoggerFactory


internal class DBQueryBuilder<O : ExtendedBaseDO<Int>>(
        val baseDao: BaseDao<O>,
        tenantService: TenantService,
        val mode: Mode,
        /**
         * Not recommended, but it seems to work. Mixes fulltext search with criteria search if criteria search
         * is needed. If false, DBResultMatcher's may be used for filtering fields without index (while iterating
         * the result list).
         */
        val combinedCriteriaSearch: Boolean = false,
        /**
         * If given, these search fields are used. If not given, the search fields defined by the baseDao are used at default.
         */
        searchFields: Array<String>? = null,
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
        //FULLTEXT_PARSER, // Not yet implemented
        /**
         * Plain criteria search without full text search.
         */
        CRITERIA
    }
    private val log = LoggerFactory.getLogger(DBQueryBuilder::class.java)
    private var dbQueryBuilderByCriteria_: DBQueryBuilderByCriteria<O>? = null
    private val dbQueryBuilderByCriteria: DBQueryBuilderByCriteria<O>
        get() {
            if (dbQueryBuilderByCriteria_ == null) dbQueryBuilderByCriteria_ = DBQueryBuilderByCriteria<O>(baseDao,
                    useHibernateCriteria = (mode != Mode.CRITERIA && combinedCriteriaSearch))
            return dbQueryBuilderByCriteria_!!
        }
    private var dbQueryBuilderByFullText_: DBQueryBuilderByFullText<O>? = null
    private val dbQueryBuilderByFullText: DBQueryBuilderByFullText<O>
        get() {
            if (dbQueryBuilderByFullText_ == null) dbQueryBuilderByFullText_ = DBQueryBuilderByFullText<O>(baseDao)
            return dbQueryBuilderByFullText_!!
        }

    /**
     * As an alternative to the query builder of the full text search, Hibernate search supports a simple query string,
     * e. g. '+name:sch* +kassel...'
     */
    //private val multiFieldParserQueryString = mutableListOf<String>()
    /**
     * matchers for filtering result list. Used e. g. for searching fields without index if criteria search is not
     * configured.
     */
    private val dbResultMatchers = mutableListOf<DBResultMatcher>()

    private val criteriaSearchAvailable: Boolean
        get() = combinedCriteriaSearch || mode == Mode.CRITERIA

    init {
        if (!ignoreTenant && tenantService.isMultiTenancyAvailable) {
            val userContext = ThreadLocalUserContext.getUserContext()
            val currentTenant = userContext.currentTenant
            if (currentTenant != null) {
                if (currentTenant.isDefault) {
                    addMatcher(DBResultMatcher.Or(DBResultMatcher.Equals("tenant", userContext.currentTenant),
                            DBResultMatcher.IsNull("tenant")))
                } else {
                    addMatcher(DBResultMatcher.Equals("tenant", userContext.currentTenant))
                }
            }
        }
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
            dbResultMatchers.add(DBResultMatcher.Equals(field, value))
        }
    }

    fun ilike(field: String, value: String) {
        if (mode == Mode.FULLTEXT && dbQueryBuilderByFullText.fieldSupported(field)) {
            dbQueryBuilderByFullText.ilike(field, value)
        } else {
            addMatcher(DBResultMatcher.Like(field, value))
        }
    }

    /**
     * Adds matcher to result matchers or, if criteria search is enabled, a new predicates for the criteria is appended.
     */
    private fun addMatcher(matcher: DBResultMatcher) {
        if (criteriaSearchAvailable) {
            dbQueryBuilderByCriteria.add(matcher)
        } else {
            dbResultMatchers.add(matcher)
        }
    }

    fun result(): DBResultIterator<O> {
        if (mode == Mode.FULLTEXT) {
            if (combinedCriteriaSearch) {
                return dbQueryBuilderByFullText.createResultIterator(dbResultMatchers, criteria = dbQueryBuilderByCriteria.buildCriteria())
            }
            return dbQueryBuilderByFullText.createResultIterator(dbResultMatchers, null)
        }
        return dbQueryBuilderByCriteria.createResultIterator()
    }

    /**
     * Not supported.
     */
    fun fulltextSearch(searchString: String) {
        if (mode == Mode.FULLTEXT) {
            dbQueryBuilderByFullText.fulltextSearch(searchString)
        } else {
            throw UnsupportedOperationException("Internal error: FullTextQuery not available for string: " + searchString)
        }
    }

    fun close() {
        if (mode == Mode.FULLTEXT) {
            dbQueryBuilderByFullText.close()
        }
    }

    /**
     * Sorting is only implemented for criteria search (also if combined with full text search).
     */
    fun addOrder(sortBy: SortBy) {
        if (mode == Mode.FULLTEXT) {
            dbQueryBuilderByFullText.addOrder(sortBy)
        } else {
            dbQueryBuilderByCriteria.addOrder(sortBy)
        }
    }
}
