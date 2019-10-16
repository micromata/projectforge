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

import org.hibernate.Session
import org.hibernate.search.Search.getFullTextSession
import org.hibernate.search.query.dsl.QueryBuilder
import org.projectforge.business.multitenancy.TenantChecker
import org.projectforge.business.multitenancy.TenantService
import org.projectforge.business.task.TaskDO
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class DBFilterQuery {
    private class BuildContext(val doClass: Class<*>,
                               val session: Session) {
        var _query: QueryBuilder? = null
        val query: QueryBuilder
            get() {
                if (_query == null) {
                    val fullTextSession = getFullTextSession(session)
                    _query = fullTextSession.searchFactory
                            .buildQueryBuilder().forEntity(doClass).get()
                }
                return _query!!
            }
        val fullText
            get() = _query != null
    }

    private class ModificationData(var queryModifiedByUserId: Int? = null,
                                   var queryModifiedFromDate: PFDateTime? = null,
                                   var queryModifiedToDate: PFDateTime? = null)

    private val log = LoggerFactory.getLogger(DBFilterQuery::class.java)

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
    @Throws(AccessException::class)
    fun <O : ExtendedBaseDO<Int>> getList(baseDao: BaseDao<O>, filter: DBFilter): List<O>? {
        val begin = System.currentTimeMillis()
        baseDao.checkLoggedInUserSelectAccess()
        if (accessChecker.isRestrictedUser == true) {
            return ArrayList()
        }
        val list: List<O>? = getList(baseDao, filter, checkAccess = true)
        val end = System.currentTimeMillis()
        if (end - begin > 2000) {
            // Show only slow requests.
            log.info(
                    "BaseDao.getList for entity class: " + baseDao.entityClass.simpleName + " took: " + (end - begin) + " ms (>2s).")
        }
        return list
    }

    /**
     * Gets the list filtered by the given filter.
     *
     * @param filter
     * @return
     */
    fun <O : ExtendedBaseDO<Int>> getList(baseDao: BaseDao<O>,
                                          filter: DBFilter,
                                          checkAccess: Boolean = true,
                                          ignoreTenant: Boolean = false)
            : List<O> {
        try {
            val criteriaSearchEntries = filter.criteriaSearchEntries
            val fullTextSearchEntries = filter.fulltextSearchEntries

            val mode = if (fullTextSearchEntries.isNullOrEmpty()) {
                DBQueryBuilder.Mode.CRITERIA // Criteria search (no full text search entries found).
            } else {
                DBQueryBuilder.Mode.FULLTEXT
            }

            val queryBuilder = DBQueryBuilder<O>(baseDao, tenantService, mode,
                    // Check here mixing fulltext and criteria searches in comparison to full text searches and DBResultMatchers.
                    combinedCriteriaSearch = false, // false is recommended by Hibernate, but true works for now...
                    ignoreTenant = ignoreTenant)

            if (filter.deleted != null) {
                queryBuilder.equal("deleted", filter.deleted!!)
            }
            val modificationData = ModificationData()
            // First, proceed all criteria search entries:

            val criteriaSearch = true// fullTextSearchEntries.isNullOrEmpty() // Test here other strategies...

            if (criteriaSearch && !criteriaSearchEntries.isNullOrEmpty()) {
                for (it in criteriaSearchEntries) {
                    if (it.field.isNullOrBlank())
                        continue // Use only field specific query (others are done by full text search
                    if (it.field == "modifiedBy") {
                        modificationData.queryModifiedByUserId = NumberHelper.parseInteger(it.value)
                        // TODO
                        log.warn("TODO: Implement modifiedBy filter setting.")
                        continue
                    }
                    if (it.field == "modifiedInterval") {
                        if (it.fromValue != null)
                            modificationData.queryModifiedFromDate = it.fromValueDate
                        if (it.toValue != null)
                            modificationData.queryModifiedToDate = it.toValueDate
                        // TODO
                        log.warn("TODO: Implement modifiedInterval filter setting.")
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
                                    //TODO criteria.add(Restrictions.`in`(it.field, it.values))
                                }
                                else -> {
                                    log.error("Unsupported searchType '${it.searchType}' for strings.")
                                }
                            }
                        }
                        Date::class.java -> {
                            if (it.fromValueDate != null) {
                                //TODO if (it.toValueDate != null) criteria.add(Restrictions.between(it.field, it.fromValueDate, it.toValueDate))
                                //TODO else criteria.add(Restrictions.ge(it.field, it.fromValueDate))
                            } else if (it.toValueDate != null) {
                                // TODO criteria.add(Restrictions.le(it.field, it.toValueDate))
                            } else log.error("Error while building query: fromValue and/or toValue must be given for filtering field '${it.field}'.")
                        }
                        Integer::class.java -> {
                            if (it.valueInt != null) {
                                queryBuilder.equal(it.field!!, it.valueInt!!)
                            } else if (it.fromValueInt != null) {
                                if (it.toValueInt != null) {
                                    // TODO criteria.add(Restrictions.between(it.field, it.fromValue, it.toValue))
                                } else {
                                    // TODO criteria.add(Restrictions.ge(it.field, it.fromValue))
                                }
                            } else if (it.toValueInt != null) {
                                // TODO criteria.add(Restrictions.le(it.field, it.toValue))
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
                                    // TODO criteria.add(Restrictions.`in`("task.id", taskIds))
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
            dbResultIterator = queryBuilder.result()
            var list = createList(baseDao, dbResultIterator, filter, modificationData, checkAccess)
            queryBuilder.close()
            list = dbResultIterator.sort(list)
/*
        try {
            val fullTextSession = Search.getFullTextSession(session)

            val query = createFullTextQuery(fullTextSession, HISTORY_SEARCH_FIELDS, null,
                    searchString, PfHistoryMasterDO::class.java)
                    ?: // An error occured:
                    return
            val fullTextQuery = fullTextSession.createFullTextQuery(query, PfHistoryMasterDO::class.java)
            fullTextQuery.isCacheable = true
            fullTextQuery.cacheRegion = "historyItemCache"
            fullTextQuery.setProjection("entityId")
            val result = fullTextQuery.list()
            if (result != null && result.size > 0) {
                for (oa in result) {
                    idSet.add(oa[0] as Int)
                }
            }
        } catch (ex: Exception) {
            val errorMsg = ("Lucene error message: "
                    + ex.message
                    + " (for "
                    + clazz.simpleName
                    + ": "
                    + searchString
                    + ").")
            filter.setErrorMessage(errorMsg)
            LOG.error(errorMsg)
        }
*/
            return list
        } catch (ex: Exception) {
            log.error("Error while querying: ${ex.message}. Magicfilter: ${filter}.", ex)
            return mutableListOf()
        }
    }

    private fun <O : ExtendedBaseDO<Int>> createList(baseDao: BaseDao<O>, dbResultIterator: DBResultIterator<O>, filter: DBFilter, modificationData: ModificationData,
                                                     checkAccess: Boolean)
            : List<O> {
        val superAdmin = TenantChecker.isSuperAdmin<ExtendedBaseDO<Int>>(ThreadLocalUserContext.getUser())
        val loggedInUser = ThreadLocalUserContext.getUser()

        val list = mutableListOf<O>()
        var next: O? = dbResultIterator.next() ?: return list
        val ensureUniqueSet = mutableSetOf<Int>()
        var resultCounter = 0
        if (modificationData.queryModifiedByUserId != null
                || modificationData.queryModifiedFromDate != null
                || modificationData.queryModifiedToDate != null
                || !filter.searchHistory.isNullOrBlank()) {
            val baseSearchFilter = BaseSearchFilter()
            baseSearchFilter.modifiedByUserId = modificationData.queryModifiedByUserId
            baseSearchFilter.startTimeOfModification = modificationData.queryModifiedFromDate?.utilDate
            baseSearchFilter.stopTimeOfModification = modificationData.queryModifiedToDate?.utilDate
            // Search now all history entries which were modified by the given user and/or in the given time period.
            val idSet = baseDao.getHistoryEntries(baseDao.session, baseSearchFilter)
            while (next != null) {
                if (!ensureUniqueSet.contains(next.id)) {
                    // Current result object wasn't yet proceeded.
                    ensureUniqueSet.add(next.id) // Mark current object as already proceeded (ensure uniqueness)
                    if ((!checkAccess || baseDao.hasSelectAccess(next, loggedInUser, superAdmin))
                            && baseDao.contains(idSet, next)) {
                        // Current result object fits the modified query:
                        list.add(next)
                        if (++resultCounter >= filter.maxRows) {
                            break
                        }
                    }
                }
                next = dbResultIterator.next()
            }
            log.error("History search not yet implemented.")
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
