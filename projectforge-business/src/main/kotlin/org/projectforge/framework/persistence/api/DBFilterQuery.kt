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

package org.projectforge.framework.persistence.api

import org.hibernate.Criteria
import org.hibernate.ScrollMode
import org.hibernate.Session
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.hibernate.search.Search.getFullTextSession
import org.hibernate.search.query.dsl.QueryBuilder
import org.projectforge.business.multitenancy.TenantChecker
import org.projectforge.business.multitenancy.TenantService
import org.projectforge.business.task.TaskDO
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

/* TODO: Under construction. */
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
        var list: List<O>? = getList(baseDao, filter, checkAccess = true)
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
            val session = baseDao.session
            val clazz = baseDao.doClass
            val criteria = session.createCriteria(clazz)
            if (!ignoreTenant && tenantService.isMultiTenancyAvailable) {
                val userContext = ThreadLocalUserContext.getUserContext()
                val currentTenant = userContext.currentTenant
                if (currentTenant != null) {
                    if (currentTenant.isDefault == true) {
                        criteria.add(Restrictions.or(Restrictions.eq("tenant", userContext.currentTenant),
                                Restrictions.isNull("tenant")))
                    } else {
                        criteria.add(Restrictions.eq("tenant", userContext.currentTenant))
                    }
                }
            }
            if (filter.deleted != null) {
                criteria.add(Restrictions.eq("deleted", filter.deleted))
            }

            var modificationData = ModificationData()
            //var bc = BuildContext(baseDao.doClass, session)
            // First, proceed all criteria search entries:
            for (it in filter.criteriaSearchEntries) {
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
                                //bc.query
                                //        .simpleQueryString()
                                //        .onField("history")
                                //        .matching("storm")
                                criteria.add(Restrictions.ilike(it.field, "${it.dbSearchString}"))
                            }
                            SearchType.FIELD_RANGE_SEARCH -> {
                                log.error("Unsupported searchType '${it.searchType}' for strings.")
                            }
                            SearchType.FIELD_VALUES_SEARCH -> {
                                criteria.add(Restrictions.`in`(it.field, it.values))
                            }
                            else -> {
                                log.error("Unsupported searchType '${it.searchType}' for strings.")
                            }
                        }
                    }
                    Date::class.java -> {
                        if (it.fromValueDate != null) {
                            if (it.toValueDate != null) criteria.add(Restrictions.between(it.field, it.fromValueDate, it.toValueDate))
                            else criteria.add(Restrictions.ge(it.field, it.fromValueDate))
                        } else if (it.toValueDate != null) criteria.add(Restrictions.le(it.field, it.toValueDate))
                        else log.error("Error while building query: fromValue and/or toValue must be given for filtering field '${it.field}'.")
                    }
                    Integer::class.java -> {
                        if (it.valueInt != null) {
                            criteria.add(Restrictions.eq(it.field, it.valueInt))
                        } else if (it.fromValueInt != null) {
                            if (it.toValueInt != null) {
                                criteria.add(Restrictions.between(it.field, it.fromValue, it.toValue))
                            } else {
                                criteria.add(Restrictions.ge(it.field, it.fromValue))
                            }
                        } else if (it.toValueInt != null) {
                            criteria.add(Restrictions.le(it.field, it.toValue))
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
                                criteria.add(Restrictions.`in`("task.id", taskIds))
                                if (log.isDebugEnabled) {
                                    log.debug("search in tasks: $taskIds")
                                }
                            } else {
                                criteria.add(Restrictions.eq("task.id", it.valueInt))
                            }
                        }
                    }
                    else -> {
                        log.error("Querying fields of type '$fieldType' not yet implemented.")
                    }
                }
            }

            var maxOrder = 3;
            for (sortProperty in filter.sortProperties) {
                var prop = sortProperty.property
                if (prop.indexOf('.') > 0)
                    prop = prop.substring(prop.indexOf('.') + 1)
                val order = if (sortProperty.sortOrder == SortOrder.ASCENDING) Order.asc(prop)
                else Order.desc(prop)
                criteria.addOrder(order)
                if (--maxOrder <= 0)
                    break // Add only 3 orders.
            }
            setCacheRegion(baseDao, criteria)
            var list = createList(baseDao, criteria, filter, modificationData, checkAccess)

            // Last, proceed all full text search entries:
            val fullTextSearchEntries = filter.fulltextSearchEntries
            if (!fullTextSearchEntries.isNullOrEmpty()) {
                for (it in filter.fulltextSearchEntries) {
                }
            }
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

    private fun setCacheRegion(baseDao: BaseDao<*>, criteria: Criteria) {
        criteria.setCacheable(true)
        if (!baseDao.useOwnCriteriaCacheRegion()) {
            return
        }
        criteria.setCacheRegion(baseDao.javaClass.name)
    }

    private fun <O : ExtendedBaseDO<Int>> createList(baseDao: BaseDao<O>, criteria: Criteria, filter: DBFilter, modificationData: ModificationData,
                                                     checkAccess: Boolean)
            : List<O> {
        val superAdmin = TenantChecker.isSuperAdmin<ExtendedBaseDO<Int>>(ThreadLocalUserContext.getUser())
        val loggedInUser = ThreadLocalUserContext.getUser()

        val scrollableResults = criteria.scroll(ScrollMode.FORWARD_ONLY)
        val list = mutableListOf<O>()
        var hasNext = scrollableResults.next()
        if (!hasNext) return list
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
            while (hasNext) {
                val obj = scrollableResults.get(0) as O
                if (!ensureUniqueSet.contains(obj.id)) {
                    // Current result object wasn't yet proceeded.
                    ensureUniqueSet.add(obj.id) // Mark current object as already proceeded (ensure uniqueness)
                    if ((!checkAccess || baseDao.hasSelectAccess(obj, loggedInUser, superAdmin))
                            && baseDao.contains(idSet, obj)) {
                        // Current result object fits the modified query:
                        list.add(obj)
                        if (++resultCounter >= filter.maxRows) {
                            break
                        }
                    }
                }
                hasNext = scrollableResults.next()
            }
            log.error("History search not yet implemented.")
        } else {
            // No modified query
            while (hasNext) {
                val obj = scrollableResults.get(0) as O
                if (!ensureUniqueSet.contains(obj.id)) {
                    // Current result object wasn't yet proceeded.
                    ensureUniqueSet.add(obj.id) // Mark current object as already proceeded (ensure uniqueness)
                    if (!checkAccess || baseDao.hasSelectAccess(obj, loggedInUser, superAdmin)) {
                        list.add(obj)
                        if (++resultCounter >= filter.maxRows) {
                            break
                        }
                    }
                }
                hasNext = scrollableResults.next()
            }
        }
        return list
    }
}
