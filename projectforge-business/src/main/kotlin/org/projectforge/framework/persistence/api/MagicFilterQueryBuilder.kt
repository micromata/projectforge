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

import org.apache.xalan.xsltc.runtime.CallFunction.clazz
import org.hibernate.Criteria
import org.hibernate.criterion.Restrictions
import org.projectforge.business.multitenancy.TenantService
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
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
        val query = MagicFilterQuery(filter, baseDao.doClass)
        query.errorMessage = null

        if (filter.deleted != null) {
            query.add(Restrictions.eq("deleted", filter.deleted))
        }
        if (!ignoreTenant && tenantService.isMultiTenancyAvailable) {
            val userContext = ThreadLocalUserContext.getUserContext()
            val currentTenant = userContext.currentTenant
            if (currentTenant != null) {
                if (currentTenant.isDefault == true) {
                    query.add(Restrictions.or(Restrictions.eq("tenant", userContext.currentTenant),
                            Restrictions.isNull("tenant")))
                } else {
                    query.add(Restrictions.eq("tenant", userContext.currentTenant))
                }
            }
        }

        filter.entries.forEach {
            if (!it.field.isNullOrBlank()) {
                val fieldType = PropUtils.getField(clazz, it.field)?.type ?: String::class.java
                when (fieldType) {
                    String::class.java -> {
                        // Will be handled throug fulltextSearch
                        if (it.value != null)
                            query.add(Restrictions.eq(it.field, it.value))
                        else if (!it.values.isNullOrEmpty())
                            query.add(Restrictions.`in`(it.field, it.values))
                        else {
                            query.add(Restrictions.ilike(it.field, "${it.search}%"))
                        }
                    }
                    Date::class.java -> {
                        if (it.fromValue != null) {
                            if (it.toValue != null) query.add(Restrictions.between(it.field, it.fromValue, it.toValue))
                            else query.add(Restrictions.ge(it.field, it.fromValue))
                        } else if (it.toValue != null) query.add(Restrictions.le(it.field, it.toValue))
                        else log.error("Error while building query: fromValue and/or toValue must be given for filtering field '${it.field}'.")
                    }
                    else -> {
                        log.error("Querying fields of type '$fieldType' not yet implemented.")
                    }
                }
            } else {
                if (!it.search.isNullOrBlank()) {

                }
            }
        }
        return mutableListOf()
    }

    private fun setCacheRegion(baseDao: BaseDao<*>, criteria: Criteria) {
        criteria.setCacheable(true)
        if (baseDao.useOwnCriteriaCacheRegion() == false) {
            return
        }
        criteria.setCacheRegion(baseDao.javaClass.name)
    }
}
