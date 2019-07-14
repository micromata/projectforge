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
import org.hibernate.criterion.Restrictions
import org.projectforge.business.multitenancy.TenantService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*


@Service
class MagicFilterQueryBuilder {

    private val log = LoggerFactory.getLogger(MagicFilterQueryBuilder::class.java)

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
    fun <O : ExtendedBaseDO<Int>> getList(baseDao: BaseDao<O>, filter: MagicFilter): List<O>? {
        val begin = System.currentTimeMillis()
        baseDao.checkLoggedInUserSelectAccess()
        if (accessChecker.isRestrictedUser == true) {
            return ArrayList()
        }
        var list: List<O>? = internalGetList(baseDao, filter)
        if (list == null || list.size == 0) {
            return list
        }
        list = baseDao.extractEntriesWithSelectAccess(list)
        val result = baseDao.sort(list)
        val end = System.currentTimeMillis()
        if (end - begin > 2000) {
            // Show only slow requests.
            log.info(
                    "BaseDao.getList for entity class: " + baseDao.entityClass.simpleName + " took: " + (end - begin) + " ms (>2s).")
        }
        return result
    }

    /**
     * Gets the list filtered by the given filter.
     *
     * @param filter
     * @return
     */
    @Throws(AccessException::class)
    fun <O : ExtendedBaseDO<Int>> internalGetList(baseDao: BaseDao<O>, filter: MagicFilter, ignoreTenant: Boolean = false): List<O> {
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

        var queryModifiedByUserId: Int? = null
        var queryModifiedFromDate: PFDateTime? = null
        var queryModifiedToDate: PFDateTime? = null
        for (it in filter.entries) {
            if (it.field.isNullOrBlank())
                continue // Use only field specific query (others are done by full text search
            if (it.field == "modifiedBy") {
                queryModifiedByUserId = NumberHelper.parseInteger(it.value)
                continue
            }
            if (it.field == "modifiedInterval") {
                if (it.fromValue != null)
                    queryModifiedFromDate = it.fromValueDate
                if (it.toValue != null)
                    queryModifiedToDate = it.toValueDate
                continue
            }
            val fieldType = it.type
            when (fieldType) {
                String::class.java -> {
                    when (it.searchType) {
                        MagicFilterEntry.SearchType.FIELD_STRING_SEARCH -> {
                            criteria.add(Restrictions.ilike(it.field, "${it.dbSearchString}"))
                        }
                        MagicFilterEntry.SearchType.FIELD_RANGE_SEARCH -> {
                            log.error("Unsupported searchType '${it.searchType}' for strings.")
                        }
                        MagicFilterEntry.SearchType.FIELD_VALUES_SEARCH -> {
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
                else -> {
                    log.error("Querying fields of type '$fieldType' not yet implemented.")
                }
            }
        }
        setCacheRegion(baseDao, criteria)
        @Suppress("UNCHECKED_CAST")
        var list = criteria.list() as List<O>

        list = baseDao.selectUnique(list)
        if (list.size > 0 && (queryModifiedByUserId != null || queryModifiedFromDate != null || queryModifiedToDate != null
                        || !filter.searchHistory.isNullOrBlank())) {
            val baseSearchFilter = BaseSearchFilter()
            baseSearchFilter.modifiedByUserId = queryModifiedByUserId
            baseSearchFilter.startTimeOfModification = queryModifiedFromDate?.utilDate
            baseSearchFilter.stopTimeOfModification = queryModifiedToDate?.utilDate
            // Search now all history entries which were modified by the given user and/or in the given time period.
            val idSet = baseDao.getHistoryEntries(baseDao.session, baseSearchFilter)
            val result = ArrayList<O>()
            for (entry in list) {
                if (baseDao.contains(idSet, entry)) {
                    result.add(entry)
                }
            }
            list = result
            log.error("Histor search not yet implemented.")
/*
            if (CollectionUtils.isNotEmpty(idSet) == true) {
                for (entry in list) {
                    if (idSet!!.contains(entry.getId()) == true) {
                        idSet!!.remove(entry.getId()) // Object does already exist in list.
                    }
                }
                if (idSet!!.isEmpty() == false) {
                    val criteria = filter.buildCriteria(baseDao.getSession(), baseDao.clazz)
                    setCacheRegion(baseDao, criteria)
                    criteria.add(Restrictions.`in`("id", idSet!!))
                    val historyMatchingEntities = criteria.list()
                    list.addAll(historyMatchingEntities)
                }
            }*/
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
    }

    private fun setCacheRegion(baseDao: BaseDao<*>, criteria: Criteria) {
        criteria.setCacheable(true)
        if (!baseDao.useOwnCriteriaCacheRegion()) {
            return
        }
        criteria.setCacheRegion(baseDao.javaClass.name)
    }
}
