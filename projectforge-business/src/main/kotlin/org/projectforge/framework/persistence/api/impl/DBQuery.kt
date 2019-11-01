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

import org.projectforge.business.multitenancy.TenantChecker
import org.projectforge.business.multitenancy.TenantService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DBQuery {
    private val log = LoggerFactory.getLogger(DBQuery::class.java)

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var tenantService: TenantService

    /**
     * Gets the list filtered by the given filter.
     *
     * @param filter
     * @return
     */
    @JvmOverloads
    fun <O : ExtendedBaseDO<Int>> getList(baseDao: BaseDao<O>,
                                          filter: QueryFilter,
                                          checkAccess: Boolean = true,
                                          ignoreTenant: Boolean = false)
            : List<O> {
        val begin = System.currentTimeMillis()
        baseDao.checkLoggedInUserSelectAccess()
        if (accessChecker.isRestrictedUser) {
            return listOf()
        }
        try {
            val dbFilter = filter.createDBFilter()
            val stats = dbFilter.createStatistics(baseDao)
            val mode = if (stats.fullTextRequired) {
                DBQueryBuilder.Mode.FULLTEXT
                //DBQueryBuilder.Mode.MULTI_FIELD_FULLTEXT_QUERY
            } else {
                DBQueryBuilder.Mode.CRITERIA // Criteria search (no full text search entries found).
            }

            val queryBuilder = DBQueryBuilder(baseDao, tenantService, mode,
                    // Check here mixing fulltext and criteria searches in comparison to full text searches and DBResultMatchers.
                    ignoreTenant = ignoreTenant)

            dbFilter.predicates.forEach {
                queryBuilder.addMatcher(it)
            }

            var maxOrder = 3
            for (sortProperty in filter.sortProperties) {
                var prop = sortProperty.property
                if (prop.indexOf('.') > 0)
                    prop = prop.substring(prop.indexOf('.') + 1)
                queryBuilder.addOrder(SortBy(prop, sortProperty.sortOrder == SortOrder.ASCENDING))
                if (--maxOrder <= 0)
                    break // Add only 3 orders.
            }
            // TODO setCacheRegion(baseDao, criteria)

            val dbResultIterator: DBResultIterator<O>
            dbResultIterator = queryBuilder.result()
            val historSearchParams = DBHistorySearchParams(filter.modifiedByUserId, filter.modifiedFrom, filter.modifiedTo, filter.searchHistory)
            var list = createList(baseDao, dbResultIterator, dbFilter, historSearchParams, checkAccess)
            queryBuilder.close()
            list = dbResultIterator.sort(list)

            val end = System.currentTimeMillis()
            if (end - begin > 2000) {
                // Show only slow requests.
                log.info(
                        "BaseDao.getList for entity class: " + baseDao.entityClass.simpleName + " took: " + (end - begin) + " ms (>2s).")
            }

            return list
        } catch (ex: Exception) {
            log.error("Error while querying: ${ex.message}. Magicfilter: ${filter}.", ex)
            return mutableListOf()
        }
    }

    private fun <O : ExtendedBaseDO<Int>> createList(baseDao: BaseDao<O>,
                                                     dbResultIterator: DBResultIterator<O>,
                                                     filter: DBFilter,
                                                     historSearchParams: DBHistorySearchParams,
                                                     checkAccess: Boolean)
            : List<O> {
        val superAdmin = TenantChecker.isSuperAdmin<ExtendedBaseDO<Int>>(ThreadLocalUserContext.getUser())
        val loggedInUser = ThreadLocalUserContext.getUser()

        val list = mutableListOf<O>()
        var next: O? = dbResultIterator.next() ?: return list
        val ensureUniqueSet = mutableSetOf<Int>()
        var resultCounter = 0
        if (historSearchParams.modifiedByUserId != null
                || historSearchParams.modifiedFrom != null
                || historSearchParams.modifiedTo != null
                || !historSearchParams.searchHistory.isNullOrBlank()) {
            // Search now all history entries which were modified by the given user and/or in the given time period.
            val idSet = if (historSearchParams.searchHistory.isNullOrBlank()) {
                DBHistoryQuery.searchHistoryEntryByCriteria(baseDao.session, baseDao.doClass, historSearchParams)
                //baseDao.getHistoryEntries(baseDao.session, baseSearchFilter) // No full text required.
            } else {
                DBHistoryQuery.searchHistoryEntryByFullTextQuery(baseDao.session, baseDao.doClass, historSearchParams)
                //baseDao.getHistoryEntriesFullTextSearch(baseDao.session, baseSearchFilter)
            }
            while (next != null) {
                if (!ensureUniqueSet.contains(next.id)) {
                    // Current result object wasn't yet proceeded.
                    ensureUniqueSet.add(next.id) // Mark current object as already proceeded (ensure uniqueness)
                    if ((!checkAccess || baseDao.hasSelectAccess(next, loggedInUser, superAdmin))
                            && baseDao.containsLong(idSet, next)
                            && match(filter, next)) {
                        // Current result object fits the modified query:
                        list.add(next)
                        if (++resultCounter >= filter.maxRows) {
                            break
                        }
                    }
                }
                next = dbResultIterator.next()
            }
        } else {
            // No modified query
            while (next != null) {
                if (!ensureUniqueSet.contains(next.id)) {
                    // Current result object wasn't yet proceeded.
                    ensureUniqueSet.add(next.id) // Mark current object as already proceeded (ensure uniqueness)
                    if (!checkAccess || baseDao.hasSelectAccess(next, loggedInUser, superAdmin) && match(filter, next)) {
                        list.add(next)
                        if (++resultCounter >= filter.maxRows) {
                            break
                        }
                    }
                }
                next = dbResultIterator.next()
            }
        }
        return list
    }

    /**
     * If predicates are definied (not used for data base query), they're checked with the given result object.
     * @return true If no predicates are given or if any predicate matches, otherwise false.
     */
    private fun match(filter: DBFilter, next: ExtendedBaseDO<Int>): Boolean {
        if (filter.predicates.isNullOrEmpty())
            return true
        for (predicate in filter.predicates) {
            if (predicate.match(next)) {
                return true
            }
        }
        return false
    }
}
