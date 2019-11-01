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
import org.projectforge.business.task.TaskDO
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.*
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

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
                                          filter: DBFilter,
                                          checkAccess: Boolean = true,
                                          ignoreTenant: Boolean = false)
            : List<O> {
        val begin = System.currentTimeMillis()
        baseDao.checkLoggedInUserSelectAccess()
        if (accessChecker.isRestrictedUser) {
            return ArrayList()
        }
        try {
            val criteriaSearchEntries = filter.criteriaSearchEntries
            val fullTextSearchEntries = filter.fulltextSearchEntries

            val mode = if (fullTextSearchEntries.isNullOrEmpty()) {
                DBQueryBuilder.Mode.CRITERIA // Criteria search (no full text search entries found).
            } else {
                DBQueryBuilder.Mode.FULLTEXT
                //DBQueryBuilder.Mode.MULTI_FIELD_FULLTEXT_QUERY
            }

            val queryBuilder = DBQueryBuilder<O>(baseDao, tenantService, mode,
                    // Check here mixing fulltext and criteria searches in comparison to full text searches and DBResultMatchers.
                    ignoreTenant = ignoreTenant)

            if (filter.deleted != null) {
                queryBuilder.equal("deleted", filter.deleted!!)
            }
            val historySearchParams = DBHistoryQuery.SearchParams()
            // First, proceed all criteria search entries:

            filter.allEntries.forEach {
                if (it.isHistoryEntry) {
                    if (it.field == MagicFilterEntry.HistorySearch.MODIFIED_BY_USER.fieldName) {
                        historySearchParams.modifiedByUserId = NumberHelper.parseInteger(it.value)
                    } else if (it.field == MagicFilterEntry.HistorySearch.MODIFIED_INTERVAL.fieldName) {
                        if (it.fromValueDate != null)
                            historySearchParams.modifiedFrom = it.fromValueDate
                        if (it.toValueDate != null)
                            historySearchParams.modifiedTo = it.toValueDate
                    } else if (it.field == MagicFilterEntry.HistorySearch.MODIFIED_HISTORY_VALUE.fieldName) {
                        historySearchParams.searchString = it.value
                    }
                }
            }

            val criteriaSearch = true// fullTextSearchEntries.isNullOrEmpty() // Test here other strategies...

            if (criteriaSearch && !criteriaSearchEntries.isNullOrEmpty()) {
                for (it in criteriaSearchEntries) {
                    if (it.field.isNullOrBlank())
                        continue // Use only field specific query (others are done by full text search
                    if (it.isHistoryEntry) {
                        // Not handled here...
                        continue
                    }
                    val fieldType = it.type
                    when (it.type) {
                        String::class.java -> {
                            when (it.searchType) {
                                SearchType.FIELD_STRING_SEARCH -> {
                                    queryBuilder.ilike(it.field!!, "${it.dbSearchString}")
                                }
                                SearchType.FIELD_RANGE_SEARCH -> {
                                    log.error("Unsupported searchType '${it.searchType}' for strings.")
                                }
                                SearchType.FIELD_VALUES_SEARCH -> {
                                    queryBuilder.anyOf(it.field!!, it.values)
                                }
                                else -> {
                                    log.error("Unsupported searchType '${it.searchType}' for strings.")
                                }
                            }
                        }
                        Date::class.java -> {
                            if (it.fromValueDate != null) {
                                if (it.toValueDate != null) {
                                    queryBuilder.between(it.field!!, it.fromValueDate!!.utilDate, it.toValueDate!!.utilDate)
                                } else {
                                    queryBuilder.greaterEqual(it.field!!, it.fromValueDate!!.utilDate)
                                }
                            } else if (it.toValueDate != null) {
                                queryBuilder.lessEqual(it.field!!, it.toValueDate!!.utilDate)
                            } else log.error("Error while building query: fromValue and/or toValue must be given for filtering field '${it.field}'.")
                        }
                        Integer::class.java -> {
                            if (it.valueInt != null) {
                                queryBuilder.equal(it.field!!, it.valueInt!!)
                            } else if (it.fromValueInt != null) {
                                if (it.toValueInt != null) {
                                    queryBuilder.between(it.field!!, it.fromValueInt!!, it.toValueInt!!)
                                } else {
                                    queryBuilder.greaterEqual(it.field!!, it.fromValueInt!!)
                                }
                            } else if (it.toValueInt != null) {
                                queryBuilder.lessEqual(it.field!!, it.toValueInt!!)
                            } else {
                                log.error("Querying field '${it.field}' of type '$fieldType' without value, fromValue and toValue. At least one required.")
                            }
                        }
                        TaskDO::class.java -> {
                            val node = TaskTreeHelper.getTaskTree().getTaskNodeById(it.valueInt)
                            if (node == null) {
                                log.warn("Can't query for given task id #${it.valueInt}, no such task node found.")
                            } else {
                                val recursive = true
                                if (recursive) {
                                    val taskIds = node.descendantIds
                                    taskIds.add(node.id)
                                    queryBuilder.anyOf(it.field!!, taskIds)
                                    if (log.isDebugEnabled) {
                                        log.debug("search in tasks: $taskIds")
                                    }
                                } else {
                                    queryBuilder.equal("task.id", it.valueInt!!)
                                }
                            }
                        }
                        else -> {
                            log.error("Querying fields of type '$fieldType' not yet implemented.")
                        }
                    }
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
            }

            val dbResultIterator: DBResultIterator<O>

            // Last, proceed all full text search entries:
            if (!fullTextSearchEntries.isNullOrEmpty()) {
                for (it in filter.fulltextSearchEntries) {
                    if (!it.value.isNullOrBlank()) {
                        queryBuilder.fulltextSearch(it.value!!)
                    }
                }
            }
            if (criteriaSearch) {
                filter.resultMatcher.forEach {
                    it.addTo(queryBuilder) // Add this to criteria search if possible.
                }
                filter.aliasList.forEach {
                    // TODO  queryBuilder.
                }
            } else {
                filter.resultMatcher.forEach {
                    queryBuilder.addMatcher(it) // Add this as result matcher, criteria not available.
                }
            }
            dbResultIterator = queryBuilder.result()
            var list = createList(baseDao, dbResultIterator, filter, historySearchParams, checkAccess)
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
                                                     filter: DBFilter, historySearchParams: DBHistoryQuery.SearchParams,
                                                     checkAccess: Boolean)
            : List<O> {
        val superAdmin = TenantChecker.isSuperAdmin<ExtendedBaseDO<Int>>(ThreadLocalUserContext.getUser())
        val loggedInUser = ThreadLocalUserContext.getUser()

        val list = mutableListOf<O>()
        var next: O? = dbResultIterator.next() ?: return list
        val ensureUniqueSet = mutableSetOf<Int>()
        var resultCounter = 0
        if (historySearchParams.modifiedByUserId != null
                || historySearchParams.modifiedFrom != null
                || historySearchParams.modifiedTo != null
                || !filter.searchHistory.isNullOrBlank()) {
            val baseSearchFilter = BaseSearchFilter()
            baseSearchFilter.modifiedByUserId = historySearchParams.modifiedByUserId
            baseSearchFilter.startTimeOfModification = historySearchParams.modifiedFrom?.utilDate
            baseSearchFilter.stopTimeOfModification = historySearchParams.modifiedTo?.utilDate
            baseSearchFilter.searchString = historySearchParams.searchString
            // Search now all history entries which were modified by the given user and/or in the given time period.
            val idSet = if (baseSearchFilter.searchString.isNullOrBlank()) {
                DBHistoryQuery.searchHistoryEntryByCriteria(baseDao.session, baseDao.doClass, historySearchParams)
                //baseDao.getHistoryEntries(baseDao.session, baseSearchFilter) // No full text required.
            } else {
                DBHistoryQuery.searchHistoryEntryByFullTextQuery(baseDao.session, baseDao.doClass, historySearchParams)
                //baseDao.getHistoryEntriesFullTextSearch(baseDao.session, baseSearchFilter)
            }
            while (next != null) {
                if (!ensureUniqueSet.contains(next.id)) {
                    // Current result object wasn't yet proceeded.
                    ensureUniqueSet.add(next.id) // Mark current object as already proceeded (ensure uniqueness)
                    if ((!checkAccess || baseDao.hasSelectAccess(next, loggedInUser, superAdmin))
                            && baseDao.containsLong(idSet, next)) {
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
                    if (!checkAccess || baseDao.hasSelectAccess(next, loggedInUser, superAdmin)) {
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
}
