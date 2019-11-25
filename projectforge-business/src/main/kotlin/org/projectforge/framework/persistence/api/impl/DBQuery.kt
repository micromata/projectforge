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
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Service
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
open class DBQuery {
    private val log = LoggerFactory.getLogger(DBQuery::class.java)

    @Autowired
    private lateinit var emgrFactory: PfEmgrFactory

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
    open fun <O : ExtendedBaseDO<Int>> getList(baseDao: BaseDao<O>,
                                               filter: QueryFilter,
                                               checkAccess: Boolean = true,
                                               ignoreTenant: Boolean = false)
            : List<O> {
        if (checkAccess) {
            baseDao.checkLoggedInUserSelectAccess()
        }
        if (checkAccess && accessChecker.isRestrictedUser) {
            return listOf()
        }
        try {
            val begin = System.currentTimeMillis()
            val dbFilter = filter.createDBFilter()
            return emgrFactory.runRoTrans { emgr ->
                val em = emgr.entityManager
                val queryBuilder = DBQueryBuilder(baseDao, em, tenantService, filter, dbFilter,
                        // Check here mixing fulltext and criteria searches in comparison to full text searches and DBResultMatchers.
                        ignoreTenant = ignoreTenant)

                val dbResultIterator: DBResultIterator<O>
                dbResultIterator = queryBuilder.result()
                val historSearchParams = DBHistorySearchParams(filter.modifiedByUserId, filter.modifiedFrom, filter.modifiedTo, filter.searchHistory)
                var list = createList(baseDao, em, dbResultIterator, queryBuilder.resultPredicates, dbFilter, historSearchParams, checkAccess)
                dbResultIterator.sort(list)

                val end = System.currentTimeMillis()
                if (end - begin > 2000) {
                    // Show only slow requests.
                    log.info(
                            "BaseDao.getList for entity class: " + baseDao.entityClass.simpleName + " took: " + (end - begin) + " ms (>2s).")
                }
                list
            }
        } catch (ex: Exception) {
            log.error("Error while querying: ${ex.message}. Magicfilter: ${filter}.", ex)
            return mutableListOf()
        }
    }

    private fun <O : ExtendedBaseDO<Int>> createList(baseDao: BaseDao<O>,
                                                     em: EntityManager,
                                                     dbResultIterator: DBResultIterator<O>,
                                                     resultPredicates: List<DBPredicate>,
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
                DBHistoryQuery.searchHistoryEntryByCriteria(em, baseDao.doClass, historSearchParams)
                //baseDao.getHistoryEntries(baseDao.entityManager, baseSearchFilter) // No full text required.
            } else {
                DBHistoryQuery.searchHistoryEntryByFullTextQuery(em, baseDao.doClass, historSearchParams)
                //baseDao.getHistoryEntriesFullTextSearch(baseDao.entityManager, baseSearchFilter)
            }
            while (next != null) {
                if (!ensureUniqueSet.contains(next.id)) {
                    // Current result object wasn't yet proceeded.
                    ensureUniqueSet.add(next.id) // Mark current object as already proceeded (ensure uniqueness)
                    if ((!checkAccess || baseDao.hasSelectAccess(next, loggedInUser, superAdmin))
                            && baseDao.containsLong(idSet, next)
                            && match(resultPredicates, next)) {
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
                    if (!checkAccess || baseDao.hasSelectAccess(next, loggedInUser, superAdmin) && match(resultPredicates, next)) {
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
     * @return true, if no predicates are given or if all predicate matches, otherwise false.
     */
    private fun match(predicates: List<DBPredicate>, next: ExtendedBaseDO<Int>): Boolean {
        if (predicates.isNullOrEmpty())
            return true
        for (predicate in predicates) {
            if (!predicate.match(next)) {
                return false
            }
        }
        return true
    }
}
