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
import mu.KotlinLogging
import org.projectforge.common.logging.LogUtils.logDebugFunCall
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty

private val log = KotlinLogging.logger {}

class DBQueryBuilder<O : ExtendedBaseDO<Long>>(
    private val baseDao: BaseDao<O>,
    private val entityManager: EntityManager,
    private val queryFilter: QueryFilter,
    dbFilter: DBFilter
) {

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

    private val dbQueryBuilderByCriteria: DBQueryBuilderByCriteria<O> by lazy {
        DBQueryBuilderByCriteria(baseDao, entityManager, queryFilter)
    }
    private val dbQueryBuilderByFullText: DBQueryBuilderByFullText<O> by lazy {
        DBQueryBuilderByFullText(
            baseDao,
            entityManager,
            useMultiFieldQueryParser = mode == Mode.MULTI_FIELD_FULLTEXT_QUERY
        )
    }
    private val mode: Mode
    private val criteriaPredicates = mutableListOf<DBPredicate>()
    private val fullTextPredicates = mutableListOf<DBPredicate>()

    /**
     * As an alternative to the query builder of the full text search, Hibernate search supports a simple query string,
     * e.g. '+name:sch* +kassel...'
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
        logDebugFunCall(log) { it.mtd("init") }
        mode = if (dbFilter.allPredicates.any { !it.criteriaSupport && !it.resultSetSupport }) {
            logDebugFunCall(log) { it.mtd("init").msg("fullTextSearch required") }
            Mode.FULLTEXT
        } else {
            logDebugFunCall(log) { it.mtd("init").msg("criteriaSearchAvailable") }
            Mode.CRITERIA
        }
        if (mode == Mode.FULLTEXT) {
            dbFilter.allPredicates.forEach { p ->
                if (p.supportedByFullTextQuery(dbQueryBuilderByFullText.searchClassInfo)) {
                    fullTextPredicates.add(p)
                    //} else if (p.criteriaSupport) { // Later add criteria search with result id's of full text search.
                    //    dbQueryBuilderByCriteria.add(p) // Not yet migrated to use criteriaPredicates.
                    //    criteriaPredicates.add(p)
                } else if (!p.resultSetSupport) {
                    log.error { "*** FullTextSearch: Predicate ${p.javaClass.simpleName} for ${baseDao.doClass.simpleName}.${p.field} neither support fullText search nor resultSet. Ignoring." }
                } else {
                    resultPredicates.add(p)
                }
            }
        } else {
            dbFilter.allPredicates.forEach { p ->
                if (p.criteriaSupport) {
                    criteriaPredicates.add(p)
                    dbQueryBuilderByCriteria.add(p) // Not yet migrated to use criteriaPredicates.
                } else if (!p.resultSetSupport) {
                    log.error { "*** CriteriaSearch: Predicate ${p.javaClass.simpleName} for ${baseDao.doClass.simpleName}.${p.field} neither support criteria search nor resultSet. Ignoring." }
                } else {
                    resultPredicates.add(p)
                }
            }
        }

        var maxOrder = 3
        for (sortProperty in dbFilter.sortProperties) {
            addOrder(sortProperty)
            if (--maxOrder <= 0)
                break // Add only 3 orders.
        }
        // TODO setCacheRegion(baseDao, criteria)

    }

    fun result(): DBResultIterator<O> {
        if (fullTextSearch && fullTextPredicates.isNotEmpty()) {
            logDebugFunCall(log) { it.mtd("result()").msg("fullTextSearch") }
            return dbQueryBuilderByFullText.createResultIterator(fullTextPredicates, resultPredicates)
        }
        logDebugFunCall(log) { it.mtd("result()").msg("criteriaSearch") }
        return dbQueryBuilderByCriteria.createResultIterator(resultPredicates, queryFilter)
    }

    /**
     * Sorting for criteria query is done by the database, for full text search by Kotlin after getting the result list.
     */
    fun addOrder(sortProperty: SortProperty) {
        if (fullTextSearch) {
            logDebugFunCall(log) {
                it.mtd("addOrder(sortProperty)").msg("fullTextSearch").params("sortProperty" to sortProperty)
            }
            dbQueryBuilderByFullText.addOrder(sortProperty)
        } else {
            logDebugFunCall(log) {
                it.mtd("addOrder(sortProperty)").msg("criteriaSearch").params("sortProperty" to sortProperty)
            }
            dbQueryBuilderByCriteria.addOrder(sortProperty)
        }
    }
}
